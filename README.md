# Event Booking

## Objective

This project demonstrates how the **bulkhead pattern** limits concurrent work
and isolates resources to reduce resource exhaustion during booking surges or
payment gateway slowdowns. It also uses a circuit breaker to pause payment
processing when the gateway is unhealthy and resume it as the gateway recovers.

The two Spring Boot services communicate only through Kafka:

- **Booking Service** accepts bookings, holds them as pending, publishes payment
  requests, and updates or expires bookings based on payment results and hold time.
- **Payment Service** processes payment requests through a mock gateway, publishes
  results, handles simulated refunds, and provides payment status queries.

The services run locally; Kafka, separate PostgreSQL databases, and the mock
gateway run with Docker Compose.

## Run

From the project root:

```sh
docker compose -f infra/docker-compose.yml up -d
mvn -pl booking-service spring-boot:run
mvn -pl payment-service spring-boot:run
```

The booking API listens on `http://localhost:8081` and the payment query API on
`http://localhost:8082`. 

Create a booking:

```sh
curl -i http://localhost:8081/bookings \
  -H 'Content-Type: application/json' \
  -d '{"eventId":"11111111-1111-1111-1111-111111111111","userId":"22222222-2222-2222-2222-222222222222","seats":2,"amount":40.00}'
```

Actuator endpoints are exposed by
both services under `/actuator`; WireMock's admin API is available on port 8089.

To simulate a slow gateway for the manual verification scenarios below, update
the seeded WireMock mapping and restore it to 200ms afterward:

```sh
curl -X PUT http://localhost:8089/__admin/mappings/41fc0ed5-2712-4c72-96a1-7710a0d81f18 \
  -H 'Content-Type: application/json' \
  -d '{"request":{"method":"POST","urlPath":"/charge"},"response":{"status":200,"fixedDelayMilliseconds":8000,"headers":{"Content-Type":"application/json"},"jsonBody":{"approved":true,"transactionId":"mock-transaction"}}}'
```

Set `fixedDelayMilliseconds` back to `200` to restore the baseline gateway.

## Verification use cases

The [test-usecases](test-usecases/) folder contains the detailed scenarios,
their prerequisites and expected behavior. These are manual experiments that
create test data and may leave messages in Kafka.

- [V1: Baseline](test-usecases/v1_usecase.md) — steady booking load with a healthy gateway.
- [V2: Gateway slow](test-usecases/v2_usecase.md) — observe gateway protection.
- [V3: Blast radius](test-usecases/v3_usecase.md) — check queries and refunds during V2.
- [V4: Booking surge](test-usecases/v4_usecase.md) — observe booking load shedding.
- [V5: Recovery](test-usecases/v5_usecase.md) — restore the gateway after V2.
- [V6: No double charge](test-usecases/v6_usecase.md) — check redelivery and payment correctness.
- [V7: Hold expiry](test-usecases/v7_usecase.md) — test cancellation and late-payment compensation.
- [V8: Control](test-usecases/v8_usecase.md) — repeat V2/V3 with gateway protections disabled.

V3 runs during V2; V5 follows V2; V7 uses a longer slowdown; V8 compares
against V2/V3. V4 and V6 are independent.


