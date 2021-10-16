package vote.fetcher

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import java.util.*

class VotingsArchiveOpener(
    private val baseUrl: HttpUrl,
    private val client: OkHttpClient = OkHttpClient()
) {
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        baseUrl.toHttpUrl(),
        client
    )

    fun getVotesInDayUrls(cadenceNo: Int): List<HttpUrl> {
        val content = fetchVotesInDayUrls(cadenceNo)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { row -> rowToUrl(row) }
    }

    private fun fetchVotesInDayUrls(cadenceNo: Int): String {
        return RestUtil.getStringContentForUrl(
            client,
            baseUrl.newBuilder()
                .addPathSegment("agent.xsp")
                .addQueryParameter("symbol", "posglos")
                .addQueryParameter("NrKadencji", cadenceNo.toString())
                .build()
        )
    }

    private fun rowToUrl(row: Element): Optional<HttpUrl> {
        return Optional.of(row.getElementsByClass("left"))
            .map { obj -> obj.first() }
            .map { element -> element!!.getElementsByTag("a") }
            .map { obj -> obj.first() }
            .map { element -> element!!.attr("href") }
            .map { path -> ParseUtil.joinBaseWithLink(baseUrl, path) }
    }

}