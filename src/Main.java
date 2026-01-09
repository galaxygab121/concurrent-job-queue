import queue.JobQueue; // import JobQueue (no package for Main, so use imports)
import worker.Consumer; // import Consumer runnable
import worker.Producer; // import Producer runnable

public class Main { // entrypoint class (contains main method)

    public static void main(String[] args) throws InterruptedException { // program start
        int capacity = 5; // maximum jobs allowed in queue at any time

        int producers = 2; // number of producer threads
        int consumers = 2; // number of consumer threads
        int jobsPerProducer = 10; // how many jobs each producer creates

        JobQueue queue = new JobQueue(capacity); // create the shared bounded queue

        Thread[] producerThreads = new Thread[producers]; // array to store producer threads
        Thread[] consumerThreads = new Thread[consumers]; // array to store consumer threads

        // Start consumers first (they will block waiting for jobs if queue is empty)
        for (int i = 0; i < consumers; i++) {
            consumerThreads[i] = new Thread(
                    new Consumer(queue, i + 1), // create consumer runnable with id
                    "Consumer-" + (i + 1) // give thread a name for debugging
            );
            consumerThreads[i].start(); // start the consumer thread
        }

        // Start producers (they will add jobs into the queue)
        for (int i = 0; i < producers; i++) {
            producerThreads[i] = new Thread(
                    new Producer(queue, i + 1, jobsPerProducer, 100L + i), // create producer with seed
                    "Producer-" + (i + 1) // name the thread
            );
            producerThreads[i].start(); // start producer thread
        }

        // Wait for all producers to finish generating jobs
        for (Thread t : producerThreads) {
            t.join(); // blocks until producer thread completes
        }

        // Tell queue no more jobs will be produced; consumers will exit once queue drains
        queue.shutdown();

        // Wait for all consumers to finish processing remaining jobs and exit cleanly
        for (Thread t : consumerThreads) {
            t.join(); // blocks until consumer thread completes
        }

        System.out.println("All jobs processed. Queue shut down cleanly."); // final message
    }
}
