package move.storage

import model.Vote
import vote.fetcher.VoteStorage

class DirectVoteStorageImpl(private val accessor: DbAccessor) : VoteStorage {
    
    override fun saveVote(vote: Vote) {
        accessor.addVote( vote )
    }
}