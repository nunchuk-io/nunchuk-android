#!/usr/bin/env bash
#
# smoke-test-apk.sh - boot an emulator, install the Play-signed universal APK,
# launch it, sign in, and assert the app does not crash.
#
# Why this exists: a release swaps the native SDK (JNI). A JNI method-name <->
# Kotlin-extern mismatch is a *runtime* crash (UnsatisfiedLinkError / SIGSEGV on
# first call), not a compile error, so it only shows up when the app actually
# runs on a device. This is that safety net, driven headless in CI/local.
#
# Zero-arg from the app repo root:
#     ./scripts/smoke-test-apk.sh
#
# Sensitive test-account creds are NOT hardcoded here - they are read from the
# gitignored local.properties (keys SmokeTestEmail / SmokeTestPassword), searched
# in the app repo then ../nunchuk-android-nativesdk/local.properties. Override per
# run with the EMAIL / PASSWORD env vars if you must.
#
# Env overrides:
#   APK_PATH   apk to test        (default: build/github-release/<versionName>.apk)
#   AVD        emulator AVD name   (default: first from `emulator -list-avds`)
#   EMAIL      login email         (default: SmokeTestEmail from local.properties)
#   PASSWORD   login password      (default: SmokeTestPassword from local.properties)
#   CONFIRM_CODE  2FA code (default: SmokeTestConfirmCode from local.properties)
#   SKIP_LOGIN 1 to skip the login step (launch + crash-watch only)
#   WATCH_SECS crash-watch window after each phase (default: 25)
#   KEEP_EMU   1 to leave the emulator running on exit
#   HEADLESS   0 to show the emulator window (default: 1 = -no-window)
#
# Exit status (it gates the automatic production rollout, so it must be honest):
#   0  passed - launched, signed in (unless SKIP_LOGIN=1), no crash
#   1  failed - a crash signature, or sign-in never left the sign-in screen
#   2  partial - 2FA screen reached but no CONFIRM_CODE; logged-in phase not exercised
set -euo pipefail

PKG="io.nunchuk.android"
SDK="${ANDROID_SDK:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
AAPT2="$(ls -1 "$SDK"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1 || true)"

# prop <key> - read a value from the first local.properties that has it (gitignored)
prop() {
  local key="$1" v=""
  for f in local.properties ../nunchuk-android-nativesdk/local.properties; do
    [ -f "$f" ] || continue
    v="$(grep -E "^$key=" "$f" | head -1 | cut -d= -f2- | tr -d ' \r')"
    [ -n "$v" ] && { printf '%s' "$v"; return 0; }
  done
  return 1
}
EMAIL="${EMAIL:-$(prop SmokeTestEmail || true)}"
PASSWORD="${PASSWORD:-$(prop SmokeTestPassword || true)}"
CONFIRM_CODE="${CONFIRM_CODE:-$(prop SmokeTestConfirmCode || true)}"   # static test 2FA code
SKIP_LOGIN="${SKIP_LOGIN:-0}"
WATCH_SECS="${WATCH_SECS:-25}"
KEEP_EMU="${KEEP_EMU:-0}"
HEADLESS="${HEADLESS:-1}"

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m[ok]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2; exit 1; }

# ---- resolve APK ------------------------------------------------------------
if [ -z "${APK_PATH:-}" ]; then
  VER="$(grep -E 'versionName *= *"' nunchuk-app/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')"
  APK_PATH="build/github-release/${VER}.apk"
fi
[ -f "$APK_PATH" ] || fail "APK not found: $APK_PATH (set APK_PATH=...)"
log "APK: $APK_PATH ($(du -h "$APK_PATH" | cut -f1))"
if [ -n "$AAPT2" ]; then
  APPCLASS="$("$AAPT2" dump xmltree --file AndroidManifest.xml "$APK_PATH" 2>/dev/null \
    | grep -A30 'E: application' | grep -m1 'android:name' | sed -E 's/.*Raw: "([^"]+)".*/\1/')"
  log "application class: ${APPCLASS:-<unknown>}"
fi

# ---- boot emulator ----------------------------------------------------------
# NB: `emulator -list-avds` also prints INFO/WARNING log lines to stdout; keep
# only lines that look like a bare AVD name.
AVD="${AVD:-$("$EMULATOR" -list-avds 2>/dev/null | grep -E '^[A-Za-z0-9._-]+$' | head -1)}"
[ -n "$AVD" ] || fail "no AVD available (create one in Android Studio)"

EMU_PID=""
cleanup() {
  [ "$KEEP_EMU" = "1" ] && return
  [ -n "$EMU_PID" ] && kill "$EMU_PID" 2>/dev/null || true
  "$ADB" -s emulator-5554 emu kill >/dev/null 2>&1 || true
}
trap cleanup EXIT

if ! "$ADB" devices | grep -q 'emulator-5554.*device'; then
  log "booting AVD '$AVD' (headless=$HEADLESS)..."
  WINFLAG="-no-window"; [ "$HEADLESS" = "0" ] && WINFLAG=""
  # shellcheck disable=SC2086
  "$EMULATOR" -avd "$AVD" -no-snapshot -no-boot-anim -no-audio -wipe-data \
    -gpu swiftshader_indirect $WINFLAG >/tmp/emu-smoke.log 2>&1 &
  EMU_PID=$!
else
  log "reusing running emulator-5554"
fi

log "waiting for device..."
"$ADB" wait-for-device
log "waiting for boot_completed..."
for _ in $(seq 1 120); do
  [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
  sleep 2
done
[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] || fail "emulator did not finish booting"
"$ADB" shell input keyevent 82 >/dev/null 2>&1 || true   # dismiss keyguard
ok "emulator booted"

# ---- install ----------------------------------------------------------------
log "uninstalling any previous $PKG..."
"$ADB" uninstall "$PKG" >/dev/null 2>&1 || true
log "installing universal APK..."
"$ADB" install -r -d "$APK_PATH" 2>&1 | tail -2
"$ADB" shell pm path "$PKG" >/dev/null 2>&1 || fail "install failed"
ok "installed"

# ---- crash watch helper -----------------------------------------------------
# Returns non-zero (and prints the offending lines) if a crash of our process
# is observed in logcat, or if the process is expected up but dead.
assert_no_crash() {
  local phase="$1"; local secs="$2"; local expect_alive="${3:-1}"
  log "watching for crashes: $phase (${secs}s)..."
  sleep "$secs"
  local crashes
  crashes="$("$ADB" logcat -d 2>/dev/null | grep -aE \
    "FATAL EXCEPTION|AndroidRuntime: .*$PKG|Process $PKG .*died|ActivityManager: Process $PKG.*has died|UnsatisfiedLinkError|F DEBUG|libnunchuk.*SIG|signal 11 \(SIGSEGV\)" \
    | grep -aiE "$PKG|libnunchuk|nunchuk" || true)"
  if [ -n "$crashes" ]; then
    printf '%s\n' "$crashes" | tail -40
    fail "crash detected during: $phase"
  fi
  if [ "$expect_alive" = "1" ]; then
    "$ADB" shell pidof "$PKG" >/dev/null 2>&1 || fail "$PKG process is not alive after: $phase"
  fi
  ok "no crash: $phase (pid=$("$ADB" shell pidof "$PKG" 2>/dev/null | tr -d '\r'))"
}

# ---- launch -----------------------------------------------------------------
"$ADB" logcat -c || true
log "launching $PKG..."
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
assert_no_crash "cold launch" "$WATCH_SECS"

# ---- login (UI automation by resource-id) -----------------------------------
UI_TMP="/tmp/nunchuk-ui.xml"
ui_dump() {
  "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
  "$ADB" exec-out cat /sdcard/ui.xml 2>/dev/null > "$UI_TMP" || true
}
# tap center of the first node whose resource-id ends with /<id>. Returns 1 on miss.
tap_id() {
  ui_dump
  local xy; xy=$(python3 - "$UI_TMP" "$1" <<'PY'
import sys,re,xml.etree.ElementTree as ET
try: root=ET.parse(sys.argv[1]).getroot()
except Exception: sys.exit(3)
rid=sys.argv[2]
for n in root.iter('node'):
    if n.get('resource-id','').endswith('/'+rid):
        b=re.findall(r'\d+',n.get('bounds',''))
        if len(b)==4: print((int(b[0])+int(b[2]))//2,(int(b[1])+int(b[3]))//2); sys.exit(0)
sys.exit(4)
PY
) || return 1
  [ -n "$xy" ] && "$ADB" shell input tap $xy
}
# tap the first EditText whose text/desc contains <substring> ("" = first EditText)
tap_edit_matching() {
  ui_dump
  local xy; xy=$(python3 - "$UI_TMP" "$1" <<'PY'
import sys,re,xml.etree.ElementTree as ET
try: root=ET.parse(sys.argv[1]).getroot()
except Exception: sys.exit(3)
needle=sys.argv[2].lower()
for n in root.iter('node'):
    if 'EditText' in n.get('class','') and needle in (n.get('text','')+n.get('content-desc','')).lower():
        b=re.findall(r'\d+',n.get('bounds',''))
        if len(b)==4: print((int(b[0])+int(b[2]))//2,(int(b[1])+int(b[3]))//2); sys.exit(0)
sys.exit(4)
PY
) || return 1
  [ -n "$xy" ] && "$ADB" shell input tap $xy
}
# is a node with resource-id ending /<id> currently on screen?
has_id() { ui_dump; grep -q "/$1\"" "$UI_TMP"; }
# the authenticated home. Assert THIS, not "we left the sign-in screen": the 2FA
# step runs in its own VerifyNewDeviceActivity, so "not on SignInActivity" is
# also true while sitting on a rejected 2FA screen.
HOME_ACTIVITY="com.nunchuk.android.main.MainActivity"
on_home() { "$ADB" shell dumpsys window 2>/dev/null | grep -q "mCurrentFocus.*$HOME_ACTIVITY"; }
# wait up to $1 seconds for the home activity to come to the foreground
wait_for_home() {
  local deadline=$(( $(date +%s) + ${1:-45} ))
  while [ "$(date +%s)" -lt "$deadline" ]; do on_home && return 0; sleep 3; done
  return 1
}
# tick the "keep me logged in" checkbox when it is on screen - without it the
# session is not persisted, so the app returns to sign-in on next launch
tick_stay_signed_in() { has_id staySignIn && { tap_id staySignIn || true; ok "ticked 'keep me logged in'"; }; }
# `input text` needs spaces as %s; email/password/code chars (@ + . _ -) are fine
type_text() { "$ADB" shell input text "$(printf '%s' "$1" | sed 's/ /%s/g')"; }
esc_kbd()   { "$ADB" shell input keyevent 111 >/dev/null 2>&1 || true; }

# Sign-in flow discovered on 2.8.0 (io.nunchuk.android):
#   Screen 1: editText (email)     -> signIn ("Continue")
#   Screen 2: editText (password)  -> signIn ("Sign in")
#   Screen 3 (optional 2FA): editText (emailed code) -> btnContinue
#   Post-login: btnNotNow ("Not now") on the notifications prompt
if [ "$SKIP_LOGIN" = "1" ]; then
  log "SKIP_LOGIN=1 - skipping sign-in"
else
  { [ -n "$EMAIL" ] && [ -n "$PASSWORD" ]; } || fail "no test creds: set SmokeTestEmail/SmokeTestPassword in local.properties (or EMAIL/PASSWORD env), or run SKIP_LOGIN=1"
  log "signing in as $EMAIL ..."
  tap_id editText || fail "email field (editText) not found on sign-in screen"
  sleep 1; type_text "$EMAIL"; esc_kbd; ok "entered email"
  tap_id signIn   || fail "Continue button (signIn) not found"
  sleep 4
  tap_edit_matching password || fail "password field not found on screen 2"
  sleep 1; type_text "$PASSWORD"; esc_kbd; ok "entered password"
  tick_stay_signed_in
  tap_id signIn || fail "Sign in button (signIn) not found"
  assert_no_crash "after sign-in submit" "$WATCH_SECS"

  # Optional 2FA - the confirmation code is emailed, so pass it via CONFIRM_CODE=...
  if has_id btnContinue && ! has_id btnNotNow; then
    if [ -n "${CONFIRM_CODE:-}" ]; then
      log "entering 2FA confirmation code..."
      tap_edit_matching "" || tap_id editText || true   # first EditText = code box
      sleep 1; type_text "$CONFIRM_CODE"; esc_kbd
      tap_id btnContinue || true
      assert_no_crash "after 2FA submit" "$WATCH_SECS"
    else
      log "2FA screen detected but CONFIRM_CODE unset - re-run with CONFIRM_CODE=<code> to finish."
      assert_no_crash "at 2FA screen" "$WATCH_SECS"
      printf '\033[1;33m[PARTIAL]\033[0m %s\n' \
        "launch + sign-in UI passed the crash gate; the logged-in phase was NOT exercised (no 2FA code)." >&2
      exit 2
    fi
  fi

  # Post-login: dismiss the notifications prompt if it appears -> confirms we
  # reached the authenticated home screen.
  REACHED_HOME=0
  if has_id btnNotNow; then tap_id btnNotNow || true; sleep 2; fi
  if wait_for_home 60; then
    REACHED_HOME=1; ok "reached authenticated home ($HOME_ACTIVITY)"
  fi
  assert_no_crash "post-login" "$WATCH_SECS"

  # A silent sign-in failure must NOT be reported as a pass. The logged-in phase
  # is where the native SDK does its real work (wallet sync / electrum), so a run
  # that never got past the sign-in screen is INCONCLUSIVE - and this script's
  # exit status gates an automatic 100% production rollout.
  if [ "$REACHED_HOME" != "1" ]; then
    printf '\033[1;31m[FAIL]\033[0m %s\n' "sign-in did not complete - never reached $HOME_ACTIVITY." >&2
    printf '       %s\n' "last activity: $("$ADB" shell dumpsys window 2>/dev/null | grep -oE 'mCurrentFocus=Window\{[^}]*\}' | head -1)" >&2
    printf '       %s\n' "Launch + sign-in-UI crash gate PASSED; the logged-in phase was NOT exercised." >&2
    printf '       %s\n' "Check SmokeTestEmail / SmokeTestPassword / SmokeTestConfirmCode in local.properties." >&2
    exit 1
  fi
fi

# ---- light exercise: monkey a few UI events, still must not crash -----------
"$ADB" logcat -c || true
log "exercising UI (monkey, 60 events)..."
"$ADB" shell monkey -p "$PKG" --throttle 400 --pct-syskeys 0 -s 42 60 >/dev/null 2>&1 || true
assert_no_crash "post-monkey" "$WATCH_SECS"

# ---- capture a screenshot for evidence --------------------------------------
SHOT="/tmp/nunchuk-smoke-${PKG}.png"
"$ADB" exec-out screencap -p > "$SHOT" 2>/dev/null && log "screenshot: $SHOT" || true

if [ "$SKIP_LOGIN" = "1" ]; then
  ok "SMOKE TEST PASSED - $PKG launched and did not crash (SKIP_LOGIN=1: sign-in not exercised)"
else
  ok "SMOKE TEST PASSED - $PKG launched, signed in, and did not crash"
fi
