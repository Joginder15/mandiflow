#!/usr/bin/env bash
# Starts all five services in the background and writes their logs to ./logs.
# Run `docker compose up -d` first. Stop everything with: scripts/stop-services.sh
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p logs

echo "Building (tests skipped)…"
mvn -q -DskipTests install

# order-service first: it creates every Kafka topic.
for svc in order-service inventory-service payment-service notification-service analytics-service; do
  echo "Starting $svc → logs/$svc.log"
  nohup java -jar "$svc/target/$svc-0.1.0-SNAPSHOT.jar" > "logs/$svc.log" 2>&1 &
  echo $! > "logs/$svc.pid"
  if [ "$svc" = "order-service" ]; then sleep 15; fi
done

echo
echo "Swagger: http://localhost:8081/swagger-ui.html (orders)  http://localhost:8082/swagger-ui.html (products)"
echo "Kafka UI: http://localhost:8090   Mailpit: http://localhost:8025"
echo "Web: cd web && npm install && npm run dev  →  http://localhost:5173"
