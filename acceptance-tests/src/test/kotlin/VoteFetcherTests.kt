import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import okhttp3.OkHttpClient
import org.junit.jupiter.api.*
import vote.fetcher.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.Comparator
import java.util.stream.Collectors
import java.util.stream.Stream


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
    fun testVotingArchiveOpener() {
        server.stubFor(
            WireMock.get("/agent.xsp?symbol=posglos&NrKadencji=7")
                .willReturn(WireMock.okXml(readFile("/cadence7.html")))
        )
        val archiveOpener = VotingsArchiveOpener( baseUrl = server.baseUrl() )
        val votesInDayUrls = archiveOpener.getVotesInDayUrls(7)
        assertUrlList(
            votesInDayUrls,
            urlsFromFile("/cadence7.txt" )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVotesInDayOpener() {
        server.stubFor(
            WireMock.get("/agent.xsp?symbol=listaglos&IdDnia=1707")
                .willReturn(WireMock.okXml(readFile("/votes12-12-2018.html")))
        )
        val archiveOpener = VotesInDayOpener( baseUrl = server.baseUrl() )
        val votingUrls = archiveOpener.fetchVotingUrls(
            RestUtil.toUrl(server.baseUrl() + "/agent.xsp?symbol=listaglos&IdDnia=1707" )
        )
        assertUrlList(
            votingUrls,
            urlsFromFile("/votes12-12-2018.txt" )
        )
    }

    private fun assertUrlList(actualVotes: List<URL>, expected: List<String>) {
        Assertions.assertEquals(
            listOrUrls(expected),
            sorted(actualVotes)
        )
    }

    private fun listOrUrls(urls: List<String>): List<URL>? {
        return urls.stream()
            .sorted()
            .map { s: String -> toUrl(s) }
            .collect(Collectors.toList())
    }

    private fun toUrl(s: String): URL {
        return try {
            URL(s)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
    }

    private fun sorted(votesInDayUrls: List<URL>): List<URL> {
        return votesInDayUrls.stream()
            .sorted(Comparator.comparing { obj: URL -> obj.toString() })
            .collect(Collectors.toList())
    }

    private fun readFile(path: String): String {
        return readFileToStream(path).collect(Collectors.joining("\n"))
    }

    private fun urlsFromFile(path: String): List<String> {
        return readFileToStream(path)
            .map { s -> s.replace( "{placeholder}", server.baseUrl() ) }
            .toList()
    }

    private fun urlMapFromFile(path: String): Map<Party, URL> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .collect(Collectors.toMap({ a -> Party(a.get(0)) }, { a -> URL(a.get(1)) }))
    }

    private fun votesFromFile(path: String): Map<Person, Vote> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .collect(Collectors.toMap({ a -> Person(a.get(0)) }, { a -> Vote.parse(a.get(1)) }))
    }

    private fun readFileToStream(path: String): Stream<String> {
        val resourceAsStream = javaClass.getResourceAsStream(path)
        Assertions.assertNotNull(resourceAsStream, "No file found $path")
        val reader = BufferedReader(InputStreamReader(resourceAsStream!!, Charsets.UTF_8))
        return reader.lines()
    }
}