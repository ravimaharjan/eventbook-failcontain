# V5: Recovery

**Prerequisites:** Run after [V2](v2_usecase.md), and after
[V3](v3_usecase.md) if testing it. The breaker should be open with a backlog
of unprocessed payment events.

**What it tests:** Whether the system resumes processing when the gateway
becomes healthy, without manual listener intervention.

**Action:** Reset the gateway delay to 200ms.

**Expected behavior:**

- After approximately 30 seconds in `OPEN`, the breaker automatically moves
  to `HALF_OPEN` and the charge listener resumes.
- Five successful permitted probe calls lead to `CLOSED`; if probes fail, the
  breaker can reopen.
- The listener processes the backlog, while the gateway bulkhead continues
  limiting concurrent calls.

**Why it matters:** Protection that never lets go would itself cause an outage.
