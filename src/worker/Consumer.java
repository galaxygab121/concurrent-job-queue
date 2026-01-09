package worker;

import queue.JobQueue;
import model.Job;

/**
 * Consumer pulls jobs from the queue and processes them.
 */
public class Consumer implements Runnable {

    private final JobQueue queue;
    private final int consumerId;
    private final boolean verbose;
    private final int logEvery;

    private int processedCount = 0;

    public Consumer(JobQueue queue, int consumerId, boolean verbose, int logEvery) {
        this.queue = queue;
        this.consumerId = consumerId;
        this.verbose = verbose;
        this.logEvery = logEvery;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Job job = queue.take();

                // Null job means queue shutdown and empty
                if (job == null) {
                    break;
                }

                if (verbose && processedCount % logEvery == 0) {
                    System.out.println(
                            "Consumer " + consumerId + " processing " + job
                    );
                }

                // Simulate work
                if (job.getDurationMs() > 0) {
                    Thread.sleep(job.getDurationMs());
                }

                processedCount++;

                if (verbose && processedCount % logEvery == 0) {
                    System.out.println(
                            "Consumer " + consumerId + " finished " + job
                    );
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getProcessedCount() {
        return processedCount;
    }
}



