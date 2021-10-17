package vote.synchronizer;

import model.Vote;

import java.util.ArrayList;
import java.util.List;

public class VoteFetcher {
    private final List<Vote> votes = new ArrayList<>();
    
    public void addVote(Vote vote) {
        votes.add(vote);
    }
    
    public List<Vote> getVotes() {
        return votes;
    }
}
