package message.consumer

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class PriorityExecutor(val threadCount: Int = Runtime.getRuntime().availableProcessors()) {
    private val tasks: BlockingQueue<PrioritizedTask> = PriorityBlockingQueue(100, reverseOrder<PrioritizedTask>())
    private val threadGroup = ThreadGroup("PriorityExecutorGroup")
    private val threadCounter = AtomicInteger()
    private var terminated = false
    private var started = false
    
    private var workers: MutableList<Worker> = ArrayList(threadCount)
    
    fun submit(priority: Int, runnable: Runnable) {
        if (terminated)
            throw IllegalStateException("Executor was already terminated")
        tasks.add(PrioritizedTask(priority, runnable))
        runThreadIfNeeded()
    }
    
    fun shutdown() {
        terminated = true
        waitForCurrentTasks()
    }
    
    private fun waitForCurrentTasks() {
        ArrayList(workers).forEach(Worker::join)
    }
    
    fun start() {
        started = true
        val size = tasks.size
        for( i in 1..size )
            startNewWorker()
    }
    
    private fun runThreadIfNeeded() {
        if (started && tasks.size > workers.size && workers.size < threadCount) {
            startNewWorker()
        }
    }
    
    private fun startNewWorker() {
        synchronized(workers) {
            val newWorker = Worker()
            workers.add(newWorker)
            newWorker.start()
        }
    }
    
    private fun removeWorker(worker: Worker) {
        synchronized(workers) {
            workers.remove(worker)
        }
    }
    
    private inner class Worker : Thread(threadGroup, "Worker" + threadCounter.incrementAndGet()) {
        override fun run() {
            while (!terminated || tasks.isNotEmpty()) {
                tasks.poll(5, TimeUnit.MILLISECONDS)?.run()
            }
            removeWorker(this)
        }
    }
}