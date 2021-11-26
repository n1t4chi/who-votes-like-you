import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import model.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.*
import vote.fetcher.*
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.*
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
        val cadenceResolver = AvailableCadenceResolver(server.baseUrl().toHttpUrl(),RestClientImpl)
        //verify
        val cadences = cadenceResolver.getCurrentCadences()
        Assertions.assertEquals(
            listOf(Cadence(7,0), Cadence(8,0)),
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
        val archiveOpener = VotingsArchiveOpener(server.baseUrl().toHttpUrl(),RestClientImpl)
        val cadence = Cadence(7,0)
        val votesInDayUrls = archiveOpener.getVotingsInDayUrls(cadence)
        Assertions.assertEquals(
            votesInDayUrls,
            map("/cadence_7.txt") { this.toVotingsInDay(cadence, it) }
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
        val archiveOpener = VotingsInDayOpener(server.baseUrl().toHttpUrl(),RestClientImpl)
        val date = LocalDate.of(2001, 1, 1)
        val cadence = Cadence(1,0)
        val votingUrls = archiveOpener.fetchVotingUrls(
            VotingsInDay(
                cadence,
                date,
                (server.baseUrl() + "/agent.xsp?symbol=listaglos&IdDnia=1707").toHttpUrl()
            )
        )
        
        Assertions.assertEquals(
            votingUrls,
            map("/votings_12-12-2018.txt") { this.toVotingAndUrl(it, cadence, date) }
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
        val voteOpener = VoteOpener(server.baseUrl().toHttpUrl(),RestClientImpl)
        val voting = Voting("Głosowanie1", 1, Cadence(1,0), LocalDate.now(),0)
        val votesUrlMap = voteOpener.fetchVotingUrlsForParties(
            VotingWithUrl(voting, (server.baseUrl() + urlPath).toHttpUrl())
        
        )
        Assertions.assertEquals(
            votingUrlsForParties(voting, "/voting_3_12-12-2018.txt"),
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
        val partyVoteOpener = PartyVoteOpener(RestClientImpl)
        val voting = Voting("Głosowanie1", 1, Cadence(1,0), LocalDate.now(),0)
        val votesUrlMap = partyVoteOpener.fetchVotesForParty(
            PartyVotingReference(
                voting,
                Party("N"),
                (server.baseUrl() + urlPath).toHttpUrl()
            )
        )
        Assertions.assertEquals(
            votesFromFile("/voting_3_party_N_12-12-2018.txt"),
            votesUrlMap.getVotes()
        )
    }
    
    private fun readFile(path: String): String {
        return readFileToStream(path).collect(Collectors.joining("\n"))
    }
    
    private fun <T> map(path: String, mapper: (List<String>) -> T): List<T> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .map(mapper)
            .toList()
    }
    
    private fun replaceUrlTemplate(s: String) = s.replace("{placeholder}", server.baseUrl())
    
    private fun votingUrlsForParties(voting: Voting, path: String): Set<PartyVotingReference> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .map { a -> PartyVotingReference(voting, Party(a.get(0)), replaceUrlTemplate(a.get(1)).toHttpUrl()) }
            .collect(Collectors.toSet())
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
    
    fun toVotingsInDay(cadence: Cadence, strings: List<String>): VotingsInDay {
        return VotingsInDay(cadence, toDate(strings[0]), replaceUrlTemplate(strings[1]).toHttpUrl())
    }
    
    private fun toDate(string: String): LocalDate {
        return LocalDate.parse(string, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    
    fun toVotingAndUrl(strings: List<String>, cadence: Cadence, date: LocalDate): VotingWithUrl {
        return VotingWithUrl(
            Voting(strings[2], strings[0].toInt(), cadence, date,0),
            replaceUrlTemplate(strings[1]).toHttpUrl()
        )
    }
}