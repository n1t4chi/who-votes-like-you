package message.system

import org.junit.jupiter.api.*

class QueueBasicTest {
    private val executor: SystemExecutor = ImmediateExecutor()
    private val queue = Queue(TestMessage::class.java,executor)
    
    @Test
    fun canSubscribeToQueue() {
        //prepare
        val subscriber = TestMessageSubscriber()
        //execute
        queue.addSubscriber(subscriber)
        //verify
        Assertions.assertEquals(
            listOf(subscriber),
            queue.subscribers
        )
    }
    
    @Test
    fun canSendMessage_givenNoSubscribers() {
        //prepare
        val testMessage = TestMessage("superDuperMessage")
        
        //execute
        queue.receive(testMessage)
    }
    
    @Test
    fun canSendMessage_givenOneSubscriber_heReceivesIt() {
        //prepare
        val subscriber = TestMessageSubscriber()
        queue.addSubscriber(subscriber)
        val testMessage = TestMessage("superDuperMessage")
        
        //execute
        queue.receive(testMessage)
        
        //verify
        Assertions.assertEquals(
            listOf(testMessage),
            subscriber.receivedMessages
        )
    }
    
    @Test
    fun canSendMessage_givenMultipleSubscribers_everyoneReceivesIt() {
        //prepare
        val subscriber1 = TestMessageSubscriber()
        val subscriber2 = TestMessageSubscriber()
        val subscriber3 = TestMessageSubscriber()
        queue.addSubscriber(subscriber1)
        queue.addSubscriber(subscriber2)
        queue.addSubscriber(subscriber3)
        
        val testMessage = TestMessage("superDuperMessage")
        
        //execute
        queue.receive(testMessage)
        
        //verify
        Assertions.assertEquals(
            listOf(testMessage),
            subscriber1.receivedMessages
        )
        Assertions.assertEquals(
            listOf(testMessage),
            subscriber2.receivedMessages
        )
        Assertions.assertEquals(
            listOf(testMessage),
            subscriber3.receivedMessages
        )
    }
}