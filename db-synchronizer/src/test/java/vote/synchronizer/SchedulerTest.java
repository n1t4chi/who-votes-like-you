package vote.synchronizer;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SchedulerTest {
    private final Synchronizer synchronizer = Mockito.mock( Synchronizer.class );
    private final Scheduler scheduler = new Scheduler(synchronizer);
    
    @Test
    void startCallsInitializeOnSynchronizer() {
        //prepare
        Mockito.doNothing().when(synchronizer).initialize();
        
        //execute
        scheduler.start();
        
        //verify
        Mockito.verify(synchronizer, Mockito.times( 1 ) ).initialize();
    }
    
}