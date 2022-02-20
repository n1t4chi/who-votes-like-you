package message.subscriber

import log.WhoLogger

abstract class BaseMessageSubscriber<T>(protected val name: String) : MessageSubscriber<T> {
    protected val logger = WhoLogger {}
    
    abstract fun receiveInner(message: T)
    
    override fun receive(message: T) {
        logger.trace { "$name received $message" }
        receiveInner(message)
        logger.trace { "$name finished processing $message" }
    }
}