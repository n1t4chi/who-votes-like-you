package message.subscriber

import message.executor.PriorityExecutorImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class PriorityExecutorImplTest {
    private val executor = PriorityExecutorImpl(1)
    
    @Test
    fun whenNotStarted_onTaskSubmit_doesNothing() {
        //prepare
        val counter = AtomicInteger()
        //execute
        executor.submit(1) { counter.incrementAndGet() }
        //verify
        executor.shutdown()
        Assertions.assertEquals(0, counter.get())
    }
    
    @Test
    fun whenAlreadyStarted_onSubmit_executesGivenTask() {
        //prepare
        executor.start()
        val counter = AtomicInteger()
        //execute
        executor.submit(1) { counter.incrementAndGet() }
        //verify
        executor.shutdown()
        Assertions.assertEquals(1, counter.get())
    }
    
    @Test
    fun whenNotStarted_onTaskSubmit_afterStart_executesTask() {
        //prepare
        val counter = AtomicInteger()
        executor.submit(1) { counter.incrementAndGet() }
        //execute
        executor.start()
        //verify
        executor.shutdown()
        Assertions.assertEquals(1, counter.get())
    }
    
    @Test
    fun givenThreeTasksWithDifferentPriority_onStart_executesThemInDescendingOrder() {
        //prepare
        val counter = AtomicInteger()
        
        val firstTaskValue = AtomicInteger()
        executor.submit(2) { firstTaskValue.set(counter.incrementAndGet()) }
        val secondTaskValue = AtomicInteger()
        executor.submit(4) { secondTaskValue.set(counter.incrementAndGet()) }
        val thirdTaskValue = AtomicInteger()
        executor.submit(1) { thirdTaskValue.set(counter.incrementAndGet()) }
        
        //execute
        executor.start()
        
        //verify
        executor.shutdown()
        Assertions.assertEquals(3, counter.get())
        Assertions.assertEquals(2, firstTaskValue.get())
        Assertions.assertEquals(1, secondTaskValue.get())
        Assertions.assertEquals(3, thirdTaskValue.get())
    }
}