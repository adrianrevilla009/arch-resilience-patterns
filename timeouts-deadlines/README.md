# timeouts-deadlines

A Resilience4j `TimeLimiter` cutting off a slow call, and a small `Deadline` record that shares one time budget across several hops, both tested in `DeadlineTest.java`.

## Goal

Show that every call needs a timeout, and that a deadline passed along a call chain lets later hops skip work that can no longer finish in time.

## Run it

```
cd timeouts-deadlines
mvn -q test
```

Expected: `DeadlineTest` passes (2 tests, 0 failures); without `-q`, Maven ends with `BUILD SUCCESS`.

## What it proves

- A call that sleeps 2 s is cut off after a 100 ms limit with `TimeoutException`, and the test finishes in under 1 s.
- A `Deadline` of 300 ms with hops costing 120 ms runs exactly 2 hops; the third sees about 60 ms left and refuses to start.
- The deadline is an absolute point in time, so each hop derives its own timeout from what remains rather than using a fixed value.

## Trade-offs

- The deadline here lives only inside one JVM test; sending it across services needs a header and agreement on the clock or a relative budget.
- The hop work is simulated with `Thread.sleep`, and the 2-hop result depends on timing.
- `cancelRunningFuture(true)` asks the task to stop, but code that ignores interruption keeps running.

## When not to use it

- For work that must finish once started, such as a database commit; cutting it off can leave unclear state.
- For a single fast local call, where a deadline adds code without benefit.
