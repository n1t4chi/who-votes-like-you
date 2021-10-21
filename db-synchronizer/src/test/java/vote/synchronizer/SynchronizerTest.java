package vote.synchronizer;

import model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SynchronizerTest {
    
    private final VoteFetcher fetcher = new VoteFetcher();
    private final VoteStorage storage = new VoteStorage();
    private final Synchronizer synchronizer = new Synchronizer(fetcher, storage);
    
    @Test
    void initialize_givenNoVotesFromFetcher_savesNothing() {
        //execute
        synchronizer.initialize();
        
        //verify
        Assertions.assertEquals(
            List.of(),
            storage.getVotes()
        );
    }
    
    @Test
    void initialize_givenSomeVotesFromFetcher_savesThemVoteStorage() {
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
            List.of(vote1, vote2, vote3),
            storage.getVotes()
        );
    }
}