package vote.synchronizer;

import model.Party;
import model.Person;
import model.Vote;
import model.Voting;
import vote.fetcher.VoteStorage;
import vote.fetcher.VoteStream;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class InitialVoteSaver {
    private static final int BATCH_SIZE = 1000;
    private static final int SINGLE_QUERY_SIZE = 250;
    private final VoteStorage storage;
    private ExecutorService mainTasksService;
    private ExecutorService voteDependencesService;
    private ExecutorService votesService;
    
    public InitialVoteSaver(VoteStorage storage) {
        this.storage = storage;
    }
    
    public void save(VoteStream votes) {
        System.out.println("Started processing votes.");
        var start = LocalTime.now();
        mainTasksService = Executors.newFixedThreadPool(2);
        voteDependencesService = Executors.newFixedThreadPool(16);
        votesService = Executors.newFixedThreadPool(32);
        BlockingQueue<Batch> batchQueue = new LinkedBlockingQueue<>();
        var voteCollector = new VoteCollector(BATCH_SIZE, votes, batchQueue);
        var voteSaver = new VoteSaver(batchQueue);
    
        List<Future<?>> tasks = List.of(
            mainTasksService.submit(voteCollector::startCollecting),
            mainTasksService.submit(voteSaver::startSaving)
        );
        System.out.println("Waiting for background processes to finish.");
        waitFor(tasks);
        System.out.println("Finished processing votes.");
        mainTasksService.shutdown();
        voteDependencesService.shutdown();
        votesService.shutdown();
        var end = LocalTime.now();
        var duration = Duration.between(start,end);
        System.out.println("Finished processing votes in " + duration);
    }
    
    private void waitFor(List<Future<?>> tasks) {
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private class VoteCollector {
        private final Map<String, Party> parties = new HashMap<>(storage.getParties());
        private final Map<String, Voting> votings = new HashMap<>(storage.getVotings());
        private final Map<String, Person> people = new HashMap<>(storage.getPeolpe());
        
        private final int batchSize;
        private final Set<Party> batchedParties = new HashSet<>(20);
        private final Set<Voting> batchedVotings = new HashSet<>(20);
        private final Set<Person> batchedPeople = new HashSet<>(500);
        private final Set<Vote> batchedVotes;
        
        private final VoteStream votes;
        private final BlockingQueue<Batch> queue;
    
        public VoteCollector(int batchSize, VoteStream votes, BlockingQueue<Batch> queue) {
            this.batchSize = batchSize;
            this.votes = votes;
            this.queue = queue;
            batchedVotes = new HashSet<>(batchSize);
        }
        
        public void startCollecting() {
            System.out.println("Started collecting batches");
            Optional<Vote> next;
            while( (next=votes.next()).isPresent() ) {
                add( next.get() );
                if(limitReached()) {
                    pushNextBatch( false );
                }
            }
            pushNextBatch( true );
            System.out.println("finished saving batches");
        }
    
        private void pushNextBatch(boolean finalBatch) {
            System.out.println("Pushing batch of " + batchedVotes.size() + " votes for saving");
            queue.add(new Batch(
                batchedParties,
                batchedVotings,
                batchedPeople,
                batchedVotes,
                finalBatch
            ));
            batchedParties.clear();
            batchedVotings.clear();
            batchedPeople.clear();
            batchedVotes.clear();
        }
    
        private boolean limitReached() {
            return batchedVotes.size() >= batchSize;
        }
    
        private void add(Vote vote) {
            addParty(vote.getParty());
            addVoting(vote.getVoting());
            addPerson(vote.getPerson());
            batchedVotes.add(vote);
        }
    
        private void addPerson(Person person) {
            if(!people.containsKey(person.getName())) {
                batchedPeople.add(person);
                people.put(person.getName(),person);
            }
        }
    
        private void addVoting(Voting voting) {
            if(!votings.containsKey(voting.getName())) {
                batchedVotings.add(voting);
                votings.put(voting.getName(),voting);
            }
        }
    
        private void addParty(Party party) {
            if(!parties.containsKey(party.getName())) {
                batchedParties.add(party);
                parties.put(party.getName(),party);
            }
        }
    }
    private class VoteSaver {
        private final BlockingQueue<Batch> queue;
    
        public VoteSaver(BlockingQueue<Batch> queue) {
            this.queue = queue;
        }
    
        public void startSaving() {
            boolean shouldContinue = true;
            System.out.println("Started saving batches");
            List<Future<?>> dependencyTasks = new ArrayList<>();
            List<Future<?>> votesTasks = new ArrayList<>();
            while( shouldContinue ) {
                try {
                    Batch batch = queue.take();
    
                    dependencyTasks.addAll(processParties(batch.batchedParties) );
                    dependencyTasks.addAll(processPeople(batch.batchedPeople) );
                    dependencyTasks.addAll(processVotings(batch.batchedVotings) );
                    
                    var currentDependencies = new ArrayList<>(dependencyTasks);
                    Future<?> votesTask = votesService.submit( () -> {
                        waitFor(currentDependencies);
                        if( !batch.batchedVotes.isEmpty() ) {
                            waitFor(process(batch.batchedVotes,storage::saveVotesUnsafe, "votes", votesService));
                        }
                    });
                    votesTasks.add(votesTask);
    
                    shouldContinue = !batch.finalBatch;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Waiting for remaining batches");
            waitFor(dependencyTasks);
            waitFor(votesTasks);
            System.out.println("Finished saving batches");
        }
    
        private List<Future<?>> processVotings(Set<Voting> votings) {
            return process(votings, this::saveVotings, "votings", voteDependencesService);
        }
    
        private void saveVotings(Collection<Voting> votings) {
            System.out.println("Saving to DB batch of " + votings.size() + " votings");
            storage.saveVotings(votings);
            System.out.println("Saved to DB batch of " + votings.size() + " votings");
        }
    
        private List<Future<?>> processPeople(Set<Person> people) {
            return process(people, this::savePeople, "people", voteDependencesService);
        }
    
        private void savePeople(Collection<Person> people) {
            System.out.println("Saving to DB batch of " + people.size() + " people");
            storage.savePeople(people);
            System.out.println("Saved to DB batch of " + people.size() + " people");
        }
    
        private List<Future<?>> processParties(Set<Party> parties) {
            return process(parties, this::saveParties, "parties", voteDependencesService);
        }
    
        private void saveParties(Collection<Party> parties) {
            System.out.println("Saving to DB batch of " + parties.size() + " parties");
            storage.saveParties(parties);
            System.out.println("Saved to DB batch of " + parties.size() + " parties");
        }
    
        private <T> List<Future<?>> process(Set<T> collection, Consumer<Collection<T>> saver, String type, ExecutorService service) {
            List<Future<?>> tasks = new ArrayList<>();
            if (!collection.isEmpty()) {
                System.out.println("Scheduling batch of " + collection.size() + " " + type + " for saving");
                ArrayList<T> list = new ArrayList<>(collection);
                for (int index = 0; index < list.size(); index += SINGLE_QUERY_SIZE) {
                    List<T> subList = list.subList(
                        index,
                        Math.min(index + SINGLE_QUERY_SIZE, list.size())
                    );
                    tasks.add(service.submit(() -> saver.accept(subList)));
                }
            }
            return tasks;
        }
    }
    
    private static class Batch {
        private final Set<Party> batchedParties;
        private final Set<Voting> batchedVotings;
        private final Set<Person> batchedPeople;
        private final Set<Vote> batchedVotes;
        private final boolean finalBatch;
        
        public Batch(
            Set<Party> batchedParties,
            Set<Voting> batchedVotings,
            Set<Person> batchedPeople,
            Set<Vote> batchedVotes,
            boolean finalBatch
        ) {
            this.batchedParties = new HashSet<>(batchedParties);
            this.batchedVotings = new HashSet<>(batchedVotings);
            this.batchedPeople = new HashSet<>(batchedPeople);
            this.batchedVotes = new HashSet<>(batchedVotes);
            this.finalBatch = finalBatch;
        }
    }
}
