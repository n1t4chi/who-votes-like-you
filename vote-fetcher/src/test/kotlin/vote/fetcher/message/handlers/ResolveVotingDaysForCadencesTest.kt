package vote.fetcher.message.handlers

import message.system.MessageSystemTestBase
import message.system.RestartTask
import model.Cadence
import model.CadenceStatus
import model.VotingDay
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.Mockito
import vote.fetcher.data.VotingDayWithUrl
import vote.fetcher.message.CurrentCadence
import vote.fetcher.message.NewCadence
import vote.fetcher.services.VotingsArchiveOpener
import vote.storage.TestableVoteStorage
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

class ResolveVotingDaysForCadencesTest: MessageSystemTestBase() {
    companion object {
        val cadence1 = Cadence(1)
        val votingDay11 = VotingDay(cadence1, LocalDate.of(2000, 1, 1), 2)
        val votingDayWithUrl11 = VotingDayWithUrl(votingDay11, "http://voting.11".toHttpUrl())
        val votingDay12 = VotingDay(cadence1, LocalDate.of(2000, 2, 2), 3)
        val votingDayWithUrl12 = VotingDayWithUrl(votingDay12, "http://voting.12".toHttpUrl())
    }
    
    private val votingsArchiveOpener = Mockito.mock(VotingsArchiveOpener::class.java)
    private val voteStorage = TestableVoteStorage()
    private val resolveCadencesHandler =
        ResolveVotingDaysForCadencesHandler(messageSystem, votingsArchiveOpener, voteStorage)
    private val receivedVotingDaysWithUrl = Collections.synchronizedSet(HashSet<VotingDayWithUrl>())
    
    @BeforeEach
    fun setupQueues() {
        defineQueue(NewCadence::class.java)
        defineQueue(CurrentCadence::class.java)
        defineQueue(VotingDayWithUrl::class.java)
        subscribe("NewCadence Handler", NewCadence::class.java, 0, resolveCadencesHandler)
        subscribe("CurrentCadence Handler", CurrentCadence::class.java, 0, resolveCadencesHandler)
        subscribe("VotingDayWithUrl collector", VotingDayWithUrl::class.java, 2) { msg ->
            logger.trace { "VotingDayWithUrl collector inserting into collection: $msg" }
            synchronized(receivedVotingDaysWithUrl) { receivedVotingDaysWithUrl.add(msg) }
        }
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given uninitialized cadence, on NewCadence message, when no votings are received, updates cadence with 0 votes`() {
        //prepare
        voteStorage.saveCadences(Cadence(1, CadenceStatus.active, -1))
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenReturn(listOf())
        
        //execute
        sendMessage(CurrentCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        Assertions.assertEquals(emptySet<VotingDayWithUrl>(), receivedVotingDaysWithUrl)
        Assertions.assertEquals(
            Cadence(1, CadenceStatus.active, 0),
            voteStorage.getCadence(1)
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given system with CurrentCadence with only, on CurrentCadence message, when no new votings are received, does nothing`() {
        //prepare
        voteStorage.saveCadences(Cadence(1, CadenceStatus.active, 0))
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenReturn(listOf())
        
        //execute
        sendMessage(CurrentCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        Assertions.assertEquals(emptySet<VotingDayWithUrl>(), receivedVotingDaysWithUrl)
        Assertions.assertEquals(
            Cadence(1, CadenceStatus.active, 0),
            voteStorage.getCadence(1)
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given unitialized cadence, on NewCadence message, voting days with URLS are resolved for it and fired back into message system and Cadence is updated with amount of days`() {
        //prepare
        voteStorage.saveCadences(Cadence(1, CadenceStatus.active, -1))
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenReturn(listOf(votingDayWithUrl11, votingDayWithUrl12))
        
        //execute
        sendMessage(NewCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        logger.trace { "Asserting receivedVotingDaysWithUrl" }
        Assertions.assertEquals(
            setOf(votingDayWithUrl11, votingDayWithUrl12),
            receivedVotingDaysWithUrl
        )
        Assertions.assertEquals(
            Cadence(1, CadenceStatus.active, 2),
            voteStorage.getCadence(1)
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given cadence with no votes, on CurrentCadence message, voting days with URLS are resolved for it and fired back into message system and Cadence is updated with amount of days`() {
        //prepare
        voteStorage.saveCadences(Cadence(1, CadenceStatus.active, 0))
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenReturn(listOf(votingDayWithUrl11, votingDayWithUrl12))
        
        //execute
        sendMessage(CurrentCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        logger.trace { "Asserting receivedVotingDaysWithUrl" }
        Assertions.assertEquals(
            setOf(votingDayWithUrl11, votingDayWithUrl12),
            receivedVotingDaysWithUrl
        )
        Assertions.assertEquals(
            Cadence(1, CadenceStatus.active, 2),
            voteStorage.getCadence(1)
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given already up to date system, on CurrentCadence message, nothing is sent`() {
        //prepare
        val cadenceInDb = Cadence(1, CadenceStatus.active, 2)
        voteStorage.saveCadence(cadenceInDb)
        voteStorage.saveVotingDay(VotingDay(cadenceInDb, LocalDate.of(2000, 1, 1), 2))
        voteStorage.saveVotingDay(VotingDay(cadenceInDb, LocalDate.of(2000, 2, 2), 3))
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenReturn(listOf(votingDayWithUrl11, votingDayWithUrl12))
        
        //execute
        sendMessage(CurrentCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        Assertions.assertEquals(emptySet<VotingDayWithUrl>(), receivedVotingDaysWithUrl)
        Assertions.assertEquals(
            cadenceInDb,
            voteStorage.getCadence(1)
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given outdated system, on CurrentCadence message, missing voting days with URLS are resolved for it and fired back into message system and Cadence is updated with amount of days`() {
        //prepare
        val cadenceInDb = Cadence(1, CadenceStatus.active, 2)
        voteStorage.saveCadence(cadenceInDb)
        voteStorage.saveVotingDay(VotingDay(cadenceInDb, LocalDate.of(2000, 1, 1), 2))
        voteStorage.saveVotingDay(VotingDay(cadenceInDb, LocalDate.of(2000, 2, 2), 3))
        
        val votingsInDay13 = VotingDay(cadence1, LocalDate.of(2000, 3, 3), 4)
        val votingDayWithUrl13 = VotingDayWithUrl(votingsInDay13, "http://voting.13".toHttpUrl())
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenReturn(listOf(votingDayWithUrl11, votingDayWithUrl12, votingDayWithUrl13))
        
        //execute
        sendMessage(CurrentCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        logger.trace { "Asserting receivedVotingDaysWithUrl" }
        Assertions.assertEquals(
            setOf(votingDayWithUrl13),
            receivedVotingDaysWithUrl
        )
        Assertions.assertEquals(
            Cadence(1, CadenceStatus.active, 3),
            voteStorage.getCadence(1)
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `when exception is thrown, restarts task`() {
        //prepare
        voteStorage.saveCadence(Cadence(1, CadenceStatus.active, -1))
        Mockito.`when`(votingsArchiveOpener.getVotingsInDayUrls(cadence1))
            .thenThrow(RuntimeException("Connection timed out"))
        
        //execute
        sendMessage(CurrentCadence(cadence1))
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoExceptions()
        logger.trace { "Asserting restarts" }
        assertRestarts(RestartTask(CurrentCadence(cadence1)))
        Assertions.assertEquals(emptySet<VotingDayWithUrl>(), receivedVotingDaysWithUrl)
        Assertions.assertEquals(
            Cadence(1, CadenceStatus.active, -1),
            voteStorage.getCadence(1)
        )
    }
}