package vote.fetcher

import vote.fetcher.Vote
import java.lang.IllegalArgumentException
import java.util.*

enum class Vote(private val polishText: String) {
    yes("Za"), no("Przeciw"), abstain("Wstrzymał się"), absent("Nieobecny");

    override fun toString(): String {
        return polishText
    }

    companion object {
        fun parse(vote: String): Vote {
            return Arrays.stream(values())
                .filter { value: Vote -> value.name == vote || value.polishText == vote }
                .findFirst()
                .orElseThrow { IllegalArgumentException("Unknown vote: $vote") }
        }
    }
}