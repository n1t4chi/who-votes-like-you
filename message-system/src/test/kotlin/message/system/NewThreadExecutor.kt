package message.system

class NewThreadExecutor : SystemExecutor {
    private val threads = mutableListOf<Thread>()
    override fun submit(runnable: Runnable) {
        val thread = Thread(runnable)
        threads.add(thread)
        thread.start()
    }
    
    override fun shutdown() {
        threads.forEach{thread -> thread.join()}
    }
}
