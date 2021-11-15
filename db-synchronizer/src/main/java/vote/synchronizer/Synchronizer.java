package vote.synchronizer;

import vote.fetcher.VoteFetcher;
import vote.fetcher.VoteStorage;
import vote.fetcher.VoteStream;

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
    
    private VoteStream receiveAllVotes() {
        System.out.println( "fetching votes" );
        return fetcher.getAllVotes();
    }
    
    private void saveVotes( VoteStream votes ) {
        System.out.println("Starting saving votes.");
        new InitialVoteSaver( storage )
            .save(votes);
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
