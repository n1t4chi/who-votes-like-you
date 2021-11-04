package vote.synchronizer;

import model.Vote;
import vote.fetcher.VoteFetcher;
import vote.fetcher.VoteStorage;

import java.util.Set;

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
    
    private Set<Vote> receiveAllVotes() {
        return fetcher.getAllVotes();
    }
    
    private void saveVotes(Set<Vote> votes ) {
        storage.saveVotes( votes );
    }
    
    public void initialize() {
        saveVotes( receiveAllVotes() );
    }
    
    public void synchronize() {
        saveVotes( receiveAllVotes() );
    }
}
