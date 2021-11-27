package message.system

import org.junit.jupiter.api.*
import java.util.concurrent.atomic.AtomicReference

class MessageSystemTest {
    private val system: MessageSystem = MessageSystem()
    private val defaultQueue = MulticastQueue(TestMessage::class.java)
    
    @Test
    fun canCreateMulticastQueue() {
        //execute
        system.defineQueue(defaultQueue)
        //verify
        Assertions.assertEquals(
            setOf(defaultQueue),
            system.activeQueues()
        )
    }
    
    @Test
    fun canCreateProducerQueue() {
        //prepare
        val producerQueue = ProducerQueue(TestMessage::class.java)
        //execute
        system.defineQueue(producerQueue)
        //verify
        Assertions.assertEquals(
            setOf(producerQueue),
            system.activeQueues()
        )
    }
    
    @Test
    fun canCreateOnlyOneQueueForGivenType() {
        //prepare
        val producerQueue = ProducerQueue(TestMessage::class.java)
        system.defineQueue(producerQueue)
        
        //execute
        val exception = Assertions.assertThrows(QueueAlreadyDefinedException::class.java){system.defineQueue(defaultQueue)}
        
        //verify
        Assertions.assertEquals(
            QueueAlreadyDefinedException(TestMessage::class.java),
            exception
        )
        Assertions.assertEquals(
            "Queue for TestMessage was already defined.",
            exception.message
        )
        Assertions.assertEquals(
            setOf(producerQueue),
            system.activeQueues()
        )
    }
    
    @Test
    fun canSubscribeToQueue() {
        //prepare
        system.defineQueue(defaultQueue)
        val subscriber = TestMessageSubscriber()
        //execute
        system.subscribeTo(TestMessage::class.java,subscriber)
        //verify
        Assertions.assertEquals(
            listOf(subscriber),
            defaultQueue.subscribers
        )
    }
    
    @Test
    fun canSendMessageToQueue() {
        //prepare
        system.defineQueue(defaultQueue)
        val testMessage = TestMessage("superDuperMessage")
    
        //execute
        system.sendMessage(testMessage)
    }
    
    @Test
    fun sendMessage_givenMessageToNotDefinedQueue_throwsException() {
        //prepare
        val testMessage = TestMessage("superDuperMessage")
        
        //execute
        val exception = Assertions.assertThrows(UndefinedQueueException::class.java) { system.sendMessage(testMessage) }
        
        //verify
        Assertions.assertEquals(
            UndefinedQueueException(TestMessage::class.java),
            exception
        )
        Assertions.assertEquals(
            "Queue for TestMessage was not defined.",
            exception.message
        )
    }
    
    @Test
    fun subscriberReceivesMessage() {
        //prepare
        system.defineQueue(defaultQueue)
        val subscriber = TestMessageSubscriber()
        system.subscribeTo(TestMessage::class.java,subscriber)
        val testMessage = TestMessage("superDuperMessage")
    
        //execute
        system.sendMessage(testMessage)
    
        //verify
        Assertions.assertEquals(
            listOf(testMessage),
            subscriber.receivedMessages
        )
    }
    
    @Test
    fun subscriberReceivesMessageFromProperQueue() {
        //prepare
        system.defineQueue(defaultQueue)
        system.defineQueue(MulticastQueue(Any::class.java))
        
        val testMessageSubscriber = TestMessageSubscriber()
        system.subscribeTo(TestMessage::class.java,testMessageSubscriber)
        val testMessage = TestMessage("superDuperMessage")
        
        val objectAccumulator = AtomicReference<Any>()
        val objectMessageSubscriber : MessageSubscriber<Any> = MessageSubscriber(objectAccumulator::set)
        system.subscribeTo(Any::class.java,objectMessageSubscriber)
        val objectMessage = Any()
    
        //execute first message
        system.sendMessage(testMessage)
        
        //verify first message
        Assertions.assertEquals(
            listOf(testMessage),
            testMessageSubscriber.receivedMessages
        )
        Assertions.assertEquals(
            null,
            objectAccumulator.get()
        )
    
        //prepare for second message
        testMessageSubscriber.receivedMessages.clear()
        
        //execute second message
        system.sendMessage(objectMessage)
        
        //verify second message
        Assertions.assertEquals(
            listOf<TestMessage>(),
            testMessageSubscriber.receivedMessages
        )
        Assertions.assertEquals(
            objectMessage,
            objectAccumulator.get()
        )
    }
    
    class TestMessageSubscriber : MessageSubscriber<TestMessage> {
        val receivedMessages: MutableList<TestMessage> = mutableListOf()
        
        override fun receive(message: TestMessage) {
            receivedMessages.add(message)
        }
    }
    
    data class TestMessage(val msg: String)
}