package vote.fetcher

import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URL
import java.util.*

class VotesInDayOpener(
    private val client: OkHttpClient,
    private val baseUrl: String
) {
    fun fetchVotingUrls(url: URL): List<URL> {
        val content = RestUtil.getStringContentForUrl(client, url)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { path: Element ->
            rowToUrl(
                baseUrl, path
            )
        }
    }

    private fun rowToUrl(baseUrl: String, row: Element): Optional<URL> {
        return Optional.of(row.getElementsByClass("bold"))
            .map { obj -> obj.first() }
            .map { element -> element!!.getElementsByTag("a") }
            .map { obj -> obj.first() }
            .map { element -> element!!.attr("href") }
            .map { path -> toUrl(baseUrl, path) }
    }

    private fun toUrl(baseUrl: String, path: String): URL {
        return RestUtil.toUrl(baseUrl + path)
    }
}