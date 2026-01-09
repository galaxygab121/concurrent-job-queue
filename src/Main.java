import queue.JobQueue;
import worker.Consumer;
import worker.Producer;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        // ---- DEFAULTS ----
        int capacity = 5;
        int producers = 2;
        int consumers = 2;
        int jobsPerProducer = 10;

        boolean verbose = true; // default: show logs
        int logEvery = 50;      // default: print only occasionally

        // ---- PARSE ARGS ----
        for (int i = 0; i < args.length; i++) {
            String a = args[i];

            if (a.equalsIgnoreCase("--capacity") && i + 1 < args.length) {
                capacity = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--producers") && i + 1 < args.length) {
                producers = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--consumers") && i + 1 < args.length) {
                consumers = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--jobs") && i + 1 < args.length) {
                jobsPerProducer = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--quiet")) {
                verbose = false; // disables noisy per-job logs
            } else if (a.equalsIgnoreCase("--logEvery") && i + 1 < args.length) {
                logEvery = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--help")) {
                printUsageAndExit();
            }
        }

        if (capacity <= 0 || producers <= 0 || consumers <= 0 || jobsPerProducer < 0) {
            System.out.println("Invalid arguments.");
            printUsageAndExit();
        }

        System.out.println("=== Configuration ===");
        System.out.println("capacity=" + capacity
                + ", producers=" + producers
                + ", consumers=" + consumers
                + ", jobsPerProducer=" + jobsPerProducer
                + ", verbose=" + verbose
                + ", logEvery=" + logEvery);
        System.out.println();

        JobQueue queue = new JobQueue(capacity);

        Thread[] producerThreads = new Thread[producers];
        Thread[] consumerThreads = new Thread[consumers];
        Consumer[] consumerWorkers = new Consumer[consumers];

        // ---- TIMING START ----
        long startNs = System.nanoTime();

        // Start consumers first
        for (int i = 0; i < consumers; i++) {
            consumerWorkers[i] = new Consumer(queue, i + 1, verbose, logEvery);
            consumerThreads[i] = new Thread(consumerWorkers[i], "Consumer-" + (i + 1));
            consumerThreads[i].start();
        }

        // Start producers
        for (int i = 0; i < producers; i++) {
            long seed = 100L + i;
            producerThreads[i] = new Thread(
                    new Producer(queue, i + 1, jobsPerProducer, seed, verbose, logEvery),
                    "Producer-" + (i + 1)
            );
            producerThreads[i].start();
        }

        // Wait producers
        for (Thread t : producerThreads) {
            t.join();
        }

        // Shutdown so consumers exit once queue drains
        queue.shutdown();

        // Wait consumers
        for (Thread t : consumerThreads) {
            t.join();
        }

        // ---- TIMING END ----
        long endNs = System.nanoTime();

        // ---- METRICS SUMMARY ----
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

        System.out.println("Total processed              : " + totalProcessed);
        System.out.println("Min processed by a consumer  : " + min);
        System.out.println("Max processed by a consumer  : " + max);

        // ---- THROUGHPUT ----
        double elapsedSeconds = (endNs - startNs) / 1_000_000_000.0;
        double jobsPerSecond = (elapsedSeconds > 0.0) ? (totalProcessed / elapsedSeconds) : 0.0;

        System.out.println(String.format("Elapsed time (s)             : %.3f", elapsedSeconds));
        System.out.println(String.format("Throughput (jobs/sec)        : %.2f", jobsPerSecond));

        // ---- FAIRNESS REPORT ----
        if (min == 0) {
            System.out.println("Fairness WARNING: at least one consumer processed 0 jobs (possible starvation).");
        } else {
            double imbalanceRatio = (double) max / (double) min;
            System.out.println("Fairness imbalance ratio (max/min): " + String.format("%.2f", imbalanceRatio));
        }
        System.out.println("Fairness spread (max - min)  : " + (max - min));

        System.out.println("\nAll jobs processed. Queue shut down cleanly.");
    }

    private static void printUsageAndExit() {
        System.out.println("Usage:");
        System.out.println("  java -cp out Main");
        System.out.println("  java -cp out Main --capacity 50 --producers 8 --consumers 8 --jobs 200 --quiet");
        System.out.println();
        System.out.println("Flags:");
        System.out.println("  --capacity   <int>   queue capacity");
        System.out.println("  --producers  <int>   number of producer threads");
        System.out.println("  --consumers  <int>   number of consumer threads");
        System.out.println("  --jobs       <int>   jobs per producer");
        System.out.println("  --quiet              disable verbose per-job logs (recommended for throughput)");
        System.out.println("  --logEvery   <int>   print progress every N jobs (when not quiet)");
        System.out.println("  --help               show this message");
        System.exit(0);
    }
}




