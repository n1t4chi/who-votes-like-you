package message.system

import log.WhoLogger
import message.subscriber.MessageSubscriber

class MessageSystem {
    private val logger = WhoLogger {}
    private val queues: MutableMap<Class<*>, Queue<*>> = mutableMapOf()
    
    fun defineQueue(queue: Queue<*>) {
        if (queues.contains(queue.type))
            throw QueueAlreadyDefinedException(queue.type)
        logger.trace { "New queue for: ${queue.type.name}" }
        queues[queue.type] = queue
    }
    
    fun clearQueues() {
        logger.trace { "clearing queues" }
        queues.clear()
    }
    
    fun activeQueues(): Set<Queue<*>> = queues.values.toSet()
    
    fun <T> getQueue(aClass: Class<T>): Queue<T> {
        @Suppress("UNCHECKED_CAST")
        return (queues[aClass] ?: throw UndefinedQueueException(aClass)) as Queue<T>
    }
    
    fun <T> subscribeTo(aClass: Class<T>, subscriber: MessageSubscriber<T>) {
        getQueue(aClass).addSubscriber(subscriber)
    }
    
    fun sendMessage(message: Any) {
        val queue = getQueue(message.javaClass)
        logger.trace { "New message for queue: ${queue.type.name}. Content:\n$message" }
        queue.receive(message)
    }
}
