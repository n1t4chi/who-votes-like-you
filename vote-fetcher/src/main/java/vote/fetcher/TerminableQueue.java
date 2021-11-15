package vote.fetcher;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class TerminableQueue<T> {
    private final Semaphore semaphore = new Semaphore(0);
    private final Queue<T> queue = new LinkedList<>();
    private boolean terminateReached = false;
    private boolean allQueried = false;
    
    private final String name;
    private final int consumers;
    private int terminated = 0;
    
    public TerminableQueue(String name, int consumers) {
        this.name = name;
        this.consumers = consumers;
    }
    
    public void put(T element) {
        if (terminateReached)
            throw new IllegalStateException("Queue was terminated!");
        synchronized(queue) {
            queue.offer(element);
        }
        semaphore.release();
    }
    
    public synchronized void terminate() {
        terminated++;
        if ( consumers <= terminated && !terminateReached) {
            terminateReached = true;
            semaphore.release(100_000_000);
        }
    }
    
    public Optional<T> tryNext() {
        acquireSemaphore();
        if(allQueried) {
            semaphore.release();
            return Optional.empty();
        }
        T take;
        synchronized (queue) {
            take = queue.poll();
        }
        if (take != null) {
            return Optional.of(take);
        }
        allQueried = true;
        semaphore.release();
        return Optional.empty();
    }
    
    private void acquireSemaphore() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
