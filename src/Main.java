import queue.JobQueue;
import worker.Consumer;
import worker.Producer;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        int capacity = 5;
        int producers = 2;
        int consumers = 2;
        int jobsPerProducer = 10;

        JobQueue queue = new JobQueue(capacity);

        Thread[] producerThreads = new Thread[producers];
        Thread[] consumerThreads = new Thread[consumers];

        // Keep the Consumer objects so we can read processed counts after threads finish
        Consumer[] consumerWorkers = new Consumer[consumers];

        // Start consumers first: they will block on take() until jobs arrive
        for (int i = 0; i < consumers; i++) {
            consumerWorkers[i] = new Consumer(queue, i + 1);
            consumerThreads[i] = new Thread(consumerWorkers[i], "Consumer-" + (i + 1));
            consumerThreads[i].start();
        }

        // Start producers: they will produce jobs and put() into the queue
        for (int i = 0; i < producers; i++) {
            producerThreads[i] = new Thread(
                    new Producer(queue, i + 1, jobsPerProducer, 100L + i),
                    "Producer-" + (i + 1)
            );
            producerThreads[i].start();
        }

        // Wait for all producers to finish generating jobs
        for (Thread t : producerThreads) {
            t.join();
        }

        // Signal shutdown: consumers will exit after the queue drains
        queue.shutdown();

        // Wait for consumers to finish processing remaining jobs
        for (Thread t : consumerThreads) {
            t.join();
        }

        // ----- METRICS SUMMARY -----
        int totalProcessed = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        System.out.println("\n=== Metrics Summary ===");
        for (Consumer c : consumerWorkers) {
            int count = c.getProcessedCount();
            totalProcessed += count;
            min = Math.min(min, count);
            max = Math.max(max, count);
            System.out.println("Consumer " + c.getConsumerId() + " processed: " + count);
        }
        System.out.println("Total processed: " + totalProcessed);
        System.out.println("Min processed by a consumer: " + min);
        System.out.println("Max processed by a consumer: " + max);

        System.out.println("\nAll jobs processed. Queue shut down cleanly.");
    }
}

