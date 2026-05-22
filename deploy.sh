#!/usr/bin/env bash
set -euo pipefail

# Snap2Stay — EC2 Deployment Script
# Run this on your EC2 instance after cloning the repo.
#
# Prerequisites:
#   - Docker and Docker Compose installed
#   - At least 8 GB RAM (the embedding model needs ~4 GB)
#   - Security group allows inbound on port 80 (HTTP)
#
# Usage:
#   chmod +x deploy.sh
#   ./deploy.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> Snap2Stay Deployment"
echo "    Working directory: $(pwd)"
echo ""

# Ensure Docker daemon is running
if ! docker info >/dev/null 2>&1; then
    echo "ERROR: Docker is not running. Start it with: sudo systemctl start docker"
    exit 1
fi

# Pull latest changes if this is a git repo
if [ -d .git ]; then
    echo "==> Pulling latest code..."
    git pull --ff-only || echo "    (skipped — not on a tracking branch)"
    echo ""
fi

# Build and launch all services
echo "==> Building and starting containers (this may take 5-10 min on first run)..."
docker compose up --build -d

echo ""
echo "==> Waiting for services to become healthy..."

# Wait for the visual-search-api health endpoint
MAX_WAIT=300
ELAPSED=0
until docker inspect --format='{{.State.Health.Status}}' snap2stay-search 2>/dev/null | grep -q "healthy"; do
    if [ $ELAPSED -ge $MAX_WAIT ]; then
        echo "ERROR: visual-search-api did not become healthy within ${MAX_WAIT}s"
        echo "       Check logs: docker compose logs visual-search-api"
        exit 1
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    echo "    ...waiting (${ELAPSED}s)"
done

echo ""
echo "==> All services are up!"
echo ""

# Get the public IP
PUBLIC_IP=$(curl -s --connect-timeout 3 http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "<your-ec2-public-ip>")

echo "======================================"
echo "  Snap2Stay is live!"
echo ""
echo "  UI:     http://${PUBLIC_IP}"
echo "  API:    http://${PUBLIC_IP}:8081/v1/visual-search"
echo "======================================"
echo ""
echo "Useful commands:"
echo "  docker compose logs -f          # tail all logs"
echo "  docker compose logs -f ui       # tail UI logs"
echo "  docker compose ps               # check service status"
echo "  docker compose down             # stop everything"
echo "  docker compose up --build -d    # rebuild & restart"
