package vote.fetcher

import java.util.*

data class Party(private val name: String) {
    override fun toString(): String {
        return name
    }
}