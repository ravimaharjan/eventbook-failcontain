#!/usr/bin/env bash
set -euo pipefail

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is required to run the booking surge." >&2
  exit 1
fi

exec k6 run "$@" - <<'K6_SCRIPT'
import http from 'k6/http';
import { Counter, Trend } from 'k6/metrics';

const accepted = new Trend('accepted_booking_ms', true);
const rejected = new Trend('rejected_booking_ms', true);
const acceptedCount = new Counter('bookings_201');
const rejectedCount = new Counter('bookings_429');
const otherCount = new Counter('bookings_other_or_error');
export const options = {
    scenarios: {
        surge: { executor: 'per-vu-iterations', vus: 1000, iterations: 1, maxDuration: '1m' },
    },
    summaryTrendStats: ['count', 'p(95)', 'max'],
};
export default function () {
    const response = http.post('http://localhost:8081/bookings',
        JSON.stringify({
            eventId: '11111111-1111-1111-1111-111111111111',
            userId: '22222222-2222-2222-2222-222222222222',
            seats: 2, amount: 40.00,
        }),
        { headers: { 'Content-Type': 'application/json' }, timeout: '30s' });
    if (response.status === 201) {
        acceptedCount.add(1);
        accepted.add(response.timings.duration);
    } else if (response.status === 429) {
        rejectedCount.add(1);
        rejected.add(response.timings.duration);
    } else {
        otherCount.add(1);
    }
}
K6_SCRIPT
