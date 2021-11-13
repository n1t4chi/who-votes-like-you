package vote.synchronizer;

import model.Vote;
import vote.fetcher.VoteFetcher;
import vote.fetcher.VoteStorage;

import java.util.ArrayList;
import java.util.Iterator;
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
    
    private Iterator<Vote> receiveAllVotes() {
        System.out.println( "fetching votes" );
        return fetcher.getAllVotes();
    }
    
    private void saveVotes( Iterator<Vote> votes ) {
        System.out.println( "saving votes" );
        int i = 0;
        int batchFrom = 0;
        List<Vote> batch = new ArrayList<>(1000);
        while (votes.hasNext()) {
            i++;
            batch.add(votes.next());
            if(i%1000==0 || !votes.hasNext()) {
                System.out.println("Saving votes from " + batchFrom + " to " + i);
                storage.saveVotes(batch);
                batch.clear();
                batchFrom = i;
            }
        }
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
