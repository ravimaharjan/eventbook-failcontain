#!/usr/bin/env bash
set -euo pipefail

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is required to run the booking GET surge." >&2
  exit 1
fi

BOOKING_ID="${BOOKING_ID:-f2f08f31-7520-4571-bc3b-eda4ac65c2c3}"
export BOOKING_ID

exec k6 run "$@" - <<'K6_SCRIPT'
import http from 'k6/http';
import { Counter } from 'k6/metrics';

const ok = new Counter('gets_200');
const notFound = new Counter('gets_404');
const otherCount = new Counter('gets_other_or_error');
export const options = {
    scenarios: {
        surge: {
            executor: 'constant-arrival-rate',
            rate: 3000, timeUnit: '.2s', duration: '10s',
            preAllocatedVUs: 1000, maxVUs: 2000,
        },
    },
    summaryTrendStats: ['count', 'p(95)', 'max'],
};
export default function () {
    const response = http.get(`http://localhost:8081/bookings/${__ENV.BOOKING_ID}`, { timeout: '30s' });
    if (response.status === 200) {
        ok.add(1);
    } else if (response.status === 404) {
        notFound.add(1);
    } else {
        otherCount.add(1);
    }
}
K6_SCRIPT
