package vote.fetcher

import model.Cadence
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import vote.fetcher.ParseUtil.Companion.toXml

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
        return RestUtil.tryToGetStringContentForUrl(
            client,
            baseUrl.newBuilder()
                .addPathSegment("agent.xsp")
                .addQueryParameter("symbol", "posglos")
                .addQueryParameter("NrKadencji", cadenceNo.toString())
                .build()
        )
    }
}