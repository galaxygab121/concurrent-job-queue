package worker;

import model.Job;
import queue.JobQueue;

public class Consumer implements Runnable {

    private final JobQueue queue;
    private final int consumerId;
    private int processedCount = 0;

    private final boolean verbose;   // controls whether per-job logs print
    private final int logEvery;      // print every N jobs (when verbose is true)

    public Consumer(JobQueue queue, int consumerId, boolean verbose, int logEvery) {
        this.queue = queue;
        this.consumerId = consumerId;
        this.verbose = verbose;
        this.logEvery = Math.max(1, logEvery);
    }

    public int getConsumerId() {
        return consumerId;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Job job = queue.take();

                if (job == null) {
                    break;
                }

                processedCount++;

                if (verbose && (processedCount % logEvery == 0)) {
                    System.out.println("Consumer " + consumerId + " processed count=" + processedCount);
                }

                // Simulate doing the job
                Thread.sleep(job.getDurationMs());
            }

            if (verbose) {
                System.out.println("Consumer " + consumerId + " exiting. Processed=" + processedCount);
            }

        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}


