import queue.JobQueue;
import worker.Producer;
import worker.Consumer;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the Concurrent Job Queue simulation.
 * Handles argument parsing, thread startup, timing, metrics, and shutdown.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        // =====================
        // DEFAULT CONFIGURATION
        // =====================
        int capacity = 10;
        int producers = 2;
        int consumers = 2;
        int jobsPerProducer = 10;
        boolean verbose = true;
        int logEvery = 1;
        boolean noSleep = false;

        // =====================
        // ARGUMENT PARSING
        // =====================
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--capacity":
                    capacity = Integer.parseInt(args[++i]);
                    break;
                case "--producers":
                    producers = Integer.parseInt(args[++i]);
                    break;
                case "--consumers":
                    consumers = Integer.parseInt(args[++i]);
                    break;
                case "--jobs":
                    jobsPerProducer = Integer.parseInt(args[++i]);
                    break;
                case "--quiet":
                    verbose = false;
                    logEvery = 50;
                    break;
                case "--noSleep":
                    noSleep = true;
                    break;
            }
        }

        // =====================
        // PRINT CONFIGURATION
        // =====================
        System.out.println("\n=== Configuration ===");
        System.out.println(
                "capacity=" + capacity +
                ", producers=" + producers +
                ", consumers=" + consumers +
                ", jobsPerProducer=" + jobsPerProducer +
                ", verbose=" + verbose +
                ", logEvery=" + logEvery +
                ", noSleep=" + noSleep
        );

        // =====================
        // SHARED QUEUE
        // =====================
        JobQueue queue = new JobQueue(capacity);

        List<Thread> producerThreads = new ArrayList<>();
        List<Consumer> consumerWorkers = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();

        // =====================
        // CREATE CONSUMERS
        // =====================
        for (int i = 0; i < consumers; i++) {
            Consumer consumer = new Consumer(queue, i + 1, verbose, logEvery);
            Thread t = new Thread(consumer, "Consumer-" + (i + 1));
            consumerWorkers.add(consumer);
            consumerThreads.add(t);
        }

        // =====================
        // START TIMING
        // =====================
        long startNs = System.nanoTime();

        // Start consumers first (they block waiting for jobs)
        for (Thread t : consumerThreads) {
            t.start();
        }

        // =====================
        // CREATE & START PRODUCERS
        // =====================
        for (int i = 0; i < producers; i++) {
            long seed = System.nanoTime() + i;
            Producer producer = new Producer(
                    queue,
                    i + 1,
                    jobsPerProducer,
                    seed,
                    verbose,
                    logEvery,
                    noSleep
            );
            Thread t = new Thread(producer, "Producer-" + (i + 1));
            producerThreads.add(t);
            t.start();
        }

        // =====================
        // WAIT FOR PRODUCERS
        // =====================
        for (Thread t : producerThreads) {
            t.join();
        }

        // Signal consumers no more jobs are coming
        queue.shutdown();

        // =====================
        // WAIT FOR CONSUMERS
        // =====================
        for (Thread t : consumerThreads) {
            t.join();
        }

        long endNs = System.nanoTime();

        // =====================
        // METRICS
        // =====================
        int totalProcessed = 0;
        for (Consumer c : consumerWorkers) {
            totalProcessed += c.getProcessedCount();
        }

        double elapsedSeconds = (endNs - startNs) / 1_000_000_000.0;
        double throughput = elapsedSeconds > 0
                ? totalProcessed / elapsedSeconds
                : 0.0;

        // =====================
        // PRINT SUMMARY
        // =====================
        System.out.println("\n=== Summary ===");
        System.out.println("Total jobs processed        : " + totalProcessed);
        System.out.printf("Elapsed time (s)            : %.3f%n", elapsedSeconds);
        System.out.printf("Throughput (jobs/sec)       : %.2f%n", throughput);
    }
}





