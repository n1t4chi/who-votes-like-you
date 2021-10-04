package vote.fetcher

import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.net.URL
import java.util.*

class VotesInDayOpener(
    private val info: TargetServerInfo,
    private val client: OkHttpClient = OkHttpClient()
) {
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        TargetServerInfo(baseUrl),
        client
    )

    fun fetchVotingUrls(url: URL): List<URL> {
        val content = RestUtil.getStringContentForUrl(client, url)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { path: Element ->
            rowToUrl(path)
        }
    }

    private fun rowToUrl(row: Element): Optional<URL> {
        return Optional.of(row.getElementsByClass("bold"))
            .map { obj -> obj.first() }
            .map { element -> element!!.getElementsByTag("a") }
            .map { obj -> obj.first() }
            .map { element -> element!!.attr("href") }
            .map { path -> toUrl(path) }
    }

    private fun toUrl(path: String): URL {
        return RestUtil.toUrl(info.baseUrl() + path)
    }
}