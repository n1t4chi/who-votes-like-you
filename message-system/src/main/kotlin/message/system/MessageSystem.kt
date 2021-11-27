package message.system

@Suppress("UNCHECKED_CAST")
class MessageSystem {
    private val queues: MutableMap<Class<*>,Queue<*>> = mutableMapOf()
    
    fun defineQueue(queue: Queue<*>) {
        if(queues.contains(queue.type))
            throw QueueAlreadyDefinedException(queue.type)
        queues[queue.type] = queue
    }
    
    fun activeQueues(): Set<Queue<*>> = queues.values.toSet()
    
    fun <T> getQueue(aClass: Class<T>): Queue<T> {
        return (queues[aClass] ?: throw UndefinedQueueException(aClass)) as Queue<T>
    }
    
    fun <T> subscribeTo(aClass: Class<T>, subscriber: MessageSubscriber<T>) {
        getQueue(aClass).addSubscriber(subscriber)
    }
    
    fun sendMessage(message: Any) {
        val queue = getQueue(message.javaClass)
        queue.receive(message)
    }
}
