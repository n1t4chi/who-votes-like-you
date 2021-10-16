import model.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vote.fetcher.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

class VoteFetcherOnlineTests {
    val baseUrl = "https://www.sejm.gov.pl/sejm8.nsf/"

    @Test
    @Throws(Exception::class)
    fun testVotingArchiveOpener() {
        val archiveOpener = VotingsArchiveOpener(baseUrl = baseUrl)
        val votesInDayUrls = archiveOpener.getVotesInDayUrls(7)
        assertUrlList(
            votesInDayUrls,
            urlsFromFile("/resultsVotingArchiveOpener.txt")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVotesInDayOpener() {
        val votesInDayOpener = VotesInDayOpener(baseUrl = baseUrl)
        val votingUrls = votesInDayOpener.fetchVotingUrls(
            "https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=listaglos&IdDnia=1707".toHttpUrl()
        )
        assertUrlList(
            votingUrls,
            urlsFromFile("/resultsVotesInDayOpener.txt")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVoteOpener() {
        val voteOpener = VoteOpener(baseUrl = baseUrl)
        val votesUrlMap = voteOpener.fetchVotingUrlsForParties(
            "https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=glosowania&NrKadencji=8&NrPosiedzenia=74&NrGlosowania=3".toHttpUrl()
        )
        Assertions.assertEquals(
            urlMapFromFile("/resultsVoteOpener.txt"),
            votesUrlMap
        )
    }

    @Test
    @Throws(Exception::class)
    fun testPartyVoteOpener() {
        val voteOpener = PartyVoteOpener()
        val votesUrlMap = voteOpener.fetchVotingUrlsForParties(
            Party("N"),
            "https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=klubglos&IdGlosowania=50354&KodKlubu=N".toHttpUrl()
        )
        Assertions.assertEquals(
            votesFromFile("/resultsPartyVoteOpener.txt"),
            votesUrlMap.getVotes()
        )
    }

    private fun assertUrlList(actualVotes: List<HttpUrl>, expected: List<String>) {
        Assertions.assertEquals(
            listOrUrls(expected),
            sorted(actualVotes)
        )
    }

    private fun listOrUrls(urls: List<String>): List<HttpUrl>? {
        return urls.stream()
            .sorted()
            .map { s: String -> s.toHttpUrl() }
            .collect(Collectors.toList())
    }


    private fun sorted(votesInDayUrls: List<HttpUrl>): List<HttpUrl> {
        return votesInDayUrls.stream()
            .sorted(Comparator.comparing { obj: HttpUrl -> obj.toString() })
            .collect(Collectors.toList())
    }

    private fun urlsFromFile(path: String): List<String> {
        return readFileToStream(path).toList()
    }

    private fun urlMapFromFile(path: String): Map<Party, HttpUrl> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .collect(Collectors.toMap({ a -> Party(a.get(0)) }, { a -> a.get(1).toHttpUrl() }))
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