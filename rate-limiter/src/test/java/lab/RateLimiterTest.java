package lab;

import static org.junit.jupiter.api.Assertions.*;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {
    @Test
    void burstAboveLimitIsRejectedThenRefills() throws Exception {
        var rl = RateLimiter.of("orders-api", RateLimiterConfig.custom()
                .limitForPeriod(5).limitRefreshPeriod(Duration.ofMillis(300)).timeoutDuration(Duration.ZERO).build());
        int ok = 0, rejected = 0;
        for (int i = 0; i < 20; i++) {
            try {
                rl.executeSupplier(() -> "ok");
                ok++;
            } catch (RequestNotPermitted e) {
                rejected++;
            }
        }
        // the first refresh may land mid-loop, so allow one extra quota but never the full burst
        assertTrue(ok >= 5 && ok <= 10, "ok=" + ok);
        assertEquals(20, ok + rejected);
        assertTrue(rejected >= 10);

        Thread.sleep(400); // next refresh period grants a fresh quota
        assertEquals("ok", rl.executeSupplier(() -> "ok"));
    }
}
