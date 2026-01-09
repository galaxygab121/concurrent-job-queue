package queue;

import model.Job;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class JobQueue {

    private final Deque<Job> queue = new ArrayDeque<>();
    private final int capacity;

    // "true" => fair lock (threads acquire lock roughly FIFO under contention)
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private boolean shutdown = false;

    public JobQueue(int capacity) {
        this.capacity = capacity;
    }

    public void put(Job job) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (queue.size() >= capacity && !shutdown) {
                notFull.await();
            }

            if (shutdown) {
                return; // ignore jobs after shutdown (matches your current semantics)
            }

            queue.addLast(job); // FIFO
            notEmpty.signal();  // wake ONE waiting consumer
        } finally {
            lock.unlock();
        }
    }

    public Job take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (queue.isEmpty() && !shutdown) {
                notEmpty.await();
            }

            if (queue.isEmpty() && shutdown) {
                return null;
            }

            Job job = queue.removeFirst();
            notFull.signal(); // wake ONE waiting producer
            return job;
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            shutdown = true;
            // wake everyone so they can exit or stop waiting
            notEmpty.signalAll();
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }
}

