package vote.synchronizer;

import model.Vote;
import vote.fetcher.VoteFetcher;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TestableVoteFetcher implements VoteFetcher {
    private final Set<Vote> votes = new HashSet<>();
    
    public void addVote(Vote vote) {
        votes.add(vote);
    }
    
    @Override
    public Iterator<Vote> getAllVotes() {
        return votes.iterator();
    }
}
