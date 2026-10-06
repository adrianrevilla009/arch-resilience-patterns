package lab;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LoadSheddingTest {
    enum Priority { CRITICAL, NORMAL }

    /** Sheds by in-flight count: NORMAL traffic is refused at 70% of capacity, CRITICAL only at 100%. */
    static class Shedder {
        final int capacity;
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger shed = new AtomicInteger();

        Shedder(int capacity) { this.capacity = capacity; }

        boolean tryAcquire(Priority p) {
            int limit = p == Priority.CRITICAL ? capacity : (int) (capacity * 0.7);
            if (inFlight.incrementAndGet() > limit) {
                inFlight.decrementAndGet();
                shed.incrementAndGet();
                return false; // caller maps this to HTTP 503 + Retry-After
            }
            return true;
        }

        void release() { inFlight.decrementAndGet(); }
    }

    @Test
    void overloadShedsLowPriorityFirstAndKeepsServingCritical() throws Exception {
        var shedder = new Shedder(10);
        var hold = new CountDownLatch(1);
        var started = new CountDownLatch(7);
        // injected overload: 7 slow NORMAL requests occupy 70% of capacity
        for (int i = 0; i < 7; i++) {
            assertTrue(shedder.tryAcquire(Priority.NORMAL));
            Thread.ofVirtual().start(() -> {
                started.countDown();
                try { hold.await(); } catch (InterruptedException ignored) { }
                shedder.release();
            });
        }
        assertTrue(started.await(2, TimeUnit.SECONDS));

        assertFalse(shedder.tryAcquire(Priority.NORMAL), "normal traffic is shed");
        assertTrue(shedder.tryAcquire(Priority.CRITICAL), "critical still admitted");
        assertEquals(1, shedder.shed.get());
        shedder.release();

        hold.countDown();
        Thread.sleep(100);
        assertTrue(shedder.tryAcquire(Priority.NORMAL), "recovers once load drains");
    }
}
