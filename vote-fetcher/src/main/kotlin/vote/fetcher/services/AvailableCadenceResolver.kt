package vote.fetcher.services

import model.Cadence
import okhttp3.HttpUrl
import vote.fetcher.ParseUtil
import vote.fetcher.ParseUtil.Companion.toXml
import vote.fetcher.restclient.RestClient

open class AvailableCadenceResolver(
    private val baseUrl: HttpUrl,
    private val client: RestClient
) {
    private companion object {
        val initialCadenceNo = 7
        val noData = "Brak danych"
    }
    
    open fun getCurrentCadences(): List<Cadence> {
        var canContinue = true
        var index = initialCadenceNo
        val cadences: MutableList<Cadence> = arrayListOf()
        while (canContinue) {
            val content = fetchCadencePageContent(index)
            if (hasVotings(content))
                cadences.add(Cadence.newCadence(index))
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
        if (noData)
            return false
        val table = ParseUtil.findTable(document)
        return table.isPresent
    }
    
    private fun fetchCadencePageContent(cadenceNo: Int): String {
        return client.tryToGetStringContentForUrl(
            baseUrl.newBuilder()
                .addPathSegment("agent.xsp")
                .addQueryParameter("symbol", "posglos")
                .addQueryParameter("NrKadencji", cadenceNo.toString())
                .build()
        )
    }
}