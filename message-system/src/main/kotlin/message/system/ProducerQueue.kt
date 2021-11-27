package message.system

class ProducerQueue<T>(type: Class<T>) : Queue<T>(type) {
    override fun receive(message: T) {
        TODO("Not yet implemented")
    }
    
    override fun addSubscriber(callback: MessageSubscriber<T>) {
        TODO("Not yet implemented")
    }
}