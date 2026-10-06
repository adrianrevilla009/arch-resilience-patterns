package lab;

import static org.junit.jupiter.api.Assertions.*;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BulkheadTest {
    static Bulkhead compartment(String name) {
        return Bulkhead.of(name, BulkheadConfig.custom().maxConcurrentCalls(2).maxWaitDuration(Duration.ZERO).build());
    }

    @Test
    void slowDependencyCannotConsumeMoreThanItsCompartment() throws Exception {
        var slow = compartment("slow-dep");
        var healthy = compartment("healthy-dep");
        var release = new CountDownLatch(1);
        var started = new CountDownLatch(2);

        // two callers hang inside the slow dependency (injected failure: it never answers)
        for (int i = 0; i < 2; i++) {
            Thread.ofVirtual().start(() -> slow.executeRunnable(() -> {
                started.countDown();
                try { release.await(); } catch (InterruptedException ignored) { }
            }));
        }
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertEquals(0, slow.getMetrics().getAvailableConcurrentCalls());

        // third call to the slow dependency is rejected immediately instead of queueing
        assertThrows(BulkheadFullException.class, () -> slow.executeSupplier(() -> "x"));
        // the other dependency is unaffected
        assertEquals("fine", healthy.executeSupplier(() -> "fine"));

        release.countDown();
    }
}
