package vote.fetcher

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import java.util.*

class VotesInDayOpener(
    private val baseUrl: HttpUrl,
    private val client: OkHttpClient = OkHttpClient()
) {
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        baseUrl.toHttpUrl(),
        client
    )

    fun fetchVotingUrls(url: HttpUrl): List<HttpUrl> {
        val content = RestUtil.getStringContentForUrl(client, url)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { path: Element ->
            rowToUrl(path)
        }
    }

    private fun rowToUrl(row: Element): Optional<HttpUrl> {
        return Optional.of(row.getElementsByClass("bold"))
            .map { obj -> obj.first() }
            .map { element -> element!!.getElementsByTag("a") }
            .map { obj -> obj.first() }
            .map { element -> element!!.attr("href") }
            .map { path -> ParseUtil.joinBaseWithLink(baseUrl, path) }
    }
}