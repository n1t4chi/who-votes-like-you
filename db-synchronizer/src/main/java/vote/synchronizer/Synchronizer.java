package vote.synchronizer;

import model.Vote;

import java.util.List;

public class Synchronizer {
    private final VoteFetcher fetcher;
    private final VoteStorage storage;
    
    public Synchronizer(
        VoteFetcher fetcher,
        VoteStorage storage
    ) {
        this.fetcher = fetcher;
        this.storage = storage;
    }
    
    private List<Vote> getAllVotes() {
        return fetcher.getVotes();
    }
    
    private void saveVotes(List<Vote> votes ) {
        storage.saveVotes( votes );
    }
    
    public void initialize() {
        saveVotes( getAllVotes() );
    }
}
