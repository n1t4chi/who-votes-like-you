package vote.synchronizer;

import model.Vote;

import java.util.ArrayList;
import java.util.List;

public class VoteStorage {
    private final List<Vote> votes = new ArrayList<>();
    
    public void saveVotes(List<Vote> votes) {
        this.votes.addAll(votes);
    }
    
    public List<Vote> getVotes() {
        return votes;
    }
}