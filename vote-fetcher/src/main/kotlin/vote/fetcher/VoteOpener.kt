package vote.fetcher

import model.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import vote.fetcher.ParseUtil.Companion.joinBaseWithLink
import java.util.*
import java.util.stream.Collectors

open class VoteOpener(
    private val url: HttpUrl,
    private val client: OkHttpClient = OkHttpClient()
) {
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        baseUrl.toHttpUrl(),
        client
    )

    open fun fetchVotingUrlsForParties(votingWithUrl: VotingWithUrl): Set<PartyVotingReference> {
        val content = RestUtil.getStringContentForUrl(client, votingWithUrl.url)
        val rows = ParseUtil.getRows(content)
        return rowsToPartiesAndUrls(votingWithUrl.voting,rows)
    }
    
    private fun rowsToPartiesAndUrls(voting: Voting, rows: List<Element>): Set<PartyVotingReference> {
        return rows.stream()
            .map { row: Element -> rowToUrl(row) }
            .filter { obj -> obj.isPresent }
            .map { obj -> obj.get() }
            .map { (party, url) -> PartyVotingReference(voting,party, url) }
            .collect(Collectors.toSet())
    }

    private fun rowToUrl(row: Element): Optional<Pair<Party, HttpUrl>> {
        return Optional.of(row.getElementsByTag("a"))
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