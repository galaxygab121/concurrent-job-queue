import model.Job;
import org.junit.jupiter.api.Test;
import queue.JobQueue;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class JobQueueTest {

    @Test
    void takeBlocksUntilPut() throws Exception {
        JobQueue q = new JobQueue(2);

        ExecutorService exec = Executors.newSingleThreadExecutor();

        Future<Job> future = exec.submit(() -> q.take());

        // Give the consumer time to block on take()
        Thread.sleep(100);

        // Now put a job, which should unblock take()
        Job j = new Job(1, 0);
        q.put(j);

        Job got = future.get(2, TimeUnit.SECONDS);
        assertNotNull(got);
        assertEquals(1, got.getId());

        exec.shutdownNow();
    }

    @Test
    void shutdownMakesTakeReturnNullOnceEmpty() throws Exception {
        JobQueue q = new JobQueue(2);

        // Put and take one job
        q.put(new Job(1, 0));
        Job a = q.take();
        assertNotNull(a);

        // Shutdown: now queue is empty and shutdown is true, so take should return null
        q.shutdown();
        Job b = q.take();
        assertNull(b);
    }

    @Test
    void boundedQueueDoesNotLoseJobsSingleThread() throws Exception {
        JobQueue q = new JobQueue(3);

        q.put(new Job(1, 0));
        q.put(new Job(2, 0));
        q.put(new Job(3, 0));

        assertEquals(1, q.take().getId());
        assertEquals(2, q.take().getId());
        assertEquals(3, q.take().getId());

        q.shutdown();
        assertNull(q.take());
    }
}
