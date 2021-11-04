package vote.synchronizer;

import model.Vote;
import vote.fetcher.VoteFetcher;

import java.util.*;

public class TestableVoteFetcher implements VoteFetcher {
    private final Set<Vote> votes = new HashSet<>();
    
    public void addVote(Vote vote) {
        votes.add(vote);
    }
    
    @Override
    public Set<Vote> getAllVotes() {
        return votes;
    }
}
