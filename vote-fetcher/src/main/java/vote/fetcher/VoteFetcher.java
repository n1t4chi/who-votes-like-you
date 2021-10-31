package vote.fetcher;

import model.Vote;

import java.util.Set;

public interface VoteFetcher {
    Set<Vote> getVotes();
}
