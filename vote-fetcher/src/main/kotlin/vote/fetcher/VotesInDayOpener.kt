package vote.fetcher

import model.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.util.*

open class VotesInDayOpener(
    private val baseUrl: HttpUrl,
    private val client: OkHttpClient = OkHttpClient()
) {
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        baseUrl.toHttpUrl(),
        client
    )

    open fun fetchVotingUrls(url: HttpUrl, cadence: Cadence, date: LocalDate): List<Pair<Voting,HttpUrl>> {
        val content = RestUtil.getStringContentForUrl(client, url)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { path: Element ->
            rowToPair(path, cadence, date)
        }
    }

    private fun rowToPair(row: Element, cadence: Cadence, date: LocalDate): Optional<Pair<Voting,HttpUrl>> {
        val columns = row.getElementsByTag("td")
        if( columns.size < 3 ) {
            return Optional.empty()
        }
        
        val numberColumn = columns.get(0)
        val numberReferences = numberColumn.getElementsByTag("a")
        if( numberReferences.size < 1 ) {
            return Optional.empty()
        }
        val numberElement = numberReferences.first()!!
        val number = numberElement.text().toInt()
        val path = numberElement.attr("href")
        val url = ParseUtil.joinBaseWithLink(baseUrl, path)
    
        val topicColumn = columns.get(2)
        val topicReferences = topicColumn.getElementsByTag("a")
        if( topicReferences.size < 1 ) {
            return Optional.empty()
        }
        val topicElement = topicReferences.first()!!
        val topic = topicElement.text()
        
        return Optional.of( Voting(topic,number,cadence, date) to url )
    }
}