package vote.fetcher;

import model.Vote;

import java.util.Collection;
import java.util.List;

public interface VoteStorage {
    default void saveVotes(Vote... votes) {
        saveVotes( List.of( votes ) );
    }
    
    default void saveVotes(Collection<Vote> votes) {
        votes.forEach(this::saveVote);
    }
    
    void saveVote(Vote vote);
}
