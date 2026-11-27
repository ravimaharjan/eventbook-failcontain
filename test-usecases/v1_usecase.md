# V1: Baseline

**Prerequisites:** None. Run this first with the gateway healthy and the
payment circuit breaker closed.

**What it tests:** Steady booking load with all protections enabled and a
healthy gateway. It also shows where the configured limits are tight.

**Action:** Set the gateway delay to 200ms and send 100 bookings/sec for
2 minutes. From the repository root, run:

```sh
booking-service/scripts/v1_booking-baseline.sh
```

**Expected behavior:**

- At 100 bookings/sec and 200ms per gateway call, roughly 20 concurrent calls
  would be needed to keep up, ignoring other overhead. The charge listener
  has 8 threads and the gateway bulkhead allows only 5 concurrent calls.
- When a call is rejected by the bulkhead, it is retried with 1s, 2s and 4s
  backoffs. After the initial attempt and three unsuccessful retries, the
  record can be sent to `payment.requested.DLT`. 
- Retry logs can be seen in the payment service logs.
- Command is given below to see the DLT messages when processing is ongoing.
- The circuit breaker should remain closed when the gateway is healthy, but check
  its actual state rather than assuming it.

**Observe:** 

To see DLT messages, including exception headers:

```sh
docker compose -f infra/docker-compose.yml exec kafka \
  /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic payment.requested.DLT --from-beginning \
  --property print.key=true --property print.headers=true
```

The bulkhead, listener concurrency and partition count must be sized together
to sustain the desired rate. Lowering the offered rate or increasing capacity
may help, but neither guarantees zero DLT records.

**Why it matters:** This provides a reference for later scenarios and shows
why limits must be sized for load. See
[Kafka consumer throttling](../kafka-consumer-throttle.md) for the consumer
thread model.
