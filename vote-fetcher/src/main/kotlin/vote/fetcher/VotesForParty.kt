package vote.fetcher

import java.util.HashMap

class VotesForParty(private val party: Party, votes: Map<Person, Vote>) {
    private val votes: MutableMap<Person, Vote> = HashMap()
    fun getVotes(): Map<Person, Vote> {
        return votes
    }

    init {
        this.votes.putAll(votes)
    }
}