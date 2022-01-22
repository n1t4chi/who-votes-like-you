package message.system

import org.junit.jupiter.api.*
import java.util.concurrent.*

class MessageSystemAsyncTest : MessageSystemTestBase() {
    val cellectedMessage3 = mutableSetOf<Message3>()
    
    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun canCreateTreeLikeConsumerProducerSystem() {
        log("start")
        //prepare
        defineQueue(Message1::class.java)
        defineQueue(Message2::class.java)
        defineQueue(Message3::class.java)
        
        subscribe(Message1::class.java, 0) { msg1 -> handleMessage1(msg1) }
        subscribe(Message2::class.java, 2) { msg2 -> handleMessage2(msg2) }
        subscribe(Message3::class.java, 5) { msg3 -> handleMessage3(msg3) }
    
        //execute
        log("execute")
        fireMessage1(1, 2, 2)
        fireMessage1(2, 2, 2)
        
        //wait for tasks
        waitForCurrentTasks()
        
        //verify
        log("verify")
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
    
    private fun fireMessage1(message1No: Int, messages2Count: Int, messages3Count: Int) {
        log("fireMessage1($message1No,$messages2Count,$messages3Count)")
        sendMessage(Message1(message1No, messages2Count, messages3Count))
    }
    
    private fun fireMessage2(message1No: Int, message2No: Int, messages3Count: Int) {
        log("fireMessage2($message1No,$message2No,$messages3Count)")
        sendMessage(Message2(message1No, message2No, messages3Count))
    }
    
    private fun fireMessage3(message1No: Int, message2No: Int, message3No: Int) {
        log("fireMessage3($message1No,$message2No,$message3No)")
        sendMessage(msg3(message1No, message2No, message3No))
    }
    
    private fun msg3(message1No: Int, message2No: Int, message3No: Int) = Message3(message1No, message2No, message3No)
    
    private fun handleMessage1(message: Message1) {
        log("handleMessage1($message)")
        for (message2No in 1..message.messages2Count)
            fireMessage2(message.message1No, message2No, message.messages3Count)
    }
    
    private fun handleMessage2(message: Message2) {
        log("handleMessage2($message)")
        for (message3No in 1..message.messages3Count)
            fireMessage3(message.message1No, message.message2No, message3No)
    }
    
    private fun handleMessage3(message: Message3) {
        log("handleMessage3($message)")
        synchronized(cellectedMessage3){
            cellectedMessage3.add(message)
        }
    }
    
    data class Message1(val message1No: Int, val messages2Count: Int, val messages3Count: Int)
    data class Message2(val message1No: Int, val message2No: Int, val messages3Count: Int)
    data class Message3(val message1No: Int, val message2No: Int, val message3No: Int)
}