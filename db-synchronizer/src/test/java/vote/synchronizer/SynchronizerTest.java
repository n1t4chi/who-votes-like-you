package vote.synchronizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SynchronizerTest {

    private final Synchronizer synchronizer = new Synchronizer();

    @Test
    void synchronizerCanAccessVoteFetcher() {
        VoteFetcher voteFetcher = synchronizer.getVoteFetcher();
        Assertions.assertNotNull(voteFetcher);
    }

    @Test
    void synchronizerCanFetchVotes() {
    }
}