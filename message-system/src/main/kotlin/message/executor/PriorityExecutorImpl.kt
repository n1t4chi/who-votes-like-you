package message.executor

import log.WhoLogger
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PriorityExecutorImpl(val threadCount: Int = Runtime.getRuntime().availableProcessors()) : PriorityExecutor {
    private val logger = WhoLogger {}
    private val uncaughtExceptions = mutableListOf<Throwable>()
    private val tasks = PriorityBlockingQueue(100, reverseOrder<PrioritizedTask>())
    private val threadGroup = ThreadGroup("PriorityExecutorGroup")
    private val threadCounter = AtomicInteger()
    private var terminated = false
    private var started = false
    
    private var workers: MutableList<Worker> = ArrayList(threadCount)
    
    override fun submit(task: PrioritizedTask) {
        if (terminated)
            throw IllegalStateException("Executor was already terminated")
        logger.debug { "Submitting task $task" }
        tasks.add(task)
        runThreadIfNeeded()
    }
    
    override fun shutdown() {
        terminated = true
        waitForCurrentTasks()
    }
    
    override fun waitForCurrentTasks() {
        logger.trace {
            "Started waiting for workers.\n" +
                "Current tasks amount: ${tasks.size}\n" +
                "Current workers amount: ${workers.size}"
        }
        while (tasks.isNotEmpty() && workers.isNotEmpty()) {
            logger.trace { "Waiting for workers: ${workers.joinToString { "\n" }}" }
            ArrayList(workers)
                .forEach(Worker::join)
        }
        logger.trace { "Finished waiting for workers" }
    }
    
    override fun start() {
        started = true
        val size = tasks.size
        for (i in 1..size)
            startNewWorker()
    }
    
    private fun runThreadIfNeeded() {
        if (started) {
            synchronized(workers) {
                if (tasks.size > workers.size && workers.size < threadCount) {
                    startNewWorker()
                }
            }
        } else {
            logger.warn {
                "Calling runThreadIfNeeded() but executor not started yet. " +
                    "Tasks will not be executed until executor is started."
            }
        }
    }
    
    private fun startNewWorker() {
        val newWorker = Worker()
        logger.debug { "Starting new worker: $newWorker" }
        synchronized(workers) {
            workers.add(newWorker)
            newWorker.start()
            logger.debug { "$newWorker started" }
        }
    }
    
    private fun removeWorker(worker: Worker) {
        logger.debug { "Removing worker: $worker" }
        synchronized(workers) {
            workers.remove(worker)
            logger.debug { "$worker removed" }
        }
    }
    
    override fun collectExceptions(): List<Throwable> {
        return uncaughtExceptions
    }
    
    private inner class Worker : Thread(threadGroup, "Worker" + threadCounter.incrementAndGet()) {
        override fun run() {
            while (tasks.isNotEmpty()) {
                logger.trace { "worker $name handling new task" }
                val task = tasks.poll(5, TimeUnit.MILLISECONDS)
                try {
                    task?.run()
                } catch (throwable: Throwable) {
                    logger.error(throwable) { "worker $name encountered an unhandled error" }
                    synchronized(uncaughtExceptions) {
                        uncaughtExceptions.add(throwable)
                    }
                }
            }
            logger.trace { "worker $name finished working" }
            removeWorker(this)
        }
    }
}