# Signer intro

The caller passes `SignerIntroRequest` to `openSignerIntroScreen`. Choose the operation explicitly;
wallet IDs and a non-empty claim magic phrase are not operation selectors.

| Flow | Purpose |
| --- | --- |
| `AddKey` | Add a standalone key, including primary-key flows through `keyFlow` |
| `AddWalletKey` | Add a key while building/recovering a free, Miniscript or Liquid wallet |
| `AddAssistedWalletKey` | Ordinary assisted-wallet key; uses normal device setup |
| `ReplaceWalletKey` | Documents ordinary replacement intent; uses the same standard setup as `AddKey` |
| `OnChainTimelockKey` | Regular or inheritance slot of an assisted on-chain timelock wallet |
| `OffChainInheritanceKey` | Owner sets up or replaces an off-chain inheritance key |
| `ClaimInheritance` | Beneficiary claim, explicitly on-chain or off-chain |
| `VerifyBackup` | Re-read a restored device; never offer an existing key as verification |

The four standard flows (`AddKey`, `AddWalletKey`, `AddAssistedWalletKey`, `ReplaceWalletKey`)
currently share routing and picker behavior. Their names document caller intent; they do not
select different completion contracts.

`walletType`, `supportedSigners`, `keyFlow`, wallet/group IDs and `accountCount` remain separate
request data. In particular, Taproot restrictions arrive through `supportedSigners`; operation
alone does not determine which devices are eligible.

## Responsibilities

- `SignerIntroPickerPolicy` in `nunchuk-signer` owns the visible/enabled key types, server config
  filtering by wallet type and inheritance slot, ordering and fallback behavior.
- `SignerIntroCoordinator` owns selection and continuation decisions. `select` may offer existing
  keys; `createNew` resumes the same `KeyType` without offering them again.
- `SignerIntroViewModel` loads configs/keys, owns sheet state and emits buffered effects to one UI
  consumer. Initialization is idempotent; verification launch and pending sheet selection use saved state.
- `SignerIntroRoute` renders Compose navigation and sheets.
- `SignerDeviceLauncher` adapts resolved actions to existing device Intents and result contracts.
- `SignerIntroActivity` connects these components and returns results to its caller.

`SignerIntroRequest.deviceParams` is the compatibility adapter to downstream device screens that
still use `OnChainAddSignerParam`. Do not construct flags in signer-intro callers or infer an
operation from those flags in the coordinator.

## Adding a signer

1. Add its `KeyType` and display/type/tag mappings in `SignerDisplayInfo.kt`.
2. Update the appropriate catalog/fallback in `SignerIntroCatalog.kt`. Server-controlled setup
   still requires matching backend configs; adding a default card does not override those configs.
3. Route it in the coordinator. Reuse an air-gap action for ordinary air-gap devices. This screen
   never pairs a device: pairing lives in the device's own Activity (`LedgerActivity`,
   `BitBoxActivity`, `TrezorActivity`, …). The device launcher only builds that Activity's Intent
   and reads its result; shared claim/verify policy stays in the coordinator. In-app hardware
   devices are listed in `SignerHardwareDevice`; adding an entry requires exhaustive Intent and
   result adapters. Firmware gates use `SignerFirmwareDevice` rather than arbitrary signer tags.
4. Cover its supported operations in coordinator and picker-policy tests.

## Completion details to preserve

- Ledger/BitBox/Trezor claims return a `SingleSigner`, normalized to `SignerModel`; their desktop/USB
  fallback remains a device-specific result action.
- Hardware backup verification returns the original verification result, not an added key.
- When verification skips the picker, cancellation returns to the verification caller too.
- Air-gap replacement is completed by the device screen. Returning a signer again can replace twice.
- Jade's firmware/claim guide completes its own hand-off. Ordinary Jade setup may return a signer.
- Generic air-gap, Portal and hardware-tag selection keep their existing completion behavior.

Validation: `./gradlew :nunchuk-app:compileDevelopmentDebugKotlin :nunchuk-signer:testDebugUnitTest`.
Result routing from each device Activity, desktop hand-offs and recreation while a child Activity is
open also need manual QA.

## Claim desktop hand-off

This path does not return a signer from `AddDesktopKeyActivity`:

1. Ledger/BitBox return their desktop action; Trezor returns its USB action. The claim launcher
   opens `AddDesktopKeyActivity` with the selected tag, claim magic, `SETUP_INHERITANCE` and
   `isInheritanceKey = true`. It does not complete the signer-intro result at this point.
2. With non-empty magic, `AddDesktopKeyViewModel` creates an inheritance request, tagged with
   `INHERITANCE` and the device tag. The returned request ID is passed to the waiting screen.
3. The desktop completes that request externally. `AddDesktopKeyCompleted` wakes
   `WaitingDesktopKeyViewModel`, which checks the server using the current magic and request ID.
   The Continue button performs the same check if the push was missed; this is not periodic polling.
4. A completed request saves the key and shows success. Continue returns to the existing claim
   Activity via CLEAR_TOP + SINGLE_TOP, removing the intervening picker/desktop screens.
5. On claim resume, `checkRequestedAddDesktopKey` fetches completed desktop keys for the magic,
   then `addSigner` validates key origins and continues the on-chain/off-chain claim path.

`ClaimSignerAdded` is a separate in-app air-gap event; desktop completion uses the request/status
path above.

### Known gaps — deferred, not fixed

A fix was drafted and reverted; this code is unchanged from `HEAD`. Revisit these together:

1. `InheritanceRepositoryImpl.getAddedKeys` reads only `keys`, never the singular `key`, so an
   off-chain (MULTI_SIG) desktop key is saved by `saveKeyIfNeeded` but never returned to the claim.
2. The result map is keyed by `requestId`, so an on-chain request returning two accounts keeps only
   the last one.
3. `saveKeyIfNeeded` (Miniscript reads `keys`, others `key`) and `getAddedKeys` (always `keys`)
   read the payload differently; one shared reader should serve both.
4. A completed request without keys throws `RequestAddKeyCancelException` inside the
   `getAddedKeys` loop and hides the other requests' keys. `checkKeyAddedForInheritance` must keep
   throwing it — the waiting screen relies on it.
5. `checkRequestedAddDesktopKey` calls `addSigner` once per key; each call overwrites
   `uiState.event`, so e.g. an "Incorrect key" error can be hidden by a later key's event.
6. Off-chain constraint for any fix: `VerifyInheritanceMessageScreen` signs `signers.last()` and
   compares `signers.size` with `requiredKeyCount`, so off-chain must add one key at a time —
   appending several desktop keys before signing counts keys that were never signed.
7. `handledRequestIds` is in memory only; after process death, resume re-adds keys and shows
   `KeyAlreadyAdded`.

## Review findings (2026-09-28)

Parity with `HEAD` was checked flow by flow (AddKey/AddWalletKey/AddAssisted/ReplaceWalletKey,
OnChainTimelockKey, OffChainInheritanceKey, on-/off-chain ClaimInheritance, VerifyBackup) against
every key type: routing, result contracts and device Intent arguments match. Unit tests and
`:nunchuk-app:compileDevelopmentDebugKotlin` pass. Original findings are retained below;
see the resolution status after them.

### Original review — should fix

1. **Restored pending selection can auto-launch a device** — `SignerIntroViewModel.loadAllSigners`
   re-runs `offerExisting(pending)` after process death. If the filtered list is now empty (key
   deleted, filtered by `existingSigners`), `offerExisting` dispatches `createNew(...)` and opens the
   device screen without a user tap. On restore, an empty result must only clear `PENDING_KEY`; only
   a user selection may fall through to `createNew`.
2. **Runtime crashes where the compiler could check** —
   - `SignerIntroActivity.handleEvent`: `else -> error("UI action must be handled by SignerIntroRoute")`.
   - `SignerIntroCoordinator.firmwareChecked`: `else -> error(...)` (the old code was a no-op here).
   - `SignerDeviceLauncher.launch`: `HardwareDevice.entries.single { it.tag == action.tag }` — a new
     hardware `KeyType` routed by the coordinator compiles but crashes here.

   Split the event so every `when` is exhaustive without `else`, e.g.
   `SignerIntroEvent.OpenDevice(SignerDeviceAction)`, `CheckFirmware(tag)`, `ReturnHardwareTag(tag)`,
   `ReturnPlatformKey`. For `HardwareDevice`, map from `KeyType` with an exhaustive `when`, or add a
   test asserting every hardware `KeyType` has an entry.

### Original review — nits

3. **Coordinator built twice** — `SignerIntroActivity` (`by lazy`) and `SignerIntroViewModel.init`
   each create one. Stateless, so not a bug, but two sources for the same answers. Expose
   `verifyingKeyType` / `isInheritanceSetup` from the view model (or inject one coordinator).
4. **`AddKey`, `AddWalletKey`, `AddAssistedWalletKey`, `ReplaceWalletKey` behave identically** in the
   coordinator, picker policy and launcher. Keeping them to document caller intent is fine, but the
   Flow table above says `ReplaceWalletKey` means "the caller/device owns completion" as if code
   differed — it does not. Either correct the table or collapse them into one `Standard` flow until
   a difference exists.
5. **Silent defaults for new flows** — `shouldOfferExisting` and `SignerIntroPickerPolicy.isInheritanceSlot`
   use `else`, so a new `SignerIntroFlow` gets a default there instead of a compile error. List the
   cases explicitly, like `isManagedKeyFlow` and `hardware()` already do.

### Resolution status

1. Restoration now goes through `SignerIntroSelectionState.restore`, which only restores a sheet
   or clears the saved selection. It cannot emit navigation. The user-selection path alone can
   fall through to creating a key. Tests cover an empty filtered list, valid restored sheet,
   dismissal and an unknown saved key type.
2. `SignerIntroEvent` distinguishes firmware navigation from `SignerIntroHostEvent`; the Activity
   only accepts host events. Dispatch is exhaustive. Firmware continuation accepts
   `SignerFirmwareDevice`, while hardware launch accepts `SignerHardwareDevice` directly, with
   exhaustive adapters instead of a tag lookup. A test checks all hardware `KeyType` mappings.
3. Only the ViewModel creates the coordinator. The Activity and route read `verifyingKeyType` and
   `isInheritanceSetup` from it.
4. The flow table now states explicitly that standard flows currently share behavior.
5. Reuse policy and inheritance-slot policy enumerate every flow instead of defaulting new flows.

Post-review validation: `:nunchuk-app:compileDevelopmentDebugKotlin` succeeded and all 31 signer
unit tests passed (including 5 added for the review fixes).

Follow-up: `CheckFirmwareDestination.signerTagName` renamed to `deviceName`, since it now carries a
`SignerFirmwareDevice` name rather than a `SignerTag` name.

### Manual QA still needed

This screen does not pair devices, so BLE/USB pairing itself is out of scope — it lives in the
device Activities, which this refactor does not touch. What changed here is how those Activities
are launched and how their results come back, so QA that boundary:

- For each flow, pick each device and check the right screen opens and its result reaches the
  caller (signer, hardware tag, platform key, or verification result), or that this screen
  finishes when the device screen owns completion.
- Claim with Ledger/BitBox/Trezor: a returned key goes up as a signer; choosing the desktop/USB row
  on the device intro opens the add-desktop-key screen for that device.
- Recreate this Activity (rotate / "Don't keep activities") while a device Activity is open, then
  finish it: the result must land on the matching launcher — especially the three claim launchers
  — and a verification flow must not relaunch its device.
