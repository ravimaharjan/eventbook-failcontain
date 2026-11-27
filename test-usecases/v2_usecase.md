# V2: Gateway slow

**Prerequisites:** Establish the healthy baseline in [V1](v1_usecase.md),
with the breaker closed.

**What it tests:** How payment-service reacts when the external gateway
becomes very slow.

**Action:** From the repository root, with the services and WireMock running:

1. Set the WireMock gateway delay to 8 seconds:

   ```sh
   curl -fsS -X PUT http://localhost:8089/__admin/mappings/41fc0ed5-2712-4c72-96a1-7710a0d81f18 \
     -H 'Content-Type: application/json' \
     -d '{"request":{"method":"POST","urlPath":"/charge"},"response":{"status":200,"fixedDelayMilliseconds":8000,"headers":{"Content-Type":"application/json"},"jsonBody":{"approved":true,"transactionId":"mock-transaction"}}}'
   ```

2. In another terminal, watch the payment-service circuit breaker:

   ```sh
   watch -n 1 'curl -fsS http://localhost:8082/actuator/circuitbreakers | jq'
   ```

   The event history at `http://localhost:8082/actuator/circuitbreakerevents`
   can help you see transitions that are brief.

3. Generate bookings using the existing V1 script:

   ```sh
   bash booking-service/scripts/v1_booking-baseline.sh
   ```

   The script currently uses `rate: 100` per second and `duration: '30s'`.
   You can change those values in the script for this experiment; no specific
   RPS is required. The rate and duration still affect how quickly enough
   calls reach the breaker: allow enough load and time for at least 20
   recorded calls. Booking requests are not the same as recorded gateway
   calls because processing is asynchronous and calls may be retried or
   rejected. Check the breaker rather than assuming it opened when the script
   finishes.

4. When finished, restore the gateway to its 200ms baseline using the same
   `curl` command from step 1 with `"fixedDelayMilliseconds":200` in place
   of `8000`. Keep it slow if continuing with V3, or restore it to start V5.

**Expected behavior:**

- The gateway bulkhead limits concurrent gateway calls to five.
- After at least 20 recorded calls, the breaker opens if the configured
  failure or slow-call threshold is met; inspect
  `/actuator/circuitbreakers` on payment-service.
- The open breaker pauses `charge-listener`, stopping new charge consumption
  while events accumulate in Kafka. In-flight work may still complete or retry.
- HTTP timeouts (1s connect, 2s read) bound individual gateway waits.

**Why it matters:** This failure sets up [V3](v3_usecase.md),
[V5](v5_usecase.md), [V7](v7_usecase.md) and [V8](v8_usecase.md).
