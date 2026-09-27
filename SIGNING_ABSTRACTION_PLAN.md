# Per-device signing abstraction — plan (for review)

**Status:** proposal, not started. No code written yet.
**Review status (2026-09-25):** direction supported. Sections 4–9 were revised to take in every
section 10 finding that both the author and the reviewer agree on; section 10 stays as the review
record. Section 11 lists author additions the reviewer has **not** yet seen — they are not part of
the agreed plan until approved.
**Scope of first delivery:** the off-chain inheritance claim flow — challenge-message signing
(`VerifyInheritanceMessageScreen`) and the claiming PSBT (`ClaimTransactionActivity`).
**Base:** branch `2.8.6` at `6073135dc` ("Claim an off-chain plan with a Keystone or SeedSigner").

Line numbers below are as of `6073135dc`.

---

## 1. Why

Every new key type that can claim an inheritance is currently added by editing a chain of `when`
tables spread across screens, view models and sheets. Adding Keystone + SeedSigner (`6073135dc`)
touched 6 files; Krux (`298a51a24`) touched 15; Passport (`245369eb7`) 19. The devices differ in
data (which transport, which request format, which file name, which copy), not in behaviour, yet
each difference is encoded as another branch.

## 2. Current state (evidence)

All paths below are under
`nunchuk-main/src/main/java/com/nunchuk/android/main/components/tabs/services/inheritanceplanning/claim/`
unless stated.

### 2.1 Where a device is decided, for message signing alone

| Location | What it decides |
|---|---|
| `verifymessage/VerifyInheritanceMessageScreen.kt:154` `fileSigningCallbacks` | save/share/pick-file lambdas |
| `verifymessage/VerifyInheritanceMessageScreen.kt:187` `airgapQrSigningCallbacks` | plain-text QR out, signature QR in; device name on the export screen |
| `verifymessage/VerifyInheritanceMessageScreen.kt:217` `signingCallbacks = when {…}` | which callback set per device; `else` = Coldcard (QR/BBQR + NFC + file) |
| `verifymessage/VerifyInheritanceMessageScreen.kt:381` `onSignClick` | TAPSIGNER NFC / software passphrase / export-import sheet / Ledger / BitBox / Trezor; `else -> Unit` |
| `verifymessage/VerifyInheritanceMessageScreen.kt:412` `ColdCardSigningBottomSheets(isQrOnly, isFileOnly, supportsNfc)` | which pickers the sheet skips |
| `verifymessage/VerifyInheritanceMessageViewModel.kt:200` `airgapSignMessageRequest` | Specter QR request |
| `verifymessage/VerifyInheritanceMessageViewModel.kt:317` `buildMessageFile` | Passport / Krux / `else` = Coldcard file format |
| `verifymessage/VerifyInheritanceMessageViewModel.kt:342` `messageFileName` | `passport_message.txt` / `krux_message.txt` / `else` = `coldcard_message.txt` |
| `exportcomplete/ExportCompleteScreen.kt:146` `importSignatureRoutes` | Krux = QR+file, `else` = file |
| `exportcomplete/ExportCompleteScreen.kt:157` `exportCompletedInstructionsRes` | on-device steps copy per tag, `else` = Coldcard |
| `nunchuk-core/.../core/util/SignerUtil.kt:138,162` `isQrOnlyAirgap`, `signsByExportImport` | device families |

The claiming PSBT repeats the pattern: `ClaimTransactionActivity.kt:270` (`onSignClick` `when`) and
three separate `ColdCardSigningBottomSheets` instances at `:351` (QR-only), `:410` (SD-card), `:437`
(Coldcard), with their own callback sets (`:380` `psbtFileCallbacks`).

### 2.2 Concrete problems

1. **Silent fall-through.** `buildMessageFile`, `messageFileName`, `signingCallbacks`,
   `importSignatureRoutes`, `exportCompletedInstructionsRes` all end in `else` = Coldcard. A new
   device that misses one of them compiles and produces a Coldcard-formatted request.
2. **Invalid flag combinations.** `isQrOnly = true, isFileOnly = true` compiles. The three booleans
   really encode "which transports does this device export/import over".
3. **Same payload, different handling per transport.** Fixed in `6073135dc`: a PSBT scanned back
   over QR was decoded without the wallet while the same PSBT from a file was decoded against the
   claim's signers (`ClaimTransactionViewModel.decodeSignedPsbt`), so SeedSigner/Krux PSBTs came
   back unsigned over QR only. Nothing structural prevents the next such divergence.
4. **The view model holds every device.** `VerifyInheritanceMessageViewModel` injects ~20
   dependencies (Passport, Krux, Coldcard, BitBox, Trezor, Mk4 NFC, …) for a screen that signs with
   exactly one signer.

## 3. Key observation

12 key types collapse into **5 signing methods**:

| Method | Devices | Shape |
|---|---|---|
| Direct | Software, TAPSIGNER | a use case, no leaving the screen (TAPSIGNER needs an NFC tap) |
| ExportImport | Coldcard, Jade, Keystone, SeedSigner, Passport, Krux | request out, signature/PSBT back, over QR / file / NFC |
| InApp | Ledger, BitBox | BLE/USB sheet |
| ExternalApp | Trezor | Trezor Suite deeplink + `TrezorCallbackHolder` |
| Unsupported | desktop-only hardware, server key | message only |

ExportImport is half the devices and is where new keys keep arriving; within it devices differ only
in data. So the unit of abstraction is **a per-device data profile + one UI host per method**, not
a per-device class with `sign()`.

### Why not `interface KeySigner { suspend fun sign(): Signature }`

Signing on Android is UI-bound: `ActivityResultLauncher`s must be registered during composition,
NFC arrives through the activity-scoped `NfcViewModel`, Ledger/BitBox are composable sheets, Trezor
replies through a deeplink. A `suspend sign()` cannot own those, so each implementation would take
the activity/launchers and re-implement QR/file/NFC plumbing — N devices × M hosts again.


## 4. Design (revised per section 10)

The profile describes **device capability per operation**; which routes a given screen offers is a
separate, **context-specific** selection (10.1). Payload format and transport framing are separate
concerns (10.2). One owner processes results; the composable only does UI (10.3). Signing runs as an
explicit session (10.4).

### 4.1 Classifier — `SigningDevice` (new, `nunchuk-core`)

`KeyType` is not the key: it lives in `nunchuk-signer` (`SignerIntroScreen.kt:297`), which
`nunchuk-core` does not depend on, and `SignerModel.toReAddKeyType()` (`SignerDisplayInfo.kt:58`)
returns `null` for software/server keys and unknown hardware tags. `KeyType` stays the picker's enum
(agreed, 10.5).

```kotlin
enum class SigningDevice {
    SOFTWARE, FOREIGN_SOFTWARE, TAPSIGNER, PORTAL, COLDCARD, JADE, KEYSTONE, SEEDSIGNER, PASSPORT,
    KRUX, GENERIC_AIRGAP, LEDGER, BITBOX, TREZOR, OTHER_HARDWARE, SERVER, PLATFORM, UNKNOWN,
}

/** Total: every SignerModel maps to exactly one value. The one place type+tag is read for claim signing. */
fun SignerModel.signingDevice(): SigningDevice
```

**Precedence — documented as it is today, not as a single rule** (10.5; the earlier wording
"Coldcard first" was wrong):

| Dispatcher | Order of checks |
|---|---|
| Message Sign click (`VerifyInheritanceMessageScreen.kt:381`) | `type == NFC` → `type == SOFTWARE` → `signsByExportImport` → Ledger → BitBox → Trezor → `else -> Unit` |
| Message callback set (`VerifyInheritanceMessageScreen.kt:217`) | `isQrOnlyAirgap` → Passport → Krux → `else` = Coldcard |
| PSBT Sign click (`ClaimTransactionActivity.kt:270`) | `type == NFC` → `type == SOFTWARE` → `COLDCARD_NFC \|\| tags.contains(COLDCARD)` → `isQrOnlyAirgap` → SD-card (Passport/Krux) → Ledger → BitBox → Trezor (no `else`) |

These do not agree for every input (a signer carrying more than one device tag). One classifier
cannot claim to preserve all of them silently: step 1 pins what each dispatcher does today,
including conflicting/multiple tags, and the intended resolution is decided explicitly, as its own
change.

**Limit of exhaustiveness** (10.5): a `when` over `SigningDevice` only forces a profile for a *new
enum value*. A new upstream `SignerTag` that the classifier does not map still lands in a catch-all
category. The classifier therefore gets table tests over supported tags *and* unsupported types; enum
exhaustiveness is not treated as complete protection.

**Fixed decisions:**
- **Signer type first; the `COLDCARD` tag counts only on `AIRGAP` and `HARDWARE`** (owner decision,
  2026-09-25). The classifier switches on `SignerType` and reads tags only inside the `AIRGAP` and
  `HARDWARE` branches:

  ```kotlin
  fun SignerModel.signingDevice(): SigningDevice = when (type) {
      SignerType.NFC -> TAPSIGNER
      SignerType.COLDCARD_NFC -> COLDCARD
      SignerType.PORTAL_NFC -> PORTAL
      SignerType.SOFTWARE -> SOFTWARE
      SignerType.FOREIGN_SOFTWARE -> FOREIGN_SOFTWARE
      SignerType.SERVER -> SERVER
      SignerType.AIRGAP -> airgapDevice(tags)       // COLDCARD tag considered here
      SignerType.HARDWARE -> hardwareDevice(tags)   // COLDCARD tag considered here
      SignerType.PLATFORM -> PLATFORM
      SignerType.UNKNOWN -> UNKNOWN
      // exhaustive over nativesdk 1.2.23 SignerType, no else
  }
  ```

  *Intentional behaviour change, tracked separately (4.6):* today `isColdCard`
  (`SignerUtil.kt:43`, `type == COLDCARD_NFC || tags.contains(COLDCARD)`) treats a `COLDCARD` tag on
  **any** type as a Coldcard. `NFC` and `SOFTWARE` are checked before it in every claim dispatcher, so
  they do not change; `PORTAL_NFC`, `FOREIGN_SOFTWARE` and `SERVER` carrying the tag move from the
  Coldcard sheet (via `signsByExportImport` on the message screen, `ClaimTransactionActivity.kt:283`
  on the PSBT screen) to `Unsupported`. The claim picker offers none of those three types, so no
  reachable claim path is expected to change. The shared `isColdCard` helper, used outside the claim
  (`CreateWalletViewModel`, `GroupDashboardViewModel`, …), is not changed by this plan.
- `HARDWARE` + `COLDCARD` keeps today's claim behaviour (signs as a Coldcard). Changing it in other
  signing flows is a separate decision (10.5).
- `FOREIGN_SOFTWARE` cannot sign in the app (owner decision, 2026-09-25). It is `Unsupported` for both
  operations — explicitly, not via fall-through. Today it already reaches `else -> Unit` on the
  message screen because the check is `type == SOFTWARE`.

### 4.2 Profile: capability per operation (`nunchuk-core`, pure)

```kotlin
enum class Transport { FILE, QR, NFC }            // transport identity only (10.2)
enum class SigningOperation { MESSAGE, PSBT }

sealed interface SigningMethod {
    data object Software : SigningMethod
    data object TapSigner : SigningMethod
    data class ExportImport(
        val export: List<ExportRoute>,
        val import: List<ImportRoute>,
    ) : SigningMethod
    data class InApp(val device: InAppDevice) : SigningMethod   // LEDGER, BITBOX
    data object TrezorSuite : SigningMethod
    /** Explicit per operation, with an explicit UI outcome (see 4.2.4). */
    data object Unsupported : SigningMethod
}

data class SignerProfile(
    @StringRes val displayNameRes: Int,
    val message: SigningMethod,
    val psbt: SigningMethod,
    @StringRes val exportInstructionsRes: Int?,   // "Export completed" screen copy
)

/** Exhaustive, no `else`. */
fun SigningDevice.profile(): SignerProfile = when (this) { … }
```

The profile holds **kinds and configuration only** — no codec instances, no use cases (10.2).

#### 4.2.1 Routes: payload codec + transport configuration (10.2)

```kotlin
sealed interface ExportRoute {
    val transport: Transport
    data class File(val codec: PayloadCodecKind, val fileName: String) : ExportRoute   // FILE
    data class Qr(val codec: PayloadCodecKind, val framing: QrFraming) : ExportRoute   // QR
    data class Nfc(val codec: PayloadCodecKind) : ExportRoute                          // NFC
}

enum class PayloadCodecKind { SPECTER_MESSAGE, COLDCARD_JSON, PASSPORT_TEXT, KRUX_TEXT, RAW_PSBT }
enum class QrFraming { PLAIN, BBQR_JSON, UR, BBQR }
```

File names are FILE route metadata; QR framing is QR route configuration; codecs build the payload.
Every configured route must resolve to a codec; an unsupported combination fails explicitly, not by
default.

Message routes, per device:

| Device / route | Payload codec | Route configuration |
|---|---|---|
| Coldcard / QR | `COLDCARD_JSON` | `BBQR_JSON` |
| Coldcard / FILE | `COLDCARD_JSON` | `coldcard_message.txt` |
| Coldcard / NFC | `COLDCARD_JSON` | Mk4 NDEF |
| Jade, Keystone, SeedSigner / QR | `SPECTER_MESSAGE` | `PLAIN` |
| Krux / QR | `SPECTER_MESSAGE` | `PLAIN` |
| Krux / FILE | `KRUX_TEXT` | `krux_message.txt` |
| Passport / FILE | `PASSPORT_TEXT` | `passport_message.txt` |

`COLDCARD_JSON` keeps today's `addressType = AddressType.LEGACY` argument
(`VerifyInheritanceMessageViewModel.kt:317`).

PSBT routes: payload is the PSBT itself (`RAW_PSBT`); QR framing is `BBQR` for Coldcard and `UR` for
every other device.

#### 4.2.2 Navigation adapter (no rewrite of the shared screens)

A small adapter maps route configuration onto the existing `ExportTransactionActivity` /
`ImportTransactionActivity` API (`SignFlowType` + `isBBQR`, chosen today in
`ExportTransactionViewModel.handleExportTransactionQRs`):

| Operation / framing | Export | Import |
|---|---|---|
| Message / `PLAIN` | `ClaimAirgapMessage` | `ClaimAirgapMessage` |
| Message / `BBQR_JSON` | `ClaimDummy` | `ClaimDummy` |
| PSBT / `UR` | `NormalDummy`, `isBBQR = false` | `NormalDummy` |
| PSBT / `BBQR` | `NormalDummy`, `isBBQR = true` | `NormalDummy` |

#### 4.2.3 Route selection is per UI context (10.1)

The same device offers different lists in different places, so route selection takes a context:

```kotlin
enum class RouteContext { SIGNING_SHEET, AFTER_FILE_EXPORT }
fun SignerProfile.routes(operation: SigningOperation, context: RouteContext): Routes
```

Current behaviour to preserve, **in this order** (verified against `ColdCardSigningBottomSheets.kt`,
`ExportCompleteScreen.kt:146` and `ClaimTransactionActivity.kt:351,410,437`):

| Context | Export routes | Import routes |
|---|---|---|
| Coldcard — message / PSBT signing sheet | FILE, QR, NFC | FILE, QR, NFC |
| Krux — message signing sheet | FILE, QR | FILE, QR |
| Passport / Krux — PSBT signing sheet | FILE, QR | FILE, QR |
| Passport — message signing sheet | FILE | FILE |
| Jade / Keystone / SeedSigner — signing sheet (message and PSBT) | QR | QR |
| Coldcard / Passport — message "Export completed" | — | FILE |
| Krux — message "Export completed" | — | QR, FILE |

Rules, all preserved:
- A single route skips the transport picker.
- A single FILE export **still opens the Save/Share sheet**; only the picker is skipped.
- "Export completed" is reached only after a file export (save or share).

Validation: every route list is non-empty and duplicate-free, and every displayed route has a real
handler — no default no-op callback (today `ColdCardSigningCallbacks` defaults every lambda to `{}`).

#### 4.2.4 Unsupported, per operation (10.5)

Portal, generic air-gap, server, platform, `FOREIGN_SOFTWARE`, `UNKNOWN` and other hardware are marked
`Unsupported` explicitly for each operation, after checking the current routes. NFC or QR capability
alone does not imply claim-signing support.

`Unsupported` gets an explicit UI outcome instead of the accidental `else -> Unit`. For the
behaviour-preserving extraction that outcome is **today's** (nothing happens); showing a message is a
separate, explicitly agreed behaviour change.

### 4.3 Payload codecs (`nunchuk-core`, over existing use cases — no new native calls)

```kotlin
interface PayloadCodec {
    suspend fun build(path: String, payload: String): Result<String>
}
/** Injected; resolves a PayloadCodecKind to its codec. Keeps profile() pure. */
interface PayloadCodecFactory { fun get(kind: PayloadCodecKind): PayloadCodec }
```

| Kind | Use case |
|---|---|
| `SPECTER_MESSAGE` | `GenerateMessageSigningQrUseCase` |
| `COLDCARD_JSON` | `GenerateColdCardHealthCheckMessageStringUseCase` (`AddressType.LEGACY`) |
| `PASSPORT_TEXT` | `GeneratePassportMessageSigningUseCase` |
| `KRUX_TEXT` | `GenerateKruxMessageSigningUseCase` |
| `RAW_PSBT` | identity |

Codecs build a payload; they do not know the transport.

### 4.4 Ownership: one coordinator, a thin composable host (10.3)

**Signing request / session** carries everything a method needs:

```kotlin
data class SigningRequest(
    val signer: SignerIdentity,          // fingerprint, master signer id, derivation path
    val operation: OperationContext,
)
sealed interface OperationContext {
    data class Message(val challengeId: String, val text: String) : OperationContext
    data class Psbt(
        val transaction: Transaction,    // keeps amount / fee / fee rate / subtractFee for the decoder
        val claimSigners: List<SingleSigner>,
        val registrationBsms: String?,   // Ledger / BitBox / Trezor
    ) : OperationContext
}
```

**One owner** — a coordinator on the ViewModel side — owns use-case calls, pending state, errors and
result normalisation. The **composable host** only:
- registers the QR / file / NFC launchers (unconditionally, at composition);
- renders sheets from the selected routes;
- performs UI effects (start NFC, share intent, open Trezor Suite);
- forwards raw transport results and events to the coordinator.

The host does **not** decode. Business decisions stay in the claim layer: updating claim state,
`checkAndClaimIfAllSigned`, claiming and broadcasting.

**Required inputs per method:**

| Method | Needs |
|---|---|
| Software | signer id + path; passphrase prompt when `needPassPhraseSent` |
| TAPSIGNER | signer + path; NFC `IsoDep` + CVC |
| ExportImport | signer path; codec per route; for PSBT the full `Psbt` context |
| Ledger | fingerprint + path (message); registration wallet (PSBT) |
| BitBox | fingerprint + **resolved sign-message path** (`GetBitBoxSignMessagePathUseCase`) for messages; registration wallet (PSBT) |
| Trezor | `SingleSigner` + message (message); registration wallet (PSBT) |

**Result processing — extract per transport, then converge:**

| Source | Extraction | Then |
|---|---|---|
| Message / FILE | `ExtractMessageSignatureUseCase(text)` | common message-acceptance path |
| Message / QR | none — `ImportTransactionActivity` already returns a bare signature (`GlobalResultKey.SIGNATURE`). **Do not extract it again.** | same |
| Message / NFC | `ExtractColdcardSignatureFromRecordsUseCase(records)` | same |
| Message / Ledger, BitBox sheet | none (bare signature) | same |
| Message / Trezor | `ParseTrezorSignMessageResponseUseCase` | same |
| Message / TAPSIGNER, Software | use case returns `SignedMessage` | same |
| PSBT / QR, FILE, NFC, Ledger, BitBox, Trezor | — | **one decoder** with the full claim transaction context (today `ClaimTransactionViewModel.decodeSignedPsbt`) |

The PSBT decoder must be the same for every source; verifying that holds today across QR / FILE /
NFC / hardware is a step 1 item.

### 4.5 Session lifecycle and callback ownership (10.4)

Specify, before step 4:
- request identity tied to signer + challenge (message) or transaction (PSBT);
- states: pending, awaiting launch confirmation, launched, completed, cancelled, failed;
- **cancel before launch** distinct from **dismiss after launch**;
- retry, repeated Sign, late and duplicate callback policy;
- which correlation fields the external protocol really provides — a local request id alone cannot
  correlate a callback that does not carry it;
- remembered sheet state and result handlers scoped to, and reset with, the active session;
- behaviour on recomposition (a Sign trigger is consumed once, never relaunched) and on Activity
  recreation;
- inactive sessions must not consume NFC or Trezor results that belong to the active one.

**Existing limitation (not a regression of this proposal):** `requestSignMessageByTrezor()` sets
`awaitingTrezorSignature = true` as soon as the deeplink is built
(`VerifyInheritanceMessageViewModel.kt:253`), before the user confirms. `dismissTrezorSuiteDialog()`
(`:263`) only clears the deeplink, and it runs both on Cancel and right after the link is opened
(`VerifyInheritanceMessageScreen.kt:442` onward), so it cannot simply clear the flag. After a
cancelled request the screen keeps waiting. The screen's view model stays in the back stack while the
next key is signed, so in a two-Trezor claim it can take the second key's reply. Any fix is tracked
**separately** from the behaviour-preserving extraction.

### 4.6 Invariants

- Behaviour-preserving at every step: same options, same order **per context**, same file names,
  same copy, same Save/Share step.
- Any intentional behaviour change (Trezor fix, Unsupported message, tag-conflict resolution) is a
  separate, explicitly agreed change.
- No `else` in any table keyed by `SigningDevice`.
- `ExportTransactionActivity` / `ImportTransactionActivity` / `SignFlowType` are called through the
  adapter, not rewritten.
- On-chain timelock flows untouched (same scope guard as `plan.md`).
- Payoff first is fewer duplicated device decisions and one result-processing path — not moving all
  signing code into one large host.

## 5. Cost of the next key, after refactor

| New key | Today | After |
|---|---|---|
| Air-gapped, existing codec | ~6 files | enum value + classifier branch + profile row + string |
| Air-gapped, new request format | ~7 files | + 1 codec kind |
| New transport family | ~6 files | + 1 `SigningMethod` + 1 host branch |

## 6. Steps (revised order, per 10.6)

1. **Pin current behaviour.** Characterisation tests for: route lists and order per context (4.2.3),
   picker skipping and the FILE → Save/Share step, classifier precedence of all three dispatchers
   including conflicting/multiple tags (4.1), QR navigation mappings (4.2.2), file names and codec
   arguments. Any intended change found here is resolved separately.
   *Done when:* the matrix is committed as tests and passes on the current code.
2. **Classifier + operation/route profiles**, with explicit `Unsupported` per operation.
   *Done when:* the step 1 tests pass against the profile; route lists validated non-empty,
   duplicate-free, every route with a handler.
3. **Payload codecs + navigation adapter.** *Done when:* Krux QR and FILE tested independently;
   Coldcard JSON `LEGACY` argument, file names, and QR mappings for messages and PSBTs tested; an
   unconfigured route fails explicitly.
4. **Message host + coordinator + session lifecycle** (4.4, 4.5). *Done when:* ownership and inputs
   documented for every method; Ledger/BitBox registration, BitBox message path, software
   passphrase, NFC errors verified; lifecycle tests in 7.1 pass.
5. **PSBT reuse** of the proven pieces, keeping registration wallet and full decode context.
   *Done when:* PSBT decoding is one path for QR / FILE / NFC / hardware.
6. **Deferred (separate decision):** `WalletAuthenticationViewModel.onSignerSelect` (`:399`, dummy tx
   incl. sign-in) and `TransactionDetailComposeActivity` (`onSignClick`, `nunchuk-transaction`).

## 7. Testing

### 7.1 Automated
- Classifier: supported tags, unsupported types, conflicting/multiple tags.
- Routes: ordered assertions per context, picker skipping, FILE → Save/Share, validation.
- Codecs: meaningful payload fixtures where feasible — not only "which use case was called".
- Navigation adapter: every framing for messages and PSBTs.
- Failure and cancellation paths; callback ownership: cancel → retry, repeated Sign, switching keys
  in a two-key claim, late and duplicate callbacks, recomposition, Activity recreation, inactive
  session cannot consume NFC/Trezor results.

### 7.2 Manual — release check
For TAPSIGNER, Software (with/without passphrase), Coldcard (NFC, QR, file), Jade, Keystone,
SeedSigner, Passport, Krux (QR and file), Ledger, BitBox, Trezor: claim an off-chain plan end to end
(challenge signed, Continue, claiming PSBT signed, broadcast) and compare every sheet with the current
build. Static review does not establish hardware compatibility; this matrix does.

## 8. Risks

- **Over-abstraction.** Ledger/BitBox/Trezor stay their own methods; the payoff is mainly in
  ExportImport. If the early steps do not visibly remove duplicated decisions, stop there.
- **Host growth.** Keep the composable thin (4.4); logic belongs to the coordinator.
- **Launcher lifetime.** Launchers registered unconditionally at composition, never inside a `when`.
- **Two-key claims.** The screen is shown once per inheritance key (`claimData.signers.last()`);
  per-session state and callback ownership are required (4.5). The current Trezor guard does not
  fully provide this.
- **Classifier drift.** Picker and add-key flows still read type+tag directly; until step 6,
  `signingDevice()` governs claim signing only.
- **Exhaustiveness is partial.** Unmapped new tags are caught by classifier tests, not the compiler.

## 9. Decisions (agreed)

1. **Classifier:** separate `SigningDevice` enum; `KeyType` stays the picker's (10.5).
2. **QR encoding:** QR route configuration (`QrFraming`), mapped by an adapter to the existing
   `SignFlowType` / `isBBQR`; codecs build payloads only (10.2).
3. **Host location:** claim package until message and PSBT reuse is demonstrated; pure profile and
   codec definitions in `nunchuk-core` where dependency direction permits (10.5).
4. **Portal and other categories:** explicitly `Unsupported` per operation after checking current
   routes (10.5).
5. **Coldcard over USB:** keep today's claim behaviour (10.5).
6. **`FOREIGN_SOFTWARE`:** cannot sign in the app — explicitly `Unsupported` (owner, 2026-09-25).
7. **Behaviour fixes** are tracked separately from the extraction (10.4).
8. **Classifier precedence:** signer type first; the `COLDCARD` tag is read only for `AIRGAP` and
   `HARDWARE` (owner, 2026-09-25; see 4.1).
9. **One device tag per key:** an `AIRGAP` key never carries `COLDCARD` together with another device
   tag (`KEYSTONE`, `JADE`, `SEEDSIGNER`, `PASSPORT`, `KRUX`); the only extra tag in practice is
   `INHERITANCE` (owner, 2026-09-25). Precedence between device tags therefore changes no real key,
   and 11.1 is closed. Classifier and tests must ignore `INHERITANCE` when picking the device.

## 10. Review feedback and required plan updates (2026-09-25)

This is a static review of the current claim screens, view models, signing sheets and this
proposal. No implementation or device testing was performed. The profile-based direction is
appropriate, especially for ExportImport, but the following contracts need clarification before
implementation. These notes are review recommendations, not authorization to implement or expand
the scope.

### 10.1 Preserve route order and distinguish device capability from UI context

**Finding [P2]:** section 4.2 says route lists determine sheet order, but its table differs from
the current `verifymessage/ColdCardSigningBottomSheets.kt`:

| Context | Current export routes | Current import routes |
|---|---|---|
| Coldcard message / PSBT signing sheet | FILE, QR, NFC | FILE, QR, NFC |
| Krux message signing sheet | FILE, QR | FILE, QR |
| Passport / Krux PSBT signing sheet | FILE, QR | FILE, QR |
| Passport message signing sheet | FILE | FILE |
| Jade / Keystone / SeedSigner signing sheet | QR | QR |
| Coldcard / Passport message Export Complete | — | FILE |
| Krux message Export Complete | — | QR, FILE |

`exportcomplete/ExportCompleteScreen.kt` deliberately has a different route list from the main
signing sheet. Feeding the same `profile.import` list into both would change available options
and ordering. A single FILE export also still opens the Save/Share sheet; only the transport
picker is skipped.

**Update required:** correct the section 4.2 table and model context-specific UI route selection
separately from device capabilities, e.g. signing-sheet routes versus post-file-export routes.
Preserve existing options/order unless a behavior change is explicitly agreed separately.

**Acceptance:** tests assert ordered routes for both contexts, including picker skipping and the
FILE Save/Share step. Validate non-empty, duplicate-free route lists and ensure every displayed
route has a real handler, rather than falling through to a default no-op callback.

### 10.2 Select request codec per export route

**Finding [P2]:** the single `SignerProfile.messageCodec` in section 4.2 does not directly express
Krux's two formats: QR uses `GenerateMessageSigningQrUseCase`, while FILE uses
`GenerateKruxMessageSigningUseCase`. The section 4.3 interface can branch on transport, but that
would require an explicit composite codec or move transport dispatch back into device codecs.
The sample profile also stores a codec instance while the prose says it stores a codec kind.

**Update required:** prefer export route specifications that select a codec kind plus transport
configuration. Resolve codec kinds through an injected factory; keep the profile pure. For example:

| Device / operation / route | Payload codec | Route configuration |
|---|---|---|
| Krux / message / QR | Specter message request | Plain QR; ClaimAirgapMessage adapter |
| Krux / message / FILE | Krux text | krux_message.txt |
| Coldcard / message / QR | Coldcard JSON | BBQR; ClaimDummy adapter |
| Coldcard / message / FILE | Coldcard JSON | coldcard_message.txt |
| Passport / message / FILE | Passport text | passport_message.txt |

Keep `Transport` as the transport identity. File names belong to FILE route metadata; QR encoding
belongs to QR route configuration. Codecs build the payload. The navigation adapter maps that
configuration to the existing `SignFlowType` / `isBBQR` APIs. This distinguishes payload format
from QR framing and does not require rewriting the shared transaction screens.

**Acceptance:** test Krux QR and FILE independently, Coldcard JSON's existing LEGACY address-type
argument, file names, and QR navigation mappings for both messages and PSBTs. A configured route
must resolve to a codec; unsupported combinations must fail explicitly.

### 10.3 Define host inputs and one owner for signing state / result processing

**Finding [P2]:** section 4.4's `SigningHost(method, payload, trigger, onResult)` lacks the selected
signer's identity. Message signing needs fingerprint/master signer identity as well as path and
text. PSBT signing with Ledger/BitBox/Trezor also needs the registration wallet, not merely the
PSBT and a list of signers. `ClaimTransactionViewModel.decodeSignedPsbt` currently preserves
amount/fee context from the current transaction while decoding with the claim's signers.

Section 4.4 also assigns decoding to the host while retaining `VM.import(transport, data)`, leaving
two potential owners for processing the same result.

**Update required:** define an explicit signing request/session with selected signer identity and
operation context. Choose one coordinator/ViewModel owner for use-case calls, pending state,
errors and result normalization. The composable host should register launchers, render sheets,
perform UI effects and forward transport results/events to that owner. Keep business decisions
such as updating claim state and claiming/broadcasting in the claim layer.

Transport adapters may need different extraction steps: NFC records, file text, and a QR activity
result are not interchangeable raw inputs. Converge after extraction into a common message-result
acceptance path; converge PSBT inputs into one decoder using the full claim transaction context.
Do not accidentally extract an already-extracted bare signature again.

**Acceptance:** document ownership and required inputs for every method. Verify Ledger/BitBox
wallet registration, BitBox message path resolution, software passphrase handling, NFC errors,
and consistent PSBT decoding across QR / FILE / NFC / hardware results.

### 10.4 Model session lifecycle and callback ownership explicitly

The section 8 statement that the Trezor boolean guard keeps a reply tied to its requesting screen
is stronger than the implementation guarantees. `requestSignMessageByTrezor()` sets
`awaitingTrezorSignature` when the deeplink is created, before launch confirmation, while
`dismissTrezorSuiteDialog()` only clears the displayed deeplink. Canceling the confirmation can
therefore leave that ViewModel awaiting a response. This is an existing limitation, not a proven
new regression caused by the proposal.

**Update required:** specify request identity tied to signer + challenge/transaction, pending
states, launch confirmation, cancellation, retry, completion, and late/duplicate callback policy.
Distinguish cancel-before-launch from dismiss-after-launch. State which correlation fields the
external protocol actually provides; local request IDs alone cannot correlate a callback that
does not carry them. Scope/reset remembered sheet state and result handlers to the active session.
Define recreation behavior and how a Sign trigger is consumed without relaunching on recomposition.
Track any intentional behavior fix separately from the behavior-preserving extraction.

**Acceptance:** cover cancel → retry, repeated Sign, switching keys in a two-key claim, late and
duplicate callbacks, recomposition and Activity recreation. Verify that inactive sessions cannot
consume NFC/Trezor results belonging to the active session.

### 10.5 Classifier and answers to section 9

- Keep `SigningDevice` separate from picker `KeyType` for this scope. An exhaustive profile table
  only catches newly added `SigningDevice` values; a new upstream tag omitted from the classifier
  can still map to an unknown category. Add explicit coverage for supported tags and unsupported
  types, rather than treating enum exhaustiveness as complete protection.
- Correct the precedence wording in section 4.1: current click dispatchers check NFC and SOFTWARE
  before Coldcard. Message callback selection separately checks QR-only/Passport/Krux before its
  Coldcard fallback. Test conflicting/multiple tags and document the intended resolution; one
  classifier cannot silently claim to preserve inconsistent existing branches for all inputs.
- Keep the UI host in the claim package until message and PSBT reuse is demonstrated. Pure profile
  and codec definitions may live in core where dependency direction permits.
- Mark Portal, generic air-gap, server and other unsupported categories explicitly per operation
  after checking the current routes. NFC/QR capability alone does not imply claim-signing support.
  Give Unsupported an explicit UI outcome rather than keeping `else -> Unit` accidentally.
- Preserve the existing claim behavior for HARDWARE + COLDCARD in this refactor. Changing how
  those keys work in other signing flows is a separate decision.

### 10.6 Revised delivery / verification recommendation

1. Pin the current behavior matrix first, including context-specific route order and classifier
   precedence. Resolve any intended behavior changes separately.
2. Add classifier and operation/route profiles with explicit unsupported cases.
3. Extract request codecs and navigation mappings; verify payload and route combinations.
4. Extract the message host with a defined coordinator and session lifecycle.
5. Reuse the proven pieces for PSBT signing, retaining registration-wallet and decoding context.

Extend section 7 beyond tests that merely assert which use case was called: add meaningful payload
fixtures where feasible, ordered UI route assertions, failure/cancellation paths and callback
ownership tests. Keep the end-to-end device matrix as a release check; static review does not
establish hardware compatibility. The main payoff should first be fewer duplicated device
decisions and one result-processing path, rather than moving all signing code into a large host.

## 11. Author additions — pending reviewer approval

Not part of the agreed plan until the reviewer signs off. Each is verified in code unless stated.

1. **[Closed — decision 9.9: no such key exists.]** **A concrete tag-conflict case for 4.1.** Old inheritance-key uploads tagged every non-Jade key
   `COLDCARD` (fixed in `152263542`; a BitBox came back tagged `INHERITANCE, COLDCARD, BITBOX`). For
   an air-gapped key carrying `COLDCARD` plus its own tag (e.g. `KEYSTONE`), the claim disagrees
   with itself: the message screen picks `isQrOnlyAirgap` first (QR-only, correct), while the PSBT
   dispatcher checks `tags.contains(COLDCARD)` before `isQrOnlyAirgap` and opens the Coldcard sheet
   (BBQR, NFC). *Unverified:* whether such a signer can reach the claim — the claim signs with the
   Beneficiary's local key, whose tags are set when it is added on this device. Decision 9.8 settles
   the type level (the tag only counts on `AIRGAP` / `HARDWARE`); this case sits *inside* those two
   types, so it is still open. Proposed rule: within `AIRGAP` / `HARDWARE`, a specific device tag wins
   over the legacy `COLDCARD` tag, as `152263542` did for the dummy-tx dispatcher; `COLDCARD` applies
   only when no other device tag is present.
2. **Trezor fix as an immediate, standalone commit.** 4.5 already agrees the fix is separate; this
   proposes doing it now, before step 1, rather than waiting for step 4: clear the flag on
   cancel-before-launch only, keep it after launch.
3. **Trezor callback correlation.** `TrezorCallbackPayload` (`TrezorDeeplinkUtil.kt:37`) carries
   `xfp`, `path` and `message`. In a two-key claim both keys sign the same challenge, so `message`
   does not identify the key, but `xfp` could. *Unverified:* whether Trezor Suite fills `xfp` in a
   sign-message reply. If it does, correlate on it instead of a boolean.
4. **Route selection as a pure function.** `SignerProfile.routes(operation, context)` returns the
   ordered list, and the sheet only renders it, so the ordered-route assertions in 7.1 run as JVM
   tests with no Compose UI test harness.
5. **Pause point after step 3.** Steps 1–3 are pure code (tests, classifier, profiles, codecs, adapter)
   and already remove the duplicated device decisions. Ship them, then decide on steps 4–5 (host +
   session), which carry most of the lifecycle risk.
