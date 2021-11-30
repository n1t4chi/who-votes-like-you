package message.system

class ImmediateExecutor : SystemExecutor {
    override fun submit(runnable: Runnable) {
        runnable.run()
    }
    
    override fun shutdown() {
    }
}
