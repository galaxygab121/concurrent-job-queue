import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import queue.JobQueue;
import worker.Consumer;
import worker.Producer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProducerConsumerIntegrationTest {

    @Test
    void endToEnd_allJobsProcessed_noDeadlock() throws Exception {
        // (keep your existing end-to-end test as-is)
    }

    @Test
    void fairness_distribution_is_reasonable() throws Exception {
        int capacity = 50;
        int producers = 8;
        int consumers = 8;
        int jobsPerProducer = 200;

        boolean verbose = false;
        int logEvery = 50;

        boolean producerNoSleep = true;
        boolean consumerNoSleep = true;

        JobQueue queue = new JobQueue(capacity);

        // --- Start consumers ---
        List<Consumer> consumerWorkers = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();

        for (int i = 0; i < consumers; i++) {
            Consumer c = new Consumer(queue, i + 1, verbose, logEvery, consumerNoSleep);
            consumerWorkers.add(c);

            Thread t = new Thread(c, "Consumer-" + (i + 1));
            consumerThreads.add(t);
            t.start();
        }

        // --- Start producers ---
        List<Thread> producerThreads = new ArrayList<>();
        for (int i = 0; i < producers; i++) {
            long seed = 100L + i; // deterministic-ish
            Producer p = new Producer(queue, i + 1, jobsPerProducer, seed, verbose, logEvery, producerNoSleep);

            Thread t = new Thread(p, "Producer-" + (i + 1));
            producerThreads.add(t);
            t.start();
        }

        // --- Wait for producers then shutdown queue ---
        for (Thread t : producerThreads) t.join();

        queue.shutdown();

        // --- Wait for consumers to drain queue ---
        for (Thread t : consumerThreads) t.join();

        // --- Collect counts ---
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
        assertEquals(expectedTotal, total, "All jobs should be processed");

        // --- Fairness metrics ---
        double jain = jainsFairnessIndex(counts);
        double gini = giniCoefficient(counts);
        double minShare = (total == 0) ? 0.0 : (min / (double) total);

        // --- Report (useful in CI logs) ---
        System.out.println("\n=== Fairness Test Report ===");
        System.out.println("Counts: " + Arrays.toString(counts));
        System.out.println("Total : " + total);
        System.out.printf("Jain  : %.4f%n", jain);
        System.out.printf("Gini  : %.4f%n", gini);
        System.out.printf("MinShare: %.4f (min=%d, max=%d)%n", minShare, min, max);

        // --- Tightened thresholds (based on your new output: Jain~0.9998, minShare~0.1231) ---
        // These are still safe but will catch regressions/starvation.
        final double JAIN_THRESHOLD = 0.98;     // close to perfect, but tolerant
        final double GINI_THRESHOLD = 0.05;     // low inequality expected
        final double MIN_SHARE_THRESHOLD = 0.10; // 8 consumers -> fair share ~0.125

        // Optional: hard starvation check (good since your new output shows fairness is fixed)
        assertTrue(min > 0, "Starvation detected: at least one consumer processed 0 jobs");

        assertTrue(jain >= JAIN_THRESHOLD, "Fairness too low (Jain index)");
        assertTrue(gini <= GINI_THRESHOLD, "Inequality too high (Gini coefficient)");
        assertTrue(minShare >= MIN_SHARE_THRESHOLD, "One consumer received too small a share of jobs");
    }

    /** Jain's Fairness Index: (sum(x)^2) / (n * sum(x^2)) in (0,1] */
    private static double jainsFairnessIndex(int[] counts) {
        double sum = 0.0;
        double sumSq = 0.0;
        for (int c : counts) {
            sum += c;
            sumSq += (double) c * c;
        }
        int n = counts.length;
        if (n == 0 || sumSq == 0.0) return 0.0;
        return (sum * sum) / (n * sumSq);
    }

    /**
     * Gini coefficient (0..1):
     * 0 = perfectly equal distribution, 1 = maximal inequality.
     * Uses sorted formula:
     * G = (2*Σ(i*xi))/(n*Σxi) - (n+1)/n
     */
    private static double giniCoefficient(int[] counts) {
        int n = counts.length;
        if (n == 0) return 0.0;

        int[] x = Arrays.copyOf(counts, n);
        Arrays.sort(x);

        long sum = 0;
        for (int v : x) sum += v;
        if (sum == 0) return 0.0;

        long weightedSum = 0;
        for (int i = 0; i < n; i++) {
            weightedSum += (long) (i + 1) * x[i];
        }

        double g = (2.0 * weightedSum) / (n * (double) sum) - (n + 1.0) / n;
        return Math.max(0.0, Math.min(1.0, g));
    }
}


