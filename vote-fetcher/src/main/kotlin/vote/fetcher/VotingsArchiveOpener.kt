package vote.fetcher

import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.net.URL
import java.util.*

class VotingsArchiveOpener(
    private val info: TargetServerInfo,
    private val client: OkHttpClient = OkHttpClient()
) {
    constructor(client : OkHttpClient = OkHttpClient(), baseUrl: String ) : this(
        TargetServerInfo( baseUrl ),
        client
    )

    companion object{
        private val getArchive: String = "agent.xsp?symbol=posglos&NrKadencji="
    }

    fun getVotesInDayUrls(cadenceNo: Int): List<URL> {
        val content = fetchVotesInDayUrls(cadenceNo)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { row -> rowToUrl(row) }
    }

    private fun fetchVotesInDayUrls(cadenceNo: Int): String {
        return RestUtil.getStringContentForUrl(
            client,
            info.urlBuilder()
                .addPathSegment("agent.xsp")
                .addQueryParameter("symbol","posglos")
                .addQueryParameter("NrKadencji", cadenceNo.toString())
                .build()
        )
    }

    private fun rowToUrl(row: Element): Optional<URL> {
        return Optional.of(row.getElementsByClass("left"))
            .map { obj -> obj.first() }
            .map { element -> element!!.getElementsByTag("a") }
            .map { obj -> obj.first() }
            .map { element -> element!!.attr("href") }
            .map { path -> toUrl(path) }
    }

    private fun toUrl(path: String): URL {
        return URL( info.baseUrl() + path )
    }
}