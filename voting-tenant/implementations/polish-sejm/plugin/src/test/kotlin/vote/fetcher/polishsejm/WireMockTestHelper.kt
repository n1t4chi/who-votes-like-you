package vote.fetcher.polishsejm

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import vote.fetcher.polishsejm.client.apis.VotingsApi

/**
 * Helper class for setting up WireMock tests with the Sejm API client.
 */
class WireMockTestHelper(
    private val runtimeInfo: WireMockRuntimeInfo,
) {
    /**
     * Creates a VotingsApi instance configured to use the WireMock server.
     */
    fun createVotingsApi(): VotingsApi = VotingsApi(basePath = runtimeInfo.httpBaseUrl)

    /**
     * Stub GET /sejm/term{term}/votings to return proceeding days.
     */
    fun stubGetTermVotings(
        term: Int,
        jsonResponse: String,
    ) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo("/sejm/term$term/votings"))
                .willReturn(WireMock.okJson(jsonResponse)),
        )
    }

    /**
     * Stub GET /sejm/term{term}/votings/search to return votings for a date.
     */
    fun stubGetVotingsSearch(
        term: Int,
        dateFrom: String,
        jsonResponse: String,
    ) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo("/sejm/term$term/votings/search"))
                .withQueryParam("dateFrom", WireMock.equalTo(dateFrom))
                .willReturn(WireMock.okJson(jsonResponse)),
        )
    }

    /**
     * Stub GET /sejm/term{term}/votings/{sitting}/{num} to return voting details.
     */
    fun stubGetVotingDetails(
        term: Int,
        sitting: Int,
        num: Int,
        jsonResponse: String,
    ) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo("/sejm/term$term/votings/$sitting/$num"))
                .willReturn(WireMock.okJson(jsonResponse)),
        )
    }

    /**
     * Stub GET /sejm/term{term}/votings to return empty list.
     */
    fun stubGetTermVotingsEmpty(term: Int) {
        stubGetTermVotings(term, "[]")
    }

    /**
     * Stub GET /sejm/term{term}/votings/search to return empty list.
     */
    fun stubGetVotingsSearchEmpty(
        term: Int,
        dateFrom: String,
    ) {
        stubGetVotingsSearch(term, dateFrom, "[]")
    }

    /**
     * Stub GET /sejm/term{term}/votings to return 500 error.
     */
    fun stubGetTermVotingsError(term: Int) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo("/sejm/term$term/votings"))
                .willReturn(WireMock.serverError()),
        )
    }

    /**
     * Stub GET /sejm/term{term}/votings/search to return 500 error.
     */
    fun stubGetVotingsSearchError(
        term: Int,
        dateFrom: String,
    ) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo("/sejm/term$term/votings/search"))
                .withQueryParam("dateFrom", WireMock.equalTo(dateFrom))
                .willReturn(WireMock.serverError()),
        )
    }

    /**
     * Stub GET /sejm/term{term}/votings/{sitting}/{num} to return 500 error.
     */
    fun stubGetVotingDetailsError(
        term: Int,
        sitting: Int,
        num: Int,
    ) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo("/sejm/term$term/votings/$sitting/$num"))
                .willReturn(WireMock.serverError()),
        )
    }

    companion object {
        /**
         * Sample proceeding day JSON for term 10.
         */
        const val PROCEEDING_DAY_10_JSON = """[{"proceeding":1,"date":"2023-10-05","votingsNum":5}]"""

        /**
         * Sample voting list JSON for a day in term 10.
         */
        const val VOTING_LIST_10_JSON = """[{"votingNumber":42,"title":"Test Vote","totalVoted":380,"sitting":1}]"""

        /**
         * Sample voting details JSON with individual votes.
         */
        const val VOTING_DETAILS_JSON = """{
            "votes": [
                {"firstName":"Jan","lastName":"Kowalski","club":"PiS","vote":"YES"},
                {"firstName":"Alicja","lastName":"Nowak","club":"PO","vote":"NO"}
            ]
        }"""
    }
}
