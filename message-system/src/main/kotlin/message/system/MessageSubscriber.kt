package message.system

fun interface MessageSubscriber<T> {
    fun receive(message: T)
}
