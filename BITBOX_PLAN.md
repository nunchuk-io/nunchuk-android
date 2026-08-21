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
`SignerIntroActivity` to a created signer. Phase 3 (sign flows) is next.

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

### Phase 3 — Sign flows ⬜ ← next

All three dispatchers are separate `when`s over type/tag — each needs its own BITBOX branch.

| # | Task | File | State |
|---|---|---|---|
| 3.1 | `BitBoxSheet.kt` + `BitBoxSheetViewModel` (mirror `LedgerSheet.kt` / `LedgerSheetViewModel`) | `nunchuk-core/.../bitbox/` | ⬜ |
| 3.2 | Register-wallet-before-sign; `isWalletRegistered` gate (BitBox self-reports, no HMAC cache needed unlike Ledger) | — | ⬜ |
| 3.3 | **Normal tx**: `isLedger` sibling | `TransactionDetailsViewModel.kt:785`, `TransactionDetailComposeActivity.kt:294` | ⬜ |
| 3.4 | **Dummy tx**: `isLedger` sibling → `GetDummyTransactionSignatureUseCase` | `WalletAuthenticationViewModel.kt:549`, `DummyTransactionDetailsFragment.kt:203/211` | ⬜ |
| 3.5 | **Sign-in BSMS**: same dispatcher as 3.4 with `args.walletId` blank — must not key off a local wallet | `WalletAuthenticationViewModel` (`isSignInSignatureFlow`) | ⬜ |
| 3.6 | Membership `CheckSignMessageFragment` sheet | `CheckSignMessageFragment.kt:114` | ⬜ |
| 3.7 | Health check via `signMessage` + `HealthCheckSingleSigner` | `SignerInfoViewModel.kt:353`, `SignMessageUiState.kt:35` | ⬜ |
| 3.8 | Verify address on device via `getWalletAddress` | `UnusedAddressViewModel.kt:115` | ⬜ |

### Phase 4 — Assisted / membership + replace ⬜

Mirror the Ledger branch at every dispatcher; each is independent, a miss silently
falls through to "use the desktop app".

| # | Task | File | State |
|---|---|---|---|
| 4.1 | Assisted personal | `AddKeyListFragment.kt:312/337` | ⬜ |
| 4.2 | Assisted group (Byzantine) | `AddByzantineKeyListFragment.kt:287/364` | ⬜ |
| 4.3 | Replace key | `ReplaceKeysFragment.kt:373/396` | ⬜ |
| 4.4 | On-chain timelock add key (two accounts of the same device, `expectedXfp`) | `OnChainTimelockAddKeyListFragment.kt:392/616` | ⬜ |
| 4.5 | On-chain replace keys (no desktop path — decide: enable BitBox or keep the "not supported" dialog) | `OnChainReplaceKeysFragment.kt:562` | ⬜ |
| 4.6 | Inheritance-key seed-phrase backup verify (`verifyXfpOnly` mode, relay `EXTRA_VERIFIED_XFP`) | `BackUpSeedPhraseActivity` chain | ⬜ |
| 4.7 | Remove BitBox from `AddDesktopKeyFragment` copy paths that now have an in-app flow | `AddDesktopKeyFragment.kt:85/93` | ⬜ |

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
  integration") — gate it like the < 9.0 case.
