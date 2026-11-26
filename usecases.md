# Verification Use Cases (V1 to V8)

Each scenario below explains what it tests, what it should show, and how it depends on other scenarios. Resource numbers (#1 to #6) refer to the **Resilience Configuration** table in `requirement.md`. Pass criteria are measurements to verify, not guarantees.

## Dependency overview

```
V1 (baseline)
 ├── V2 (gateway slow) ──┬── V3 (blast radius, run DURING V2)
 │                       ├── V5 (recovery, run AFTER V2)
 │                       ├── V7 (hold expiry, variant of V2 with a longer slowdown)
 │                       └── V8 (control, repeats V2 + V3 with protection off)
 ├── V4 (booking surge)   independent
 └── V6 (no double charge) independent
```

Run V1 first to confirm the system is healthy. Reset the gateway to 200ms and confirm the breaker is `CLOSED` before starting an independent scenario.

---

## V1: Baseline

**Prerequisites:** none. Run this first.

**What it tests:** Steady load with every protection enabled and a healthy (200ms) gateway. It also shows where the configured limits are tight for this load.

**Action:** Gateway responds in 200ms. Send 100 bookings/sec for 2 minutes.

**Command:** `booking-service/scripts/v1_booking-baseline.sh`

**Expected result:**
- Booking side: `POST /bookings` stays fast (p95 < 300ms). Booking creation does not wait for payment.
- Payment side: 100 bookings/sec at 200ms per gateway call needs about 20 concurrent gateway calls (100 × 0.2s = 20).
  - The gateway bulkhead (#4) allows only 5 concurrent calls (`maxWaitDuration=0`), and `charge-listener` has 8 threads.
  - Extra calls are rejected with `BulkheadFullException`, which is converted to a retryable exception.
  - Rejected records are retried with 1s → 2s → 4s backoff. After 3 attempts they go to `payment.requested.DLT`.
- So with the current configuration, V1 is **not** expected to show 0 DLT messages. Expect:
  - Gateway throughput is capped at about 5 / 0.2s = 25 calls/sec, well below the 100/sec arriving, so `payment.requested` consumer lag grows during the run.
  - Some retries and some DLT messages.
  - Many bookings stay `PENDING` for a while.
  - The breaker is not expected to open, since rejected bulkhead calls are not gateway failures. Check `/actuator/circuitbreakers`.
- To get a clean baseline (0 DLT), either raise the bulkhead `max-concurrent-calls` to at least the load needed (about 20, and `charge-listener` concurrency to match) or lower the V1 rate to about 25 bookings/sec.
- Check: `/actuator/bulkheads`, `/actuator/circuitbreakers`, and the DLT topic.

**Why it matters:** It gives the reference numbers that V2 to V8 are compared against, and shows that the limits must be sized for the expected load. See `kafka-consumer-throttle.md` for why the consumer does not scale to 100 threads.

---

## V2: Gateway slow

**Prerequisites:** V1 passes (healthy baseline, breaker `CLOSED`).

**What it tests:** How payment-service reacts when its external dependency (the payment gateway) becomes very slow.

**Action:** Set the WireMock gateway delay to 8s while bookings keep flowing.

**What it shows:**
- Gateway bulkhead (#4): never more than 5 concurrent gateway calls, so a slow gateway can't tie up all threads.
- Circuit breaker (#5): after the first 20 calls the breaker goes `OPEN` (`/actuator/circuitbreakers`).
- `charge-listener` is paused, so payment-service stops hammering a gateway that is known to be unhealthy. Events wait safely in Kafka.
- HTTP timeouts (1s connect, 2s read) bound each individual call.

**Why it matters:** This is the failure that V3, V5, V7 and V8 build on.

---

## V3: Blast radius

**Prerequisites:** Runs **during V2**. The gateway must still be slow and the breaker `OPEN`.

**What it tests:** Whether the gateway failure stays contained, so that unrelated parts of payment-service keep working. "Blast radius" means how much of the system one failure takes down.

**Action:** While V2 is in effect, poll `GET /payments/{id}` at 50 req/s and send refund events.

**What it shows:**
- Payment queries stay fast (p95 < 200ms, 0 errors). They use a separate Hikari pool (`query-pool`, #6), so stuck charge work can't use up their DB connections.
- Refunds are still processed. They run in their own listener container (`refund-listener`, #3), and the breaker pauses only `charge-listener`.

**Why it matters:** It shows the isolation actually works. V8 proves it by removing the protection and repeating this test.

---

## V4: Booking surge

**Prerequisites:** None. Independent of V2/V3; start from the V1 state (gateway healthy).

**What it tests:** How booking-service handles a sudden traffic spike on `POST /bookings`.

**Action:** Send 1,000 concurrent `POST /bookings`.

**Command:** `booking-service/scripts/v4_booking-surge.sh`

**What it shows:**
- The `createBooking` bulkhead (#1) allows a limited number of concurrent creations and rejects the rest immediately with HTTP 429 (in < 50ms) instead of queueing them.
- Accepted requests stay fast (p95 < 500ms).
- `/actuator/health` stays `UP`, so the service remains available.
- Rejected requests still briefly use a Tomcat thread; the bulkhead does not reserve threads for other endpoints.

**Why it matters:** It shows load shedding: fail fast for the excess rather than degrade everyone.

---

## V5: Recovery

**Prerequisites:** Runs **after V2** (and after V3 if you ran it). The breaker must be `OPEN` with a backlog of unprocessed payment events.

**What it tests:** Whether the system heals itself once the gateway is healthy again, without manual intervention.

**Action:** Reset the gateway delay to 200ms.

**What it shows:**
- After 30s (`waitDurationInOpenState`) the breaker moves to `HALF_OPEN`. This happens automatically (`automaticTransitionFromOpenToHalfOpenEnabled`), because the paused listener makes no calls to trigger it.
- It allows 5 probe calls; after 5 successes it goes `CLOSED`.
- `charge-listener` resumes and the Kafka backlog drains.
- The gateway bulkhead (#4) still caps concurrent calls during the catch-up burst.

**Why it matters:** Protection that never lets go would be an outage by itself.

---

## V6: No double charge

**Prerequisites:** None (independent). Needs the normal healthy setup from V1, plus in-flight payments when payment-service is restarted.

**What it tests:** Payment correctness when Kafka redelivers events, for example after a crash.

**Action:** Restart payment-service while events are being processed so they are redelivered.

**What it shows:**
- Exactly 1 gateway call and 1 `payments` row per `bookingId`.
- The idempotency check and the unique `booking_id` constraint stop duplicate payment rows.
- Manual ack (only after the DB write and result publish succeed) means events are retried, not lost.
- Note: preventing duplicate external charges after a crash at an uncertain moment needs gateway-side idempotency.

**Why it matters:** It is a correctness test, not a bulkhead or circuit-breaker test. Customers must never be charged twice.

---

## V7: Hold expiry

**Prerequisites:** Builds on **V2**: it needs a slow gateway, but kept slow longer than the hold time. Set the hold to 2 minutes for testing (`BOOKING_HOLD_DURATION`).

**What it tests:** The booking lifecycle and compensation when payment takes too long.

**Action:** Keep the gateway slow past the hold time.

**What it shows:**
- The scheduled expiry job (every 30s) cancels `PENDING` bookings older than the hold time, releasing the held seats.
- If `payment.completed` arrives late for a `CANCELLED` booking, booking-service publishes `payment.refund.requested` and the payment becomes `REFUNDED`. The customer isn't charged for a booking they no longer hold.

**Why it matters:** Seats aren't held forever by stuck payments, and money is returned when a payment succeeds too late.

---

## V8: Control (negative test)

**Prerequisites:** Requires **V2 and V3** to have been run with protection on, since it repeats them and compares the results.

**What it tests:** Whether the protections are really responsible for the good results in V2 and V3.

**Action:** Disable the gateway bulkhead and circuit breaker in config, then repeat V2 + V3.

**What it shows:**
- Expected: `GET /payments/{id}` p95 degrades (> 2s) and consumers stall. Without a concurrency cap and a breaker, slow gateway calls pile up and starve other work.
- Compared with V2/V3, this is evidence that the patterns protect the system.
- Separate pools and listeners remain unless you also disable them, so degradation is an experimental outcome, not guaranteed.

**Why it matters:** Without a control run, V2/V3 could have passed even without the patterns.
