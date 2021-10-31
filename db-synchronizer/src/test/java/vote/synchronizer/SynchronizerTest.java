package vote.synchronizer;

import model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class SynchronizerTest {
    
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
            new Voting("Głosowanie nr.1"),
            new Person("Marcin Prokop"),
            VoteResult.yes,
            new Party("Kukiz")
        );
        Vote vote2 = new Vote(
            new Voting("Głosowanie nr.1"),
            new Person("Jan Zawisza Biały"),
            VoteResult.absent,
            new Party("Nie Litwie")
        );
        Vote vote3 = new Vote(
            new Voting("Głosowanie nr.2"),
            new Person("Marcin Prokop"),
            VoteResult.abstain,
            new Party("Kukiz")
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
    void synchronize_givenNoVotesFromFetcher_whenNoVotesAreInStorage_savesNothing(){
        //execute
        synchronizer.synchronize();
        
        //verify
        Assertions.assertEquals(
            Set.of(),
            storage.getVotes()
        );
    }
    
    @Test
    void synchronize_receivesVotesFromFetcher_whenSameVotesInStorage_savesNothing(){
        //prepare
        Vote vote = new Vote(
            new Voting("Głosowanie nr.1"),
            new Person("Marcin Prokop"),
            VoteResult.yes,
            new Party("Kukiz")
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
    void synchronize_receivesVotesFromFetcher_whenEmptyStorage_savesVotes(){
        //prepare
        Vote vote = new Vote(
            new Voting("Głosowanie nr.1"),
            new Person("Marcin Prokop"),
            VoteResult.yes,
            new Party("Kukiz")
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