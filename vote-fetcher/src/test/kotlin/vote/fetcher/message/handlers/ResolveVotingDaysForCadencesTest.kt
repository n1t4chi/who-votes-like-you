package vote.fetcher.message.handlers

import message.system.MessageSystemTestBase
import model.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.*
import org.mockito.Mockito
import vote.fetcher.data.VotingDayWithUrl
import vote.fetcher.message.*
import vote.fetcher.services.VotingsArchiveOpener
import vote.storage.TestableVoteStorage
import java.time.LocalDate
import java.util.concurrent.*

class ResolveVotingDaysForCadencesTest: MessageSystemTestBase() {
    companion object {
        val cadence1 = Cadence(1, 0)
        val votingsInDay11 = VotingDay(cadence1, LocalDate.of(2000,1,1),2)
        val votingDayWithUrl11 = VotingDayWithUrl(votingsInDay11, "http://voting.11".toHttpUrl())
        val votingsInDay12 = VotingDay(cadence1, LocalDate.of(2000,2,2),3)
        val votingDayWithUrl12 = VotingDayWithUrl(votingsInDay12, "http://voting.12".toHttpUrl())
    }
    private val votingsArchiveOpener = Mockito.mock(VotingsArchiveOpener::class.java)
    private val voteStorage = TestableVoteStorage()
    private val resolveCadencesHandler = ResolveVotingDaysForCadencesHandler(messageSystem, votingsArchiveOpener, voteStorage)
    private val receivedVotingDaysWithUrl = mutableSetOf<VotingDayWithUrl>()
    
    @BeforeEach
    fun setupQueues() {
        defineQueue(NewCadence::class.java)
        defineQueue(CurrentCadence::class.java)
        defineQueue(VotingDayWithUrl::class.java)
        subscribe(NewCadence::class.java, 0, resolveCadencesHandler)
        subscribe(CurrentCadence::class.java, 0, resolveCadencesHandler)
        subscribe(VotingDayWithUrl::class.java, 2) { msg -> receivedVotingDaysWithUrl.add(msg) }
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given empty system, on NewCadence message, voting days with URLS are resolved for it and fired back into message system`() {
        //prepare
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenReturn(listOf(votingDayWithUrl11, votingDayWithUrl12))
        
        //execute
        sendMessage(NewCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
    
        //verify
        Assertions.assertEquals(
            setOf(votingDayWithUrl11, votingDayWithUrl12),
            receivedVotingDaysWithUrl
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given empty system, on CurrentCadence message, voting days with URLS are resolved for it and fired back into message system`() {
        //prepare
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenReturn(listOf(votingDayWithUrl11, votingDayWithUrl12))
        
        //execute
        sendMessage(CurrentCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        Assertions.assertEquals(
            setOf(votingDayWithUrl11, votingDayWithUrl12),
            receivedVotingDaysWithUrl
        )
    }
}