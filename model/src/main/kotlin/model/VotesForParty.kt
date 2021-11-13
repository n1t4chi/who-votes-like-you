package model

class VotesForParty(val party: Party, votes: Map<Person, VoteResult>) {
    private val votes: MutableMap<Person, VoteResult> = HashMap()
    fun getVotes(): Map<Person, VoteResult> {
        return votes
    }

    fun size(): Int {
        return votes.size
    }

    init {
        this.votes.putAll(votes)
    }
}