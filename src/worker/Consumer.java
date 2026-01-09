package worker;

import model.Job;
import queue.JobQueue;

public class Consumer implements Runnable {

    private final JobQueue queue;        // shared queue to pull jobs from
    private final int consumerId;        // consumer identifier used for logs
    private int processedCount = 0;      // number of jobs successfully processed

    public Consumer(JobQueue queue, int consumerId) {
        this.queue = queue;             // store shared queue reference
        this.consumerId = consumerId;   // store consumer id
    }

    public int getConsumerId() {
        return consumerId;              // expose id for summary reporting
    }

    public int getProcessedCount() {
        return processedCount;          // expose processed count for summary reporting
    }

    @Override
    public void run() {
        try {
            while (true) {
                // take() blocks if the queue is empty (until a producer adds work or shutdown occurs)
                Job job = queue.take();

                // null means: queue is shutdown AND empty -> time to exit cleanly
                if (job == null) {
                    break;
                }

                processedCount++; // record that we are processing a job
                System.out.println("Consumer " + consumerId + " processing " + job);

                // simulate real work by sleeping for durationMs
                Thread.sleep(job.getDurationMs());

                System.out.println("Consumer " + consumerId + " finished " + job);
            }

            // after loop exits, log final count
            System.out.println("Consumer " + consumerId + " exiting. Processed=" + processedCount);

        } catch (InterruptedException ignored) {
            // restore interrupt flag (best practice) so calling code can detect interruption
            Thread.currentThread().interrupt();
        }
    }
}

