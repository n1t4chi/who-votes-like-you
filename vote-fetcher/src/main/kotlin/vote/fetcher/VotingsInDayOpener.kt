package vote.fetcher

import model.*
import okhttp3.HttpUrl
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.util.*

open class VotingsInDayOpener(
    private val baseUrl: HttpUrl,
    private val client: RestClient
) {
    open fun fetchVotingUrls(votingsInDay: VotingsInDay): List<VotingWithUrl> {
        val content = client.getStringContentForUrl(votingsInDay.votingUrl)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { path: Element ->
            rowToVotingWithUrl(path, votingsInDay.cadence, votingsInDay.date)
        }
    }
    
    private fun rowToVotingWithUrl(row: Element, cadence: Cadence, date: LocalDate): Optional<VotingWithUrl> {
        val columns = row.getElementsByTag("td")
        if (columns.size < 3) {
            return Optional.empty()
        }
        
        val numberColumn = columns.get(0)
        val numberReferences = numberColumn.getElementsByTag("a")
        if (numberReferences.size < 1) {
            return Optional.empty()
        }
        val numberElement = numberReferences.first()!!
        val number = numberElement.text().toInt()
        val path = numberElement.attr("href")
        val url = ParseUtil.joinBaseWithLink(baseUrl, path)
        
        val topicColumn = columns.get(2)
        val topicReferences = topicColumn.getElementsByTag("a")
        if (topicReferences.size < 1) {
            return Optional.empty()
        }
        val topicElement = topicReferences.first()!!
        val topic = topicElement.text()
        
        return Optional.of(VotingWithUrl(Voting(topic, number, cadence, date), url))
    }
}