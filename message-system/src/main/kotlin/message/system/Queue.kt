package message.system

abstract class Queue<T>(val type: Class<T>) {
    abstract fun receive(message: T)
    abstract fun addSubscriber(callback: MessageSubscriber<T>)
}

