package vote.synchronizer;

import model.Vote;

import java.util.List;

public class Synchronizer {
    
    private final VoteFetcher fetcher;
    
    public Synchronizer(VoteFetcher fetcher) {
        this.fetcher = fetcher;
    }
    
    public List<Vote> getAllVotes() {
        return fetcher.getVotes();
    }
}
