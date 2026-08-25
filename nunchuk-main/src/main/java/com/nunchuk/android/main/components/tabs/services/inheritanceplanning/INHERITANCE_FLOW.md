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
  `keyTypes` (TAPSIGNER vs COLDCARD, from signers tagged `INHERITANCE`), `walletType`, and — for
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
`keyOrigins`, `bsms`, `challenge`.

`isOnChainClaim = bsms != null || walletType == MINISCRIPT` — the main branch in the whole claim
flow (on-chain claims download a wallet; off-chain claims sign a challenge message).

### 2.3 Claim path

```
claimMagicPhrase          magic phrase → InheritanceClaimingInitUseCase
   └─ >1 key ─► addInheritanceKey (index / totalKeys)
   └─ else ───► prepareInheritanceKey
prepareInheritanceKey  ─ HARDWARE_DEVICE ─► SignerIntroActivity (OnChainAddSignerParam + magic)
                       └─ SEED_PHRASE ── on-chain? recoverInheritanceKey : claimBackupPassword
recoverInheritanceKey  ─ hardware ─► restoreSeedPhraseHardware ─► SignerIntroActivity
                       └─ seed ────► RecoverSeedScreen ─► createSoftwareSignerFromMnemonic
claimBackupPassword    ─► signers + InheritanceAdditional
                       └─ bufferPeriodCountdown != null ─► claimBufferPeriod (blocked)
addSigner(signer)      ─ off-chain ─► verifyInheritanceMessage (sign the challenge)
                       ─ enough keys ─► downloadWalletForClaim ─► claimNote
                       └─ else ──────► addInheritanceKey (next)
claimNote  ─► claimWithdrawBitcoin | claimReleaseIntro ─► claimReleaseSchedule
inheritanceError            (INHERITANCE_PLAN_NOT_FOUND / NOT_ACTIVE)
```

Key details:

- `addSigner` validates the signer against `keyOrigins` (xfp, plus derivation path for non-master
  signers) and rejects duplicates (`KeyAlreadyAdded`).
- Keys added from the **desktop app** arrive two ways: a `PushEvent.ClaimSignerAdded` push, and a
  `LifecycleResumeEffect` poll (`checkRequestedAddDesktopKey` → `GetAddedKeysForInheritanceUseCase`,
  de-duplicated via `handledRequestIds`).
- `generateClaimSigningChallengeIfNeeded` fetches a `ClaimSigningChallenge` once per claim, for
  off-chain claims only.
- `verifymessage/VerifyInheritanceMessageViewModel` signs that challenge with TAPSIGNER
  (`SignMessageByTapSignerUseCase`), a software key (`SignMessageBySoftwareKeyUseCase`, with
  passphrase), or COLDCARD (NFC `SendDataToMk4UseCase` / exported file →
  `ExtractColdcardMessageSignatureUseCase`, with `exportComplete` as the file hand-off screen).

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
