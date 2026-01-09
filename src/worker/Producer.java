package worker;

import queue.JobQueue;
import model.Job;

import java.util.Random;

/**
 * Producer generates jobs and inserts them into the shared queue.
 */
public class Producer implements Runnable {

    private final JobQueue queue;
    private final int producerId;
    private final int jobsToProduce;
    private final Random rand;
    private final boolean verbose;
    private final int logEvery;
    private final boolean noSleep;

    public Producer(
            JobQueue queue,
            int producerId,
            int jobsToProduce,
            long seed,
            boolean verbose,
            int logEvery,
            boolean noSleep
    ) {
        this.queue = queue;
        this.producerId = producerId;
        this.jobsToProduce = jobsToProduce;
        this.rand = new Random(seed);
        this.verbose = verbose;
        this.logEvery = logEvery;
        this.noSleep = noSleep;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < jobsToProduce; i++) {

                // Generate simulated job duration
                int durationMs = 200 + rand.nextInt(401);

                // Disable simulated work if benchmarking
                if (noSleep) {
                    durationMs = 0;
                }

                int jobId = producerId * 1000 + i;

                Job job = new Job(jobId, durationMs);

                queue.put(job);

                if (verbose && i % logEvery == 0) {
                    System.out.println(
                            "Producer " + producerId + " produced " + job
                    );
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


