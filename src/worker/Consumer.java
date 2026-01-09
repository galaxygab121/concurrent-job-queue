package worker; // declares this file belongs to the "worker" package

import model.Job; // import Job
import queue.JobQueue; // import JobQueue

public class Consumer implements Runnable { // Consumer runs in a thread

    private final JobQueue queue; // reference to shared job queue
    private final int consumerId; // identifier for logging
    private int processedCount = 0; // tracks how many jobs this consumer processed

    public Consumer(JobQueue queue, int consumerId) {
        this.queue = queue; // store queue reference
        this.consumerId = consumerId; // store consumer id
    }

    public int getProcessedCount() { // getter for metrics
        return processedCount; // return number of processed jobs
    }

    @Override
    public void run() { // thread executes this method
        try {
            while (true) { // keep consuming until shutdown signal (null job) is received
                Job job = queue.take(); // take a job (may BLOCK if queue is empty)

                if (job == null) { // null means queue is shutdown and empty
                    break; // exit the loop and end the thread
                }

                processedCount++; // increment metrics
                System.out.println("Consumer " + consumerId + " processing " + job); // log

                Thread.sleep(job.getDurationMs()); // simulate job processing time

                System.out.println("Consumer " + consumerId + " finished " + job); // log completion
            }

            System.out.println("Consumer " + consumerId + " exiting. Processed=" + processedCount); // final log
        } catch (InterruptedException ignored) { // if interrupted while sleeping/waiting
            Thread.currentThread().interrupt(); // restore interrupt status
        }
    }
}
