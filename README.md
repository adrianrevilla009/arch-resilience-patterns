# arch-resilience-patterns

Seven resilience patterns (circuit breaker, retry, bulkhead, rate limiter, timeouts, fallback, load shedding) each proven with an injected failure, plus a Toxiproxy demo that breaks a real network path. The examples use a tiny Orders-style domain (payments, catalog, orders API).

## What is inside

| Folder | What it shows | Run |
| --- | --- | --- |
| [`circuit-breaker`](./circuit-breaker) | Resilience4j breaker going CLOSED, OPEN, HALF_OPEN, CLOSED against a failing payments call | `mvn -q test` |
| [`retry-backoff-jitter`](./retry-backoff-jitter) | Exponential backoff, jitter bounds, retry until success, give up after max attempts | `mvn -q test` |
| [`bulkhead`](./bulkhead) | A hung dependency fills only its own two-call compartment | `mvn -q test` |
| [`rate-limiter`](./rate-limiter) | A burst of 20 calls against a 5-per-period limit, then a refill | `mvn -q test` |
| [`timeouts-deadlines`](./timeouts-deadlines) | `TimeLimiter` cutting off a slow call, and a deadline shared across hops | `mvn -q test` |
| [`fallback`](./fallback) | Degrading to a cached or default value, including when the breaker is open | `mvn -q test` |
| [`load-shedding`](./load-shedding) | Priority-aware shedder that refuses normal traffic at 70% of capacity | `mvn -q test` |
| [`toxiproxy-failure-demo`](./toxiproxy-failure-demo) | Latency and cut-connection toxics against a client with timeout, retry and fallback | `docker compose up -d` then `python3 demo.py` |

## Prerequisites

- Java 21 and Maven 3.8 or newer (each Java folder is its own Maven project; there is no wrapper, so run `mvn` inside the folder).
- Docker with the Compose plugin and Python 3 (standard library only) for `toxiproxy-failure-demo`.

## How to read it

Start with `circuit-breaker`, then `retry-backoff-jitter` and `fallback`, which build on it. The Java folders run offline once dependencies are cached; `toxiproxy-failure-demo` is the only one that needs containers.
