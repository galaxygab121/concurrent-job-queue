import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import queue.JobQueue;
import worker.Consumer;
import worker.Producer;

/**
 * Integration tests for the Producer–Consumer system.
 * - Verifies all jobs processed (no deadlocks)
 * - Runs 5 trials and asserts worst-case fairness
 */
public class ProducerConsumerIntegrationTest {

    @Test
    void endToEnd_allJobsProcessed_noDeadlock() throws Exception {
        int capacity = 20;
        int producers = 4;
        int consumers = 4;
        int jobsPerProducer = 100;

        JobQueue queue = new JobQueue(capacity);

        List<Consumer> consumerWorkers = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();
        for (int i = 0; i < consumers; i++) {
            Consumer c = new Consumer(queue, i + 1, false, 50, true);
            consumerWorkers.add(c);

            Thread t = new Thread(c, "Consumer-" + (i + 1));
            consumerThreads.add(t);
            t.start();
        }

        List<Thread> producerThreads = new ArrayList<>();
        for (int i = 0; i < producers; i++) {
            Producer p = new Producer(queue, i + 1, jobsPerProducer, i + 42, false, 50, true);

            Thread t = new Thread(p, "Producer-" + (i + 1));
            producerThreads.add(t);
            t.start();
        }

        for (Thread t : producerThreads) t.join();
        queue.shutdown();
        for (Thread t : consumerThreads) t.join();

        int totalProcessed = consumerWorkers.stream()
                .mapToInt(Consumer::getProcessedCount)
                .sum();

        assertEquals(producers * jobsPerProducer, totalProcessed, "All jobs should be processed");
    }

    /**
     * Runs 5 trials and asserts on WORST-CASE fairness metrics:
     * - min Jain
     * - max Gini
     * - min MinShare
     *
     * Also asserts that no consumer starves (min processed > 0) in any trial.
     */
    @Test
    void fairness_distribution_is_reasonable_worstCaseOver5Trials() throws Exception {

        // ---------- Test configuration ----------
        final int trials = 5;
        final int capacity = 50;
        final int producers = 8;
        final int consumers = 8;
        final int jobsPerProducer = 200;

        final boolean verbose = false;
        final int logEvery = 50;
        final boolean producerNoSleep = true;
        final boolean consumerNoSleep = true;

        // Based on your new output (Jain ~0.9998, Gini ~0.006–0.008, MinShare ~0.122–0.123)
        // These are "tight but safe" thresholds for worst-case across 5 trials.
        final double JAIN_THRESHOLD = 0.9970;
        final double GINI_THRESHOLD = 0.0200;
        final double MIN_SHARE_THRESHOLD = 0.1200;

        double worstJain = Double.POSITIVE_INFINITY;        // we want min
        double worstMinShare = Double.POSITIVE_INFINITY;    // we want min
        double worstGini = Double.NEGATIVE_INFINITY;        // we want max

        int worstTrialIndex = -1;
        int[] worstCounts = null;

        for (int trial = 1; trial <= trials; trial++) {

            JobQueue queue = new JobQueue(capacity);

            // ----- Start consumers -----
            List<Consumer> consumerWorkers = new ArrayList<>();
            List<Thread> consumerThreads = new ArrayList<>();
            for (int i = 0; i < consumers; i++) {
                Consumer c = new Consumer(queue, i + 1, verbose, logEvery, consumerNoSleep);
                consumerWorkers.add(c);

                Thread t = new Thread(c, "Trial-" + trial + "-Consumer-" + (i + 1));
                consumerThreads.add(t);
                t.start();
            }

            // ----- Start producers -----
            List<Thread> producerThreads = new ArrayList<>();
            for (int i = 0; i < producers; i++) {
                long seed = (trial * 10_000L) + (100 + i); // deterministic per trial
                Producer p = new Producer(queue, i + 1, jobsPerProducer, seed, verbose, logEvery, producerNoSleep);

                Thread t = new Thread(p, "Trial-" + trial + "-Producer-" + (i + 1));
                producerThreads.add(t);
                t.start();
            }

            for (Thread t : producerThreads) t.join();
            queue.shutdown();
            for (Thread t : consumerThreads) t.join();

            // ----- Collect counts -----
            int[] counts = new int[consumerWorkers.size()];
            int total = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for (int i = 0; i < consumerWorkers.size(); i++) {
                int processed = consumerWorkers.get(i).getProcessedCount();
                counts[i] = processed;
                total += processed;
                min = Math.min(min, processed);
                max = Math.max(max, processed);
            }

            int expectedTotal = producers * jobsPerProducer;
            assertEquals(expectedTotal, total, "Trial " + trial + ": all jobs should be processed");
            assertTrue(min > 0, "Trial " + trial + ": starvation detected (a consumer processed 0 jobs)");

            double jain = jainsFairnessIndex(counts);
            double gini = giniCoefficient(counts);
            double minShare = (total == 0) ? 0.0 : (min / (double) total);

            // ----- Report -----
            System.out.println("\n=== Fairness Trial " + trial + " Report ===");
            System.out.println("Counts: " + Arrays.toString(counts));
            System.out.println("Total : " + total);
            System.out.printf("Jain  : %.4f%n", jain);
            System.out.printf("Gini  : %.4f%n", gini);
            System.out.printf("MinShare: %.4f (min=%d, max=%d)%n", minShare, min, max);

            // ----- Track worst-case -----
            if (jain < worstJain) worstJain = jain;
            if (gini > worstGini) worstGini = gini;
            if (minShare < worstMinShare) worstMinShare = minShare;

            // Save the trial counts when any metric sets a new worst record
            if (worstCounts == null || jain == worstJain || gini == worstGini || minShare == worstMinShare) {
                worstTrialIndex = trial;
                worstCounts = Arrays.copyOf(counts, counts.length);
            }
        }

        // ----- Final worst-case summary -----
        System.out.println("\n=== Worst-Case Summary (over " + trials + " trials) ===");
        System.out.println("Worst trial: " + worstTrialIndex);
        System.out.println("Counts     : " + Arrays.toString(worstCounts));
        System.out.printf("Min Jain   : %.4f (threshold %.4f)%n", worstJain, JAIN_THRESHOLD);
        System.out.printf("Max Gini   : %.4f (threshold %.4f)%n", worstGini, GINI_THRESHOLD);
        System.out.printf("Min MinShare: %.4f (threshold %.4f)%n", worstMinShare, MIN_SHARE_THRESHOLD);

        assertTrue(worstJain >= JAIN_THRESHOLD, "Worst-case Jain below threshold");
        assertTrue(worstGini <= GINI_THRESHOLD, "Worst-case Gini above threshold");
        assertTrue(worstMinShare >= MIN_SHARE_THRESHOLD, "Worst-case MinShare below threshold");
    }

    // ======================
    // Fairness metric helpers
    // ======================

    private static double jainsFairnessIndex(int[] counts) {
        double sum = 0.0;
        double sumSq = 0.0;

        for (int c : counts) {
            sum += c;
            sumSq += (double) c * c;
        }

        return (sum * sum) / (counts.length * sumSq);
    }

    private static double giniCoefficient(int[] counts) {
        int n = counts.length;

        double mean = Arrays.stream(counts).average().orElse(0.0);
        if (mean == 0) return 0.0;

        double sumDiff = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                sumDiff += Math.abs(counts[i] - counts[j]);
            }
        }

        return sumDiff / (2.0 * n * n * mean);
    }
}






