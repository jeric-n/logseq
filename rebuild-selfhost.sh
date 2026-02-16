#!/bin/bash
# Rebuild self-host Logseq (sync server + webapp)

set -e

echo "Cleaning old build..."
rm -rf .shadow-cljs target static/js/main.js 2>/dev/null || true

echo ""
echo "Rebuilding db-sync server..."
cd deps/db-sync
export COGNITO_ISSUER="https://cognito-idp.ap-southeast-2.amazonaws.com/ap-southeast-2_NGw6Pg4GC"
export COGNITO_CLIENT_ID="7gk3dcuo5klcn848v449llu2p8"
export COGNITO_JWKS_URL="https://cognito-idp.ap-southeast-2.amazonaws.com/ap-southeast-2_NGw6Pg4GC/.well-known/jwks.json"
export DB_SYNC_PORT=8787
yarn build:node-adapter
cd ..

echo ""
echo "Rebuilding web app..."
ENABLE_DB_SYNC_LOCAL=true yarn release

echo ""
echo "Done! Run ./run-selfhost.sh to start."
