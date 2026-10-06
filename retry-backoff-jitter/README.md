# retry-backoff-jitter

Resilience4j retry with exponential backoff and random jitter, checked by four JUnit tests in `RetryTest.java`.

## Goal

Show that retries should wait longer each time and be randomised, so many clients do not retry in lockstep, and that retrying must stop after a bounded number of attempts.

## Run it

```
cd retry-backoff-jitter
mvn -q test
```

Expected: `RetryTest` passes (4 tests, 0 failures); without `-q`, Maven ends with `BUILD SUCCESS`.

## What it proves

- Without jitter, a 100 ms base with multiplier 2 gives waits of 100, 200 and 400 ms for attempts 1 to 3.
- With 50% jitter, 50 samples of the attempt-3 wait all fall between 200 and 600 ms and produce more than 10 distinct values.
- A call that fails twice then succeeds returns `ok` after 3 calls, and the retry metrics record one success after a retry.
- A call that always fails stops after exactly 3 attempts and rethrows the original exception.

## Trade-offs

- Retries multiply load on a struggling dependency; they need a cap (as here) and ideally a circuit breaker in front.
- The tests assert on interval values, not on real elapsed time, so they do not show the total latency a caller feels.
- Only idempotent calls are safe to retry; the tests do not model that.

## When not to use it

- For non-idempotent operations such as charging a card, unless the call carries an idempotency key.
- When the failure is permanent (validation error, 4xx): retrying only adds delay.
