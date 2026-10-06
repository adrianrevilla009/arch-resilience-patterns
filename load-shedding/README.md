# load-shedding

A small priority-aware load shedder written for this folder (a `Shedder` class inside `LoadSheddingTest.java`) and a test that overloads it.

## Goal

Show how a service under load can refuse low-priority work first so that critical requests keep being served.

## Run it

```
cd load-shedding
mvn -q test
```

Expected: `LoadSheddingTest` passes (1 test, 0 failures); without `-q`, Maven ends with `BUILD SUCCESS`.

## What it proves

- With capacity 10, seven slow NORMAL requests fill 70% of it; the eighth NORMAL request is refused and the shed counter becomes 1.
- A CRITICAL request is still admitted in that state, because its limit is the full capacity of 10.
- After the held requests are released, a NORMAL request is admitted again.

## Trade-offs

- The shedder counts in-flight requests only; it does not look at latency or queue age, which real systems often use.
- The 70% threshold and the two priorities are hard-coded for the example.
- The refusal is just `false`; the comment says a caller would map it to HTTP 503 with `Retry-After`, but no HTTP server is in this folder.

## When not to use it

- When all requests have the same value; a plain concurrency limit (`bulkhead`) is simpler.
- When you can scale out fast enough that overload is short-lived.
