import org.junit.jupiter.api.Test;
import queue.JobQueue;
import worker.Consumer;
import worker.Producer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProducerConsumerIntegrationTest {

    @Test
    void producersAndConsumersProcessAllJobs_noDeadlock() throws Exception {
        int capacity = 20;
        int producers = 4;
        int consumers = 4;
        int jobsPerProducer = 200;

        boolean verbose = false;
        int logEvery = 50;
        boolean noSleep = true; // IMPORTANT: keep tests fast + deterministic

        JobQueue queue = new JobQueue(capacity);

        List<Consumer> consumerWorkers = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();
        for (int i = 0; i < consumers; i++) {
            Consumer c = new Consumer(queue, i + 1, verbose, logEvery);
            Thread t = new Thread(c, "Consumer-" + (i + 1));
            consumerWorkers.add(c);
            consumerThreads.add(t);
            t.start();
        }

        List<Thread> producerThreads = new ArrayList<>();
        for (int i = 0; i < producers; i++) {
            long seed = 100L + i;
            Producer p = new Producer(queue, i + 1, jobsPerProducer, seed, verbose, logEvery, noSleep);
            Thread t = new Thread(p, "Producer-" + (i + 1));
            producerThreads.add(t);
            t.start();
        }

        // Wait producers
        for (Thread t : producerThreads) t.join();

        // Shutdown and wait consumers
        queue.shutdown();
        for (Thread t : consumerThreads) t.join();

        int totalProcessed = 0;
        for (Consumer c : consumerWorkers) totalProcessed += c.getProcessedCount();

        int expected = producers * jobsPerProducer;
        assertEquals(expected, totalProcessed, "All produced jobs should be processed");
    }

    @Test
    void fairness_noStarvationInNoSleepMode() throws Exception {
        int capacity = 50;
        int producers = 8;
        int consumers = 8;
        int jobsPerProducer = 200;

        boolean verbose = false;
        int logEvery = 50;
        boolean noSleep = true;

        JobQueue queue = new JobQueue(capacity);

        List<Consumer> consumerWorkers = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();
        for (int i = 0; i < consumers; i++) {
            Consumer c = new Consumer(queue, i + 1, verbose, logEvery);
            Thread t = new Thread(c, "Consumer-" + (i + 1));
            consumerWorkers.add(c);
            consumerThreads.add(t);
            t.start();
        }

        List<Thread> producerThreads = new ArrayList<>();
        for (int i = 0; i < producers; i++) {
            long seed = 100L + i;
            Producer p = new Producer(queue, i + 1, jobsPerProducer, seed, verbose, logEvery, noSleep);
            Thread t = new Thread(p, "Producer-" + (i + 1));
            producerThreads.add(t);
            t.start();
        }

        for (Thread t : producerThreads) t.join();

        queue.shutdown();
        for (Thread t : consumerThreads) t.join();

        // In noSleep mode, starvation should be unlikely; fail if any consumer got 0
        int starved = 0;
        for (Consumer c : consumerWorkers) {
            if (c.getProcessedCount() == 0) starved++;
        }
        assertEquals(0, starved, "No consumer should starve in this run");
    }
}
