package worker; // declares this file belongs to the "worker" package

import model.Job; // import Job
import queue.JobQueue; // import JobQueue
import java.util.Random; // used to generate random job durations

public class Producer implements Runnable { // Producer runs in a thread, so it implements Runnable

    private final JobQueue queue; // reference to the shared job queue
    private final int producerId; // identifier for this producer (for logging + job IDs)
    private final int jobsToProduce; // number of jobs this producer will create
    private final Random rand; // random generator for job durations

    public Producer(JobQueue queue, int producerId, int jobsToProduce, long seed) {
        this.queue = queue; // store queue reference
        this.producerId = producerId; // store producer id
        this.jobsToProduce = jobsToProduce; // store number of jobs to produce
        this.rand = new Random(seed); // initialize random with a seed for reproducible runs
    }

    @Override
    public void run() { // the thread executes this method
        try {
            for (int i = 0; i < jobsToProduce; i++) { // loop to generate jobs
                int jobId = producerId * 1000 + i; // unique-ish job id based on producer + index
                int durationMs = 200 + rand.nextInt(401); // random duration between 200..600 ms

                Job job = new Job(jobId, durationMs); // create a new job object
                queue.put(job); // put job into the shared queue (may BLOCK if queue is full)

                System.out.println("Producer " + producerId + " produced " + job); // log

                Thread.sleep(75); // small pause to simulate time between job submissions
            }
        } catch (InterruptedException ignored) { // if thread is interrupted while waiting/sleeping
            Thread.currentThread().interrupt(); // re-set interrupt flag (best practice)
        }
    }
}
