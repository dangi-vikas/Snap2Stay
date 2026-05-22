#!/bin/bash

# Snap2Stay Local Startup Script (No Docker)
# This script starts all services in the correct order

set -e

# Set Java 21 (required by pom.xml)
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

WORKSPACE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PIDS_FILE="$WORKSPACE_ROOT/.local-pids"

# Ensure logs directory exists
mkdir -p "$WORKSPACE_ROOT/logs"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   Snap2Stay - Local Startup (No Docker)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Clean up previous PIDs file
rm -f "$PIDS_FILE"

# Function to cleanup on exit
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down services...${NC}"
    if [ -f "$PIDS_FILE" ]; then
        while IFS= read -r pid; do
            if ps -p "$pid" > /dev/null 2>&1; then
                echo "Stopping PID: $pid"
                kill "$pid" 2>/dev/null || true
            fi
        done < "$PIDS_FILE"
        rm -f "$PIDS_FILE"
    fi
    echo -e "${GREEN}All services stopped.${NC}"
}

trap cleanup EXIT INT TERM

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=90
    local attempt=0

    echo -e "${YELLOW}Waiting for $service_name to be ready...${NC}"

    while [ $attempt -lt $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ $service_name is ready!${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
        echo -n "."
    done

    echo ""
    echo -e "${RED}✗ $service_name failed to start within timeout${NC}"
    return 1
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed. Please install Maven first.${NC}"
    exit 1
fi

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python 3 is not installed. Please install Python 3 first.${NC}"
    exit 1
fi

# Check if Node.js is installed (for UI)
if ! command -v node &> /dev/null; then
    echo -e "${YELLOW}Warning: Node.js is not installed. UI will not be started.${NC}"
    START_UI=false
else
    START_UI=true
fi

echo -e "${BLUE}Step 1: Building Java services...${NC}"
cd "$WORKSPACE_ROOT"
mvn -s .mvn/settings-public.xml clean install -DskipTests
echo -e "${GREEN}✓ Java services built successfully${NC}"
echo ""

# Start Embedding Service (Port 8082)
echo -e "${BLUE}Step 2: Starting Embedding Service (Port 8082)...${NC}"
cd "$WORKSPACE_ROOT/embedding-service"

# Check if venv exists, create if not
if [ ! -d "venv" ]; then
    echo "Creating Python virtual environment..."
    python3 -m venv venv
fi

# Activate venv and install dependencies
source venv/bin/activate
pip install -q -r requirements.txt

# Start the service in background
HF_HUB_DOWNLOAD_TIMEOUT=120 nohup uvicorn main:app --host 0.0.0.0 --port 8082 > "$WORKSPACE_ROOT/logs/embedding-service.log" 2>&1 &
EMBEDDING_PID=$!
echo "$EMBEDDING_PID" >> "$PIDS_FILE"
echo -e "${GREEN}✓ Embedding Service started (PID: $EMBEDDING_PID)${NC}"
echo -e "  Logs: $WORKSPACE_ROOT/logs/embedding-service.log"

# Wait for embedding service to be ready
wait_for_service "http://localhost:8082/health" "Embedding Service"
echo ""

# Start Content Server (Port 8083)
echo -e "${BLUE}Step 3: Starting Content Server (Port 8083)...${NC}"
cd "$WORKSPACE_ROOT"
nohup mvn -s .mvn/settings-public.xml -pl content-server spring-boot:run > "$WORKSPACE_ROOT/logs/content-server.log" 2>&1 &
CONTENT_PID=$!
echo "$CONTENT_PID" >> "$PIDS_FILE"
echo -e "${GREEN}✓ Content Server started (PID: $CONTENT_PID)${NC}"
echo -e "  Logs: $WORKSPACE_ROOT/logs/content-server.log"

# Wait for content server to be ready
wait_for_service "http://localhost:8083/actuator/health" "Content Server"
echo ""

# Start Visual Search API (Port 8081)
echo -e "${BLUE}Step 4: Starting Visual Search API (Port 8081)...${NC}"
cd "$WORKSPACE_ROOT"
nohup mvn -s .mvn/settings-public.xml -pl visual-search-api spring-boot:run > "$WORKSPACE_ROOT/logs/visual-search-api.log" 2>&1 &
SEARCH_PID=$!
echo "$SEARCH_PID" >> "$PIDS_FILE"
echo -e "${GREEN}✓ Visual Search API started (PID: $SEARCH_PID)${NC}"
echo -e "  Logs: $WORKSPACE_ROOT/logs/visual-search-api.log"

# Wait for visual search API to be ready
wait_for_service "http://localhost:8081/v1/health/live" "Visual Search API"
echo ""

# Run Ingestion Service (runs once and exits)
echo -e "${BLUE}Step 5: Running Image Ingestion Service (one-time)...${NC}"
cd "$WORKSPACE_ROOT"
echo "This may take a few minutes to process all images..."
mvn -s .mvn/settings-public.xml -pl image-ingestion-service spring-boot:run > "$WORKSPACE_ROOT/logs/image-ingestion-service.log" 2>&1
echo -e "${GREEN}✓ Image Ingestion completed${NC}"
echo -e "  Logs: $WORKSPACE_ROOT/logs/image-ingestion-service.log"
echo ""

# Start UI (Port 5173) - Optional
if [ "$START_UI" = true ]; then
    echo -e "${BLUE}Step 6: Starting UI (Port 5173)...${NC}"
    cd "$WORKSPACE_ROOT/ui"

    if [ ! -d "node_modules" ]; then
        echo "Installing UI dependencies..."
        npm install
    fi

    nohup npm run dev > "$WORKSPACE_ROOT/logs/ui.log" 2>&1 &
    UI_PID=$!
    echo "$UI_PID" >> "$PIDS_FILE"
    echo -e "${GREEN}✓ UI started (PID: $UI_PID)${NC}"
    echo -e "  Logs: $WORKSPACE_ROOT/logs/ui.log"

    sleep 5
    echo ""
fi

# Summary
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ All services are running!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo "Service URLs:"
echo -e "  ${GREEN}Embedding Service:${NC}    http://localhost:8082"
echo -e "  ${GREEN}Content Server:${NC}       http://localhost:8083"
echo -e "  ${GREEN}Visual Search API:${NC}    http://localhost:8081"
if [ "$START_UI" = true ]; then
    echo -e "  ${GREEN}UI:${NC}                   http://localhost:5173"
fi
echo ""
echo "Test the API:"
echo -e "  ${YELLOW}curl -X POST http://localhost:8081/v1/visual-search -F \"image=@/path/to/photo.jpg\"${NC}"
echo ""
echo "View logs:"
echo -e "  ${YELLOW}tail -f $WORKSPACE_ROOT/logs/*.log${NC}"
echo ""
echo -e "${BLUE}Press Ctrl+C to stop all services${NC}"
echo ""

# Keep script running
wait