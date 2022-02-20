package vote.fetcher.message.handlers

import message.system.AssertionsExt
import message.system.MessageSystemTestBase
import message.system.RestartTask
import model.Cadence
import model.CadenceStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.Mockito
import vote.fetcher.message.CurrentCadence
import vote.fetcher.message.NewCadence
import vote.fetcher.message.ResolveCadences
import vote.fetcher.services.AvailableCadenceResolver
import vote.storage.TestableVoteStorage
import java.util.*
import java.util.concurrent.TimeUnit

class ResolveCadencesHandlerTest : MessageSystemTestBase() {
    private val availableCadenceResolver = Mockito.mock(AvailableCadenceResolver::class.java)
    private val voteStorage = TestableVoteStorage()
    private val resolveCadencesHandler = ResolveCadencesHandler(messageSystem, availableCadenceResolver, voteStorage)
    private val newCadences = Collections.synchronizedSet(HashSet<NewCadence>())
    private val currentCadences = Collections.synchronizedSet(HashSet<CurrentCadence>())
    
    @BeforeEach
    fun setupQueues() {
        defineQueue(ResolveCadences::class.java)
        defineQueue(NewCadence::class.java)
        defineQueue(CurrentCadence::class.java)
        subscribe("ResolveCadences Handler", ResolveCadences::class.java, 0, resolveCadencesHandler)
        subscribe("NewCadence collector", NewCadence::class.java, 2) { msg ->
            synchronized(newCadences) {
                logger.trace { "NewCadence collector inserting into collection: $msg" }
                newCadences.add(msg)
            }
        }
        subscribe("CurrentCadence collector", CurrentCadence::class.java, 2) { msg ->
            synchronized(newCadences) {
                logger.trace { "CurrentCadence collector inserting into collection: $msg" }
                currentCadences.add(msg)
            }
        }
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given empty system, on ResolveCadences message, when no cadence is received, nothing happens`() {
        //prepare
        Mockito.`when`(availableCadenceResolver.getCurrentCadences())
            .thenReturn(listOf())
        
        //execute
        sendMessage(ResolveCadences())
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        AssertionsExt.assertEmpty(voteStorage.getCadences(), "cadences in DB")
        AssertionsExt.assertEmpty(newCadences, "new cadences")
        AssertionsExt.assertEmpty(currentCadences, "current cadences")
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given empty system, on ResolveCadences message, cadences are resolved, saved to DB with newest being marked as current, and then fired back into message system as NewCadence`() {
        //prepare
        Mockito.`when`(availableCadenceResolver.getCurrentCadences())
            .thenReturn(
                listOf(
                    Cadence(1),
                    Cadence(2),
                    Cadence(3)
                )
            )
        
        //execute
        sendMessage(ResolveCadences())
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        AssertionsExt.assertUnorderedEquals(
            setOf(
                Cadence(1, CadenceStatus.old, -1),
                Cadence(2, CadenceStatus.old, -1),
                Cadence(3, CadenceStatus.active, -1),
            ),
            voteStorage.getCadences(),
        )
        AssertionsExt.assertUnorderedEquals(
            setOf(
                NewCadence(Cadence(1, CadenceStatus.old, -1)),
                NewCadence(Cadence(2, CadenceStatus.old, -1)),
                NewCadence(Cadence(3, CadenceStatus.active, -1)),
            ),
            newCadences
        )
        AssertionsExt.assertEmpty(currentCadences, "current cadences")
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given system with cadences, on ResolveCadences message, when new cadence is available, update sends old one as ActiveCadence and current as NewCadence into message system then updates previous active and old`() {
        //prepare
        voteStorage.saveCadences(
            Cadence(1, CadenceStatus.old, 2),
            Cadence(2, CadenceStatus.active, 2)
        )
        Mockito.`when`(availableCadenceResolver.getCurrentCadences())
            .thenReturn(
                listOf(
                    Cadence(1),
                    Cadence(2),
                    Cadence(3)
                )
            )
        
        //execute
        sendMessage(ResolveCadences())
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoRestarts()
        assertNoExceptions()
        AssertionsExt.assertUnorderedEquals(
            setOf(
                Cadence(1, CadenceStatus.old, 2),
                Cadence(2, CadenceStatus.old, 2),
                Cadence(3, CadenceStatus.active, -1),
            ),
            voteStorage.getCadences(),
        )
        AssertionsExt.assertUnorderedEquals(
            setOf(NewCadence(Cadence(3, CadenceStatus.active, -1))),
            newCadences
        )
        AssertionsExt.assertUnorderedEquals(
            setOf(CurrentCadence(Cadence(2, CadenceStatus.old, 2))),
            currentCadences
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given system with cadences, on ResolveCadences message, when same cadences are available, only sends last one as CurrentCadence into message system`() {
        //prepare
        voteStorage.saveCadences(
            Cadence(1, CadenceStatus.old, 2),
            Cadence(2, CadenceStatus.old, 2),
            Cadence(3, CadenceStatus.active, 2)
        )
        Mockito.`when`(availableCadenceResolver.getCurrentCadences())
            .thenReturn(
                listOf(
                    Cadence(1),
                    Cadence(2),
                    Cadence(3)
                )
            )
    
        //execute
        sendMessage(ResolveCadences())
    
        //wait for tasks
        waitForCurrentTasks()
    
        //verify
        assertNoRestarts()
        assertNoExceptions()
        AssertionsExt.assertUnorderedEquals(
            setOf(
                Cadence(1, CadenceStatus.old, 2),
                Cadence(2, CadenceStatus.old, 2),
                Cadence(3, CadenceStatus.active, 2),
            ),
            voteStorage.getCadences(),
        )
        AssertionsExt.assertEmpty(newCadences, "new cadences")
        AssertionsExt.assertUnorderedEquals(
            setOf(CurrentCadence(Cadence(3, CadenceStatus.active, 2))),
            currentCadences
        )
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `when exception is thrown, restarts task and does not change system state`() {
        //prepare
        voteStorage.saveCadences(
            Cadence(1, CadenceStatus.old, 2),
            Cadence(2, CadenceStatus.active, -1)
        )
        Mockito.`when`(availableCadenceResolver.getCurrentCadences())
            .thenThrow(RuntimeException("Connection timed out"))
        
        //execute
        sendMessage(ResolveCadences())
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        assertNoExceptions()
        logger.trace { "Asserting restarts" }
        assertRestarts(RestartTask(ResolveCadences()))
        AssertionsExt.assertUnorderedEquals(
            setOf(
                Cadence(1, CadenceStatus.old, 2),
                Cadence(2, CadenceStatus.active, -1)
            ),
            voteStorage.getCadences(),
        )
        AssertionsExt.assertEmpty(newCadences, "new cadences")
        AssertionsExt.assertEmpty(currentCadences, "current cadences")
    }
}