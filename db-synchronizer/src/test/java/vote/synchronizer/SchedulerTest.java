package vote.synchronizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {
    
    
    private final Scheduler scheduler = new Scheduler(new Synchronizer(null, null));
    
    @Test
    void schedulerCanReceiveSynchronizer() {
    
    }
}