package vote.fetcher.message.handlers

import message.system.MessageSystemTestBase
import model.Cadence
import org.junit.jupiter.api.*
import org.mockito.Mockito
import vote.fetcher.message.*
import vote.fetcher.services.AvailableCadenceResolver
import vote.storage.TestableVoteStorage
import java.util.concurrent.*

class ResolveCadencesHandlerTest: MessageSystemTestBase() {
    private val availableCadenceResolver = Mockito.mock(AvailableCadenceResolver::class.java)
    private val voteStorage = TestableVoteStorage()
    private val resolveCadencesHandler = ResolveCadencesHandler(messageSystem, availableCadenceResolver, voteStorage)
    private val newCadences = mutableSetOf<NewCadence>()
    private val currentCadences = mutableSetOf<CurrentCadence>()
    
    @BeforeEach
    fun setupQueues() {
        defineQueue(ResolveCadences::class.java)
        defineQueue(NewCadence::class.java)
        defineQueue(CurrentCadence::class.java)
        subscribe(ResolveCadences::class.java, 0, resolveCadencesHandler)
        subscribe(NewCadence::class.java, 2) { msg -> newCadences.add(msg) }
        subscribe(CurrentCadence::class.java, 2) { msg -> currentCadences.add(msg) }
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given empty system, on ResolveCadences message, cadences are resolved as NewCadence and fired back into message system`() {
        //prepare
        Mockito.`when`(availableCadenceResolver.getCurrentCadences()).thenReturn(listOf(
            Cadence(1),
            Cadence(2),
            Cadence(3)
        ))
        
        //execute
        sendMessage(ResolveCadences())
        
        //wait for tasks
        waitForCurrentTasks()
    
        //verify
        Assertions.assertEquals(
            setOf(
                NewCadence(Cadence(1)),
                NewCadence(Cadence(2)),
                NewCadence(Cadence(3)),
            ),
            newCadences
        )
        Assertions.assertEquals(setOf<CurrentCadence>(), currentCadences)
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given system with cadences, on ResolveCadences message, when new cadence is available, only sends new one as NewCadence into message system`() {
        //prepare
        voteStorage.saveCadences(Cadence(1), Cadence(2))
        Mockito.`when`(availableCadenceResolver.getCurrentCadences()).thenReturn(listOf(
            Cadence(1),
            Cadence(2),
            Cadence(3)
        ))
        
        //execute
        sendMessage(ResolveCadences())
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        Assertions.assertEquals(
            setOf(NewCadence(Cadence(3)),),
            newCadences
        )
        Assertions.assertEquals(setOf<CurrentCadence>(), currentCadences)
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun `given system with cadences, on ResolveCadences message, when same cadences are available, only sends last one as CurrentCadence into message system`() {
        //prepare
        voteStorage.saveCadences(Cadence(1), Cadence(2), Cadence(3))
        Mockito.`when`(availableCadenceResolver.getCurrentCadences()).thenReturn(listOf(
            Cadence(1),
            Cadence(2),
            Cadence(3)
        ))
        
        //execute
        sendMessage(ResolveCadences())
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        Assertions.assertEquals(setOf<NewCadence>(), newCadences)
        Assertions.assertEquals(setOf(CurrentCadence(Cadence(3))), currentCadences)
    }
}