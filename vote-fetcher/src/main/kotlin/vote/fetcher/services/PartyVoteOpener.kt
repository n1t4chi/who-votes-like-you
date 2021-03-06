package vote.fetcher.services

import model.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import vote.fetcher.ParseUtil
import vote.fetcher.data.PartyVotingReference
import vote.fetcher.restclient.RestClient

open class PartyVoteOpener(private val client: RestClient) {
    open fun fetchVotesForParty(partyVoteReference: PartyVotingReference): VotesForParty {
        val content: String = client.getStringContentForUrl(partyVoteReference.url)
        val rows: List<Element> = ParseUtil.getRows(content)
        val votes = parseVotes(rows)
        return VotesForParty(partyVoteReference.party, votes)
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