package message.system

import java.util.concurrent.*

class SystemExecutorImpl: SystemExecutor {
    private val service = Executors.newCachedThreadPool()
    override fun submit(runnable: Runnable) {
        service.submit(runnable)
    }
    
    override fun shutdown() {
        service.awaitTermination(10, TimeUnit.SECONDS)
    }
}