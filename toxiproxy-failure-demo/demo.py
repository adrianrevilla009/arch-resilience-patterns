"""Inject latency and a hard cut with Toxiproxy and show a client with timeout + retry + fallback coping.
Stdlib only. Needs `docker compose up -d` first (see README)."""
import json, sys, time, urllib.request

API = "http://localhost:8474"
URL = "http://localhost:18080/"


def api(method, path, body=None):
    req = urllib.request.Request(API + path, method=method, data=json.dumps(body).encode() if body is not None else None,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=5) as r:
        return r.read()


def call(timeout=0.5, attempts=2, fallback="cached"):
    """Client with per-attempt timeout, bounded retry, and a fallback."""
    for _ in range(attempts):
        try:
            with urllib.request.urlopen(URL, timeout=timeout) as r:
                return "live", r.status
        except Exception:
            continue
    return fallback, None


def main():
    for _ in range(30):  # wait for the admin API
        try:
            api("GET", "/version"); break
        except Exception:
            time.sleep(1)
    else:
        sys.exit("toxiproxy not reachable")
    try:
        api("DELETE", "/proxies/backend")
    except Exception:
        pass
    api("POST", "/proxies", {"name": "backend", "listen": "0.0.0.0:18080", "upstream": "backend:8080"})

    for _ in range(20):  # backend container may still be starting
        if call() == ("live", 200):
            break
        time.sleep(0.5)
    assert call() == ("live", 200), "healthy path must be live"
    print("healthy: live")

    api("POST", "/proxies/backend/toxics", {"name": "lag", "type": "latency", "attributes": {"latency": 3000}})
    t0 = time.time()
    assert call() == ("cached", None), "latency toxic must trigger timeout then fallback"
    assert time.time() - t0 < 2.5, "timeout must bound the wait"
    print("latency 3s: timeout -> fallback in %.1fs" % (time.time() - t0))
    api("DELETE", "/proxies/backend/toxics/lag")

    api("POST", "/proxies/backend", {"enabled": False})
    assert call() == ("cached", None), "cut connection must fall back"
    print("proxy down: fallback")
    api("POST", "/proxies/backend", {"enabled": True})

    assert call() == ("live", 200), "recovers after toxics removed"
    print("recovered: live")
    print("OK")


main()
