import model.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vote.fetcher.data.PartyVotingReference
import vote.fetcher.data.VotingDayWithUrl
import vote.fetcher.data.VotingWithUrl
import vote.fetcher.restclient.RestClientImpl
import vote.fetcher.services.PartyVoteOpener
import vote.fetcher.services.VoteOpener
import vote.fetcher.services.VotingsArchiveOpener
import vote.fetcher.services.VotingsInDayOpener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import java.util.stream.Stream

class VoteFetcherOnlineTests {
    val baseUrl = "https://www.sejm.gov.pl/sejm8.nsf/"
    
    @Test
    @Throws(Exception::class)
    fun testVotingArchiveOpener() {
        val archiveOpener = VotingsArchiveOpener(baseUrl.toHttpUrl(), RestClientImpl)
        val cadence = Cadence(7)
        val votesInDayUrls = archiveOpener.getVotingsInDayUrls(cadence)
        Assertions.assertEquals(
            votesInDayUrls,
            map("/resultsVotingArchiveOpener.txt") { this.toVotingsInDay(cadence, it) }
                .collect(Collectors.toList())
        )
    }
    
    @Test
    @Throws(Exception::class)
    fun testVotesInDayOpener() {
        val votingsInDayOpener = VotingsInDayOpener(baseUrl.toHttpUrl(), RestClientImpl)
        val date = LocalDate.of(2001, 1, 1)
        val cadence = Cadence(1)
        val votingUrls = votingsInDayOpener.fetchVotingUrls(
            VotingDayWithUrl(
                VotingDay(cadence, date),
                "https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=listaglos&IdDnia=1707".toHttpUrl()
            )
        )
        
        Assertions.assertEquals(
            votingUrls,
            map("/resultsVotesInDayOpener.txt") { this.toVotingAndUrl(it, cadence, date) }
                .collect(Collectors.toList())
        )
    }
    
    @Test
    @Throws(Exception::class)
    fun testVoteOpener() {
        val voteOpener = VoteOpener(baseUrl.toHttpUrl(), RestClientImpl)
        val voting = Voting("Głosowanie1", 1, VotingDay(Cadence(1), LocalDate.now()))
        val votesUrlMap = voteOpener.fetchVotingUrlsForParties(
            VotingWithUrl(
                voting,
                "https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=glosowania&NrKadencji=8&NrPosiedzenia=74&NrGlosowania=3".toHttpUrl()
            )
        )
        Assertions.assertEquals(
            votingUrlsForParties(voting, "/resultsVoteOpener.txt"),
            votesUrlMap
        )
    }
    
    @Test
    @Throws(Exception::class)
    fun testPartyVoteOpener() {
        val partyVoteOpener = PartyVoteOpener(RestClientImpl)
        val voting = Voting("Głosowanie1", 1, VotingDay(Cadence(1), LocalDate.now()))
        val votesUrlMap = partyVoteOpener.fetchVotesForParty(
            PartyVotingReference(
                voting,
                Party("N"),
                "https://www.sejm.gov.pl/sejm8.nsf/agent.xsp?symbol=klubglos&IdGlosowania=50354&KodKlubu=N".toHttpUrl()
            )
        )
        Assertions.assertEquals(
            votesFromFile("/resultsPartyVoteOpener.txt"),
            votesUrlMap.getVotes()
        )
    }
    
    private fun <T> map(path: String, mapper: (List<String>) -> T): Stream<T> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .map(mapper)
    }
    
    private fun votingUrlsForParties(voting: Voting, path: String): Set<PartyVotingReference> {
        return readFileToStream(path)
            .map { s -> s.split(Regex("\\s{2,}")).toList() }
            .map { a -> PartyVotingReference(voting, Party(a.get(0)), a.get(1).toHttpUrl()) }
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
    
    fun toVotingsInDay(cadence: Cadence, strings: List<String>): VotingDayWithUrl {
        return VotingDayWithUrl(VotingDay(cadence, toDate(strings[0])), strings[1].toHttpUrl())
    }
    
    private fun toDate(String: String): LocalDate {
        return LocalDate.parse(String, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    
    fun toVotingAndUrl(strings: List<String>, cadence: Cadence, date: LocalDate): VotingWithUrl {
        return VotingWithUrl(
            Voting(strings[2], strings[0].toInt(), VotingDay(cadence, date)),
            strings[1].toHttpUrl()
        )
    }
}