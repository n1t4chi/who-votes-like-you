package vote.synchronizer;

import model.Vote;
import vote.fetcher.VoteFetcher;
import vote.fetcher.VoteStorage;

import java.util.Iterator;

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
    
    private Iterator<Vote> receiveAllVotes() {
        System.out.println( "fetching votes" );
        return fetcher.getAllVotes();
    }
    
    private void saveVotes( Iterator<Vote> votes ) {
        System.out.println( "saving votes" );
        while (votes.hasNext()) {
            Vote next = votes.next();
            storage.saveVote(next);
        }
        votes.forEachRemaining( storage::saveVote );
    }
    
    public void initialize() {
        System.out.println( "starting initialize" );
        saveVotes( receiveAllVotes() );
        System.out.println( "initialize done" );
    }
    
    public void synchronize() {
        saveVotes( receiveAllVotes() );
    }
}
