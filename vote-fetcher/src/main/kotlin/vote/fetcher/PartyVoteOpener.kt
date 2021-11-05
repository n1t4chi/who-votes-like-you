package vote.fetcher

import model.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.*

open class PartyVoteOpener(private val client: OkHttpClient = OkHttpClient() ) {

    open fun fetchVotingUrlsForParties(party: Party, url: HttpUrl): VotesForParty {
        val content: String = RestUtil.getStringContentForUrl(client, url)
        val rows: List<Element> = ParseUtil.getRows(content)
        val votes = parseVotes(rows)
        return VotesForParty(party, votes)
    }

    private fun parseVotes(rows: List<Element>): Map<Person, VoteResult> {
        val votes: MutableMap<Person, VoteResult> = HashMap()
        for (row in rows) {
            val cells = row.getElementsByTag("td")
            if (cells.size == 6) {
                val name = requireText(cells, 4)
                val vote = requireText(cells, 5)
                votes[Person(name)] = VoteResult.parse(vote)
            }
            if (cells.size >= 3) {
                val name = requireText(cells, 1)
                val vote = requireText(cells, 2)
                votes[Person(name)] = VoteResult.parse(vote)
            }
        }
        return votes
    }

    private fun requireText(cells: Elements, i: Int): String {
        return cells[i].text()
    }
}