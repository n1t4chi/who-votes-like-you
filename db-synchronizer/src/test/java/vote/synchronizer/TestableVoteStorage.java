package vote.synchronizer;

import model.Party;
import model.Person;
import model.Vote;
import model.Voting;
import vote.fetcher.VoteStorage;

import java.util.*;
import java.util.stream.Collectors;

public class TestableVoteStorage implements VoteStorage {
    private final Map<String,Voting> votings = new HashMap<>();
    private final Map<String,Person> people = new HashMap<>();
    private final Map<String,Party> parties = new HashMap<>();
    private final Set<Vote> votes = new HashSet<>();
    
    public Set<Vote> getVotes() {
        return votes;
    }
    
    @Override
    public Collection<Vote> getVotesFor(Voting voting) {
        return votes.stream()
            .filter(vote -> vote.getVoting().equals(voting))
            .collect(Collectors.toList());
    }
    
    @Override
    public Voting getVoting(String name) {
        return votings.get(name);
    }
    
    @Override
    public Person getPerson(String name) {
        return people.get(name);
    }
    
    @Override
    public Party getParty(String name) {
        return parties.get(name);
    }
    
    @Override
    public Map<String, Voting> getVotings() {
        return votings;
    }
    
    @Override
    public Map<String, Person> getPeople() {
        return people;
    }
    
    @Override
    public Map<String, Party> getParties() {
        return parties;
    }
    
    @Override
    public void saveVoteUnsafe(Vote vote) {
        votes.add(vote);
    }
    
    @Override
    public void saveParty(Party party) {
        parties.put(party.getName(),party);
    }
    
    @Override
    public void saveVoting(Voting voting) {
        votings.put(voting.getName(),voting);
    }
    
    @Override
    public void savePerson(Person person) {
        people.put(person.getName(),person);
    }
}
