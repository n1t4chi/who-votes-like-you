package message.subscriber

fun interface MessageSubscriber<T> {
    fun receive(message: T)
}
