#!/usr/bin/env bash
# Promote the production DRAFT release for a given versionCode to a live rollout.
#
# CI (.github/workflows/ci.yml) uploads the signed AAB to the production track
# with status: draft, deliberately leaving the final rollout to a human. This
# script performs that final step over the Play Developer API so the release can
# be driven from the terminal instead of the Play Console UI.
#
# Usage (from the app repo root):
#   ./scripts/rollout-play-production.sh                 # dry run: show the planned change
#   CONFIRM=yes ./scripts/rollout-play-production.sh      # 100% rollout (status: completed)
#   CONFIRM=yes USER_FRACTION=0.1 ./scripts/rollout-play-production.sh   # staged 10%
#
# Env overrides:
#   VERSION_CODE  target versionCode (default: parsed from nunchuk-app/build.gradle.kts)
#   SA_JSON       Play service-account key (default: ./nunchuk-service-account.json)
#   USER_FRACTION staged rollout fraction 0<f<1; omit for a full 100% rollout
#   CONFIRM       must be "yes" to actually commit the edit
#
# Requirements: openssl, curl, jq  (no gcloud / python google libs needed)
set -euo pipefail

PACKAGE="io.nunchuk.android"
API="https://androidpublisher.googleapis.com/androidpublisher/v3"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

log() { printf '\n=== %s\n' "$*"; }
die() { printf '\nERROR: %s\n' "$*" >&2; exit 1; }

for bin in openssl curl jq; do command -v "$bin" >/dev/null || die "missing dependency: $bin"; done

GRADLE="$REPO_ROOT/nunchuk-app/build.gradle.kts"
VERSION_CODE="${VERSION_CODE:-$(sed -n 's/.*versionCode *= *\([0-9]\{1,\}\).*/\1/p' "$GRADLE" | head -1)}"
[ -n "$VERSION_CODE" ] || die "could not determine VERSION_CODE from $GRADLE"
SA_JSON="${SA_JSON:-$REPO_ROOT/nunchuk-service-account.json}"
[ -f "$SA_JSON" ] || die "service-account json not found at '$SA_JSON' (it is the CI secret PLAY_SERVICE_ACCOUNT_JSON; drop it in the repo root)"
USER_FRACTION="${USER_FRACTION:-}"
CONFIRM="${CONFIRM:-no}"

# ---- OAuth token (RS256 JWT via openssl) ----------------------------------
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

log "Requesting Play API access token..."
TOKEN=$(mint_token)

log "Opening an edit..."
EDIT=$(curl -sS -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Length: 0" \
  "$API/applications/$PACKAGE/edits")
EDIT_ID=$(echo "$EDIT" | jq -e -r '.id') || die "edits.insert failed: $EDIT"
cleanup_edit() { curl -sS -X DELETE -H "Authorization: Bearer $TOKEN" \
  "$API/applications/$PACKAGE/edits/$EDIT_ID" >/dev/null 2>&1 || true; }

log "Reading current production track..."
TRACK=$(curl -sS -H "Authorization: Bearer $TOKEN" \
  "$API/applications/$PACKAGE/edits/$EDIT_ID/tracks/production")
echo "$TRACK" | jq -e '.releases' >/dev/null 2>&1 || { cleanup_edit; die "tracks.get failed: $TRACK"; }

echo "$TRACK" | jq -r '.releases[] | "  - versionCodes=\(.versionCodes // []|join(",")) status=\(.status) name=\(.name // "-") userFraction=\(.userFraction // "-")"'

# The target release must already exist on the track as a draft (uploaded by CI).
TARGET=$(echo "$TRACK" | jq --arg vc "$VERSION_CODE" '.releases[] | select((.versionCodes // []) | index($vc))')
[ -n "$TARGET" ] || { cleanup_edit; die "versionCode $VERSION_CODE is not on the production track (has CI's Play upload finished?)"; }
TARGET_STATUS=$(echo "$TARGET" | jq -r '.status')
log "Found versionCode $VERSION_CODE on production with status=$TARGET_STATUS"
[ "$TARGET_STATUS" = "completed" ] && { cleanup_edit; log "Already fully rolled out - nothing to do."; exit 0; }

# Keep the release's own metadata (name, releaseNotes) and only change how it
# is being served. Submitting just this release supersedes the previous one.
if [ -n "$USER_FRACTION" ]; then
  NEW_RELEASE=$(echo "$TARGET" | jq --argjson f "$USER_FRACTION" '. + {status:"inProgress", userFraction:$f}')
  DESC="staged rollout at $(awk -v f="$USER_FRACTION" 'BEGIN{printf "%g%%", f*100}')"
else
  NEW_RELEASE=$(echo "$TARGET" | jq 'del(.userFraction) + {status:"completed"}')
  DESC="full 100% rollout"
fi
BODY=$(jq -n --argjson r "$NEW_RELEASE" '{track:"production", releases:[$r]}')

log "Planned change: versionCode $VERSION_CODE -> $DESC"
echo "$BODY" | jq .

if [ "$CONFIRM" != "yes" ]; then
  cleanup_edit
  log "DRY RUN - edit discarded. Re-run with CONFIRM=yes to publish."
  exit 0
fi

log "Updating the production track..."
UPD=$(curl -sS -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "$BODY" "$API/applications/$PACKAGE/edits/$EDIT_ID/tracks/production")
echo "$UPD" | jq -e '.releases' >/dev/null 2>&1 || { cleanup_edit; die "tracks.update failed: $UPD"; }

log "Committing the edit (this goes live)..."
COMMIT=$(curl -sS -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Length: 0" \
  "$API/applications/$PACKAGE/edits/$EDIT_ID:commit")
echo "$COMMIT" | jq -e '.id' >/dev/null 2>&1 || die "edits.commit failed: $COMMIT"

log "Done - versionCode $VERSION_CODE is now $DESC on the production track."
log "Verifying..."
TOKEN=$(mint_token)
VEDIT=$(curl -sS -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Length: 0" \
  "$API/applications/$PACKAGE/edits" | jq -r '.id')
curl -sS -H "Authorization: Bearer $TOKEN" \
  "$API/applications/$PACKAGE/edits/$VEDIT/tracks/production" \
  | jq -r '.releases[] | "  - versionCodes=\(.versionCodes // []|join(",")) status=\(.status) userFraction=\(.userFraction // "-")"'
curl -sS -X DELETE -H "Authorization: Bearer $TOKEN" \
  "$API/applications/$PACKAGE/edits/$VEDIT" >/dev/null 2>&1 || true
