#!/bin/bash
# Self-host Logseq sync server and webapp with HTTPS
# Usage: ./run-selfhost.sh [port]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORT=${1:-3000}
SYNC_PORT=8787
HOST="100.88.172.48"

CERTS_DIR="$SCRIPT_DIR/certs"
CERT_FILE="$CERTS_DIR/cert.pem"
KEY_FILE="$CERTS_DIR/key.pem"

# Create certs directory
mkdir -p "$CERTS_DIR"

# Auto-generate self-signed certificates if missing
if [ ! -f "$CERT_FILE" ] || [ ! -f "$KEY_FILE" ]; then
    echo "Generating self-signed certificate for $HOST..."
    openssl req -x509 -newkey rsa:4096 \
        -keyout "$KEY_FILE" \
        -out "$CERT_FILE" \
        -days 365 -nodes \
        -subj "/CN=$HOST" \
        -addext "subjectAltName=IP:$HOST,DNS:localhost" 2>/dev/null
    if [ $? -ne 0 ]; then
        echo "Error: Failed to generate certificates. Make sure openssl is installed."
        exit 1
    fi
    echo "Certificates generated successfully."
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
echo "Note: You may need to accept the self-signed certificate in your browser."
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
