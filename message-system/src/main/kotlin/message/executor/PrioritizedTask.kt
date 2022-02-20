package message.executor

data class PrioritizedTask(val priority: Int, val task: Runnable): Comparable<PrioritizedTask>, Runnable {
    override fun compareTo(other: PrioritizedTask): Int {
        return this.priority.compareTo(other.priority)
    }
    
    override fun run() {
        task.run()
    }
}
