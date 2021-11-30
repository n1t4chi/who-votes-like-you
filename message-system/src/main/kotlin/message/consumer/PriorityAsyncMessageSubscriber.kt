package message.consumer

import message.system.MessageSubscriber
import java.util.function.Consumer

class PriorityAsyncMessageSubscriber<T>(
    val priority: Int,
    val priorityExecutor: PriorityExecutor,
    val consumer: Consumer<T>
): MessageSubscriber<T> {
    override fun receive(message: T) {
        priorityExecutor.submit(priority) { consumer.accept(message) }
    }
}