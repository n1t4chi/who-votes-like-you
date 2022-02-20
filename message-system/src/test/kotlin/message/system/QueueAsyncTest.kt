package message.system

import log.WhoLogger
import message.executor.PriorityExecutor
import message.executor.PriorityExecutorImpl
import org.junit.jupiter.api.*
import java.time.Duration
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class QueueAsyncTest {
    private val logger = WhoLogger {}
    private val executor: PriorityExecutor = PriorityExecutorImpl()
    private val queue = Queue(TestMessage::class.java, executor)
    
    @BeforeEach
    internal fun setUp() {
        executor.start()
    }
    
    @AfterEach
    internal fun tearDown() {
        executor.shutdown()
    }
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun sendingMessageToSubscriberIsDoneAsynchronously() {
        //prepare
        val messageHolder = AtomicReference<TestMessage>()
        val beforeSetMessageSemaphore = Semaphore(0)
        val afterSetMessageSemaphore = Semaphore(0)
        queue.addSubscriber { message ->
            logger.trace { "beforeSetMessageSemaphore.acquire()" }
            beforeSetMessageSemaphore.acquire()
            logger.trace { "messageHolder.set(message)" }
            messageHolder.set(message)
            logger.trace { "afterSetMessageSemaphore.release()" }
            afterSetMessageSemaphore.release()
        }
        val testMessage = TestMessage("superDuperMessage")
        
        //execute
        Assertions.assertTimeoutPreemptively(
            Duration.ofMillis(200),
            {queue.receive(testMessage)},
            "Receiving message did not quickly complete execution."
        )
        
        //verify that message was not updated before releasing the semaphore
        Assertions.assertEquals(null, messageHolder.get())
        
        //release the semaphore for subscriber to set message and wait for another semaphore to continue verification
        logger.trace { "beforeSetMessageSemaphore.release()" }
        beforeSetMessageSemaphore.release()
        if(!afterSetMessageSemaphore.tryAcquire(200,TimeUnit.MILLISECONDS))
            Assertions.fail<Any>("Subscriber did not release after set message semaphore in time")
        
        //verify new message
        Assertions.assertEquals(testMessage, messageHolder.get())
    }
}