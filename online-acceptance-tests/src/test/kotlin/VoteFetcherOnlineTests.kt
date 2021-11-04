import model.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vote.fetcher.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        Assertions.assertEquals(
            votesInDayUrls,
            map("/resultsVotingArchiveOpener.txt", this::toDateAndUrl )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testVotesInDayOpener() {
        val votesInDayOpener = VotesInDayOpener(baseUrl = baseUrl)
        val date = LocalDate.of(2001, 1, 1)
        val votingUrls = votesInDayOpener.fetchVotingUrls(
            "https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=listaglos&IdDnia=1707".toHttpUrl(),
            date
        )
    
        Assertions.assertEquals(
            votingUrls,
            map("/resultsVotesInDayOpener.txt") { this.toVotingAndUrl(it, date) }
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
    
    private fun <T> map(path: String, mapper: (List<String>) -> T): List<T> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .map(mapper)
            .toList()
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
    
    fun toDateAndUrl(strings: List<String>): Pair<LocalDate, HttpUrl> {
        return toDate(strings[0]) to strings[1].toHttpUrl()
    }
    
    private fun toDate(String: String): LocalDate {
        return LocalDate.parse( String, DateTimeFormatter.ofPattern( "yyyy-MM-dd" ) )
    }
    
    fun toVotingAndUrl(strings: List<String>, date: LocalDate): Pair<Voting, HttpUrl> {
        return Pair(
            Voting(strings[2], strings[0].toInt(), date),
            strings[1].toHttpUrl()
        )
    }
}