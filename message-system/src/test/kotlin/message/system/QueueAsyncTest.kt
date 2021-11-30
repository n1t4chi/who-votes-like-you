package message.system

import org.junit.jupiter.api.*
import java.time.Duration
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

class QueueAsyncTest {
    private val executor: SystemExecutor = NewThreadExecutor()
    private val queue = Queue(TestMessage::class.java,executor)
    
    
    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    fun sendingMessageToSubscriberIsDoneAsynchronously() {
        //prepare
        val messageHolder = AtomicReference<TestMessage>()
        val beforeSetMessageSemaphore = Semaphore(0)
        val afterSetMessageSemaphore = Semaphore(0)
        queue.addSubscriber{ message ->
            println("beforeSetMessageSemaphore.acquire()")
            beforeSetMessageSemaphore.acquire()
            println("messageHolder.set(message)")
            messageHolder.set(message)
            println("afterSetMessageSemaphore.release()")
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
        println("beforeSetMessageSemaphore.release()")
        beforeSetMessageSemaphore.release()
        if(!afterSetMessageSemaphore.tryAcquire(200,TimeUnit.MILLISECONDS))
            Assertions.fail<Any>("Subscriber did not release after set message semaphore in time")
        
        //verify new message
        Assertions.assertEquals(testMessage, messageHolder.get())
    }
}