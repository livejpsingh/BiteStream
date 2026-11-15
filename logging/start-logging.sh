#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "=========================================================="
echo "  Starting BiteStream Observability & Logging Stack       "
echo "=========================================================="

docker compose up -d

echo ""
echo "Observability Stack Started Successfully!"
echo "  - Grafana:     http://localhost:3000 (admin / admin)"
echo "  - Prometheus:  http://localhost:9090"
echo "  - Loki:        http://localhost:3100"
