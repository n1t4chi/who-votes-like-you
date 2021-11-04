package vote.fetcher

import model.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import vote.fetcher.ParseUtil.Companion.joinBaseWithLink
import java.util.*
import java.util.stream.Collectors

class VoteOpener(
    private val url: HttpUrl,
    private val client: OkHttpClient = OkHttpClient()
) {
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        baseUrl.toHttpUrl(),
        client
    )

    fun fetchVotingUrlsForParties(url: HttpUrl): Map<Party, HttpUrl> {
        val content = RestUtil.getStringContentForUrl(client, url)
        val rows = ParseUtil.getRows(content)
        return rowsToPartiesAndUrls(rows)
    }
    
    private fun rowsToPartiesAndUrls(rows: List<Element>): Map<Party, HttpUrl> {
        return rows.stream()
            .map { row: Element -> rowToUrl(row) }
            .filter { obj -> obj.isPresent }
            .map { obj -> obj.get() }
            .collect(
                Collectors.toMap({ p -> p.first }, { p -> p.second })
            )
    }

    private fun rowToUrl(row: Element): Optional<Pair<Party, HttpUrl>> {
        return Optional.of(row.getElementsByClass("left"))
            .map { obj -> obj.first() }
            .map { element -> element!!.getElementsByTag("a") }
            .map { obj -> obj.first() }
            .flatMap { element -> mapToPartyUrlPair(element!!) }
    }

    private fun mapToPartyUrlPair(element: Element): Optional<Pair<Party, HttpUrl>> {
        val href = element.attr("href")
        if (href.isBlank())
            return Optional.empty()
        val partyName = element.getElementsByTag("strong")
            .first()
        return if (partyName == null || !partyName.hasText()) {
            Optional.empty()
        } else {
            Optional.of(
                Pair(
                    Party(partyName.text()),
                    joinBaseWithLink(url, href)
                )
            )
        }
    }
}