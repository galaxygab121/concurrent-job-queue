import queue.JobQueue;
import worker.Consumer;
import worker.Producer;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        // =====================
        // DEFAULT CONFIGURATION
        // =====================
        int capacity = 5;            // Max number of jobs allowed in the queue
        int producers = 2;           // Number of producer threads
        int consumers = 2;           // Number of consumer threads
        int jobsPerProducer = 10;    // Jobs each producer will generate

        // =====================
        // PARSE COMMAND-LINE ARGS
        // =====================
        // Supported flags:
        // --capacity <int>
        // --producers <int>
        // --consumers <int>
        // --jobs <int>
        // --help
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equalsIgnoreCase("--capacity") && i + 1 < args.length) {
                capacity = Integer.parseInt(args[++i]);
            } else if (arg.equalsIgnoreCase("--producers") && i + 1 < args.length) {
                producers = Integer.parseInt(args[++i]);
            } else if (arg.equalsIgnoreCase("--consumers") && i + 1 < args.length) {
                consumers = Integer.parseInt(args[++i]);
            } else if (arg.equalsIgnoreCase("--jobs") && i + 1 < args.length) {
                jobsPerProducer = Integer.parseInt(args[++i]);
            } else if (arg.equalsIgnoreCase("--help")) {
                printUsageAndExit();
            }
        }

        // =====================
        // BASIC VALIDATION
        // =====================
        if (capacity <= 0 || producers <= 0 || consumers <= 0 || jobsPerProducer < 0) {
            System.out.println("Invalid arguments provided.");
            printUsageAndExit();
        }

        // =====================
        // PRINT CONFIGURATION
        // =====================
        System.out.println("=== Configuration ===");
        System.out.println("Queue capacity      : " + capacity);
        System.out.println("Producers           : " + producers);
        System.out.println("Consumers           : " + consumers);
        System.out.println("Jobs per producer   : " + jobsPerProducer);
        System.out.println();

        // =====================
        // INITIALIZE QUEUE
        // =====================
        JobQueue queue = new JobQueue(capacity);

        // Arrays to manage threads and collect metrics
        Thread[] producerThreads = new Thread[producers];
        Thread[] consumerThreads = new Thread[consumers];
        Consumer[] consumerWorkers = new Consumer[consumers];

        // =====================
        // START CONSUMERS FIRST
        // =====================
        // Consumers block on take() until jobs arrive
        for (int i = 0; i < consumers; i++) {
            consumerWorkers[i] = new Consumer(queue, i + 1);
            consumerThreads[i] = new Thread(
                    consumerWorkers[i],
                    "Consumer-" + (i + 1)
            );
            consumerThreads[i].start();
        }

        // =====================
        // START PRODUCERS
        // =====================
        for (int i = 0; i < producers; i++) {
            long seed = 100L + i; // deterministic randomness per producer
            producerThreads[i] = new Thread(
                    new Producer(queue, i + 1, jobsPerProducer, seed),
                    "Producer-" + (i + 1)
            );
            producerThreads[i].start();
        }

        // =====================
        // WAIT FOR PRODUCERS
        // =====================
        for (Thread t : producerThreads) {
            t.join(); // Wait until all jobs are produced
        }

        // =====================
        // SHUTDOWN QUEUE
        // =====================
        // Signals consumers to exit once the queue is empty
        queue.shutdown();

        // =====================
        // WAIT FOR CONSUMERS
        // =====================
        for (Thread t : consumerThreads) {
            t.join(); // Wait until consumers finish processing
        }

        // =====================
        // METRICS + FAIRNESS REPORT
        // =====================
        int totalProcessed = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        System.out.println("\n=== Metrics Summary ===");

        for (Consumer c : consumerWorkers) {
            int count = c.getProcessedCount();
            totalProcessed += count;
            min = Math.min(min, count);
            max = Math.max(max, count);

            System.out.println(
                    "Consumer " + c.getConsumerId() +
                    " processed " + count + " jobs"
            );
        }

        System.out.println("Total jobs processed        : " + totalProcessed);
        System.out.println("Minimum jobs by a consumer  : " + min);
        System.out.println("Maximum jobs by a consumer  : " + max);

        // =====================
        // FAIRNESS ANALYSIS
        // =====================
        if (min == 0) {
            System.out.println(
                    "Fairness WARNING: At least one consumer processed 0 jobs (possible starvation)"
            );
        } else {
            double imbalanceRatio = (double) max / (double) min;
            System.out.println(
                    "Fairness imbalance ratio (max/min): " +
                    String.format("%.2f", imbalanceRatio)
            );
        }

        int spread = max - min;
        System.out.println("Fairness spread (max - min) : " + spread);

        System.out.println("\nAll jobs processed. Queue shut down cleanly.");
    }

    // =====================
    // HELP MESSAGE
    // =====================
    private static void printUsageAndExit() {
        System.out.println("Usage:");
        System.out.println("  java -cp out Main");
        System.out.println("  java -cp out Main --capacity 50 --producers 8 --consumers 8 --jobs 200");
        System.out.println();
        System.out.println("Flags:");
        System.out.println("  --capacity   <int>   Queue capacity");
        System.out.println("  --producers  <int>   Number of producer threads");
        System.out.println("  --consumers  <int>   Number of consumer threads");
        System.out.println("  --jobs       <int>   Jobs per producer");
        System.out.println("  --help              Show this help message");
        System.exit(0);
    }
}



