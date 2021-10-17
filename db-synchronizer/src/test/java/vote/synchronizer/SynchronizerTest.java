package vote.synchronizer;

import model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SynchronizerTest {
    
    private final VoteFetcher fetcher = new VoteFetcher();
    private final Synchronizer synchronizer = new Synchronizer(fetcher);
    
    
    @Test
    void getAllVotes_whenNoVotesAvailable_returnsEmptyList() {
        List<Vote> votes = synchronizer.getAllVotes();
        Assertions.assertEquals(
            List.of(),
            votes
        );
    }
    
    @Test
    void getAllVotes_whenSingleVoteAvailable_returnsSingleton() {
        Vote vote = new Vote(
            new Voting("Głosowanie nr.1"),
            new Person("Marcin Prokop"),
            VoteResult.yes,
            new Party("Kukiz")
        );
        fetcher.addVote(vote);
        List<Vote> votes = synchronizer.getAllVotes();
        Assertions.assertEquals(
            List.of(vote),
            votes
        );
    }
}