# fallback

Resilience4j `Decorators` that return a cached or default answer when a catalog lookup fails, tested in `FallbackTest.java`.

## Goal

Show how to degrade gracefully instead of returning an error, including when a circuit breaker has already opened.

## Run it

```
cd fallback
mvn -q test
```

Expected: `FallbackTest` passes (2 tests, 0 failures); without `-q`, Maven ends with `BUILD SUCCESS`.

## What it proves

- When `liveLookup` throws `IllegalStateException("catalog down")`, the fallback returns `cached: 2 x book` from an in-memory map.
- With a circuit breaker (window of 2) in front, five calls in a row all return `default: unavailable`.
- After those calls the breaker is OPEN, so the fallback handled both the real failures and the later `CallNotPermittedException`.

## Trade-offs

- Fallback data can be stale or wrong; the cache here is a fixed `Map`, not a real cache with expiry.
- The fallback only covers the exception types you list; anything else still propagates.
- A fallback hides failures from callers, so metrics or logs must show how often it fires.

## When not to use it

- When a wrong answer is worse than an error, such as a payment status or stock level used for a purchase.
- When there is no sensible default; a clear error is more honest.
