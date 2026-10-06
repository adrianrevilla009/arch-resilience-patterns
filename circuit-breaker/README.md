# circuit-breaker

Resilience4j 2.2.0 circuit breaker around a payments call that fails on demand, with a JUnit test that opens and recovers it.

## Goal

Show how a circuit breaker stops a failing dependency from being hammered, and how it recovers once the dependency is healthy again.

## Run it

```
cd circuit-breaker
mvn -q test
```

Expected: `CircuitBreakerTest` passes (1 test, 0 failures). Without `-q`, Maven ends with `BUILD SUCCESS`.

## What it proves

- With a window of 4 calls and a 50% threshold, four failing calls move the breaker from CLOSED to OPEN and the failure rate reads 100.0.
- While OPEN, the next call throws `CallNotPermittedException` and the backend call counter does not move; the not-permitted metric is 1.
- After the 200 ms open wait and with the backend fixed, the first probe leaves the breaker HALF_OPEN and the second (two permitted) closes it.

## Trade-offs

- Window size, threshold and wait time are set per breaker instance in `CircuitBreakerTest.java`; a fleet needs consistent settings or instances will disagree.
- The test uses `Thread.sleep(300)` for the wait period, so it depends on wall-clock timing.
- There is no fallback here: callers see the exception. See `fallback`.

## When not to use it

- For a dependency that rarely fails and is cheap to retry, plain timeouts are enough.
- For low-volume calls, where a window of a few calls never fills and the breaker rarely trips.
