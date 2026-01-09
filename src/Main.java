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
                case "--logEvery":
                    logEvery = Integer.parseInt(args[++i]);
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
        // METRICS (THROUGHPUT)
        // =====================
        int totalProcessed = 0;
        int[] counts = new int[consumerWorkers.size()];

        for (int i = 0; i < consumerWorkers.size(); i++) {
            counts[i] = consumerWorkers.get(i).getProcessedCount();
            totalProcessed += counts[i];
        }

        double elapsedSeconds = (endNs - startNs) / 1_000_000_000.0;
        double throughput = (elapsedSeconds > 0) ? (totalProcessed / elapsedSeconds) : 0.0;

        // =====================
        // FAIRNESS / STARVATION
        // =====================
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int starved = 0;

        for (int c : counts) {
            min = Math.min(min, c);
            max = Math.max(max, c);
            if (c == 0) starved++;
        }

        double mean = (counts.length > 0) ? (totalProcessed / (double) counts.length) : 0.0;

        // Std dev
        double variance = 0.0;
        for (int c : counts) {
            double diff = c - mean;
            variance += diff * diff;
        }
        variance = (counts.length > 0) ? (variance / counts.length) : 0.0;
        double stdDev = Math.sqrt(variance);

        // Coefficient of variation: stdDev / mean
        double cv = (mean > 0) ? (stdDev / mean) : 0.0;

        // Imbalance ratio: max/min (avoid divide-by-zero)
        double imbalanceRatio = (min > 0) ? (max / (double) min) : Double.POSITIVE_INFINITY;

        int spread = max - min;

        // Simple label (tweak thresholds however you want)
        // CV near 0 => very even distribution
        String fairnessLabel = (starved > 0) ? "STARVATION" : (cv <= 0.20 ? "FAIR" : "UNEVEN");

        // =====================
        // PRINT SUMMARY
        // =====================
        System.out.println("\n=== Summary ===");
        System.out.println("Total jobs processed        : " + totalProcessed);
        System.out.printf("Elapsed time (s)            : %.3f%n", elapsedSeconds);
        System.out.printf("Throughput (jobs/sec)       : %.2f%n", throughput);

        System.out.println("\n=== Fairness Report ===");
        for (Consumer c : consumerWorkers) {
            int processed = c.getProcessedCount();
            double pct = (totalProcessed > 0) ? (100.0 * processed / totalProcessed) : 0.0;
            System.out.printf("Consumer %d processed: %d (%.2f%%)%n", c.getConsumerId(), processed, pct);
        }

        System.out.println("Min processed               : " + min);
        System.out.println("Max processed               : " + max);
        System.out.println("Spread (max-min)            : " + spread);
        System.out.printf("Mean                         : %.2f%n", mean);
        System.out.printf("Std Dev                      : %.2f%n", stdDev);
        System.out.printf("Coeff of Variation (CV)      : %.3f%n", cv);
        System.out.printf("Imbalance ratio (max/min)    : %s%n",
                Double.isInfinite(imbalanceRatio) ? "INF (min=0)" : String.format("%.2f", imbalanceRatio));
        System.out.println("Starved consumers (0 jobs)  : " + starved);
        System.out.println("Fairness label              : " + fairnessLabel);
    }
}






