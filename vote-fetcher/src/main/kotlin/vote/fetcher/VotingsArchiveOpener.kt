package vote.fetcher

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class VotingsArchiveOpener(
    private val baseUrl: HttpUrl,
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("d LLLL yyyy 'r.'")
            .withLocale(Locale.forLanguageTag("pl"))
        private val monthMappers = mapOf(
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

    fun getVotesInDayUrls(cadenceNo: Int): List<Pair<LocalDate,HttpUrl>> {
        val content = fetchCadencePageContent(cadenceNo)
        val rows = ParseUtil.getRows(content)
        return ParseUtil.rowsToUrls(rows) { row -> rowToDateUrlPair(row) }
    }

    private fun fetchCadencePageContent(cadenceNo: Int): String {
        return RestUtil.getStringContentForUrl(
            client,
            baseUrl.newBuilder()
                .addPathSegment("agent.xsp")
                .addQueryParameter("symbol", "posglos")
                .addQueryParameter("NrKadencji", cadenceNo.toString())
                .build()
        )
    }

    private fun rowToDateUrlPair(row: Element): Optional<Pair<LocalDate,HttpUrl>> {
        return Optional.of(row.getElementsByClass("left"))
            .map { obj -> obj.first() }
            .map { element -> element!!.getElementsByTag("a") }
            .map { obj -> obj.first() }
            .map { path -> toPair(path!!) }
    }
    
    private fun toPair(element: Element): Pair<LocalDate,HttpUrl> {
        val value = element.text()
        val date = LocalDate.parse(correctMonthName(value.lowercase()), dateTimeFormatter)
        val url = ParseUtil.joinBaseWithLink(baseUrl, element.attr("href") )
        
        return date to url
    }
    
    private fun correctMonthName(value: String): String {
        var result = value
        for ((old,new) in monthMappers) {
            result = result.replace( old,new )
        }
        return result
    }
}