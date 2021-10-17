import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import model.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.*
import vote.fetcher.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.util.Comparator
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList


class VoteFetcherTests {
    companion object {
        val server = WireMockServer(wireMockConfig().port(0))

        @BeforeAll
        @JvmStatic
        fun setUp() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            server.stop()
        }
    }

    @Test
    @Throws(Exception::class)
    fun availableCadenceResolver_startsFromCadence7_stopsOnFirstWithoutData() {
        //prepare
        server.stubFor(
            WireMock.get("/agent.xsp?symbol=posglos&NrKadencji=7")
                .willReturn(WireMock.okXml(readFile("/cadence_7.html")))
        )
        server.stubFor(
            WireMock.get("/agent.xsp?symbol=posglos&NrKadencji=8")
                .willReturn(WireMock.okXml(readFile("/cadence_7.html")))
        )
        server.stubFor(
            WireMock.get("/agent.xsp?symbol=posglos&NrKadencji=9")
                .willReturn(WireMock.okXml(readFile("/no_votings.html")))
        )
        //execute
        val cadenceResolver = AvailableCadenceResolver(baseUrl = server.baseUrl())
        //verify
        val cadences = cadenceResolver.getCurrentCadences()
        Assertions.assertEquals(
            listOf( Cadence( 7 ), Cadence( 8 ) ),
            cadences
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVotingArchiveOpener() {
        //prepare
        server.stubFor(
            WireMock.get("/agent.xsp?symbol=posglos&NrKadencji=7")
                .willReturn(WireMock.okXml(readFile("/cadence_7.html")))
        )
        //execute
        val archiveOpener = VotingsArchiveOpener(baseUrl = server.baseUrl())
        val votesInDayUrls = archiveOpener.getVotesInDayUrls(7)
        assertUrlList(
            votesInDayUrls,
            urlsFromFile("/cadence_7.txt")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVotesInDayOpener() {
        //prepare
        server.stubFor(
            WireMock.get("/agent.xsp?symbol=listaglos&IdDnia=1707")
                .willReturn(WireMock.okXml(readFile("/votings_12-12-2018.html")))
        )
        //execute
        val archiveOpener = VotesInDayOpener(baseUrl = server.baseUrl())
        val votingUrls = archiveOpener.fetchVotingUrls(
            (server.baseUrl() + "/agent.xsp?symbol=listaglos&IdDnia=1707").toHttpUrl()
        )
        assertUrlList(
            votingUrls,
            urlsFromFile("/votings_12-12-2018.txt")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVoteOpener() {
        //prepare
        val urlPath = "/agent.xsp?symbol=glosowania&NrKadencji=8&NrPosiedzenia=74&NrGlosowania=3"
        server.stubFor(
            WireMock.get(urlPath)
                .willReturn(WireMock.okXml(readFile("/voting_3_12-12-2018.html")))
        )
        //execute
        val voteOpener = VoteOpener(baseUrl = server.baseUrl())
        val votesUrlMap = voteOpener.fetchVotingUrlsForParties(
            (server.baseUrl() + urlPath).toHttpUrl()
        )
        Assertions.assertEquals(
            urlMapFromFile("/voting_3_12-12-2018.txt"),
            votesUrlMap
        )
    }

    @Test
    @Throws(Exception::class)
    fun testPartyVoteOpener() {
        //prepare
        val urlPath = "/agent.xsp?symbol=klubglos&IdGlosowania=50354&KodKlubu=N"
        server.stubFor(
            WireMock.get(urlPath)
                .willReturn(WireMock.okXml(readFile("/voting_3_party_N_12-12-2018.html")))
        )
        //execute
        val partyVoteOpener = PartyVoteOpener()
        val votesUrlMap = partyVoteOpener.fetchVotingUrlsForParties(
            Party("N"),
            (server.baseUrl() + urlPath).toHttpUrl()
        )
        Assertions.assertEquals(
            votesFromFile("/voting_3_party_N_12-12-2018.txt"),
            votesUrlMap.getVotes()
        )
    }

    private fun assertUrlList(actualVotes: List<HttpUrl>, expected: List<String>) {
        Assertions.assertEquals(
            listOrUrls(expected),
            sorted(actualVotes)
        )
    }

    private fun listOrUrls(urls: List<String>): List<HttpUrl> {
        return urls.stream()
            .sorted()
            .map { s: String -> toUrl(s) }
            .collect(Collectors.toList())
    }

    private fun toUrl(s: String): HttpUrl {
        return try {
            s.toHttpUrl()
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
    }

    private fun sorted(votesInDayUrls: List<HttpUrl>): List<HttpUrl> {
        return votesInDayUrls.stream()
            .sorted(Comparator.comparing { obj: HttpUrl -> obj.toString() })
            .collect(Collectors.toList())
    }

    private fun readFile(path: String): String {
        return readFileToStream(path).collect(Collectors.joining("\n"))
    }

    private fun urlsFromFile(path: String): List<String> {
        return readFileToStream(path)
            .map(this@VoteFetcherTests::replaceUrlTemplate)
            .toList()
    }

    private fun replaceUrlTemplate(s: String) = s.replace("{placeholder}", server.baseUrl())

    private fun urlMapFromFile(path: String): Map<Party, HttpUrl> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .collect(Collectors.toMap({ a -> Party(a.get(0)) }, { a -> replaceUrlTemplate(a.get(1)).toHttpUrl() }))
    }

    private fun votesFromFile(path: String): Map<Person, VoteResult> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .collect(Collectors.toMap({ a -> Person(a.get(0)) }, { a -> VoteResult.parse(a.get(1)) }))
    }

    private fun readFileToStream(path: String): Stream<String> {
        val resourceAsStream = javaClass.getResourceAsStream(path)
        Assertions.assertNotNull(resourceAsStream, "No file found $path")
        val reader = BufferedReader(InputStreamReader(resourceAsStream!!, Charsets.UTF_8))
        return reader.lines()
    }
}