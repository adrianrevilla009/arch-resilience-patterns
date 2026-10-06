# bulkhead

Two Resilience4j semaphore bulkheads, one per dependency, with a test that hangs one dependency and checks the other still works.

## Goal

Show that limiting concurrent calls per dependency keeps one slow dependency from using up all the caller's capacity.

## Run it

```
cd bulkhead
mvn -q test
```

Expected: `BulkheadTest` passes (1 test, 0 failures); without `-q`, Maven ends with `BUILD SUCCESS`.

## What it proves

- Each compartment allows 2 concurrent calls and waits 0 ms for a permit (`BulkheadTest.compartment`).
- Two virtual threads blocked inside `slow-dep` leave `getAvailableConcurrentCalls()` at 0.
- A third call to `slow-dep` fails at once with `BulkheadFullException` instead of queueing.
- A call to `healthy-dep` during that time still returns `fine`.

## Trade-offs

- The limit is a fixed number that has to be sized per dependency; too low rejects healthy traffic, too high gives no protection.
- This is the semaphore flavour: it caps callers but does not time out a stuck call (see `timeouts-deadlines`).
- Rejected calls need a caller-side answer, such as a fallback.

## When not to use it

- When a dependency is called from one place at low concurrency, so it cannot exhaust anything.
- When you need isolation of threads and queues; this test uses semaphores only and does not cover a thread-pool bulkhead.
