package vote.fetcher;

import model.Vote;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface VoteStream {
    Optional<Vote> next();
    
    default List<Vote> collectRemianing() {
        var list = new ArrayList<Vote>();
        var shouldContinue = true;
        while(shouldContinue) {
            var nextElement = next();
            if( nextElement.isPresent() ) {
                list.add(nextElement.get());
            } else {
                shouldContinue = false;
            }
        }
        return list;
    }
}
