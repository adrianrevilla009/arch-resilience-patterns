package lab;

import static org.junit.jupiter.api.Assertions.*;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class FallbackTest {
    static final Map<String, String> CACHE = Map.of("o-1", "cached: 2 x book");

    static String liveLookup(String id) { throw new IllegalStateException("catalog down"); }

    @Test
    void degradesToCacheOnFailure() {
        Supplier<String> s = Decorators.ofSupplier(() -> liveLookup("o-1"))
                .withFallback(List.of(IllegalStateException.class), e -> CACHE.get("o-1"))
                .decorate();
        assertEquals("cached: 2 x book", s.get());
    }

    @Test
    void fallbackAlsoCoversOpenCircuit() {
        var cb = CircuitBreaker.of("catalog", CircuitBreakerConfig.custom()
                .slidingWindowSize(2).minimumNumberOfCalls(2).build());
        Supplier<String> s = Decorators.ofSupplier(() -> liveLookup("o-1"))
                .withCircuitBreaker(cb)
                .withFallback(List.of(IllegalStateException.class, CallNotPermittedException.class),
                        e -> "default: unavailable")
                .decorate();
        for (int i = 0; i < 5; i++) assertEquals("default: unavailable", s.get());
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }
}
