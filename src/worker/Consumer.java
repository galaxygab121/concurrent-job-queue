package worker;

import model.Job;
import queue.JobQueue;

public class Consumer implements Runnable {

    private final JobQueue queue;
    private final int consumerId;
    private final boolean verbose;
    private final int logEvery;
    private final boolean noSleep;

    private int processedCount = 0;

    // Backward-compatible constructor (defaults noSleep=false)
    public Consumer(JobQueue queue, int consumerId, boolean verbose, int logEvery) {
        this(queue, consumerId, verbose, logEvery, false);
    }

    // New constructor for tests/benchmarks
    public Consumer(JobQueue queue, int consumerId, boolean verbose, int logEvery, boolean noSleep) {
        this.queue = queue;
        this.consumerId = consumerId;
        this.verbose = verbose;
        this.logEvery = logEvery;
        this.noSleep = noSleep;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Job job = queue.take();
                if (job == null) break;

                if (verbose && processedCount % logEvery == 0) {
                    System.out.println("Consumer " + consumerId + " processing " + job);
                }

                if (!noSleep && job.getDurationMs() > 0) {
                    Thread.sleep(job.getDurationMs());
                }

                processedCount++;

                if (verbose && processedCount % logEvery == 0) {
                    System.out.println("Consumer " + consumerId + " finished " + job);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getProcessedCount() {
        return processedCount;
    }
    public int getConsumerId() {
        return consumerId;
    }
  
}





