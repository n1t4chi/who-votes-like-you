package message.system

class TestMessageSubscriber : MessageSubscriber<TestMessage> {
    val receivedMessages: MutableList<TestMessage> = mutableListOf()
    
    override fun receive(message: TestMessage) {
        receivedMessages.add(message)
    }
}