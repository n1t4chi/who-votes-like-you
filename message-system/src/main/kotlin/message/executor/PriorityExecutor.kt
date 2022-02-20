package message.executor

interface PriorityExecutor {
    fun submit(priority: Int, runnable: Runnable) {
        submit(PrioritizedTask(priority, runnable))
    }
    
    fun submitAsap(runnable: Runnable) {
        submit(PrioritizedTask(Int.MAX_VALUE, runnable))
    }
    
    fun submit(task: PrioritizedTask)
    fun shutdown()
    fun waitForCurrentTasks()
    fun start()
    fun collectExceptions(): List<Throwable>
}