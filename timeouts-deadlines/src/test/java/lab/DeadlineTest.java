package lab;

import static org.junit.jupiter.api.Assertions.*;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class DeadlineTest {
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Deadline = absolute point in time; each hop derives its own timeout from what is left. */
    record Deadline(long expiresAtNanos) {
        static Deadline in(Duration d) { return new Deadline(System.nanoTime() + d.toNanos()); }
        Duration remaining() { return Duration.ofNanos(Math.max(0, expiresAtNanos - System.nanoTime())); }
    }

    @Test
    void timeLimiterCutsOffSlowCall() {
        var exec = Executors.newVirtualThreadPerTaskExecutor();
        var tl = TimeLimiter.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(100)).cancelRunningFuture(true).build());
        long t0 = System.nanoTime();
        assertThrows(TimeoutException.class,
                () -> tl.executeFutureSupplier(() -> CompletableFuture.supplyAsync(() -> { sleep(2000); return "late"; }, exec)));
        assertTrue(Duration.ofNanos(System.nanoTime() - t0).toMillis() < 1000);
    }

    @Test
    void deadlinePropagatesAndLaterHopsFailFast() {
        var deadline = Deadline.in(Duration.ofMillis(300));
        int hopsExecuted = 0;
        // each hop costs 120ms and refuses to start if the remaining budget cannot cover it
        for (int hop = 0; hop < 5; hop++) {
            if (deadline.remaining().toMillis() < 120) break;
            sleep(120);
            hopsExecuted++;
        }
        assertEquals(2, hopsExecuted, "third hop must be skipped: only ~60ms left");
    }
}
