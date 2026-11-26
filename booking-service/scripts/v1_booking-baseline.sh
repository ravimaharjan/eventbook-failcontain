#!/usr/bin/env bash
set -euo pipefail

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is required to run the booking baseline." >&2
  exit 1
fi

exec k6 run "$@" - <<'K6_SCRIPT'
import http from 'k6/http';
import { Counter } from 'k6/metrics';

const acceptedCount = new Counter('bookings_201');
const rejectedCount = new Counter('bookings_429');
const otherCount = new Counter('bookings_other_or_error');
export const options = {
    scenarios: {
        baseline: {
            executor: 'constant-arrival-rate',
            rate: 100, timeUnit: '1s', duration: '2m',
            preAllocatedVUs: 100, maxVUs: 300,
        },
    },
    thresholds: { 'http_req_duration{expected_response:true}': ['p(95)<300'] },
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
    } else if (response.status === 429) {
        rejectedCount.add(1);
    } else {
        otherCount.add(1);
    }
}
K6_SCRIPT
