# Self-Host Sync Setup Changes

This document summarizes all changes made to enable self-hosted Logseq sync with custom AWS Cognito credentials.

## Files Modified

### 1. `src/main/frontend/config.cljs`

**Purpose:** Configure Cognito credentials and sync server URLs for self-hosted setup.

**Changes:**
- Replaced the conditional `if ENABLE-FILE-SYNC-PRODUCTION` block with a simple `do` block containing custom credentials
- Added custom Cognito credentials:
  ```clojure
  (def LOGIN-URL "https://<your-domain>.auth.<region>.amazoncognito.com/login?client_id=<client-id>&response_type=code&scope=email+openid+phone&redirect_uri=logseq%3A%2F%2Fauth-callback")
  (def COGNITO-IDP "https://cognito-idp.<region>.amazonaws.com/")
  (def COGNITO-CLIENT-ID "<your-client-id>")
  (def REGION "<your-region>")
  (def USER-POOL-ID "<your-user-pool-id>")
  (def IDENTITY-POOL-ID "<your-identity-pool-id>")
  (def OAUTH-DOMAIN "<your-oauth-domain>")
  ```
- Changed sync server URLs to use HTTPS with Tailscale IP:
  ```clojure
  (defonce db-sync-ws-url "wss://100.88.172.48:8787/sync/%s")
  (defonce db-sync-http-base "https://100.88.172.48:8787")
  ```

### 2. `src/main/frontend/handler/user.cljs`

**Purpose:** Bypass the user group check that restricts remote sync to Logseq's paid users.

**Changes:**
- Modified `rtc-group?` function to always return `true`:
  ```clojure
  (defn rtc-group?
    []
    true)
  ```
- Original code checked for "team" or "rtc_2025_07_10" groups from Logseq's API server

### 3. `src/main/frontend/components/repo.cljs`

**Purpose:** Improve error handling and logging for remote graph creation.

**Changes:**
- Added console logging and user-facing error notifications in `new-db-graph` component
- Added logging to `ensure-e2ee-rsa-key-for-cloud!` function for debugging
- Fixed issue where `set-creating-db?` wasn't being reset when cloud sync was disabled

### 4. `deps/db-sync/src/logseq/db_sync/node/config.cljs`

**Purpose:** Add SSL configuration support for HTTPS.

**Changes:**
- Added SSL key and cert path configuration:
  ```clojure
  :ssl-key (env-value env "SSL_KEY_PATH")
  :ssl-cert (env-value env "SSL_CERT_PATH")
  ```
- Added `:ssl-key` and `:ssl-cert` to allowed config keys

### 5. `deps/db-sync/src/logseq/db_sync/node/server.cljs`

**Purpose:** Add HTTPS server support with self-signed certificates.

**Changes:**
- Added imports for `https` and `fs` modules
- Modified `start!` function to:
  - Check for SSL key and cert files
  - Create HTTPS server when SSL is configured, HTTP otherwise
  - Use appropriate scheme (`https` or `http`) in request handling
  - Log the server URL on startup

## Files Created

### 1. `run-selfhost.sh`

**Purpose:** Start both the sync server and webapp with HTTPS using self-signed certificates.

**Features:**
- Creates `certs/` directory in repo root
- Auto-generates self-signed certificates for `100.88.172.48` (Tailscale IP) if missing
- Starts sync server with SSL environment variables
- Starts web server with HTTPS using `serve --ssl-cert --ssl-key`

**Usage:**
```bash
./run-selfhost.sh [port]  # Default port is 3000
```

### 2. `rebuild-selfhost.sh`

**Purpose:** Rebuild both the sync server and webapp after code changes.

**Usage:**
```bash
./rebuild-selfhost.sh
```

### 3. `certs/` directory

**Purpose:** Store self-signed SSL certificates (auto-generated).

**Contents:**
- `cert.pem` - Self-signed SSL certificate
- `key.pem` - Private key for the certificate

## AWS Cognito Setup Requirements

1. **User Pool** - Create a user pool with email sign-in
2. **App Client** - Create an app client WITHOUT a client secret (browser apps can't keep secrets)
3. **App Client Settings** - Configure:
   - Callback URLs: `logseq://auth-callback` and `https://100.88.172.48:3000`
   - Allowed OAuth flows: Authorization code grant
   - Scopes: `email`, `openid`, `phone`
4. **Identity Pool** - Create an identity pool linked to your user pool
5. **Domain** - Configure a domain for OAuth (e.g., `your-app.auth.region.amazoncognito.com`)

## Build Requirements

1. **Install dependencies:**
   ```bash
   yarn install --frozen-lockfile
   cd deps/db-sync && yarn install --frozen-lockfile
   ```

2. **Build the sync server:**
   ```bash
   cd deps/db-sync
   COGNITO_ISSUER=https://cognito-idp.<region>.amazonaws.com/<user-pool-id> \
   COGNITO_CLIENT_ID=<client-id> \
   COGNITO_JWKS_URL=https://cognito-idp.<region>.amazonaws.com/<user-pool-id>/.well-known/jwks.json \
   DB_SYNC_PORT=8787 \
   yarn build:node-adapter
   ```

3. **Build the webapp:**
   ```bash
   ENABLE_DB_SYNC_LOCAL=true yarn release
   ```

## Running

1. Start the sync server and webapp:
   ```bash
   ./run-selfhost.sh
   ```

2. Open https://100.88.172.48:3000 in your browser

3. **Accept the self-signed certificate warning** in your browser

4. Create an account and sign in

5. Create a new graph with "Use Logseq Sync" checked

## HTTPS Requirements

HTTPS is required for:
- **OPFS (Origin Private File System)** - Used by SQLite for database storage
- **Secure Context** - Required by browsers for certain APIs
- **Remote access** - Accessing from other devices (Tailscale, etc.)

The self-signed certificate is generated for:
- IP: `100.88.172.48` (Tailscale IP)
- DNS: `localhost` (for local testing)

## Troubleshooting

### Certificate Warnings
- Accept the self-signed certificate warning in your browser
- For Chrome, you may need to type `thisisunsafe` on the warning page
- For Firefox, add a permanent exception

### SQLite/OPFS Errors
- Ensure you're using HTTPS (not HTTP)
- Check that the certificate matches the host you're accessing
- Verify you accepted the certificate warning

### CORS Errors
- Ensure both webapp and sync server are using the same host (IP or localhost)
- Check that HTTPS is used for both

### JWT Verification Failures
- Check that Cognito credentials match between the webapp and sync server
- Verify the User Pool ID and Client ID are correct

### RSA Key Errors
- E2EE requires the sync server to have correct Cognito credentials
- Check server logs for authentication errors

### Graph Creation Silently Fails
- Check browser console for error messages
- Verify sync server is running at https://100.88.172.48:8787/health

### Connection Refused
- Verify both servers are running
- Check that ports 3000 and 8787 are not blocked by firewall
- Ensure Tailscale is connected if using Tailscale IP
