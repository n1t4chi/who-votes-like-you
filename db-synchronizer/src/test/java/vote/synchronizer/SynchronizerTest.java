package vote.synchronizer;

import model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

class SynchronizerTest {
    private static final Voting voting1 = new Voting("Głosowanie nr.1", 1, new Cadence(1,0), LocalDate.of(2001, 1, 1),0);
    private static final Voting voting2 = new Voting("Głosowanie nr.2", 2, new Cadence(2,0), LocalDate.of(2001, 1, 1),0);
    private final TestableVoteFetcher fetcher = new TestableVoteFetcher();
    private final TestableVoteStorage storage = new TestableVoteStorage();
    private final Synchronizer synchronizer = new Synchronizer(fetcher, storage);
    
    @Test
    void initialize_givenNoVotesFromFetcher_savesNothing() {
        //execute
        synchronizer.initialize();
        
        //verify
        Assertions.assertEquals(
            Set.of(),
            storage.getVotes()
        );
    }
    
    @Test
    void initialize_givenSomeVotesFromFetcher_savesThemToVoteStorage() {
        //prepare
        Vote vote1 = new Vote(
            voting1,
            new Party("Kukiz"), new Person("Marcin Prokop"),
            VoteResult.yes
        );
        Vote vote2 = new Vote(
            voting1,
            new Party("Nie Litwie"), new Person("Jan Zawisza Biały"),
            VoteResult.absent
        );
        Vote vote3 = new Vote(
            voting2,
            new Party("Kukiz"), new Person("Marcin Prokop"),
            VoteResult.abstain
        );
        fetcher.addVote(vote1);
        fetcher.addVote(vote2);
        fetcher.addVote(vote3);
        
        //execute
        synchronizer.initialize();
        
        //verify
        Assertions.assertEquals(
            Set.of(vote1, vote2, vote3),
            storage.getVotes()
        );
    }
    
    @Test
    void synchronize_givenNoVotesFromFetcher_whenNoVotesAreInStorage_savesNothing() {
        //execute
        synchronizer.synchronize();
        
        //verify
        Assertions.assertEquals(
            Set.of(),
            storage.getVotes()
        );
    }
    
    @Test
    void synchronize_receivesVotesFromFetcher_whenSameVotesInStorage_savesNothing() {
        //prepare
        Vote vote = new Vote(
            voting1,
            new Party("Kukiz"), new Person("Marcin Prokop"),
            VoteResult.yes
        );
        fetcher.addVote(vote);
        storage.saveVotes(vote);
        
        //execute
        synchronizer.synchronize();
        
        //verify
        Assertions.assertEquals(
            Set.of(vote),
            storage.getVotes()
        );
    }
    
    @Test
    void synchronize_receivesVotesFromFetcher_whenEmptyStorage_savesVotes() {
        //prepare
        Vote vote = new Vote(
            voting1,
            new Party("Kukiz"), new Person("Marcin Prokop"),
            VoteResult.yes
        );
        fetcher.addVote(vote);
        
        //execute
        synchronizer.synchronize();
        
        //verify
        Assertions.assertEquals(
            Set.of(vote),
            storage.getVotes()
        );
    }
}