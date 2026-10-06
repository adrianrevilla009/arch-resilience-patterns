# toxiproxy-failure-demo

A Docker Compose file with a Python HTTP backend behind Toxiproxy 2.9.0, and a standard-library script `demo.py` that injects failures and shows a client coping.

## Goal

Show timeout, bounded retry and fallback working against real network faults (added latency and a cut connection) rather than mocked exceptions.

## Run it

```
cd toxiproxy-failure-demo
docker compose up -d
python3 demo.py
docker compose down
```

Expected output when it works:

```
healthy: live
latency 3s: timeout -> fallback in <about 1.0>s
proxy down: fallback
recovered: live
OK
```

Not run end to end: the containers were not started, so the output above is taken from the `print` calls in `demo.py`, not from a real run. The script asserts each step.

## What it proves

- `demo.py` creates a proxy on port 18080 to `backend:8080` through the Toxiproxy admin API on 8474.
- A 3000 ms latency toxic makes both 0.5 s attempts time out, so the client returns `cached` in under 2.5 s.
- Disabling the proxy (a hard cut) also gives `cached`, and re-enabling it gives `live` again.

## Trade-offs

- The client is a few lines of `urllib`, not a library; it shows the idea, not production retry logic.
- Timing checks (under 2.5 s) can be flaky on a slow machine.
- The backend is `python -m http.server`, so it serves a directory listing and says nothing about a real service.

## When not to use it

- For fast unit checks of a pattern; the Java folders do that without containers.
- For testing against a managed service you cannot put behind a proxy.
