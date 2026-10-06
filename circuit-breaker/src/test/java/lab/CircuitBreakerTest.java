package lab;

import static org.junit.jupiter.api.Assertions.*;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CircuitBreakerTest {
    final AtomicBoolean down = new AtomicBoolean(true);
    final AtomicInteger backendCalls = new AtomicInteger();

    String payments() {
        backendCalls.incrementAndGet();
        if (down.get()) throw new IllegalStateException("payments down");
        return "paid";
    }

    @Test
    void closedOpenHalfOpenClosed() throws Exception {
        var cb = CircuitBreaker.of("payments", CircuitBreakerConfig.custom()
                .slidingWindowSize(4).minimumNumberOfCalls(4).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(200))
                .permittedNumberOfCallsInHalfOpenState(2).build());
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        for (int i = 0; i < 4; i++) {
            assertThrows(IllegalStateException.class, () -> cb.executeSupplier(this::payments));
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertEquals(100.0f, cb.getMetrics().getFailureRate());

        // open: calls are rejected without touching the backend
        int before = backendCalls.get();
        assertThrows(CallNotPermittedException.class, () -> cb.executeSupplier(this::payments));
        assertEquals(before, backendCalls.get());
        assertEquals(1, cb.getMetrics().getNumberOfNotPermittedCalls());

        // backend recovers, wait period elapses, probes close the circuit
        down.set(false);
        Thread.sleep(300);
        assertEquals("paid", cb.executeSupplier(this::payments));
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
        assertEquals("paid", cb.executeSupplier(this::payments));
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }
}
