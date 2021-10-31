package vote.synchronizer;

import model.Vote;
import vote.fetcher.VoteStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestableVoteStorage implements VoteStorage {
    private final Set<Vote> votes = new HashSet<>();
    
    public Set<Vote> getVotes() {
        return votes;
    }
    
    @Override
    public void saveVote(Vote vote) {
        votes.add(vote);
    }
}
