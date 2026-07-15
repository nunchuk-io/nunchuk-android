#!/usr/bin/env bash
#
# publish-github-release.sh
#
# Publish a Nunchuk Android GitHub release the reproducible-signature way:
#   1. Download the *Google-signed* universal APK for a given versionCode from
#      the Play Developer API (generatedApks). This APK is signed with the Play
#      **app signing key**, i.e. exactly what users install from the Play Store
#      -- NOT the local upload key (nunchuk-keystore.jks). That is why we pull it
#      from Play instead of building + signing locally.
#   2. Create SHA256SUMS over the APK.
#   3. GPG clearsign it -> SHA256SUMS.asc (Nunchuk binary release signing key).
#   4. Create the GitHub release for the tag and upload all three assets, to
#      match the convention of previous releases (2.6.0.apk / SHA256SUMS /
#      SHA256SUMS.asc).
#
# Requirements: openssl, curl, jq, gpg  (no gcloud / python google libs needed)
#
# Usage (zero-arg from the app repo root — everything is auto-derived):
#   ./scripts/publish-github-release.sh
#     - VERSION / VERSION_CODE  <- nunchuk-app/build.gradle.kts
#     - TAG                     <- android.<VERSION>
#     - GITHUB_TOKEN (githubToken) + GPG_PASSPHRASE (GPGpass) <- local.properties
#       (searches ./local.properties then ../nunchuk-android-nativesdk/local.properties)
#     - SA_JSON                 <- ./nunchuk-service-account.json (or play-service-account.json)
#   Any of these can still be overridden via env, e.g. VERSION=2.7.1 ./scripts/...
#
# Optional env overrides:
#   CREDS_PROPS  extra local.properties to read githubToken/GPGpass from (searched first)
#   SA_JSON      path to Play service-account json
#   APK_PATH     use this already-downloaded signed universal APK and SKIP the
#                Play download (SA_JSON not needed). Use when you grabbed the
#                "Signed, universal APK" from Play Console > App bundle explorer.
#   PACKAGE      app package name                   (default: io.nunchuk.android)
#   REPO         github owner/repo                  (default: nunchuk-io/nunchuk-android)
#   GPG_KEY      key id/email to sign with          (default: tatattai@gmail.com)
#   WORKDIR      staging dir for artifacts          (default: ./build/github-release)
#   SKIP_GITHUB  if "1", stop after signing (no release/upload)
#
set -euo pipefail

# ---- config ---------------------------------------------------------------
PACKAGE="${PACKAGE:-io.nunchuk.android}"
REPO="${REPO:-nunchuk-io/nunchuk-android}"
GPG_KEY="${GPG_KEY:-tatattai@gmail.com}"
WORKDIR="${WORKDIR:-build/github-release}"
SKIP_GITHUB="${SKIP_GITHUB:-0}"
APP_GRADLE="${APP_GRADLE:-nunchuk-app/build.gradle.kts}"

# Auto-derive version from the app module unless overridden -> zero-arg run.
if [ -z "${VERSION:-}" ] && [ -f "$APP_GRADLE" ]; then
  VERSION=$(grep -oE 'versionName = "[^"]+"' "$APP_GRADLE" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
fi
if [ -z "${VERSION_CODE:-}" ] && [ -f "$APP_GRADLE" ]; then
  VERSION_CODE=$(grep -oE 'versionCode = [0-9]+' "$APP_GRADLE" | head -1 | grep -oE '[0-9]+')
fi
VERSION="${VERSION:?set VERSION or run from repo root so $APP_GRADLE is readable}"
VERSION_CODE="${VERSION_CODE:?set VERSION_CODE or run from repo root so $APP_GRADLE is readable}"
TAG="${TAG:-android.${VERSION}}"

# Service-account json: honour SA_JSON, else the first known filename present.
if [ -z "${SA_JSON:-}" ]; then
  for c in nunchuk-service-account.json play-service-account.json; do
    [ -f "$c" ] && { SA_JSON="$c"; break; }
  done
  SA_JSON="${SA_JSON:-nunchuk-service-account.json}"
fi

# Creds: env first, else read from a gitignored local.properties. Searches the
# app repo then the sibling nativesdk repo (matches the documented layout).
CREDS_FILES=("${CREDS_PROPS:-}" "local.properties" "../nunchuk-android-nativesdk/local.properties")
prop() {
  local key="$1" f v
  for f in "${CREDS_FILES[@]}"; do
    [ -n "$f" ] && [ -f "$f" ] || continue
    v=$(grep -iE "^${key}=" "$f" | head -1 | cut -d= -f2- | tr -d '\r')
    [ -n "$v" ] && { printf '%s' "$v"; return; }
  done
}
: "${GITHUB_TOKEN:=$(prop githubToken | tr -d '[:space:]')}"
: "${GPG_PASSPHRASE:=$(prop GPGpass)}"

API="https://androidpublisher.googleapis.com/androidpublisher/v3"

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

for bin in openssl curl jq gpg; do command -v "$bin" >/dev/null || die "missing dependency: $bin"; done
# GNU sha256sum or macOS shasum -a 256 (both emit "<hash>  <file>")
if command -v sha256sum >/dev/null; then SHA="sha256sum"; else SHA="shasum -a 256"; fi

# ---- helpers: OAuth token (RS256 JWT via openssl) + Play download ---------
b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

mint_token() {
  local iss key now iat exp header claim signing_input sig jwt resp
  iss=$(jq -r '.client_email' "$SA_JSON")
  key=$(mktemp); trap 'rm -f "$key"' RETURN
  jq -r '.private_key' "$SA_JSON" > "$key"
  now=$(date +%s); iat=$now; exp=$((now + 3600))
  header=$(printf '{"alg":"RS256","typ":"JWT"}' | b64url)
  claim=$(printf '{"iss":"%s","scope":"https://www.googleapis.com/auth/androidpublisher","aud":"https://oauth2.googleapis.com/token","iat":%s,"exp":%s}' "$iss" "$iat" "$exp" | b64url)
  signing_input="${header}.${claim}"
  sig=$(printf '%s' "$signing_input" | openssl dgst -sha256 -sign "$key" -binary | b64url)
  jwt="${signing_input}.${sig}"
  resp=$(curl -sS -X POST https://oauth2.googleapis.com/token \
    --data-urlencode 'grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer' \
    --data-urlencode "assertion=${jwt}")
  echo "$resp" | jq -e -r '.access_token' 2>/dev/null \
    || die "token request failed: $resp"
}

download_from_play() {
  local token listing download_id
  log "Requesting Play API access token..."
  token=$(mint_token)
  log "Listing generated APKs for versionCode ${VERSION_CODE}..."
  listing=$(curl -sS -H "Authorization: Bearer $token" \
    "$API/applications/$PACKAGE/generatedApks/$VERSION_CODE")
  download_id=$(echo "$listing" | jq -r '.generatedApks[]?.generatedUniversalApk.downloadId // empty' | head -1)
  [ -n "$download_id" ] || die "no universal APK for versionCode $VERSION_CODE yet (Play may still be processing the upload, or App Signing is off). Raw: $listing"
  log "Downloading universal APK -> $APK"
  curl -sS -L -H "Authorization: Bearer $token" \
    "$API/applications/$PACKAGE/generatedApks/$VERSION_CODE/downloads/$download_id:download?alt=media" \
    -o "$APK"
}

# ---- 1/2. Obtain the Play-signed universal APK ----------------------------
APK_PATH="${APK_PATH:-}"
mkdir -p "$WORKDIR"
APK="$WORKDIR/${VERSION}.apk"

if [ -n "$APK_PATH" ]; then
  [ -f "$APK_PATH" ] || die "APK_PATH set but file not found: $APK_PATH"
  log "Using pre-downloaded APK: $APK_PATH (skipping Play download)"
  [ "$APK_PATH" -ef "$APK" ] || cp "$APK_PATH" "$APK"
else
  [ -f "$SA_JSON" ] || die "service-account json not found at '$SA_JSON' (it is the CI secret PLAY_SERVICE_ACCOUNT_JSON; drop it in the repo root). Or set APK_PATH to a manually-downloaded signed universal APK."
  download_from_play
fi
# sanity: must be a real zip/apk, not a JSON error blob
head -c4 "$APK" | grep -q 'PK' || die "file is not an APK/zip: $(head -c200 "$APK")"
log "APK size: $(du -h "$APK" | cut -f1)"

# ---- 3. Checksum + GPG clearsign ------------------------------------------
# Run in the workdir so the file column in SHA256SUMS is just "<version>.apk".
# The hash is deterministic, so regenerating SHA256SUMS is always safe; we only
# (re)sign if a valid SHA256SUMS.asc isn't already present -- this makes the
# script re-runnable after signing SHA256SUMS by hand in a real terminal (needed
# when no GUI pinentry is available and gpg can't prompt from a non-TTY shell).
( cd "$WORKDIR" && $SHA "${VERSION}.apk" > SHA256SUMS && cat SHA256SUMS )
if ( cd "$WORKDIR" && [ -f SHA256SUMS.asc ] && gpg --verify SHA256SUMS.asc 2>/dev/null ); then
  log "existing SHA256SUMS.asc verified [ok] -- skipping signing"
else
  log "Clearsigning SHA256SUMS..."
  if [ -n "${GPG_PASSPHRASE:-}" ]; then
    # non-interactive: loopback pinentry, passphrase via stdin (not argv -> not in ps)
    if ! printf '%s' "$GPG_PASSPHRASE" | ( cd "$WORKDIR" && gpg --batch --yes --pinentry-mode loopback \
        --passphrase-fd 0 --local-user "$GPG_KEY" --clearsign --output SHA256SUMS.asc SHA256SUMS ); then
      die "gpg clear-sign failed (wrong passphrase, or key $GPG_KEY not usable)."
    fi
  elif ! ( cd "$WORKDIR" && gpg --local-user "$GPG_KEY" --clearsign --yes --output SHA256SUMS.asc SHA256SUMS ); then
    die "gpg clear-sign failed (no interactive pinentry?). Set GPG_PASSPHRASE, or sign in a terminal then re-run with APK_PATH=$APK."
  fi
  ( cd "$WORKDIR" && gpg --verify SHA256SUMS.asc ) && log "signature verified [ok]"
fi

if [ "$SKIP_GITHUB" = "1" ]; then
  log "SKIP_GITHUB=1 -- artifacts ready in $WORKDIR:"; ls -la "$WORKDIR"; exit 0
fi

# ---- 4. Create the GitHub release + upload the three assets ----------------
[ -n "${GITHUB_TOKEN:-}" ] || die "GITHUB_TOKEN not set (needs a token with 'repo' write to $REPO)"
gh_api() { curl -sS -H "Authorization: Bearer $GITHUB_TOKEN" -H "Accept: application/vnd.github+json" "$@"; }

# Release body = the Play "what's new" notes (source of truth: the Slack
# release-notes thread -> distribution/whatsnew/whatsnew-en-US). Override with
# RELEASE_BODY or RELEASE_BODY_FILE.
BODY_FILE="${RELEASE_BODY_FILE:-distribution/whatsnew/whatsnew-en-US}"
if [ -n "${RELEASE_BODY:-}" ]; then BODY="$RELEASE_BODY"
elif [ -f "$BODY_FILE" ]; then BODY="$(cat "$BODY_FILE")"
else BODY=""; fi

log "Ensuring GitHub release for tag $TAG..."
rel=$(gh_api "https://api.github.com/repos/$REPO/releases/tags/$TAG")
rel_id=$(echo "$rel" | jq -r '.id // empty')
if [ -z "$rel_id" ]; then
  rel=$(gh_api -X POST "https://api.github.com/repos/$REPO/releases" \
    -d "$(jq -n --arg t "$TAG" --arg n "$VERSION" --arg b "$BODY" '{tag_name:$t,name:$n,body:$b,draft:false,prerelease:false}')")
  rel_id=$(echo "$rel" | jq -r '.id // empty')
  [ -n "$rel_id" ] || die "failed to create release: $rel"
  log "created release $TAG (id=$rel_id)"
else
  log "release already exists (id=$rel_id) -- updating notes + (re)uploading assets"
  gh_api -X PATCH "https://api.github.com/repos/$REPO/releases/$rel_id" \
    -d "$(jq -n --arg n "$VERSION" --arg b "$BODY" '{name:$n,body:$b}')" >/dev/null
fi

upload_asset() {
  local f="$1" name; name=$(basename "$f")
  # delete an existing asset of the same name first (idempotent re-runs)
  local existing
  existing=$(gh_api "https://api.github.com/repos/$REPO/releases/$rel_id/assets" \
    | jq -r --arg n "$name" '.[] | select(.name==$n) | .id')
  [ -n "$existing" ] && gh_api -X DELETE "https://api.github.com/repos/$REPO/releases/assets/$existing" >/dev/null
  log "uploading $name..."
  curl -sS -H "Authorization: Bearer $GITHUB_TOKEN" \
    -H "Content-Type: application/octet-stream" \
    --data-binary @"$f" \
    "https://uploads.github.com/repos/$REPO/releases/$rel_id/assets?name=$name" \
    | jq -e -r '.name' >/dev/null || die "upload failed for $name"
}

upload_asset "$APK"
upload_asset "$WORKDIR/SHA256SUMS"
upload_asset "$WORKDIR/SHA256SUMS.asc"

log "Done. Release: https://github.com/$REPO/releases/tag/$TAG"
