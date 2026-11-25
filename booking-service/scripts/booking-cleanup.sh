#!/usr/bin/env bash
set -euo pipefail

TEST_EVENT_ID="11111111-1111-1111-1111-111111111111"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/../../infra/docker-compose.yml"
COMPOSE=(docker compose -f "$COMPOSE_FILE")
BOOKING_DB=("${COMPOSE[@]}" exec -T booking-postgres psql -X -v ON_ERROR_STOP=1 -U booking -d bookings)
PAYMENT_DB=("${COMPOSE[@]}" exec -T payment-postgres psql -X -v ON_ERROR_STOP=1 -U payment -d payments)
KAFKA=("${COMPOSE[@]}" exec -T kafka)

if [[ $# -gt 1 || ( $# -eq 1 && "$1" != "--allow-retained-kafka" ) ]]; then
  echo "Usage: bash booking-service/scripts/booking-cleanup.sh [--allow-retained-kafka]" >&2
  exit 2
fi

kafka_container="$("${COMPOSE[@]}" ps --all -q kafka)"
if [[ -n "$kafka_container" ]]; then
  if [[ "${1:-}" != "--allow-retained-kafka" ]]; then
    echo "Remove the Kafka container or use --allow-retained-kafka after draining consumers." >&2
    echo "See the README for cleanup instructions and replay warnings." >&2
    exit 1
  fi

  echo "Waiting for reported consumer lag to reach zero..."
  while true; do
    rows="$("${KAFKA[@]}" /opt/kafka/bin/kafka-consumer-groups.sh \
      --bootstrap-server localhost:9092 --describe --all-groups < /dev/null)"
    lag="$(printf '%s\n' "$rows" | awk '$6 ~ /^[0-9]+$/ {s+=$6} END {print s+0}')"
    [[ "$lag" -eq 0 ]] && break
    echo "Waiting for consumer lag to reach zero (current lag: $lag)..."
    sleep 2
  done
  echo "Kafka records are retained; offset resets or replay can recreate payments for deleted bookings."
  echo "Stop all load generators and other producers before continuing."
fi

ids_file="$(mktemp)"
trap 'rm -f -- "$ids_file"' EXIT

"${BOOKING_DB[@]}" -At -c \
  "SELECT id FROM bookings WHERE event_id = '$TEST_EVENT_ID' ORDER BY id" < /dev/null > "$ids_file"

count="$(wc -l < "$ids_file")"
if [[ "$count" -eq 0 ]]; then
  echo "No bookings found for event $TEST_EVENT_ID."
  exit 0
fi

echo "Found $count bookings for event $TEST_EVENT_ID."
echo "This also deletes their payment rows. Any booking with this event ID is included,"
echo "including manually created bookings."
if [[ -z "$kafka_container" ]]; then
  echo "Both application services must be stopped."
else
  echo "Do not reset consumer offsets or replay retained events after cleanup."
fi
read -r -p "Type DELETE to confirm: " confirmation
if [[ "$confirmation" != "DELETE" ]]; then
  echo "Cleanup cancelled."
  exit 1
fi

{
  printf '%s\n' 'BEGIN;' 'CREATE TEMP TABLE cleanup_ids (id uuid PRIMARY KEY) ON COMMIT DROP;' \
    '\copy cleanup_ids (id) FROM STDIN'
  cat "$ids_file"
  printf '%s\n' '\.' 'DELETE FROM payments WHERE booking_id IN (SELECT id FROM cleanup_ids);' 'COMMIT;'
} | "${PAYMENT_DB[@]}"

{
  printf '%s\n' 'BEGIN;' 'CREATE TEMP TABLE cleanup_ids (id uuid PRIMARY KEY) ON COMMIT DROP;' \
    '\copy cleanup_ids (id) FROM STDIN'
  cat "$ids_file"
  printf '%s\n' '\.'
  printf "DELETE FROM bookings WHERE event_id = '%s' AND id IN (SELECT id FROM cleanup_ids);\n" "$TEST_EVENT_ID"
  printf '%s\n' 'COMMIT;'
} | "${BOOKING_DB[@]}"

echo "Cleanup complete. Kafka records were not modified by this script."
