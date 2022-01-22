package message.consumer

import message.system.MessageSubscriber

class PriorityAsyncMessageSubscriber<T>(
    val priority: Int,
    val priorityExecutor: PriorityExecutor,
    val consumer: MessageSubscriber<T>
): MessageSubscriber<T> {
    override fun receive(message: T) {
        priorityExecutor.submit(priority) { consumer.receive(message) }
    }
}