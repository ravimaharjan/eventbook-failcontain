# V3: Blast radius

**Prerequisites:** Run during [V2](v2_usecase.md), while the gateway is slow
and the breaker is open.

**What it tests:** Whether a gateway failure stays contained so unrelated
parts of payment-service keep working. "Blast radius" means how much of the
system one failure affects.

**Action:** During V2, poll `GET /payments/{id}` at 50 req/s using an existing
payment ID, and send refund events.

**Expected behavior:**

- Payment queries remain responsive: p95 under 200ms with zero errors. Their
  separate Hikari `query-pool` reserves connections independently of charge
  DB activity.
- Refunds continue to be processed by their own `refund-listener`; the
  breaker pauses only `charge-listener`.

**Why it matters:** This measures isolation of query connections and refund
threads, not all shared resources. [V8](v8_usecase.md) compares the outcome
with the gateway protections disabled.
