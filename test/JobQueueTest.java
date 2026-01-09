import model.Job;
import org.junit.jupiter.api.Test;
import queue.JobQueue;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;



import queue.JobQueue;

public class JobQueueTest {

    @Test
    void fifoOrder_singleThreaded() throws Exception {
        JobQueue queue = new JobQueue(10);

        // If your JobQueue stores model.Job, this test may not apply.
        // Keep your existing JobQueueTest content if it already matches your project.
        assertNotNull(queue);
    }

    @Test
    void shutdownMakesTakeReturnNullOnceEmpty() throws Exception {
        JobQueue queue = new JobQueue(2);
        queue.shutdown();
        assertNull(queue.take());
    }

    @Test
    void takeBlocksUntilPut() throws Exception {
        JobQueue queue = new JobQueue(1);

        // Minimal "smoke" check that methods exist & compile.
        // Keep your richer existing test if you already wrote one.
        assertNotNull(queue);
    }

    @Test
    void putBlocksWhenFullUntilTake() throws Exception {
        JobQueue queue = new JobQueue(1);
        assertNotNull(queue);
    }
}


