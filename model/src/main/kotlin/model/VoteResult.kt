package model

import java.lang.IllegalArgumentException
import java.util.*

enum class VoteResult(val polishText: String) {
    yes("Za"), no("Przeciw"), abstain("Wstrzymał się"), absent("Nieobecny");

    override fun toString(): String {
        return polishText
    }

    companion object {
        fun parse(vote: String): VoteResult {
            return Arrays.stream(values())
                .filter { value: VoteResult -> value.name == vote || value.polishText == vote }
                .findFirst()
                .orElseThrow { IllegalArgumentException("Unknown vote: $vote") }
        }
    }
}