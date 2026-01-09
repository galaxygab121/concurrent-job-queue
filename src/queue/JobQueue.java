package queue; // declares this file belongs to the "queue" package

import java.util.LinkedList; // LinkedList implementation
import java.util.Queue; // Queue interface
import model.Job; // import the Job class from modelmodel package

public class JobQueue { // defines a thread-safe job queue class

    private final Queue<Job> queue = new LinkedList<>(); // internal storage for jobs (FIFO) First In First Out
    private final int capacity; // maximum number of jobs allowed in the queue
    private boolean shutdown = false; // flag to indicate "no more jobs will arrive"

    public JobQueue(int capacity) { // constructor to set queue capacity
        this.capacity = capacity; // store capacity
    }

    // PUT: called by PRODUCERS to add jobs to the queue
    // synchronized means only ONE thread can execute this method at a time (prevents race conditions)
    public synchronized void put(Job job) throws InterruptedException {
        // While the queue is full AND we are not shutting down, producer must wait
        while (queue.size() >= capacity && !shutdown) {
            wait(); // release the lock and sleep until another thread calls notifyAll()
        }

        // If shutdown was triggered, do not accept new jobs (just return)
        if (shutdown) {
            return;
        }

        queue.add(job); // add the job to the queue (FIFO tail) First In First Out
        notifyAll(); // wake up any waiting consumers (and possibly producers) to re-check conditions
    }

    // TAKE: called by CONSUMERS to remove jobs from the queue
    // synchronized means only ONE thread can execute this method at a time (prevents race conditions)
    public synchronized Job take() throws InterruptedException {
        // While the queue is empty AND we are not shutting down, consumer must wait
        while (queue.isEmpty() && !shutdown) {
            wait(); // release the lock and sleep until a producer calls notifyAll()
        }

        // If queue is empty AND shutdown is true, return null to signal consumers to exit
        if (queue.isEmpty() && shutdown) {
            return null;
        }

        Job job = queue.remove(); // remove the head (FIFO) First In First Out
        notifyAll(); // wake up producers (queue might have space now) and other consumers
        return job; // return job for processing
    }

    // SHUTDOWN: called when no more jobs will be produced
    public synchronized void shutdown() {
        shutdown = true; // signal shutdown mode
        notifyAll(); // wake all waiting threads so they can stop waiting and exit if needed
    }
}
