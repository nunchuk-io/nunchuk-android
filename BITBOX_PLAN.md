# BitBox02 integration plan

Bring BitBox02 to parity with Ledger/Trezor: **add-key flow, assisted (membership)
flow, and all three sign flows** (normal transaction, dummy transaction, sign-in BSMS
transaction) — plus the BitBox-specific device setup and management surface that
Ledger/Trezor do not have.

## Sources

| What | Where |
|---|---|
| Slack thread | [C017CLBP6P3 p1786701441876789](https://nunchuk.slack.com/archives/C017CLBP6P3/p1786701441876789) |
| Design | https://bitbox.leenunchuk.workers.dev/ — **revised 2026-08-21**: cut to the add-key flow only (10 screens); the 23-screen setup/management exploration is now a side link |
| Native API doc | [Confluence · Bitbox](https://nunchuck.atlassian.net/wiki/spaces/NUN/pages/1203568642/Bitbox) |
| libnunchuk | `origin/bitbox` branch, `src/utils/bitbox/` |
| Reference impl | Ledger — `nunchuk-core/.../core/ledger/`, `nunchuk-signer/.../signer/ledger/`, `ledger-jni.cpp` |

The Confluence doc says explicitly: *"Start with the Ledger integration guide for the
general flow, transport readiness, stable IDs, BLE write queue, and single-command
rule."* BitBox is a **mirror of the Ledger architecture**, not a new design.

## Scope, after the 2026-08-21 revision

The first design carried the whole BitBoxApp surface — device setup (seed creation,
recovery-word restore, microSD backups) and device management (rename, password,
passphrase, backups, firmware upgrade, factory reset). **That is all cut.** An
unprepared device is now sent to BitBoxApp via a single screen (05B), so the Nunchuk
side is the same shape as Ledger: *prepare → pick wallet config → pair → read xpub →
sign*. Phases 5 and 6 are dropped; their old contents are kept at the bottom for
reference if setup is ever picked up again.

Per Hugo (2026-08-21), BitBox must work everywhere Ledger does:

- unassisted **and** assisted wallets
- off-chain **and** on-chain protocols
- non-inheritance **and** inheritance keys

> **Firmware caveat (Huy).** A BitBox on current firmware can create and sign
> transactions on an on-chain timelock wallet, but **cannot spend after a time-based
> timelock** — i.e. it can't claim. That needs newer BitBox firmware. So on-chain
> inheritance keys are addable and signable now; claiming is the gap.

## Architecture

```
BLE / USB transport   →   BitBoxController (Kotlin)   →   bitbox-jni.cpp   →   BitBoxSession (C++)
   (mirror of LedgerBleController)                          (mirror of ledger-jni.cpp)
```

Every operation returns a `BitBoxStep`; the controller drives it to a terminal
`COMPLETE` / `FAILED` / `REBOOT` before starting another. Differences from Ledger that
the bindings must handle:

- `BitBoxManager` needs a live `Nunchuk&` (Ledger's manager is default-constructed), so
  it must be created lazily from `NunchukProvider` **and torn down when the SDK
  re-initialises** (account/chain switch) — pairing data is stored per account/chain.
- `BitBoxStep` carries `retry_after_ms`, `pairing_code` and `progress` on top of
  Ledger's fields → two extra step types (`RETRY_AFTER`, `AWAITING_USER`) plus `REBOOT`.
- Results are structs, not single strings (`InitializeResult`, `ListBackupsResult`,
  `BitBoxDeviceInfo`, …), so `ledgerResultString()`'s one-string shortcut does **not**
  carry over — real model classes are required.
- A separate **bootloader session** exists for firmware upgrade.

## Screen order

Settled on the thread (2026-08-21): the intro/prepare screen **always comes first**, for
both transports. Trezor Android already does this (`ActionIntro → SuiteIntro →
SelectWalletType`); Ledger Android is the odd one out — it shows its instruction screen
for Bluetooth only and sends USB straight to select-wallet-type. BitBox follows Trezor.

```
01A/01B transport choice → 02A/02B prepare (per transport) → 03 wallet & address type
   → 04 device picker → 05 confirm pairing → 06 name your key → return signer
                             ↘ 05B BitBoxApp required   ↘ 05C attestation warning
```

Screen 04 reuses `LedgerDeviceScanBody` and its Bluetooth/USB refresh behaviour, per the
design note.

## Design assets

The illustrations come from `~/Downloads/bitbox/` (Huy's export). `NcImageAppBar` loads its
background with `painterResource`, which accepts only a VectorDrawable or a raster — so each
one is converted to a `<vector>`, never a `<layer-list>`.

| Asset | Drawable | Used by |
|---|---|---|
| `connect-bitbox.svg` (375×300) | `bg_bitbox_illustration.xml` | 01A transport choice, 02A/02B prepare |
| `attestation-warning.svg` (160×160) | `bg_bitbox_attestation_warning.xml` | 05C attestation warning |
| `bitbox-device.svg` (120×160) | `bg_bitbox_device.xml` | 05B BitBoxApp required |

The spot illustrations (160×160, 120×160) are centred on the shared 375×300 denim panel that
`NcImageAppBar` expects. All three take their backdrop from `@color/nc_fill_denim` rather than
the asset's literal `#D0E2FF`, so they track the theme the way `bg_ledger_illustration` does —
`#D0E2FF` is that colour's light-mode entry, and a fixed light panel would break dark mode.

Not wired up:

- `wallet-address-type.svg` — screen 03 is the shared `SelectWalletTypeScreen` (also used by
  Trezor and Ledger) and it takes no illustration parameter, so using this asset means changing
  that shared screen for all three key types. Product call.
- `setup-complete.svg`, `microsd-card.svg`, `firmware-file.svg` — belong to the dropped setup
  and firmware-upgrade phases.

## Transport facts (from Confluence)

- BLE service `e1511a45-f3db-44c0-82b8-6c880790d1f1`, write `799d485c-d354-4ed0-b577-f8ee79ec275a`,
  indicate `419572a5-9f53-4eb1-8db7-61bcab928867`, product info `9d1c9a77-8b03-4e49-8053-3955cda7da93`
- Bond + request MTU 512; initialise only after indications **and** writes are ready
- USB: VID/PID `0x03eb:0x2403`, claim interface 0, bulk IN/OUT
- Session id: BLE `BluetoothDevice.address`, USB `UsbDevice.deviceName`
- Queue each 64-byte report in `BitBoxStep.writes`, one write at a time
- Firmware < 9.0 is upgrade-only; ≥ 10.0 needs an updated integration
- BLE = BitBox02 Nova only; USB = all models (**iOS has no USB — hide it there**)

---

## Progress

Legend: ⬜ not started · 🟡 in progress · ✅ done · ⏹ dropped

**Where things stand.** Phases 0–2 are done and the add-key flow has been **confirmed working
on a real BitBox02**. The native bindings ship as `io.nunchuk.android:nativesdk:1.2.22-bitbox`,
the BLE/USB controller lives in `nunchuk-core`, and the flow runs end to end from
`SignerIntroActivity` to a created signer. Phase 4 (assisted / membership + replace) is now
wired at every dispatcher — off-chain personal, off-chain group, replace, and on-chain timelock
all pair BitBox in-app instead of handing off to the desktop app. Phase 3 has started: the
health check (3.7) runs in-app off the new BitBox sheet and is **confirmed working on a real
BitBox02**, and signing is wired everywhere it matters (3.1–3.6) — a wallet transaction, the
membership dummy tx, sign-in via digital signature, and the membership sign-message check all
sign in-app, an address can be verified on the device, and a message can be signed. Still open:
nothing — phase 3 is complete. Everything except the health check is **not yet exercised on
hardware**, and sign message carries one assumption worth checking there first: the RFC2440 block
is assembled in Kotlin, so its output should be compared against a Ledger or software-key export.

### Picking this up

Everything lives on a `bitbox` branch in both repos, with a third branch in libnunchuk:

| Repo | Branch | Note |
|---|---|---|
| `my-android` | `bitbox` | off `2.8.3` |
| `nunchuk-android-nativesdk` | `bitbox` | off `jade-qr-pin-unlock`, so it carries Jade QR PIN unlock too |
| `libnunchuk` (submodule) | `bitbox-integration` | merge of the Jade/Electrum head with `origin/bitbox`; pushed under a **new name** so the native team's own `bitbox` branch is untouched |

Two things will bite anyone building this branch:

1. **`nativeSdk = "1.2.22-bitbox"` resolves from local Maven only.** Build and
   `./gradlew publish` the SDK from its `bitbox` branch first, or the app won't resolve.
2. **Debug variants only.** `NativeSdkConventionPlugin` puts the catalog version on
   `debugImplementation`; `releaseImplementation` uses the GitHub prebuild pinned by
   `prebuildNativeSdk` (still tag 1.2.22). A release build has no BitBox until a prebuild is
   published and that pin moves.

Smaller loose ends:

- `wallet-address-type.svg` is unwired — screen 03 is the shared `SelectWalletTypeScreen`, so
  using it changes Trezor and Ledger too.
- The design's screen 01B (iPhone, "USB not supported") has no Android equivalent; Android
  offers both transports, so only 01A is implemented.
- `bg_bitbox_attestation_warning` and `bg_bitbox_device` centre a spot illustration on the denim
  panel because `NcImageAppBar` wants a full-width background. If the designer supplies 375×300
  versions, drop them in under the same names.

### Phase 0 — Native SDK bindings (`nunchuk-android-nativesdk`) ✅

Was the blocking prerequisite: nativesdk 1.2.23 had zero BitBox classes. Now shipped as
**`1.2.22-bitbox`** — a branch build rather than a claimed version number, following the
existing `1.1.58-coin` convention. 36 JNI entry points verified present in
`libnunchuk-android.so` (arm64) and 15 BitBox Kotlin classes in the published AAR;
`io.nunchuk.android:nativesdk:1.2.22-bitbox` resolves on this repo's `debugCompileClasspath`.

> **Release-variant caveat.** `NativeSdkConventionPlugin` wires the catalog version to
> `debugImplementation` only; `releaseImplementation` uses the GitHub prebuild pinned by
> `prebuildNativeSdk` (still tag 1.2.22). BitBox therefore works in **debug builds only**
> until a matching prebuild is published and `prebuildNativeSdk` is bumped — worth doing
> before any Firebase distribution of a release variant.

| # | Task | State |
|---|---|---|
| 0.1 | Move libnunchuk submodule to `bitbox` — merged `origin/bitbox` (`f759629`) into the Jade-QR/Electrum head (`0070716`) on branch `bitbox-integration` → `71faaaf`; clean merge, both feature sets verified present | ✅ |
| 0.2 | `BitBoxStepType` / `BitBoxTransport` / `BitBoxUserInteraction` / `BitBoxProduct` / `BitBoxAttestationStatus` / `BitBoxMnemonicLength` (ordinals match `types.hpp`) + `BitBoxErrorCode` (**explicit values, not ordinals** — the 1xx block comes from the firmware, so it maps via `from(value)`) | ✅ |
| 0.3 | `BitBoxStep` (optionals flattened: null `pairingCode`/`error`, `NO_PROGRESS = -1.0` sentinel), `BitBoxError`, `BitBoxDeviceInfo`, `BitBoxInitializeResult`, `BitBoxBackup`, `BitBoxFirmwareInfo` | ✅ |
| 0.4 | `bitbox-jni.cpp` — session lifecycle (`initialize`, `onData`, `resume`, `confirmPairing`); manager rebuilt whenever `NunchukProvider::nu` changes so pairing writes never target a freed database | ✅ |
| 0.5 | JNI: key ops — `getMasterFingerprint`, `getExtendedPublicKey`, `isWalletRegistered`, `registerWallet`, `signMessage`, `signPsbt`, `getWalletAddress` | ✅ |
| 0.6 | JNI: setup ops — `setDeviceName`, `createNewSeed`, `showMnemonic`, `restoreFromMnemonic`, `checkSdCard`, `insertSdCard`, `listBackups`, `restoreBackup` | ✅ |
| 0.7 | JNI: management ops — `changePassword`, `setMnemonicPassphraseEnabled`, `createBackup`, `checkBackup`, `factoryReset`, `enterFirmwareUpgrade`, `inspectFirmware`, bootloader session (`upgradeFirmware` / `onData` / `reboot`) | ✅ |
| 0.8 | JNI: results — `resultString` / `resultBoolean` / `initializeResult` / `listBackupsResult` / `deviceInfo`, plus `getBitBoxSignMessagePath` / `getBitBoxSignMessageAddress` | ✅ |
| 0.9 | 34 `LibNunchukAndroid.kt` externals + `NunchukNativeSdk.kt` wrappers; `bitbox-jni.cpp` added to `CMakeLists.txt` | ✅ |
| 0.10 | Built `assembleArm64_v8aRelease` (arm64 + v7a), published `1.2.22-bitbox` to local Maven, pointed `nativeSdk` in `gradle/libs.versions.toml` at it | ✅ |

### Phase 1 — Transport + controller (`nunchuk-core`) ✅

`BitBoxController.kt` (1119 lines) is a port of `LedgerBleController` onto the `bitbox*`
bindings. Compiles against `1.2.22-bitbox`; not yet exercised against hardware.

| # | Task | State |
|---|---|---|
| 1.1 | `BitBoxController.kt` — BLE + USB, scan/bond/MTU 512/connect, 64-byte write queue, transport-agnostic step loop, 24 commands + 5 typed result accessors | ✅ |
| 1.2 | `RETRY_AFTER` → delayed `bitboxResume`; `AWAITING_USER` → pairing code surfaced once per session then `confirmPairing`; `REBOOT` → write frames, then `onReboot` (terminal) | ✅ |
| 1.3 | Disconnect (BLE state change / USB detach) clears runtime, resets interaction + pairing state and fires `onDisconnected`; there is no resume path across it — the host re-runs `initialize()` | ✅ |
| 1.4 | `reportInteraction` drops repeats and `NONE`, and resets per command, so polling can't re-fire the same prompt | ✅ |
| 1.5 | `BitBoxUsbAttachActivity` + `bitbox_usb_device_filter.xml` (vendor 1003 / product 9219) + manifest entry. **Pins the product id**, unlike the Ledger filter — `0x03eb` is Atmel's shared vendor id and would otherwise match unrelated hardware | ✅ |
| 1.6 | Failures carry a typed `BitBoxErrorCode` + message to `onCommandFailed`, and `isSessionLost()` marks the codes needing a fresh session. Mapping each code to a *screen* (setup / connect / firmware) belongs to the flows — phase 2 | ✅ |

### Phase 2 — Add-key flow (`nunchuk-signer`) ✅

`BitBoxActivity` + 8 destinations in `nunchuk-signer/.../bitbox/`, mirroring `LedgerActivity`.
Two departures forced by the BitBox protocol:

- **`initialize()` opens every session.** Its result decides whether the device is genuine,
  whether the firmware is usable, and whether it has been set up — so the readiness gate
  runs before the fingerprint is ever requested.
- **The xpub needs an explicit derivation path.** Ledger passes wallet type / address type /
  index to the native call; BitBox takes a path string, so the path is resolved between the
  fingerprint and xpub round trips (`onMasterFingerprintReceived` → `FetchXpub` → `onXpubReceived`)
  and held in state for the `createSigner` that follows.

The device picker is genuinely shared: `LedgerDeviceScanBody` moved to
`nunchuk-core/.../hardware/HardwareDeviceScanContent.kt` as `HardwareDeviceScanBody`, with the
vendor bits (title / subtitle / USB-empty copy / row icon) as parameters defaulting to Ledger's.
`LedgerDevice` / `LedgerTransportKind` and `BitBoxDevice` / `BitBoxTransportKind` are now
typealiases of `HardwareDevice` / `HardwareTransportKind`, so Ledger's ~35 references were
untouched.

Screens 01A/01B, 02A/02B, 03, 04, 05, 05B, 05C, 06 (the whole revised design).

| # | Task | Screen | State |
|---|---|---|---|
| 2.1 | `BitBoxActivity` + nav graph, `verifyXfpOnly` / `accountIndex` / `expectedXfp` params, membership vs standalone modes, `RESULT_ACTION_OPEN_DESKTOP_FLOW` | — | ✅ |
| 2.2 | Transport choice — Bluetooth (Nova) / USB (all models), each with its model subtitle; new `ic_bluetooth` drawable | 01A/01B | ✅ |
| 2.3 | Device picker via the shared `HardwareDeviceScanBody`, BitBox copy + `ic_bitbox_hardware` rows | 04 | ✅ |
| 2.4 | Confirm pairing — code shown once per session, "Codes match" / "Codes don't match" → `confirmPairing(accepted)` | 05 | ✅ |
| 2.5 | Attestation warning — dead end, no "continue anyway"; reached from the initialize result or an `ATTESTATION` failure | 05C | ✅ |
| 2.6 | **BitBoxApp required** (replaces the old firmware-upgrade + setup screens): uninitialized / upgrade-only / bootloader → Open BitBoxApp (falls back to Play) or Scan again | 05B | ✅ |
| 2.7 | Prepare-your-BitBox instructions, one variant per transport, **always before** wallet/address type | 02A/02B | ✅ |
| 2.8 | Select wallet & address type (shared `SelectWalletTypeScreen`) then Name your key (shared `SetKeyNameScreen`); membership flows auto-name `BitBox`, `BitBox 2`, … | 03, 06 | ✅ |
| 2.9 | `isInAppHardwareTag` now includes `BITBOX` (`nunchuk-core/util/SignerUtil.kt`) | — | ✅ |
| 2.10 | `SignerIntroActivity`: `KeyType.BITBOX` → `openBitBoxScreen()` instead of the desktop hand-off; `verifyLedgerBackupLauncher` renamed `verifyHardwareBackupLauncher` and shared | — | ✅ |

### Phase 2 review (2026-08-21)

Findings from re-reading the whole implementation, all fixed:

| Finding | Fix |
|---|---|
| **Ledger-branded copy shown in the BitBox flow.** `nc_ledger_bluetooth_permission_required`, `_bluetooth_off`, `_no_devices_found` and `_communicating` all name Ledger in their text — user-visible in four places | Added `nc_bitbox_*` equivalents. Genuinely neutral strings (`"Connect"`, `"Connecting to %1$s…"`, the picker section headers) are still shared on purpose |
| **Readiness decided in two places.** The activity called `onInitialized(...)` *and* recomputed `isReady` itself to continue, so the two could drift — and the view model cleared `isProcessing` before the next command started, dropping the spinner mid-conversation | The view model owns the decision and emits `ReadMasterFingerprint`; the activity only reacts |
| **Failure paths faked initialize flags.** `DEVICE_UNINITIALIZED` / `ATTESTATION` called `onInitialized(...)` with invented booleans to trigger navigation | Explicit `onDeviceNotReady()` / `onAttestationInvalid()` |
| **"Scan again" always rescanned Bluetooth.** A USB user hitting it on screen 05B got a Bluetooth permission prompt | Transport persisted as `isUsbTransport`; rescan and scan-again both honour it |
| **Single-command rule unenforced.** Confluence requires driving a step to a terminal type before starting another; `commandActive` was write-only, so nothing stopped two conversations interleaving on one session | `startCommand` refuses while a command is live — with `INITIALIZE` exempt, since it builds a fresh native session and is the documented way out of a stuck one |
| **Dead state.** `productCharacteristic` (BLE product info, left scope with firmware upgrade) and `bleMtuPayload` (native hands over pre-chunked 64-byte reports) were written and never read | Removed; the MTU 512 *request* stays |
| Fragile route comparison against `qualifiedName` | Type-safe `hasRoute<BitBoxConfirmPairingRoute>()` |
| `nc_bitbox_scanning` added but unused; the picker blanked its status line instead | Wired to the scanning status, matching the design copy |

**Pairing-code timing (found on a second pass over the Confluence page + native source).**
The code is *not* exclusive to `AWAITING_USER`. `BitBoxSession` attaches it to the WRITE that
asks the device to confirm (`sendRaw({'v'}, Phase::PAIRING_VERIFY, CONFIRM_PAIRING,
pairing_code)`) and re-attaches it to every `READ_MORE` and `RETRY_AFTER` while that request is
outstanding. Reading it only on `AWAITING_USER` was wrong twice over:

- **USB** — `AWAITING_USER` only arrives *after* the device side is confirmed, so the user was
  asked to compare a code on the device that Nunchuk had not shown yet.
- **BLE** — `trusted_transport` makes `app_pairing_confirmed = true`, so `PAIRING_VERIFY` goes
  straight to `finishPairing()` and `AWAITING_USER` **never fires**. The code was never shown at
  all, even when the device asked for it.

The screen also suppresses the flow's modal `NcLoadingDialog` while it is showing: the inline
spinner beside "Confirm the code on your BitBox" *is* that screen's loading state, and the
modal would have covered the two answer buttons the step depends on.

Fixed by surfacing the code from whatever step carries it (still once per session), leaving
`AWAITING_USER` to mean only "now the answer must come from the user". Because pairing can then
resolve without either button being pressed, every exit from initialization
(`ReadMasterFingerprint`, both dead ends, and `Error`) dismisses the screen. Screen 05 also
gained the device icon and the spinner beside "Confirm the code on your BitBox", which is what
that waiting state looks like in the design.

Also split per the request: `BitBoxScanUiState.kt`, `BitBoxScanEvent.kt` and `BitBoxViewModel.kt`
are now three files (`LedgerViewModel.kt` still keeps all three in one — worth aligning if
Ledger is next to be touched).

Left alone deliberately:

- `BitBoxRequest` still carries the setup/management operations (`createNewSeed`,
  `restoreBackup`, `factoryReset`, firmware upgrade…) and `BitBoxController` the matching
  one-line commands. Unused by add-key, but they are the only callers the phase-0 bindings will
  ever need if setup returns, and they are documented. Delete them if the dropped phases are
  considered permanent.
- The shared picker's section headers are still named `nc_ledger_bluetooth_devices` /
  `nc_ledger_usb_devices` / `nc_ledger_device_available`. Text is vendor-neutral; renaming the
  keys touches Ledger's sheet for no user-visible gain.

### Free-user (standalone) add-key review

Traced the whole non-membership path: `SignerIntroActivity` → `openBitBoxScreen()` →
`BitBoxActivity(isMembershipFlow = false)` → 01A → 02A/02B → 03 → 04 → 05 → 06 →
`openSignerInfoScreen`.

Reachability checks out. A free user has no server-driven `supportedSigners`, so
`resolveSignersToDisplay` falls through to `defaultSupportedSigners`, which carries the
`HARDWARE`/`BITBOX` entry — the card is listed and enabled.

Fixed here:

| Finding | Fix |
|---|---|
| **BitBox card still read "Desktop only".** `SignerDisplayInfo` gave the BITBOX card a `descriptionRes` that Ledger and Trezor never had — it now pairs in-app, so the label was simply wrong | Removed the `descriptionRes` |
| **Two loading indicators for one wait.** The picker's Connect button already spins (`HardwareConnectButton(isBusy)`) and confirm-pairing has its inline spinner, but the modal `NcLoadingDialog` rendered on top of both | The modal is now gated on the destination: screens that report their own progress don't get it. In practice it is left for the create-signer wait on "Name your key" |

Checked and fine:

- **Taproot.** Address type is a free-user-only choice (membership fixes native segwit), so a
  user could in principle pick an address type the device can't do. `SelectWalletTypeScreen`
  defaults to `TrezorTaprootSupportState()` with `isLoaded = false`, which makes
  `isTaprootSupported()` false for every wallet type — taproot isn't offered. Same default
  Ledger uses.
- **Free group wallet.** Uses this same standalone path; the created signer reaches the group
  slot through `PushEvent.LocalUserSignerAdded`, which the view model pushes.
- **Second dispatcher.** `checkFirmwareDestination` in `SignerIntroActivity` only handles
  COLDCARD/JADE and is on-chain-only, so it needs no BitBox branch.
- `SignerIntroScreen.kt`'s remaining "Desktop only" is sample text inside `SignerItemPreview`,
  a preview of the generic disabled row — not BitBox-specific, left alone.

### Phase 3 — Sign flows ✅

All three dispatchers are separate `when`s over type/tag — each needs its own BITBOX branch.

| # | Task | File | State |
|---|---|---|---|
| 3.1 | `BitBoxSheet.kt` + `BitBoxSheetViewModel` (mirror `LedgerSheet.kt` / `LedgerSheetViewModel`) | `nunchuk-core/.../bitbox/` | ✅ health check + sign PSBT |
| 3.2 | Register-wallet-before-sign; `isWalletRegistered` gate (BitBox self-reports, no HMAC cache needed unlike Ledger) | `BitBoxTransactionSigner` | ✅ |
| 3.3 | **Normal tx**: `isLedger` sibling | `TransactionDetailsViewModel.kt`, `TransactionDetailComposeActivity.kt` | ✅ |
| 3.4 | **Dummy tx**: `isLedger` sibling → `GetDummyTransactionSignatureUseCase` | `WalletAuthenticationViewModel.kt`, `DummyTransactionDetailsFragment.kt` | ✅ |
| 3.5 | **Sign-in BSMS**: same dispatcher as 3.4 with `args.walletId` blank — must not key off a local wallet | `WalletAuthenticationViewModel` (`isSignInSignatureFlow`) | ✅ |
| 3.6 | Membership `CheckSignMessageFragment` sheet | `CheckSignMessageFragment.kt` | ✅ |
| 3.7 | Health check via `signMessage` + `HealthCheckSingleSigner` | `SignerInfoFragment` / `SignerInfoViewModel` | ✅ verified on device |
| 3.8 | Verify address on device via `getWalletAddress` | `UnusedAddressViewModel.kt` | ✅ |
| 3.9 | **Sign message** (the "..." menu beside health check) | `SignMessageViewModel` / `SignMessageFragment` | ✅ |

#### 3.7 Health check — what shipped

`BitBoxSheetViewModel` + `BitBoxSheet` in `nunchuk-core/.../bitbox/`, the BitBox counterpart of
the Ledger pair, wired to "Run health check" on Key Info through the same relay Ledger uses
(`SignerInfoViewModel.isBitBoxSigner()` → `BitBoxHealthCheckSheet` → `onHardwareHealthCheckResult`,
renamed from `onLedgerHealthCheckResult` now that two key types share it).

The device conversation follows Confluence §0 + "4. Sign message":

1. connect → `initialize()` — **every** BitBox session opens with it, and its result is the
   readiness gate (attestation, firmware, set-up), so nothing is asked of the device before it
2. pairing, if the device asks — the sheet shows the code *in place of* the picker, because that
   step is waiting on exactly those two buttons
3. `GetBitBoxSignMessagePath(signer)` → `signMessage(path, "Run health check")`
4. `HealthCheckSingleSigner(signer, message, signature)` → the verdict the host displays

Three things that are genuinely different from Ledger, not stylistic:

- **The signing path is not the signer's derivation path.** BitBox signs with the
  compact-signature key below it (`GetBitBoxSignMessagePath`), which is also where the displayed
  address comes from. New `GetBitBoxSignMessagePathUseCase` — deliberately with *no* fallback,
  unlike `GetTrezorSignMessagePathUseCase`: a guessed path signs with the wrong key and then
  fails verification with nothing to point at.
- **The readiness gate has nowhere to go in a sheet.** The add-key flow routes attestation
  failure and not-ready devices to full dead-end screens (05C / 05B). A sheet reports them
  (`nc_bitbox_attestation_error`, `nc_bitbox_not_ready_error`) and stays open to retry.
- **Only a completed verification reports a result.** `HealthCheckResult` dismisses the sheet and
  posts a verdict, so a device that never signed — declined pairing, wrong firmware, transport
  drop — emits `Error` instead. Otherwise "not set up" would be recorded as a *failed* health
  check.

Deduped rather than copied, since the sheet and the add-key flow now need the same things:

| Was | Now |
|---|---|
| `BitBoxActivity.interactionText()` — the Confluence §0 UI-text table | `BitBoxUserInteraction.statusText(context)` in `nunchuk-core/.../bitbox/` |
| `BitBoxConfirmPairingScreen`'s body (heading, code card, waiting line) | `BitBoxPairingCodeBody` in core; the screen keeps its Scaffold and the sheet supplies its own buttons |
| 17 `nc_bitbox_*` strings in `nunchuk-signer` | moved to `nunchuk-core` (`nonTransitiveRClass=false`, so signer's existing `R.string.*` references still resolve) |

#### 3.1–3.2, 3.4–3.6 Dummy-transaction signing — what shipped

Signing is a multi-step conversation, so the sheet gained the piece the health check didn't need:
**`BitBoxCommandExecutor`**, the counterpart of `LedgerControllerExecutor`. Confluence §3 is four
lines — ask whether the wallet is registered, register if not, sign, read the PSBT — and as a
chain of `onCommandComplete` branches that is a state machine with one branch per step. As a
suspend sequence it is the four lines the doc shows.

The whole sheet moved onto it, health check included, so there is one shape for every action
instead of two. That also avoids the `!executor.isAwaiting` special case the Ledger sheet carries,
which exists only because its health check predates its coordinator.

`BitBoxTransactionSigner` (`core/domain/utils/`) is the §3 coordinator, and it is genuinely
smaller than `LedgerTransactionSigner`: **BitBox self-reports registration**, so there is no
registration HMAC and therefore no `LedgerWalletRegistrar`, no per-wallet cache, and no
`cacheRegistration = false` special case for a wallet with no local storage. `isWalletRegistered`
is asked every time and is cheap; registering is the expensive part because it needs an approval
on the device.

It does keep two things from Ledger, both correctness rather than style:

- **the device check.** Signing targets a specific signer, so the fingerprint is read first and a
  mismatch raises `BitBoxWrongDeviceException` → "connect the correct device". Add-key can accept
  whatever is connected; signing cannot.
- **the policy-name fallback.** A wallet parsed from a BSMS has no name (the format doesn't carry
  one) and the name is shown on the device, so it registers as "Nunchuk".

Hosts: one event serves both in-app key types rather than two parallel ones.
`RequestSignLedger` becomes `RequestSignHardwareKey(tag, fingerprint, psbt, wallet)` and
`handleSignLedgerKey` becomes `handleHardwareSignedPsbt` — Ledger and BitBox take the same PSBT in
and hand the same signed PSBT back, so `tag` only decides which sheet the fragment puts up. That
keeps `DummyTransactionDetailsFragment` and `CheckSignMessageFragment` at one block each.

3.5 needs no separate work: the sign-in BSMS wallet already travels in the event's `wallet` field
(non-null only when there is no local wallet to look the policy up by id), and
`requestSignTransactionInApp` resolves it the same way for both key types.

Failure handling is where a sheet differs from the add-key flow, and it matters more here than for
the health check, because a hung sequence looks identical to a slow device:

- a transport drop, an `onError`, or a `REBOOT` fails the suspended command rather than leaving it
  waiting on a session that no longer exists — nothing resumes across a drop
- `close()` doesn't report a disconnect, so dismissing the sheet cancels the sequence explicitly
- `CancellationException` is rethrown rather than reported: `runCatching` catches it, and a
  cancelled sequence is the user closing the sheet, not a failure
- declining pairing or aborting on the device releases the button silently; only a real fault
  reports

#### 3.3 Signing a wallet transaction

The same §3 conversation as the dummy tx, plus the step a real transaction needs and a dummy one
doesn't: `BitBoxTransactionSigner.sign` reads the PSBT off the pending transaction, signs it, and
**imports the signed PSBT back into the wallet** — where a dummy tx hands its PSBT to the caller
to extract a signature from.

`TransactionDetailComposeActivity` keeps one fingerprint field per key type, because each sheet
drives its own transport and only one can be up at a time. Success is deliberately *not* reported
by the sheet: both sheets call `handleHardwareSignSuccess` (renamed from `handleSignLedgerSuccess`
now that two key types share it), which reloads the transaction and emits the same
`SignTransactionSuccess` every other signer emits — so the success message stays decided in one
place rather than once per key type.

#### Review pass (2026-08-27)

Swept every `SignerTag.LEDGER` dispatcher outside the Ledger packages for a missing BitBox
sibling — none found. Checked and confirmed fine:

- **Miniscript refresh after an in-app sign.** `handleHardwareSignSuccess` → `getTransactionInfo()`
  → `loadLocalTransaction()`, which does call `getSignedSigners()`, so miniscript signer state is
  refreshed. (`getTransactionFromNetwork()` doesn't, but that path is inheritance-claim only and
  returns early.)
- **Sign-in.** `enabledSigners` is restricted only for the health-check payload, so BitBox is
  tappable; the BSMS wallet reaches the sheet through the event's `wallet` field.
- **Replace key.** BitBox is correctly absent from the *off-chain inheritance* option sheet, which
  is TAPSIGNER/COLDCARD only by design.
- **On-chain.** BitBox is in `defaultSupportedSigners`, in `handleSignerTypeLogic`, and has its own
  icon in `SignerUtil`.

Two limitations found and left alone, both pre-existing rather than BitBox regressions:

- **Verify address can't be steered on a mixed wallet.** `isTrezorWallet()` / `isLedgerWallet()` /
  `isBitBoxWallet()` are wallet-level `any {}` checks read in that order, so a wallet holding both
  a Ledger and a BitBox always routes to Ledger with no way to pick. Label and dispatcher use the
  same order, so they at least agree. Trezor+Ledger had this already.
- **On-chain timelock claiming will fail on the device.** Current BitBox firmware can't spend after
  a time-based timelock (Huy, in Scope above). Adding and signing work; claiming hits a device
  error with no in-app explanation, and gating it would need a firmware-version rule we don't have.

#### Screen timeout during signing

`KeepScreenOn()` (`nunchuk-core/.../hardware/`) is held for the lifetime of the Ledger and BitBox
sheets. A device conversation is minutes of reading a pairing code and approving on the device,
with nothing to touch on the phone, so the display times out under it — which can disturb the BLE
link and, worse, drops the sign result.

The result is dropped because everything downstream of a signed PSBT reports through one-shot
events on a no-replay `MutableSharedFlow`, and every collector on that flow is gated at `STARTED`
(both dummy-tx fragments and `WalletAuthenticationActivity`). With the screen off there are no
subscribers, so `emit` returns immediately and the value is discarded; on resume there is nothing
to replay. The upload paths write state before emitting, which is why the screen comes back
half-updated rather than frozen — the signature count is right, but `SignDummyTxSuccess` /
`UploadSignatureSuccess` are gone and the key still reads as unsigned.

**Still open:** an outcome landing while the app is *backgrounded* is lost the same way. Fixing
that means making the shared event bus durable, and that bus also feeds TapSigner, Coldcard,
Portal, Trezor and software keys. Related, in shared code: `handleSignatureResult`'s
non-dummy-tx / non-sign-in branch puts the **final** signature only in `SignDummyTxSuccess` and
never in state, unlike the non-final branch — so dropping that event drops the signature itself,
not just the navigation.

#### 3.8 Verify address on device

Confluence §5: register the policy if the device doesn't have it, ask for the address at that
index, compare. `BitBoxAddressVerifier` mirrors `LedgerAddressVerifier`, and like it does **not**
check the device fingerprint first — any BitBox holding a key of this wallet derives the same
address, and one that doesn't can't register the wallet in the first place.

Two things worth knowing:

- **`checkOnDevice` is now passed explicitly.** The binding defaults it to `true`, which is
  correct, but it is the flag that makes the device *display* the address — the entire point of
  verifying one. Left implicit, a flipped default would silently turn verification into comparing
  a number the device computed without ever showing the user, and nothing would look wrong.
- **`BitBoxWalletRegistrar`** was extracted from `BitBoxTransactionSigner`, since signing (§3) and
  showing an address (§5) open with the same step. It also owns the BSMS policy-name fallback, so
  that rule lives in one place. Still far thinner than `LedgerWalletRegistrar`: no HMAC, no cache.

Ledger's private `LedgerVerifyAddressBox` became the shared `HardwareVerifyAddressBox`, with the
"check this on your <device>" label as a parameter.

#### Exception handling — the Confluence §6 table

Audited both hosts against §6. Three gaps, all fixed:

| Gap | Was | Now |
|---|---|---|
| **Lost Noise session** (`SESSION_LOST` / `DEVICE_NOISE_ENCRYPT` / `DEVICE_NOISE_DECRYPT`) | fell through to a generic error toast, so the user had to reconnect by hand after nothing visibly went wrong | initializes a fresh session and restarts the operation, as §6 says. Capped at one rebuild per connect so a device failing this way can't spin, and **only while the transport is still up** — §6 is explicit that a disconnect is not resumable, so `onDisconnected` marks it unrecoverable |
| **`deviceCode` was dropped** | `onCommandFailed` never carried it, so §6's `showOperationError(message, device_code)` couldn't be implemented | plumbed through the listener and `BitBoxCommandException`; the `DEVICE_INVALID_INPUT` / `DEVICE` / `DEVICE_INVALID_STATE` errors now report it, where the message alone rarely says which device state was wrong |
| **`UNSUPPORTED_FIRMWARE` was generic** | replaced by generic "not ready" copy, same as `DEVICE_UNINITIALIZED` | names the version and the direction to move in — see below |

`isUserCancellation()` and `isOperationError()` now sit next to `isSessionLost()` in
`BitBoxController.kt`, so both hosts read the same table rather than each spelling out the codes.

##### Firmware messages

Confluence §0 defines a *window*: below 9.0 is upgrade-only, 10.0 or newer needs an updated
integration. One `UNSUPPORTED_FIRMWARE` code covers both ends, and the two ends need **opposite
actions** — too old is fixed in BitBoxApp, too new is fixed by updating Nunchuk. Telling someone
to update firmware that is already ahead of us sends them the wrong way, and showing the
firmware's own wording (written for a developer reading a log) doesn't tell them either way.

So the version decides the copy, via `String.isBitBoxFirmwareTooNew()` next to the other §6
classifiers:

| Case | Message |
|---|---|
| `firmwareUpgradeRequired` (below 9.0) | names the version, "update it in BitBoxApp" |
| major ≥ 10 | names the version, "Nunchuk doesn't support it yet — update Nunchuk" |
| version unknown | covers both directions rather than guessing |
| not initialized | its own copy now — "hasn't been set up yet", which is what `DEVICE_UNINITIALIZED` means |

`UNSUPPORTED_FIRMWARE` carries no version of its own, so both hosts remember the one from the last
successful `initialize()`; when initialize itself is what failed there is nothing to go on, and
the fallback copy says so instead of guessing.

**This also closes the ≥ 10.0 gate** the plan listed as an open question. It is checked right after
initialize, alongside the below-9.0 case — without it a device ahead of the integration sailed
past the readiness gate and failed later on some unrelated-looking command. In the add-key flow it
deliberately does *not* route to the "continue in BitBoxApp" hand-off (screen 05B): that screen
tells the user to upgrade, which is exactly the wrong move for a device that is already ahead.

**Retry shape.** The rebuild is a loop inside the action's own job, not a re-entrant call to
`runAction` — re-entering would call `actionJob.cancel()` on the job the retry was running in.

#### 3.9 Sign message

Phase 2.9 put `BITBOX` in `isInAppHardwareTag`, which is what `canSignMessage` keys off, so the
option had been offered since then while `SignMessageFragment` only branched on Ledger and
Trezor — a BitBox fell through to `signMessageBySoftware()`, the software path, for a key with no
master signer. That is now a real flow.

The device half is the health check's: sign at `GetBitBoxSignMessagePath`, which is displayed as
the (locked) path — Confluence §4's `showSigningPath`. The **export** half is where BitBox differs
from every other signer, and it is why this was held back:

```cpp
const auto signing_address = GetBitBoxSignMessageAddress(signer);  // compact-signature P2PKH
ExportBitcoinSignedMessage(BitcoinSignedMessage{message, signing_address, signature});
```

The address is **not the signer's**. BitBox signs with the compact-signature key *below* it, so
the block has to name that key's address. `GetSignedMessageUseCase` derives the address from the
signer — right for Ledger, which signs at the signer's own path, and wrong here: it would produce
a block that verifies against nothing, and nothing about it would look broken.

There is no binding that takes an explicit address (`getSignedMessage` only takes a signer, and
`ExportBitcoinSignedMessage` isn't exposed), so `GetBitBoxSignedMessageUseCase` assembles the
RFC2440 block itself around `getBitBoxSignMessageAddress`. **The delimiters are the ones
libnunchuk emits**, read out of `libnunchuk-android.so` rather than guessed:
`-----BEGIN BITCOIN SIGNED MESSAGE-----`, `-----BEGIN BITCOIN SIGNATURE-----`,
`-----END BITCOIN SIGNATURE-----`. If an `ExportBitcoinSignedMessage` binding is ever added, that
use case should call it and drop the assembly.

Smaller pieces:

- **The path is locked**, like Ledger's — `isPathEditable` is now `!isLedger && !isBitBox`. They
  lock it for different reasons (Ledger uses the signer's own path, BitBox the one below it) but
  in both cases an edited path signs with a key the displayed address doesn't belong to.
- **No fallback path.** If the SDK can't resolve the signing path the screen leaves it blank and
  signing refuses, rather than falling back to the generic health-check path — that would sign
  with the wrong key and read as a device fault.
- **The device is checked first**, as in Ledger's message signer: a signature from another BitBox
  verifies against a different address, which would surface as a broken export rather than as
  "wrong device".
- The confirmed path travels with the message to the sheet instead of being re-read from state;
  the two match only because the field is locked, and nothing at the call site would show that if
  it stopped being true.

**Hardware keys no longer fall into the software path.** `onSignMessage` picked its branch from
`is<Vendor>Signer()`, all of which need `remoteSigner` — loaded asynchronously in `init`. Tap Sign
before that read lands and no branch matched, so a hardware key dropped through to
`signMessageBySoftware()`, which cannot work for a key with no master signer and fails in a way
that reads as a broken key. A `SignerType.HARDWARE` branch now catches it and says which of the
two cases it is: still loading, or genuinely no in-app flow. This was Ledger's bug too — one
branch fixes both.

### Phase 4 — Assisted / membership + replace ✅

Every dispatcher is its own `when`, so each got its own BITBOX branch. All four in-app
dispatchers now follow the same three-part shape as Ledger: an `addBitBoxLauncher` that takes
the returned signer (or falls back to the desktop hand-off on
`RESULT_ACTION_OPEN_DESKTOP_FLOW`), an `openBitBoxFlow()`, and a `SignerTag.BITBOX` branch in
`openInAppHardwareOrDesktopFlow`. The `TYPE_ADD_BITBOX` option sheet entries changed with them —
each one used to run the desktop hand-off as its "no existing key" lambda.

`:nunchuk-main:compileDebugKotlin` is clean.

| # | Task | File | State |
|---|---|---|---|
| 4.1 | Assisted personal | `AddKeyListFragment.kt` | ✅ |
| 4.2 | Assisted group (Byzantine) | `AddByzantineKeyListFragment.kt` | ✅ |
| 4.3 | Replace key | `ReplaceKeysFragment.kt` | ✅ |
| 4.4 | On-chain timelock add key (two accounts of the same device, `expectedXfp`) | `OnChainTimelockAddKeyListFragment.kt` | ✅ |
| 4.5 | On-chain replace keys | `OnChainReplaceKeysFragment.kt` | ⏹ left alone — see below |
| 4.6 | Inheritance-key seed-phrase backup verify (`verifyXfpOnly` mode, relay `EXTRA_VERIFIED_XFP`) | `SignerIntroActivity` → `BackUpSeedPhraseActivity` chain | ✅ shipped in phase 2 |
| 4.7 | `AddDesktopKeyFragment` BitBox copy | `AddDesktopKeyFragment.kt` | ⏹ dropped — the copy is still reachable |

Notes on the three rows that aren't a plain "add the branch":

- **4.3 replace key.** `ReplaceKeysFragment` has no desktop path — `showAddKeyByDesktopApp()` is
  an "not supported" info dialog. BitBox's intro screen still offers "Add via desktop app"
  whenever `isMembershipFlow` is set, so pressing it in the replace flow lands on that dialog.
  That is exactly what Ledger and Trezor do there today, so it was left matching rather than
  special-cased.
- **4.5 on-chain replace.** `OnChainReplaceKeysFragment` has no in-app hardware path *for any
  key* — Ledger and Trezor both go to the same "not supported" dialog. Wiring BitBox in would
  make it the only hardware key replaceable on an on-chain wallet, which is a product call, not
  a parity fix. Left as it was.
- **4.7 desktop copy.** The plan assumed an in-app flow retires the desktop screen. It doesn't:
  `isAddViaDesktopEnabled = isMembershipFlow && !isVerifyXfpOnly` means every assisted BitBox add
  still offers the desktop hand-off, which navigates to `AddDesktopKeyFragment`. Its
  `SignerTag.BITBOX` copy (`nc_main_add_bitbox_desc` + `bg_add_bitbox`) is live and has to stay —
  Ledger and Trezor keep theirs for the same reason.

**One shared constant.** The on-chain verify relay read `LedgerActivity.EXTRA_VERIFIED_XFP` to
pull a fingerprint out of an Intent that now comes from either activity — it worked only because
the two literals happened to match, and would have broken silently if either changed. The key
moved to `GlobalResultKey.EXTRA_VERIFIED_XFP`; both companions now alias it, so their public
contracts are unchanged and no call site moved.

### Phase 5 — Device setup ⏹ dropped (2026-08-21 design revision)

Cut from the design; an unprepared device now goes to BitBoxApp via screen 05B. The native
bindings for all of this **already exist** (phase 0.6/0.7), so picking it up later is UI work
only. Rows kept for that case.

| # | Task | Screen | State |
|---|---|---|---|
| 5.1 | Setup method — new words / restore words / restore microSD | 04 | ⬜ |
| 5.2 | New BitBox details — device name (default = picked device) + 12/24 words (12 needs fw ≥ 9.6) | 05 | ⬜ |
| 5.3 | Initial backup choice — recovery words (fw ≥ 9.13) or microSD | 06 | ⬜ |
| 5.4 | Restore from recovery words | 07 | ⬜ |
| 5.5 | Restore from microSD + `checkSdCard` / `insertSdCard` | 08 | ⬜ |
| 5.6 | Backup picker (`listBackups` → `restoreBackup`) | 09 | ⬜ |
| 5.7 | Device-action progress ("Keep this screen open") driven by `UserInteraction` | 10 | ⬜ |
| 5.8 | Setup complete → hand off to screen 12 | 11 | ⬜ |

### Phase 6 — Device management ⏹ dropped (2026-08-21 design revision)

Cut from the design along with the key-info menu. As with phase 5 the JNI is already in place
(`changePassword`, `setMnemonicPassphraseEnabled`, backups, `factoryReset`, firmware upgrade +
bootloader session) if this ever comes back.

| # | Task | Screen | State |
|---|---|---|---|
| 6.1 | Key Info "more" menu with the 7 BitBox actions | 15 | ⏸ |
| 6.2 | Connect-BitBox dialog reused by every device action | 17 | ⏸ |
| 6.3 | Set device name | 16 | ⏸ |
| 6.4 | Change device password / toggle passphrase / show recovery words | 15 | ⏸ |
| 6.5 | Manage backups — create / verify | 19 | ⏸ |
| 6.6 | Firmware upgrade — file picker, `InspectFirmware` product match, bootloader session, progress, `REBOOT` | 18 | ⏸ |
| 6.7 | Factory reset — gated on "I have a valid backup" | 20 | ⏸ |

---

## Decisions / open questions

- **On-chain replace (4.5).** `OnChainReplaceKeysFragment` has no desktop path at all
  today — BitBox there is a "not supported" dialog. Enabling it is a product call.
- **iOS parity.** Design ships 01b (no USB on iPhone); Android shows both. Nothing to
  do here beyond keeping the transport list data-driven.
- **Firmware ≥ 10.0** is explicitly out of scope per Confluence ("requires an updated
  integration"). **Now gated** — checked right after initialize alongside the < 9.0 case, and
  reported with copy that points at updating Nunchuk rather than the device. See "Firmware
  messages" under phase 3.
