package vote.synchronizer;

import model.Vote;
import vote.fetcher.VoteFetcher;
import vote.fetcher.VoteStream;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class TestableVoteFetcher implements VoteFetcher {
    private final Set<Vote> votes = new HashSet<>();
    
    public void addVote(Vote vote) {
        votes.add(vote);
    }
    
    @Override
    public VoteStream getAllVotes() {
        return new VoteStream() {
            private final Iterator<Vote> iterator = votes.iterator();
            @Override
            public Optional<Vote> next() {
                if( iterator.hasNext() ) {
                    return Optional.of(iterator.next());
                } else {
                    return Optional.empty();
                }
            }
        };
    }
}
