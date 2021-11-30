package message.system

import message.consumer.*
import org.junit.jupiter.api.*
import java.time.Instant
import java.util.concurrent.TimeUnit

class MessageSystemAsyncTest {
    val systemExecutor = SystemExecutorImpl()
    val priorityExecutor = PriorityExecutor()
    val messageSystem = MessageSystem()
    val cellectedMessage3 = mutableSetOf<Message3>()
    
    @BeforeEach
    fun setup() {
        println("${now()} setup start")
        cellectedMessage3.clear()
        priorityExecutor.start()
        messageSystem.defineQueue(Queue(Message1::class.java, systemExecutor))
        messageSystem.defineQueue(Queue(Message2::class.java, systemExecutor))
        messageSystem.defineQueue(Queue(Message3::class.java, systemExecutor))
        println("${now()} setup end")
    }
    
    @AfterEach
    fun teardown() {
        println("${now()} teardown start")
        priorityExecutor.shutdown()
        println("${now()} priorityExecutor shutdown")
        systemExecutor.shutdown()
        println("${now()} teardown end")
    }
    
    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun canCreateTreeLikeConsumerProducerSystem() {
        println("${now()} start")
        //prepare
        subscribe(Message1::class.java, 0) { msg1 -> handleMessage1(msg1) }
        subscribe(Message2::class.java, 2) { msg2 -> handleMessage2(msg2) }
        subscribe(Message3::class.java, 5) { msg3 -> handleMessage3(msg3) }
    
        //execute
        println("${now()} execute")
        fireMessage1(1, 2, 2)
        fireMessage1(2, 2, 2)
        
        //wait for tasks
        Thread.sleep(10)
        println("${now()} waitForCurrentTasks")
        priorityExecutor.waitForCurrentTasks()
        
        //verify
        println("${now()} verify")
        Assertions.assertEquals(
            setOf(
                msg3(1,1,1),
                msg3(1,1,2),
                msg3(1,2,1),
                msg3(1,2,2),
                msg3(2,1,1),
                msg3(2,1,2),
                msg3(2,2,1),
                msg3(2,2,2),
            ),
            cellectedMessage3
        )
    }
    
    private fun <T> subscribe(
        type: Class<T>,
        priority: Int,
        messageHandler: (t: T) -> Unit
    ) {
        messageSystem.subscribeTo(type, PriorityAsyncMessageSubscriber(priority, priorityExecutor, messageHandler))
    }
    
    private fun fireMessage1(message1No: Int, messages2Count: Int, messages3Count: Int) {
        println("${now()} fireMessage1($message1No,$messages2Count,$messages3Count)")
        messageSystem.sendMessage(Message1(message1No, messages2Count, messages3Count))
    }
    
    private fun now(): String = Instant.now().toString()
    
    private fun fireMessage2(message1No: Int, message2No: Int, messages3Count: Int) {
        println("${now()} fireMessage2($message1No,$message2No,$messages3Count)")
        messageSystem.sendMessage(Message2(message1No, message2No, messages3Count))
    }
    
    private fun fireMessage3(message1No: Int, message2No: Int, message3No: Int) {
        println("${now()} fireMessage3($message1No,$message2No,$message3No)")
        messageSystem.sendMessage(msg3(message1No, message2No, message3No))
    }
    
    private fun msg3(message1No: Int, message2No: Int, message3No: Int) = Message3(message1No, message2No, message3No)
    
    private fun handleMessage1(message: Message1) {
        println("${now()} handleMessage1($message)")
        for (message2No in 1..message.messages2Count)
            fireMessage2(message.message1No, message2No, message.messages3Count)
    }
    
    private fun handleMessage2(message: Message2) {
        println("${now()} handleMessage2($message)")
        for (message3No in 1..message.messages3Count)
            fireMessage3(message.message1No, message.message2No, message3No)
    }
    
    private fun handleMessage3(message: Message3) {
        println("${now()} handleMessage3($message)")
        synchronized(cellectedMessage3){
            cellectedMessage3.add(message)
        }
    }
    
    data class Message1(val message1No: Int, val messages2Count: Int, val messages3Count: Int)
    data class Message2(val message1No: Int, val message2No: Int, val messages3Count: Int)
    data class Message3(val message1No: Int, val message2No: Int, val message3No: Int)
}