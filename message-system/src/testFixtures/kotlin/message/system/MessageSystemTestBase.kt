package message.system

import log.WhoLogger
import message.executor.PriorityExecutorImpl
import message.subscriber.MessageSubscriber
import message.subscriber.PriorityAsyncMessageSubscriber
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.Instant
import java.util.*

open class MessageSystemTestBase {
    val logger = WhoLogger {}
    val priorityExecutor = PriorityExecutorImpl()
    val messageSystem = MessageSystem()
    val receivedRestarts = Collections.synchronizedSet(HashSet<RestartTask<*>>())
    
    
    @BeforeEach
    fun setup() {
        logger.trace { "setup start" }
        priorityExecutor.start()
        defineQueue(RestartTask::class.java)
        subscribe("RestartTask collector", RestartTask::class.java, 4) { msg ->
            receivedRestarts.add(msg)
            logger.debug { "Added new $msg to ${receivedRestarts.joinToString { "\n" }}" }
        }
        logger.trace { "setup end" }
    }
    
    @AfterEach
    fun teardown() {
        logger.trace { "teardown start" }
        messageSystem.clearQueues()
        logger.trace { "cleared queues" }
        priorityExecutor.shutdown()
        logger.trace { "priorityExecutor shutdown" }
        logger.trace { "teardown end" }
    }
    
    fun assertNoRestarts() {
        AssertionsExt.assertEmpty(receivedRestarts, "restarts")
    }
    
    fun assertRestarts(vararg restartTasks: RestartTask<*>) {
        logger.trace { "assertRestarts on:\n${receivedRestarts.joinToString { "\n" }}" }
        AssertionsExt.assertUnorderedEquals(restartTasks.toSet(), receivedRestarts)
    }
    
    fun assertNoExceptions() {
        AssertionsExt.assertEmpty(priorityExecutor.collectExceptions(), "exceptions")
    }
    
    fun waitForCurrentTasks() {
        logger.trace { "Started waiting for current tasks" }
        priorityExecutor.waitForCurrentTasks()
        logger.trace { "Finished waiting for current tasks" }
    }
    
    fun defineQueue(clazz: Class<*>) {
        messageSystem.defineQueue(Queue(clazz, priorityExecutor))
    }
    
    fun now(): String = Instant.now().toString()
    
    fun <T> subscribe(
        name: String,
        type: Class<T>,
        priority: Int,
        messageHandler: MessageSubscriber<in T>
    ) {
        logger.trace { "subscribe to type " + type.simpleName }
        messageSystem.subscribeTo(
            type,
            PriorityAsyncMessageSubscriber(name, priority, priorityExecutor, messageHandler)
        )
    }
    
    fun sendMessage(message: Any) {
        logger.trace { "send message " + message }
        messageSystem.sendMessage(message)
    }
}