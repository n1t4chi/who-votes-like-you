package vote.fetcher

import model.Cadence
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

open class VotingsArchiveOpener(
    private val baseUrl: HttpUrl,
    private val client: OkHttpClient = OkHttpClient()
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
    
    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        baseUrl.toHttpUrl(),
        client
    )

    open fun getVotingsInDayUrls(cadence: Cadence): List<VotingsInDay> {
        val content = fetchCadencePageContent(cadence)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { row -> rowToVotingsInDay(cadence, row) }
    }

    private fun fetchCadencePageContent(cadence: Cadence): String {
        return RestUtil.getStringContentForUrl(
            client,
            baseUrl.newBuilder()
                .addPathSegment("agent.xsp")
                .addQueryParameter("symbol", "posglos")
                .addQueryParameter("NrKadencji", cadence.number.toString())
                .build()
        )
    }

    private fun rowToVotingsInDay(cadence: Cadence, row: Element): Optional<VotingsInDay> {
        return Optional.of(row.getElementsByTag("a"))
            .map { obj -> obj.first() }
            .map { path -> toVotingsInDay(cadence, path!!) }
    }
    
    private fun toVotingsInDay(cadence: Cadence, element: Element): VotingsInDay {
        val value = element.text()
        val date = LocalDate.parse(correctMonthName(value.lowercase()), dateTimeFormatter)
        val url = ParseUtil.joinBaseWithLink(baseUrl, element.attr("href") )
        
        return VotingsInDay(cadence,date, url)
    }
    
    private fun correctMonthName(value: String): String {
        var result = value
        for ((old,new) in monthMappers) {
            result = result.replace( old,new )
        }
        return result
    }
}