package message.system

class Queue<T>(val type: Class<T>,private val executor: SystemExecutor) {
    val subscribers: MutableList<MessageSubscriber<T>> = mutableListOf()
    
    fun addSubscriber(callback: MessageSubscriber<T>) {
        subscribers.add(callback)
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
        return "MulticastQueue(type=$type, subscribers=$subscribers)"
    }
    
    fun receive(message: T) {
        executor.submit{
            subscribers.forEach{ subscriber -> subscriber.receive(message) }
        }
    }
}

