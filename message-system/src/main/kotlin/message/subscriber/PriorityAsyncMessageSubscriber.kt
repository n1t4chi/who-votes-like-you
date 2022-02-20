package message.subscriber

import message.executor.PriorityExecutorImpl

class PriorityAsyncMessageSubscriber<T>(
    name: String,
    val priority: Int,
    val priorityExecutor: PriorityExecutorImpl,
    val consumer: MessageSubscriber<in T>
) : BaseMessageSubscriber<T>(name) {
    override fun receiveInner(message: T) {
        logger.debug { "$name submitting to priority executor message:\n$message" }
        priorityExecutor.submit(priority) {
            logger.debug { "$name.consumer async handling the message:\n$message" }
            consumer.receive(message)
            logger.debug { "$name.consumer async handled the message:\n$message" }
        }
    }
}