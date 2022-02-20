package message.executor

class ImmediatePriorityExecutor : PriorityExecutor {
    override fun submit(task: PrioritizedTask) {
        task.run()
    }
    
    override fun shutdown() {}
    
    override fun waitForCurrentTasks() {}
    
    override fun start() {}
    
    override fun collectExceptions(): List<Throwable> = listOf()
}