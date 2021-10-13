import model.*
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vote.fetcher.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

class VoteFetcherOnlineTests {
    val baseUrl = "https://www.sejm.gov.pl/sejm8.nsf/"

    @Test
    @Throws(Exception::class)
    fun testVotingArchiveOpener() {
        val client = OkHttpClient()
        val archiveOpener = VotingsArchiveOpener(client,baseUrl)
        val votesInDayUrls = archiveOpener.getVotesInDayUrls(7)
        assertUrlList(
            votesInDayUrls,
            urlsFromFile("/resultsVotingArchiveOpener.txt")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVotesInDayOpener() {
        val client = OkHttpClient()
        val archiveOpener = VotesInDayOpener(client,baseUrl)
        val votingUrls = archiveOpener.fetchVotingUrls(
            RestUtil.toUrl("https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=listaglos&IdDnia=1707")
        )
        assertUrlList(
            votingUrls,
            urlsFromFile("/resultsVotesInDayOpener.txt")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVoteOpener() {
        val client = OkHttpClient()
        val voteOpener = VoteOpener(client,baseUrl)
        val votesUrlMap = voteOpener.fetchVotingUrlsForParties(
            RestUtil.toUrl("https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=glosowania&NrKadencji=8&NrPosiedzenia=74&NrGlosowania=3")
        )
        Assertions.assertEquals(
            urlMapFromFile("/resultsVoteOpener.txt"),
            votesUrlMap
        )
    }

    @Test
    @Throws(Exception::class)
    fun testPartyVoteOpener() {
        val client = OkHttpClient()
        val voteOpener = PartyVoteOpener(client,baseUrl)
        val votesUrlMap = voteOpener.fetchVotingUrlsForParties(
            Party("N"),
            RestUtil.toUrl("https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=klubglos&IdGlosowania=50354&KodKlubu=N")
        )
        Assertions.assertEquals(
            votesFromFile("/resultsPartyVoteOpener.txt"),
            votesUrlMap.getVotes()
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


    private fun sorted(votesInDayUrls: List<URL>): List<URL> {
        return votesInDayUrls.stream()
            .sorted(Comparator.comparing { obj: URL -> obj.toString() })
            .collect(Collectors.toList())
    }

    private fun toUrl(s: String): URL {
        return try {
            URL(s)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
    }

    private fun urlsFromFile(path: String): List<String> {
        return readFileToStream(path).toList()
    }

    private fun urlMapFromFile(path: String): Map<Party, URL> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .collect(Collectors.toMap({ a -> Party(a.get(0)) }, { a -> URL(a.get(1)) }))
    }

    private fun votesFromFile(path: String): Map<Person, VoteResult> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}") ).toList() }
            .collect( Collectors.toMap( { a -> Person( a.get(0) ) }, { a -> VoteResult.parse( a.get(1) ) } ) )
    }

    private fun readFileToStream(path: String): Stream<String> {
        val resourceAsStream = javaClass.getResourceAsStream(path)
        Assertions.assertNotNull(resourceAsStream, "No file found $path")
        val reader = BufferedReader(InputStreamReader(resourceAsStream!!, Charsets.UTF_8))
        return reader.lines()
    }
}