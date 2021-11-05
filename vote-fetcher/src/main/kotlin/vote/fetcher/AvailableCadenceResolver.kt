package vote.fetcher

import model.Cadence
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import vote.fetcher.ParseUtil.Companion.toXml
import java.util.*

open class AvailableCadenceResolver(
    private val baseUrl: HttpUrl,
    private val client: OkHttpClient = OkHttpClient()
) {
    private companion object {
        val initialCadenceNo = 7
        val noData = "Brak danych"
    }

    constructor(client: OkHttpClient = OkHttpClient(), baseUrl: String) : this(
        baseUrl.toHttpUrl(),
        client
    )

    open fun getCurrentCadences(): List<Cadence> {
        var canContinue = true
        var index = initialCadenceNo
        val cadences: MutableList<Cadence> = arrayListOf()
        while( canContinue ) {
            val content = fetchCadencePageContent(index)
            if( hasVotings( content ) )
                cadences.add( Cadence( index ) )
            else
                canContinue = cadences.isEmpty() || false
            index++
        }
        return cadences
    }

    private fun hasVotings(content: String): Boolean {
        val document = toXml(content)

        val noData = document.getElementsByClass("pub-opis").stream()
            .map { element -> element.text() }
            .anyMatch { text -> noData == text }
        if( noData )
            return false
        val table = ParseUtil.findTable(document)
        return table.isPresent
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

    private fun rowToUrl(row: Element): Optional<HttpUrl> {
        return Optional.of(row.getElementsByClass("left"))
            .map { obj -> obj.first() }
            .map { element -> element!!.getElementsByTag("a") }
            .map { obj -> obj.first() }
            .map { element -> element!!.attr("href") }
            .map { path -> ParseUtil.joinBaseWithLink(baseUrl, path) }
    }
}