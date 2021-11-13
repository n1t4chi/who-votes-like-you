package move.storage

import model.Vote
import vote.fetcher.VoteStorage

class DirectVoteStorageImpl(private val accessor: DbAccessor) : VoteStorage {
    
    override fun saveVote(vote: Vote) {
        if( accessor.getVoting(vote.voting.name) == null ) {
            accessor.addVoting(vote.voting)
        }
        if( accessor.getParty(vote.party.name) == null ) {
            accessor.addParty(vote.party)
        }
        if( accessor.getPerson(vote.person.name) == null ) {
            accessor.addPerson(vote.person)
        }
        accessor.addVote( vote )
    }
}