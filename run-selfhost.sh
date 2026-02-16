#!/bin/bash
# Self-host Logseq sync server and webapp with HTTPS
# Usage: ./run-selfhost.sh [port]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORT=${1:-3000}
SYNC_PORT=8787
HOST="desktop-tvvuj82.tail3c3b96.ts.net"

CERTS_DIR="$SCRIPT_DIR/certs"
CERT_FILE="$CERTS_DIR/desktop-tvvuj82.tail3c3b96.ts.net.crt"
KEY_FILE="$CERTS_DIR/desktop-tvvuj82.tail3c3b96.ts.net.key"

# Check that Tailscale certs exist
if [ ! -f "$CERT_FILE" ] || [ ! -f "$KEY_FILE" ]; then
    echo "Error: Tailscale certificates not found."
    echo "Run 'tailscale cert' on Windows and copy the cert/key files to:"
    echo "  $CERT_FILE"
    echo "  $KEY_FILE"
    exit 1
fi

# Cognito credentials
export COGNITO_ISSUER="https://cognito-idp.ap-southeast-2.amazonaws.com/ap-southeast-2_NGw6Pg4GC"
export COGNITO_CLIENT_ID="7gk3dcuo5klcn848v449llu2p8"
export COGNITO_JWKS_URL="https://cognito-idp.ap-southeast-2.amazonaws.com/ap-southeast-2_NGw6Pg4GC/.well-known/jwks.json"
export DB_SYNC_PORT=$SYNC_PORT

# SSL configuration for sync server
export SSL_KEY_PATH="$KEY_FILE"
export SSL_CERT_PATH="$CERT_FILE"

echo ""
echo "Starting self-host Logseq with HTTPS..."
echo "  Sync server: https://$HOST:$SYNC_PORT"
echo "  Web app:     https://$HOST:$PORT"
echo ""
echo "Using Tailscale-provided Let's Encrypt certificate."
echo ""

# Kill any existing processes on exit
trap 'kill $(jobs -p) 2>/dev/null' EXIT

# Start sync server in background
(cd "$SCRIPT_DIR/deps/db-sync" && node worker/dist/node-adapter.js) &
SYNC_PID=$!

# Wait a moment for sync server to start
sleep 2

# Start web server with HTTPS (foreground)
npx serve "$SCRIPT_DIR/static" -l $PORT \
    --ssl-cert "$CERT_FILE" \
    --ssl-key "$KEY_FILE"
