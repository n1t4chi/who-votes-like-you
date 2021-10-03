package vote.fetcher

import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URL
import java.util.*

class PartyVoteOpener(
    private val client: OkHttpClient,
    private val baseUrl: String
) {
    fun fetchVotingUrlsForParties(party: Party?, url: URL): VotesForParty {
        val content: String = RestUtil.getStringContentForUrl(client, url)
        val rows: List<Element> = ParseUtil.getRows(content)
        val votes = parseVotes(rows)
        return VotesForParty(party!!, votes)
    }

    private fun parseVotes(rows: List<Element>): Map<Person, Vote> {
        val votes: MutableMap<Person, Vote> = HashMap()
        for (row in rows) {
            val cells = row.getElementsByTag("td")
            if (cells.size == 6) {
                val name = requireText(cells, 4)
                val vote = requireText(cells, 5)
                votes[Person(name)] = Vote.parse(vote)
            }
            if (cells.size >= 3) {
                val name = requireText(cells, 1)
                val vote = requireText(cells, 2)
                votes[Person(name)] = Vote.parse(vote)
            }
        }
        return votes
    }

    private fun requireText(cells: Elements, i: Int): String {
        return cells[i].text()
    }
}