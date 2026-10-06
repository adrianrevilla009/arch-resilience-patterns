# rate-limiter

Resilience4j rate limiter allowing 5 calls per 300 ms period, with a test that fires a burst of 20 calls and then waits for a refill.

## Goal

Show how a rate limiter rejects excess calls immediately and grants a fresh quota when the refresh period elapses.

## Run it

```
cd rate-limiter
mvn -q test
```

Expected: `RateLimiterTest` passes (1 test, 0 failures); without `-q`, Maven ends with `BUILD SUCCESS`.

## What it proves

- Of 20 immediate calls with a zero wait timeout, between 5 and 10 succeed and at least 10 throw `RequestNotPermitted`; the total is always 20.
- The upper bound of 10 allows for one refresh landing in the middle of the loop, so the exact count varies per run.
- After a 400 ms sleep a new call succeeds, because the next period granted a fresh quota.

## Trade-offs

- The limit is per instance in memory; several service instances each allow their own quota, so the total is higher.
- The assertion is a range, not an exact number, because the test depends on real time.
- Rejecting at once (timeout zero) favours latency; a non-zero timeout would queue callers instead.

## When not to use it

- For a fleet-wide quota, use a shared store or a gateway rather than a per-process limiter.
- To protect against overload from concurrency rather than rate; see `bulkhead` or `load-shedding`.
