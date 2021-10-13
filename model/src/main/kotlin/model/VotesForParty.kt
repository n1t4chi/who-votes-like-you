package model

import java.util.HashMap

class VotesForParty(val party: Party, votes: Map<Person, VoteResult>) {
    private val votes: MutableMap<Person, VoteResult> = HashMap()
    fun getVotes(): Map<Person, VoteResult> {
        return votes
    }

    init {
        this.votes.putAll(votes)
    }
}