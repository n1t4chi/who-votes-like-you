package vote.fetcher;

import model.Vote;

import java.util.Iterator;

public interface VoteFetcher {
    Iterator<Vote> getAllVotes();
}
