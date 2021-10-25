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
    
    private List<Vote> receiveAllVotes() {
        return fetcher.getVotes();
    }
    
    private void saveVotes(List<Vote> votes ) {
        storage.saveVotes( votes );
    }
    
    public void initialize() {
        saveVotes( receiveAllVotes() );
    }
    
    public void synchronize() {
        receiveAllVotes()
            .stream()
            .filter(receivedVote -> !storage.contains(receivedVote))
            .forEach(this::saveVote);
    }
    
    private void saveVote(Vote receivedVote) {
        storage.saveVotes(receivedVote);
    }
}
