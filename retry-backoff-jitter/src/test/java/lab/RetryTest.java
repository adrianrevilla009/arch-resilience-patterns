package lab;

import static org.junit.jupiter.api.Assertions.*;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryTest {
    @Test
    void backoffGrowsExponentiallyWithoutJitter() {
        var f = IntervalFunction.ofExponentialBackoff(100, 2.0);
        assertEquals(100, f.apply(1));
        assertEquals(200, f.apply(2));
        assertEquals(400, f.apply(3));
    }

    @Test
    void jitterStaysInsideBandAndSpreadsClients() {
        var f = IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5);
        List<Long> waits = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            long w = f.apply(3); // base 400ms, +-50%
            assertTrue(w >= 200 && w <= 600, "wait " + w);
            waits.add(w);
        }
        assertTrue(waits.stream().distinct().count() > 10, "jitter must de-synchronise clients");
    }

    @Test
    void retriesTransientFailureThenSucceeds() {
        var calls = new AtomicInteger();
        var retry = Retry.of("flaky", RetryConfig.custom().maxAttempts(4)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(10, 2.0, 0.5)).build());
        String r = retry.executeSupplier(() -> {
            if (calls.incrementAndGet() < 3) throw new IllegalStateException("transient");
            return "ok";
        });
        assertEquals("ok", r);
        assertEquals(3, calls.get());
        assertEquals(1, retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
    }

    @Test
    void givesUpAfterMaxAttempts() {
        var calls = new AtomicInteger();
        var retry = Retry.of("dead", RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofMillis(5)).build());
        assertThrows(IllegalStateException.class, () -> retry.executeSupplier(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("down");
        }));
        assertEquals(3, calls.get());
    }
}
