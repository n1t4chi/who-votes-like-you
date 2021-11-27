package message.system

class MulticastQueue<T>(type:Class<T>) : Queue<T>(type) {
    val subscribers: MutableList<MessageSubscriber<T>> = mutableListOf()
    
    override fun addSubscriber(callback: MessageSubscriber<T>) {
        subscribers.add(callback)
    }
    
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as MulticastQueue<*>
        
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
    
    override fun receive(message: T) {
        subscribers.forEach{ subscriber -> subscriber.receive(message) }
    }
}