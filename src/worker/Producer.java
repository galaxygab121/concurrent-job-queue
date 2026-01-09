package worker;

import model.Job;
import queue.JobQueue;
import java.util.Random;

public class Producer implements Runnable {

    private final JobQueue queue;
    private final int producerId;
    private final int jobsToProduce;
    private final Random rand;

    private final boolean verbose;   // controls whether per-job logs print
    private final int logEvery;      // print every N jobs (when verbose is true)

    public Producer(JobQueue queue, int producerId, int jobsToProduce, long seed, boolean verbose, int logEvery) {
        this.queue = queue;
        this.producerId = producerId;
        this.jobsToProduce = jobsToProduce;
        this.rand = new Random(seed);
        this.verbose = verbose;
        this.logEvery = Math.max(1, logEvery);
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < jobsToProduce; i++) {
                int jobId = producerId * 1000 + i;
                int durationMs = 200 + rand.nextInt(401);

                Job job = new Job(jobId, durationMs);
                queue.put(job);

                // Print occasionally so terminal I/O doesn't dominate throughput
                if (verbose && (i % logEvery == 0)) {
                    System.out.println("Producer " + producerId + " produced " + job);
                }

                // OPTIONAL: if you want max throughput, remove this sleep
                // Thread.sleep(75);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

