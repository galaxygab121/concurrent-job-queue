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

        // ---- PARSE CLI ARGS ----
        // Supported flags:
        // --capacity <int>
        // --producers <int>
        // --consumers <int>
        // --jobs <int>        (jobs per producer)
        for (int i = 0; i < args.length; i++) {
            String a = args[i];

            // helper: safely read next value
            if (a.equalsIgnoreCase("--capacity") && i + 1 < args.length) {
                capacity = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--producers") && i + 1 < args.length) {
                producers = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--consumers") && i + 1 < args.length) {
                consumers = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--jobs") && i + 1 < args.length) {
                jobsPerProducer = Integer.parseInt(args[++i]);
            } else if (a.equalsIgnoreCase("--help")) {
                printUsageAndExit();
            }
        }

        // Basic validation so stress tests don’t crash weirdly
        if (capacity <= 0 || producers <= 0 || consumers <= 0 || jobsPerProducer < 0) {
            System.out.println("Invalid arguments.");
            printUsageAndExit();
        }

        System.out.println("=== Config ===");
        System.out.println("capacity=" + capacity
                + ", producers=" + producers
                + ", consumers=" + consumers
                + ", jobsPerProducer=" + jobsPerProducer);
        System.out.println();

        JobQueue queue = new JobQueue(capacity);

        Thread[] producerThreads = new Thread[producers];
        Thread[] consumerThreads = new Thread[consumers];
        Consumer[] consumerWorkers = new Consumer[consumers];

        // Start consumers first (they block on empty queue)
        for (int i = 0; i < consumers; i++) {
            consumerWorkers[i] = new Consumer(queue, i + 1);
            consumerThreads[i] = new Thread(consumerWorkers[i], "Consumer-" + (i + 1));
            consumerThreads[i].start();
        }

        // Start producers
        for (int i = 0; i < producers; i++) {
            // Different seed per producer so stress runs vary but remain reproducible
            long seed = 100L + i;
            producerThreads[i] = new Thread(
                    new Producer(queue, i + 1, jobsPerProducer, seed),
                    "Producer-" + (i + 1)
            );
            producerThreads[i].start();
        }

        // Wait producers
        for (Thread t : producerThreads) {
            t.join();
        }

        // Shutdown after producers finish (consumers drain remaining jobs)
        queue.shutdown();

        // Wait consumers
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

    private static void printUsageAndExit() {
        System.out.println("Usage:");
        System.out.println("  java -cp out Main");
        System.out.println("  java -cp out Main --capacity 50 --producers 8 --consumers 8 --jobs 200");
        System.out.println("Flags:");
        System.out.println("  --capacity   <int>   queue capacity");
        System.out.println("  --producers  <int>   number of producer threads");
        System.out.println("  --consumers  <int>   number of consumer threads");
        System.out.println("  --jobs       <int>   jobs per producer");
        System.out.println("  --help              print this message");
        System.exit(0);
    }
}


