package message.system

import message.consumer.*
import org.junit.jupiter.api.*
import java.time.Instant

open class MessageSystemTestBase {
    val systemExecutor = SystemExecutorImpl()
    val priorityExecutor = PriorityExecutor()
    val messageSystem = MessageSystem()
    
    
    @BeforeEach
    fun setup() {
        log("setup start")
        priorityExecutor.start()
        log("setup end")
    }
    
    @AfterEach
    fun teardown() {
        log("teardown start")
        messageSystem.clearQueues()
        log("cleared queues")
        priorityExecutor.shutdown()
        log("priorityExecutor shutdown")
        systemExecutor.shutdown()
        log("teardown end")
    }
    
    fun waitForCurrentTasks() {
        log("waitForCurrentTasks")
        Thread.sleep(40)
        priorityExecutor.waitForCurrentTasks()
    }
    
    fun defineQueue(clazz: Class<*>) {
        log("defineQueue for " + clazz.simpleName)
        messageSystem.defineQueue(Queue(clazz, systemExecutor))
    }
    
    fun now(): String = Instant.now().toString()
    
    fun <T> subscribe(
        type: Class<T>,
        priority: Int,
        messageHandler: MessageSubscriber<T>
    ) {
        log("subscribe to type " + type.simpleName)
        messageSystem.subscribeTo(type, PriorityAsyncMessageSubscriber(priority, priorityExecutor, messageHandler))
    }
    
    fun sendMessage(message: Any) {
        log("send message " + message)
        messageSystem.sendMessage(message)
    }
    
    fun log(message: String) {
        println("${now()} $message")
    }
}