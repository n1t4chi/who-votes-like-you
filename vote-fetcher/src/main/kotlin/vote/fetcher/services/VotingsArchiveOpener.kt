package vote.fetcher.services

import model.*
import okhttp3.HttpUrl
import org.jsoup.nodes.Element
import vote.fetcher.ParseUtil
import vote.fetcher.data.VotingDayWithUrl
import vote.fetcher.restclient.RestClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

open class VotingsArchiveOpener(
    private val baseUrl: HttpUrl,
    private val client: RestClient
) {
    companion object {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("d LLLL yyyy 'r.'")
            .withLocale(Locale.forLanguageTag("pl"))
        val monthMappers = mapOf(
            "stycznia" to "styczeń",
            "lutego" to "luty",
            "marca" to "marzec",
            "kwietnia" to "kwiecień",
            "maja" to "maj",
            "czerwca" to "czerwiec",
            "lipca" to "lipiec",
            "sierpnia" to "sierpień",
            "września" to "wrzesień",
            "października" to "październik",
            "listopada" to "listopad",
            "grudnia" to "grudzień",
        )
    }
    
    open fun getVotingsInDayUrls(cadence: Cadence): List<VotingDayWithUrl> {
        val content = fetchCadencePageContent(cadence)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { row -> rowToVotingsInDay(cadence, row) }
    }
    
    private fun fetchCadencePageContent(cadence: Cadence): String {
        return client.getStringContentForUrl(
            baseUrl.newBuilder()
                .addPathSegment("agent.xsp")
                .addQueryParameter("symbol", "posglos")
                .addQueryParameter("NrKadencji", cadence.number.toString())
                .build()
        )
    }
    
    private fun rowToVotingsInDay(cadence: Cadence, row: Element): Optional<VotingDayWithUrl> {
        return Optional.of(row.getElementsByTag("a"))
            .map { obj -> obj.first() }
            .map { path -> toVotingsInDay(cadence, path!!) }
    }
    
    private fun toVotingsInDay(cadence: Cadence, element: Element): VotingDayWithUrl {
        val value = element.text()
        val date = LocalDate.parse(correctMonthName(value.lowercase()), dateTimeFormatter)
        val url = ParseUtil.joinBaseWithLink(baseUrl, element.attr("href"))
        
        return VotingDayWithUrl(VotingDay(cadence, date), url)
    }
    
    private fun correctMonthName(value: String): String {
        var result = value
        for ((old, new) in monthMappers) {
            result = result.replace(old, new)
        }
        return result
    }
}