package message.subscriber

import message.system.MessageSystem
import message.system.RestartTask

abstract class RestartableMessageSubscriber<T : Any>(
    name: String,
    protected val messageSystem: MessageSystem
) : BaseMessageSubscriber<T>(name) {
    override fun receiveInner(message: T) {
        try {
            receiveUnsafe(message)
        } catch (exception: Exception) {
            logger.warn(exception) { "$name encountered an error during handling $message" }
            messageSystem.sendMessage(RestartTask(message))
        }
    }
    
    abstract fun receiveUnsafe(message: T)
}