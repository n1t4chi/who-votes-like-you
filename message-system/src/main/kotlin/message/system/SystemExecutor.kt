package message.system

interface SystemExecutor {
    fun submit(runnable: Runnable)
    
    fun shutdown()
}
