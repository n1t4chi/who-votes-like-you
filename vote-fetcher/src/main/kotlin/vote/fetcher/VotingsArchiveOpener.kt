package vote.fetcher

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.net.URL
import java.util.*

class VotingsArchiveOpener(
    private val client: OkHttpClient,
    private val baseUrl: String
) {
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
            RestUtil.toUrl(baseUrl + getArchive + cadenceNo.toString())
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
        return RestUtil.toUrl(baseUrl + path)
    }
}