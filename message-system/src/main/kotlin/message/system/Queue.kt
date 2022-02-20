package message.system

import log.WhoLogger
import message.executor.PriorityExecutor
import message.subscriber.MessageSubscriber

class Queue<T>(val type: Class<T>, private val executor: PriorityExecutor) {
    private val logger = WhoLogger {}
    val subscribers: MutableList<MessageSubscriber<T>> = mutableListOf()
    
    fun addSubscriber(callback: MessageSubscriber<T>) {
        logger.trace { "New subscriber to queue: ${type.name}" }
        subscribers.add(callback)
    }
    
    fun receive(message: T) {
        executor.submitAsap {
            logger.trace { "Queue[n=${subscribers.size}] received new message:\n$message" }
            subscribers.forEach { subscriber ->
                logger.trace { "$subscriber.receive($message)" }
                subscriber.receive(message)
            }
            logger.trace { "Queue[n=${subscribers.size}] consumed message:\n$message" }
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Queue<*>
        
        if (type != other.type) return false
        if (subscribers != other.subscribers) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + subscribers.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "Queue(type=$type, subscribers=$subscribers)"
    }
}

