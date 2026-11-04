# Inheritance Flow

`com.nunchuk.android.main.components.tabs.services.inheritanceplanning`

Inheritance is **two separate flows** that never share a ViewModel, an activity, or a nav graph —
they only meet at the *magical phrase*, which the owner generates during creation and the
beneficiary types in to claim:

| | **Create flow** (§1) | **Claim flow** (§2) |
|---|---|---|
| Who | Wallet owner / key holder | Beneficiary |
| Does | Set up, review, update, cancel a plan | Redeem an inherited wallet and withdraw |
| Package | `inheritanceplanning/*` | `inheritanceplanning/claim/*` |
| Entry | `openInheritancePlanningScreen(...)` | `openClaimInheritanceScreen(...)` |
| Needs an account | yes (assisted/premium wallet) | no — the magical phrase is the credential |

~140 files, all Compose + type-safe navigation, across three activities:

| Activity | Flow | Host graph |
|----------|------|------------|
| `InheritancePlanningActivity` | Create — create / view / update / cancel a plan, request planning | `InheritancePlanningGraph` (`InheritancePlanningNavigation.kt`) |
| `claim/ClaimInheritanceActivity` | Claim — claim an inherited wallet | `ClaimInheritanceGraph` (inside `ClaimInheritanceActivity.kt`) |
| `claim/ClaimTransactionActivity` | Claim — sign + broadcast the claiming transaction | single screen, no graph |

---

## 1. Create flow — `InheritancePlanningActivity`

### 1.1 Entry points

Launched only via `NunchukNavigator.openInheritancePlanningScreen(...)` →
`InheritancePlanningActivity.navigate(...)` (`NunchukNavigatorImpl:392`). Callers:

- `ServicesTabFragment` — "Set up inheritance plan" / "View inheritance plan" (`SERVICE_TAB`)
- `GroupDashboardFragment` — group alerts, incl. the dummy-tx alert (`GROUP_DASHBOARD`)
- `AddKeyStepFragment`, `WalletsFragment` — end of the membership wizard (`WIZARD`)

### 1.2 Three dimensions of "which flow am I in"

**`InheritancePlanFlow`** (`nunchuk-core/util/InheritancePlanFlow.kt`) — *what the user is doing*.
Passed as `EXTRA_INHERITANCE_PLAN_FLOW`, decides the NavHost start route
(`getInheritancePlanningStartRoute`):

| Value | Meaning | Start route |
|-------|---------|-------------|
| `SETUP` (1) | First-time plan creation | `InheritanceSetupIntroRoute` |
| `VIEW` (2) | View/edit an existing plan (an `Inheritance` parcelable is passed in) | `InheritanceReviewPlanRoute` |
| `SIGN_DUMMY_TX` (4) | Another key holder co-signs a pending change | `InheritanceAlertReviewRoute` |
| `REQUEST` (5) | Member without permission asks the owner to set up a plan | `InheritanceRequestPlanningConfirmRoute` |

`EXTRA_START_DESTINATION` can override this with `START_DESTINATION_CREATE_SUCCESS`, to re-enter
straight at the success screen.

**`InheritanceSourceFlow`** — *where the user came from* (`WIZARD` / `GROUP_DASHBOARD` /
`SERVICE_TAB`); only affects post-success screens (what "Done" closes back to).

`LocalInheritancePlanFlow` (`InheritanceRemainTimeTitle.kt`) publishes the plan flow to every screen
via a `CompositionLocal` so top bars can hide the "Est. time remaining" label when merely viewing
(`estimateRemainTimeTitle`).

**`InheritanceSetupFlowType`** — *the plan's shape*, the third and most branch-heavy dimension:

| Type | Meaning |
|------|---------|
| `OLD_FLOW` | Lump-sum: one activation date, one buffer period, no schedules |
| `SINGLE_BENEFICIARY` | Customized distribution, one beneficiary, one release schedule |
| `MULTI_BENEFICIARY` | Customized distribution, N beneficiaries with % allocations |

For `MULTI_BENEFICIARY` an `InheritanceReleaseMethodType` picks between one `SHARED_SCHEDULE` for
everyone or per-beneficiary `INDIVIDUAL_SCHEDULES`.

### 1.3 Shared state — `InheritancePlanningViewModel`

Activity-scoped (`hiltViewModel(viewModelStoreOwner = activity)`), the single source of truth for
the whole graph. Every screen reads/writes it through
`activityViewModel.setOrUpdate(activityViewModel.setupOrReviewParam.copy(...))`.

`InheritancePlanningParam.SetupOrReview` is the accumulating plan draft:

```
activationDate, selectedZoneId, timelockBased, blockHeight   // when the plan activates
emails, isNotify, notificationSettings                        // who gets notified
magicalPhrase, note, inheritanceKeys                          // claiming secrets
bufferPeriod, bufferPeriodApplyType                           // legacy/shared buffer
setupFlowType, releaseMethodType                              // plan shape
beneficiaryAllocations                                        // multi-beneficiary %
sharedScheduleConfig, individualScheduleConfigs               // release schedules
isSharedScheduleConfigured, fallbackSettings
walletId, groupId, verifyToken, dummyTransactionId, planFlow, sourceFlow
```

The first `setOrUpdate` also stores `initialSetupOrReviewParam` (a deep-ish `snapshot()`), which is
what the review screen later diffs against to decide "has anything changed".

On init the VM also:
- resolves `groupWalletType` (`GetGroupUseCase`) so `MembershipStepManager` can init its steps,
- syncs the server wallet (`SyncGroupWalletUseCase` / `GetServerWalletUseCase`) to derive
  `inheritanceKeys` (device type + `claim_options` per key, from signers tagged `INHERITANCE`;
  `backupPasswordKeyIndexes` / `seedPhraseKeyIndexes` are what the setup screens branch on),
  `walletType`, and — for
  `WalletType.MINISCRIPT` — the timelock-derived activation date/timezone.
- owns BSMS export (`handleShareBsms` / `saveBSMSToLocal`), surfaced by
  `BaseShareSaveFileActivity`.

`isMiniscriptWallet()` is checked at almost every navigation fork — miniscript (on-chain timelock)
wallets skip the activation-date screen (the timelock already fixes it) and gain the
timelock-info + notification-settings screens.

### 1.4 Release-schedule drafts — `InheritanceReleaseScheduleFlowViewModel`

Release schedules are edited across 4 screens (detail → stage edit → buffer period → buffer method),
and with individual schedules there is one per beneficiary. So editing happens on a **draft**, keyed
by an id passed through the routes:

- `createDraft(...)` → `"draft_N"`, `cloneDraft`, `discardDraft`, `clearAllTransientDrafts`
- each draft holds `releaseScheduleUiState`, `pendingNewStage`, `bufferPeriod`,
  `bufferPeriodApplyType`, `hasBufferPeriodSelection`
- only on "Continue" from the detail screen is the draft committed back into
  `sharedScheduleConfig` or `individualScheduleConfigs[email]` and the draft discarded.

`pendingNewStage` exists so that backing out of "add stage" doesn't leave a half-built stage behind.

### 1.5 Screen map (setup path)

Each screen is the standard trio — `XRoute` (`@Serializable`), `NavGraphBuilder.x(...)`,
`NavController.navigateToX(...)` — with all branching kept in `InheritancePlanningNavigation.kt`
rather than in the screens.

```
intro                    InheritanceSetupIntro
  └─ miniscript? ─────► planOverview
     else ────────────► distributionMethod ─► LUMP_SUM  ─► planOverview
                                            └─ CUSTOMIZED ─► customizedDistribution (single/multi)
                                                              └─► planOverview

planOverview  ─ MULTI ──► assetAllocation ─► releaseMethod ─► beneficiarySchedules
              └─ SINGLE/OLD ──────────────► magicalPhraseIntro

beneficiarySchedules ─► (per card) releaseScheduleDetail ⇄ releaseScheduleStageEdit
                     │                     └─► bufferPeriod ─► bufferPeriodMethod ─┐
                     │                                                             │
                     │◄────────────────────────────────────────────────────────────┘
                     └─► fallbackSettings ─► magicalPhraseIntro

magicalPhraseIntro ─► miniscript? keyTip : findBackupPassword ─► keyTip
keyTip ─ SINGLE ─► releaseSchedule ─► releaseScheduleDetail ─► … ─► note
       ─ MULTI ──► note
       ─ miniscript ─► timelockInfo ─► note
       └─ OLD ───► activationDate ─► note

note ─ OLD ─► bufferPeriod ─► notifyPref
     └─ else ─► notifyPref
notifyPref ─ miniscript? notificationSettings : reviewPlan
reviewPlan ─► (sign) ─► createSuccess ─► shareSecret ─► shareSecretInfo ─► howItWorks
```

**On-chain plans with fallbacks** (configurable policy, NUN-10320): `InheritancePlanningState`
derives `primaryInheritance` and `fallbackInheritances` (`FallbackInheritanceInfo.kt`) from the
wallet's `onchain_inheritance_policy` + the plan's `inheritance_keys` (section / `fallback_index`;
keys are never inferred from signers). Review lists each fallback after the primary date:
"Fallback N · M of N keys", the server-named keys, and that fallback's own "Claimable after".
`notificationSettings` then shows, per recipient, a "Notify them when a timelock expires" switch
with one checkbox per timelock (NOTIF-03): primary = `notify_timelock_expires`, Fallback N =
index N-1 in `notify_fallbacks` (`notificationsettings/UnlockNotices.kt`); Review shows the picks
as "Unlock notices". Without fallbacks both screens keep the single Yes/No timelock notice.

The same `fallbackInheritances` drive the rest of the plan screens (Row 7 · SHARE, Row 4 · PLAN):
- `shareSecretInfo` adds item 3 "For each fallback, share its keys with the person it's intended
  for…" with a box listing "Fallback N" / "Fallback keys 2, 3 and 4 · any 2 of 3 required" (keys
  numbered across fallbacks by `fallbackKeyNumbers()`: the server's names when every key is named,
  otherwise continuing from the previous fallback), and the warning adds "and with anyone a
  fallback is intended for".
- `howItWorks`: the guided claim ends "…or the keys of a fallback whose timelock has expired", the
  self-serve claim reads "…and the same keys".
- Setup guidance (these run before the plan lists keys, so only M-of-N is used): Plan overview
  items 2 / 3 name "Fallback 1" or "each fallback" (PLAN-07 / PLAN-10); Inheritance keys adds one
  bullet per fallback and the inheritance-and-fallback backup copy (PLAN-08 / PLAN-11 / PLAN-11b);
  About the timelock switches to "timelocks" and the fallback hint (PLAN-09 / PLAN-12).

Supporting screens reachable from review/edit: `changetimezone`, `backupdownload`,
`fallbacksettings`, `assetallocation`, `releasemethod`, `beneficiaryschedules`.

`MembershipStepEffect` attaches/detaches a per-screen tracker on `MembershipStepManager` so the
"Est. time remaining" countdown keeps working across Compose destinations.

### 1.6 Release schedule model (`releasescheduledetail/ReleaseScheduleModels.kt`)

- `ReleaseScheduleUiState` = ordered `List<ReleaseScheduleStage>` + derived allocation segments,
  `totalAllocatedPercent`, `isOverAllocated`, `allocatedBeforeStage(id)`, and stage
  add/update/delete/renumber helpers.
- `ReleaseScheduleStage` = allocation %, first-withdrawal date/time + timezone, and an
  `ReleaseInstallmentConfig` (installment %, `repeatEvery`, `frequency`).
- `buildInstallmentLines()` expands a stage into cumulative installment lines; the last installment
  is the integer remainder. **Dates step one interval at a time** (`ReleaseScheduleDate.plus`) so
  calendar clamping compounds the way the backend expects
  (`2026-01-31 +1mo → 02-28 +1mo → 03-28`, not `+2mo → 03-31`). This is pinned by
  `InheritanceScheduleExpansionTest` (11 cases, incl. DST and invalid-timezone fallback).

### 1.7 Review + submit — `reviewplan/`

`InheritanceReviewPlanViewModel` is where the UI model is translated into the API payload:

- `isScheduleConfigured(param)` gates the Continue/Save button — a beneficiary with >0% allocation
  and no stages would be rejected by the backend (`beneficiaries[N].stages is required`), so the
  button stays disabled instead.
- `hasDataChanged()` diffs against `initialParam`; in `VIEW` flow saving is only enabled when
  something actually changed. `calculateReviewPlanChangeHighlights` drives the "changed" badges.
- `buildScheduleRequestData` maps `setupFlowType` → `stages` / `beneficiaries`;
  `toDistributionMethod` / `toBeneficiaryMode` / `toReleaseMethod` / `toBufferApplyOn` /
  `toFallbackPolicy` map the enums onto the API's string constants
  (`"SINGLE"/"MULTIPLE"`, `"SHARED"/"INDIVIDUAL"`, `"FIRST_WITHDRAWAL"/"EVERY_WITHDRAWAL"`,
  `"NONE"/"INACTIVITY"/"DATE_BASED"`). `WEEKLY` is sent as `DAY` × 7.

Submission is the standard assisted-wallet two-phase dance:

1. `calculateRequiredSignaturesInheritanceUseCase` + `getInheritanceUserDataUseCase`
   (or `cancelInheritanceUserDataUseCase` for the CANCEL flow).
2. If `VerificationType.SIGN_DUMMY_TX` → create the draft dummy tx
   (`createOrUpdateInheritanceUseCase(draft = true)` / `cancelInheritanceUseCase(draft = true)`),
   then `navigator.openWalletAuthentication(...)` with the returned `dummyTransactionId`.
   If `SECURITY_QUESTION` → the same launcher, no draft.
3. The launcher result (`SIGNATURE_EXTRA` / `SECURITY_QUESTION_TOKEN`) comes back into
   `handleFlow(...)` → `createOrUpdateInheritance` / `cancelInheritance` with `draft = false`,
   then `markSetupInheritanceUseCase`.

Cancelling a plan is the same pipeline with `ReviewFlow.CANCEL`, triggered from the top-bar
bottom-sheet (`showReviewActionOptions`) — only shown in `VIEW` flow.

On success: `SETUP` → `createSuccess`; `VIEW`/`CANCEL` → toast + `setResult(UPDATE_INHERITANCE,
WALLET_ID)` + `finish()`.

### 1.8 Co-signer path — `SIGN_DUMMY_TX`

`InheritanceAlertReviewViewModel` loads the pending dummy transaction
(`GetDummyTransactionPayloadUseCase` → `ParseInheritancePayloadUseCase`), renders old-vs-new, and on
Continue rebuilds the *same* user data from the payload (not from local state) so the signature
matches. `cancelChange()` deletes the dummy tx.

`InheritanceAlertReviewScreen` (`oldFlowItems`, miniscript or lump-sum plans) highlights only what
changed against the previous plan (`oldData`):

* Configurable on-chain plans (`onChainInheritancePolicy.isConfigurable`) drop the timelock box —
  each route's unlock time lives in the wallet policy, not in the plan (Figma REV-11 `4664:25401`).
* Notification preferences go through the shared `NotificationPreferencesSection`. With fallbacks
  (`fallbackCount` = the wallet policy's fallbacks) each recipient shows "Unlock notices"
  (`unlockNoticeRoutes`); without them the single Yes/No timelock row.
* Per recipient, `EmailNotificationSettings.changesFrom(previous, fallbackCount)`
  (`reviewplan/NotificationPreferenceChanges.kt`) returns a `RecipientNotificationChanges`; only
  the changed rows use `changedTextColor`. Recipients are matched by email; a new one is all
  changed (`RecipientNotificationChanges.ALL`). The owner's "Email me wallet configuration" row is
  highlighted when `emailMeWalletConfig` differs. With no `oldData` (plan creation) nothing is red.

### 1.9 `VIEW` flow — rehydrating an existing plan

`Inheritance.toSetupOrReviewParamForView(...)` (bottom of `InheritancePlanningActivity.kt`) is the
inverse of the review VM's mapping: it infers `setupFlowType` from
`distributionMethod`/`beneficiaryMode`, picks shared vs individual schedules off
`releaseMethod`, and converts API stages back into `ReleaseScheduleUiState`
(`toReleaseInstallmentConfig`, `toFallbackSettingsValue`, `mapFallbackTrigger` — including the
`DAY % 7 == 0 → WEEK` round-trip). `isNotify` deliberately resets to `false`: "also notify them
today" is a one-time action, not stored state.

---

## 2. Claim flow — `claim/`

### 2.1 Entry & start route

`NunchukNavigator.openClaimInheritanceScreen(activityContext, ClaimArgs)`. If `args.bsms` is
present (a wallet already downloaded / on-chain claim), the graph starts at `ClaimNoteRoute` and
immediately fetches status; otherwise it starts at `ClaimMagicPhraseRoute`.

### 2.2 `ClaimInheritanceViewModel`

`ClaimData` is `@Parcelize` and held in a `MutableSaveStateFlow` (survives process death):
`signers`, `signatures`, `magic`, `inheritanceAdditional`, `requiredKeyCount`, `walletType`,
`keyOrigins`, `bsms`, `challenge`, `requiredKeyCounts`, `pendingKeyOrigins`.

`isOnChainClaim = bsms != null || walletType == MINISCRIPT` — the main branch in the whole claim
flow (on-chain claims download a wallet; off-chain claims sign a challenge message).

### 2.3 Claim path

```
claimMagicPhrase          magic phrase → InheritanceClaimingInitUseCase
   └─ several required_key_counts ─► claimKeyCount (CLM-KC) ─► one of the two below
   └─ >1 key ─► addInheritanceKey (index / totalKeys)
   └─ else ───► prepareInheritanceKey
prepareInheritanceKey  ─ HARDWARE_DEVICE ─► [on-chain: claimSafetyNotice EXISTING_DEVICE] ─► SignerIntroActivity
                       └─ SEED_PHRASE ── on-chain? recoverInheritanceKey : claimBackupPassword
recoverInheritanceKey  ─ hardware ─► claimSafetyNotice RESTORE_ON_DEVICE ─► restoreSeedPhraseHardware ─► SignerIntroActivity
                       └─ seed ────► claimSafetyNotice SOFTWARE_SEED ─► RecoverSeedScreen ─► createSoftwareSignerFromMnemonic
claimBackupPassword    ─► signers + InheritanceAdditional
                       └─ bufferPeriodCountdown != null ─► claimBufferPeriod (blocked)
addSigner(signer)      ─ off-chain ─► verifyInheritanceMessage (sign the challenge)
                       ─ enough keys ─► downloadWalletForClaim ─► claimNote
                       │                 ├─ buffer period ─► claimBufferPeriod
                       │                 └─ more keys ─► key_origins (CLM-16 dialog) or addInheritanceKey
                       └─ else ──────► addInheritanceKey (next)
claimNote  ─► claimWithdrawBitcoin | claimReleaseIntro ─► claimReleaseSchedule
inheritanceError            (INHERITANCE_PLAN_NOT_FOUND / NOT_ACTIVE)
```

Key details:

- `addSigner` validates the signer against `keyOrigins` (`isInPlanOrigins`: xfp, plus derivation
  path for non-master signers) and rejects duplicates (`KeyAlreadyAdded`). XFPs compare ignoring
  case and paths across `h` / `'` (`normalizedPath`). On-chain, `addOnChainSigner` runs the checks
  in this order: a device with a pending account that came back at another one (usually Account 0
  again, already added) → CLM-16 (`WrongAccount`; never for a software key, whose accounts the app
  derives); then the plan check, skipped for a requested account since init's `key_origins` may
  list only account 0 (`NotInPlan`); then the duplicate (`AlreadyAdded`).
- **Configurable on-chain claims (NUN-10320 §7).** `claiming/init` returns `required_key_counts`
  (`InheritanceClaimingInit.requiredKeyCounts`). It is empty from an older backend, which keeps the
  single `inheritance_key_count` and the old per-signer counting. With several counts,
  `claimKeyCount` (CLM-KC) sets `requiredKeyCount`; with one, the chooser is skipped. Keys then
  count as *independent* keys (`ClaimData.addedKeyCount`: the accounts of one XFP count once).
  - Account 0 of each key is collected, then `downloadWalletForClaim` sends them. The result
    (`ClaimDownloadWalletResult`) is the wallet, a buffer period (`claimBufferPeriod`), or
    `MoreKeysNeeded(validKeys, keyOrigins)`.
  - `MoreKeysNeeded`: `ClaimData.withDownloadResult` keeps only the accepted accounts (the full
    XFP/path/xpub) and stores the requested accounts as `pendingKeyOrigins`.
    - Pure steps in `ClaimKeyCollection.kt`: `addOnChainSigner` and `nextAccountImport`.
    - A software key's account is derived in the app. It is recorded only once sent, so a failed
      derivation offers Retry, and one the backend rejects is not derived again (no loop).
    - A device's account is asked for with the CLM-16 dialog ("Add Account N from this Jade"; for
      wired devices, "Connect the Ledger…"). The dialog opens `SignerIntroActivity` with
      `ClaimInheritance(keyIndex = N, accountDevice = device)`, which skips the picker and opens
      that device's own flow (`SignerIntroCoordinator.directKeyType`): the guided account export,
      or the wired device's connect screen. It skips the firmware gate and the saved-key offer. If
      the device returns another account, the dialog shows again. Backing out of the device screen
      returns to the claim's next-key screen (`returnsToKeyCollectionOnCancel`), keys kept.
    - Each imported account is submitted at once with the accepted keys; the next response decides
      what is still needed. A requested account never applies to a new device, which starts at
      account 0.
    - With no origins, the heir sees "Add another key to continue", or the incorrect-key error when
      a key was dropped. Accepted keys are kept.
  - A network failure offers Retry. Plan-not-found / not-active still go to `inheritanceError`,
    whose message carries the condition's unlock time.
  - The claim transaction gets one account per XFP (`requiredSigners`): the last one added, which
    is the account the backend asked for.
- **Safety notices** (Figma Row 8 CLM-01/02/03, on-chain only): `claimSafetyNotice` runs before
  each new key, chosen by recovery method. Importing another account of a device already added
  skips it.
- Keys added from the **desktop app** arrive two ways: a `PushEvent.ClaimSignerAdded` push, and a
  `LifecycleResumeEffect` poll (`checkRequestedAddDesktopKey` → `GetAddedKeysForInheritanceUseCase`,
  de-duplicated via `handledRequestIds`).
- `generateClaimSigningChallengeIfNeeded` fetches a `ClaimSigningChallenge` once per claim, for
  off-chain claims only.
- `verifymessage/VerifyInheritanceMessageViewModel` signs that challenge with TAPSIGNER
  (`SignMessageByTapSignerUseCase`), a software key (`SignMessageBySoftwareKeyUseCase`, with
  passphrase), COLDCARD (NFC `SendDataToMk4UseCase` / exported file →
  `ExtractColdcardMessageSignatureUseCase`, with `exportComplete` as the file hand-off screen),
  Jade, Keystone and SeedSigner (one `GenerateMessageSigningQrUseCase` QR out, the signature QR
  back — `SignFlowType.ClaimAirgapMessage`; every device's routes come from
  `SignerModel.signingDevice().profile()` in `core/signing`), Passport (the request as a `.txt` for its microSD card —
  `GeneratePassportMessageSigningUseCase` — and the `-signed` file back through the picker, with the
  per-device "Export completed" screen in between), Krux (file like Passport —
  `GenerateKruxMessageSigningUseCase` — or QR like Jade, and the signature back either way; the
  "Export completed" screen asks QR / file), Ledger (`LedgerSignMessageSheet` at the signer's path),
  BitBox (`BitBoxSignMessageSheet` at the path `GetBitBoxSignMessagePathUseCase` resolves) and
  Trezor (`GetTrezorSignMessageDeeplinkUseCase` → Trezor Suite → `TrezorCallbackHolder` →
  `ParseTrezorSignMessageResponseUseCase`; the VM collects the holder itself and only takes
  `signMessage` replies). Every route ends in one bare `signature` string on the state. Files
  from any device are parsed by `ExtractMessageSignatureUseCase` (libnunchuk's generic
  `ExtractMessageSignature`: armored signed-message file or bare base64).
- **The claiming PSBT** (`ClaimTransactionViewModel`) is signed by software / TAPSIGNER / COLDCARD
  (NFC or file) / Jade, Keystone, SeedSigner (UR QR) as before, and by Ledger, BitBox and Trezor
  against the wallet the claim status returned: `InheritanceAdditional.registrationBsms` →
  `ClaimInheritanceTxParam` → `ClaimTransactionArgs.registrationBsms` →
  `ParseWalletDescriptorUseCase`, kept in memory only (the ticket forbids creating a wallet from
  it). A PSBT scanned back over QR is decoded again against the claim's signers
  (`importSignedTransaction`), as a file is — SeedSigner and Krux strip the metadata the
  wallet-less QR decode needs. Ledger/BitBox register that policy inside their
  `*SignPsbtSheet(wallet, …)`; Trezor builds its deeplink from it. Without the descriptor —
  the server sends it only when a key `requires_wallet_registration` — those three report
  "Cannot load wallet for … signing" and cannot sign.

### 2.4 Withdrawing

`claimWithdrawBitcoin` hands off to the normal transaction stack via `NunchukNavigator`, always
carrying a `ClaimInheritanceTxParam` (master signer ids, magic phrase, derivation paths, amount,
bsms, signatures, challenge id):

- to an **external address** → `openAddReceiptScreen` (sweep, subtract fee from amount)
- to an **existing wallet** → `openSelectWalletScreen(TYPE_INHERITANCE_WALLET)`
- to a **new wallet** → `openWalletIntermediaryScreen(QuickWalletParam)`
- a **custom amount** → `openInputAmountScreen`

If `isRequiredRegister`, `openUploadConfigurationScreen(RegisterOnly)` is launched alongside.

`ClaimTransactionActivity` + `ClaimTransactionViewModel` then sign the claiming PSBT (software /
TAPSIGNER / COLDCARD via NFC or file) and call `InheritanceClaimingClaimUseCase`.

---

## 3. Conventions used throughout

- **Type-safe routes only** — every destination is a `@Serializable` route + `NavGraphBuilder`
  extension + `NavController` extension, in a `XNavigation.kt` next to `XScreen.kt`.
- **Branching lives in the graph**, not in screens: screens expose `onContinueClicked(...)`
  callbacks; `InheritancePlanningNavigation.kt` decides where that goes based on `setupFlowType` /
  `planFlow` / `isMiniscriptWallet()`.
- **Back navigation to a specific screen** uses typed pops
  (`popBackStack<InheritanceReviewPlanRoute>(inclusive = false)`) with a plain `popBackStack()`
  fallback when the target isn't on the stack — see `returnToReleaseScheduleDetail` and the
  `returnToReviewPlan` route flags.
- **`isUpdateRequest` / `returnToReviewPlan`** route flags mark "opened for editing from review",
  so the screen pops back instead of continuing forward.
- Beneficiary map keys are always normalized: `email.trim().lowercase()`
  (`beneficiaryScheduleKey` / `toEmailKey`).

## 4. Where to add things

| Change | Touch |
|--------|-------|
| New setup screen | new package with `XScreen.kt` + `XNavigation.kt`, register + branch in `InheritancePlanningNavigation.kt` |
| New plan field | `InheritancePlanningParam.SetupOrReview`, `hasDataChanged()`, `snapshot()`, the review VM's `toXxx` mappers, and `Inheritance.toSetupOrReviewParamForView` (both directions!) |
| New flow shape | `InheritanceSetupFlowType` — then every `when(setupFlowType)` in the nav graph, `isScheduleConfigured`, `buildScheduleRequestData`, `toSetupFlowTypeForView` |
| Schedule math | `ReleaseScheduleModels.kt` + `InheritanceScheduleExpansionTest` |
| New claim signer type | `ClaimInheritanceViewModel.addSigner` + `VerifyInheritanceMessageViewModel` (off-chain) + `ClaimTransactionViewModel` (PSBT) |
