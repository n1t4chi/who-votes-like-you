package vote.fetcher.polishsejm.data

import vote.fetcher.polishsejm.client.models.SejmVotingKindDto
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.Arrays

data class Term(
    val number: Int,
    val status: TermStatus,
)

data class Party(val name: String) {
    override fun toString(): String = name
}

data class Person(val name: String) {
    override fun toString(): String = name
}

data class Vote(
    val voting: Voting,
    val party: Party,
    val person: Person,
    val result: VoteResult,
)

enum class VoteResult(val polishText: String) {
    yes("Za"),
    no("Przeciw"),
    abstain("Wstrzymał się"),
    absent("Nieobecny"),
    ;

    companion object {
        fun parse(vote: String): VoteResult =
            Arrays
                .stream(values())
                .filter { value: VoteResult -> value.name == vote || value.polishText == vote }
                .findFirst()
                .orElseThrow { IllegalArgumentException("Unknown vote: $vote") }
    }
}

class VotesForParty(val party: Party, votes: Map<Person, VoteResult>) {
    private val votes: MutableMap<Person, VoteResult> = HashMap()

    fun getVotes(): Map<Person, VoteResult> = votes

    fun size(): Int = votes.size

    init {
        this.votes.putAll(votes)
    }
}

data class Voting(
    val name: String,
    val votingNumber: Int,
    val date: LocalDateTime,
    val sitting: Sitting,
    val votesCast: Int,
    val type: SejmVotingKindDto,
)

data class Sitting(
    val term: Term,
    val sittingNumber: Int,
    val votingsCount: Int,
)

enum class TermStatus {
    old,
    active,
    unknown,
}
