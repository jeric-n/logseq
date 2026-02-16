#!/bin/bash
# Self-host Logseq sync server and webapp
# Usage: ./run-selfhost.sh [port]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORT=${1:-3000}
SYNC_PORT=8787

# Cognito credentials
export COGNITO_ISSUER="https://cognito-idp.ap-southeast-2.amazonaws.com/ap-southeast-2_NGw6Pg4GC"
export COGNITO_CLIENT_ID="7gk3dcuo5klcn848v449llu2p8"
export COGNITO_JWKS_URL="https://cognito-idp.ap-southeast-2.amazonaws.com/ap-southeast-2_NGw6Pg4GC/.well-known/jwks.json"
export DB_SYNC_PORT=$SYNC_PORT

echo "Starting self-host Logseq..."
echo "  Sync server: http://localhost:$SYNC_PORT"
echo "  Web app:     http://localhost:$PORT"
echo ""

# Kill any existing processes on exit
trap 'kill $(jobs -p) 2>/dev/null' EXIT

# Start sync server in background
(cd "$SCRIPT_DIR/deps/db-sync" && node worker/dist/node-adapter.js) &
SYNC_PID=$!

# Wait a moment for sync server to start
sleep 2

# Start web server (foreground)
npx serve "$SCRIPT_DIR/static" -l $PORT
