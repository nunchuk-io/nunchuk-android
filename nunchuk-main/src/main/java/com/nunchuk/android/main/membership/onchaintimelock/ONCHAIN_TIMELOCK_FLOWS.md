# On-chain timelock (`membership/onchaintimelock`)

Everything in this package belongs to **on-chain timelock wallets** — assisted (membership) wallets
whose `WalletType` is `MINISCRIPT`. The spending policy has two halves enforced by the Bitcoin
network itself:

| | Personal wallet | Group wallet |
|---|---|---|
| **Before the timelock** | 2-of-4 multisig | 3-of-5 multisig |
| **After the timelock** | 1-of-3 (same keyset minus the Platform key) | 2-of-4 (same keyset minus the Platform key) |

Because two different policies must be satisfied by the *same* physical devices, **every hardware
key slot in this flow holds two keys — two accounts of one device.** That single fact drives almost
all of the structure below.

---

## 1. Package map

```
onchaintimelock/
├── onchainexplanation/     OnChainTimelockExplanationFragment      — flow intro (creation)
├── addkey/                 OnChainTimelockAddKeyListFragment       — the key list (creation)
│                           OnChainTimelockAddKeyListViewModel
├── setuptimelock/          OnChainSetUpTimelockFragment            — pick the unlock date
│                           OnChainSetUpTimelockViewModel           — create OR replace timelock
├── importantpassphrase/    ImportantNoticePassphraseFragment       — inheritance-key passphrase warning
├── backupseedphrase/       BackUpSeedPhraseActivity (+ 4 Compose destinations)
│                           BackUpSeedPhraseSharedViewModel         — verify / skip the seed backup
├── replacekey/             OnChainReplaceKeyIntroFragment          — replace flow intro
│                           OnChainReplaceKeysFragment              — the key list (live wallet)
│                           OnChainReplaceKeyViewModel              — (class OnChainReplaceKeysViewModel)
├── changetimelock/         ChangeTimelockFragment                  — on-chain ⇄ off-chain switch
│                           ChangeTimelockViewModel
└── checkfirmware/          CheckFirmwareViewModel                  — firmware gate (UI lives in
                                                                      membership/signer/CheckFirmwareNavigation.kt)
```

All fragments are hosted by `MembershipActivity` and wired through
`nunchuk-main/src/main/res/navigation/membership_navigation.xml`. `BackUpSeedPhraseActivity` is the
one exception: it is its own activity with an internal Compose `NavHost`.

---

## 2. The two card models

Both key-list screens render a list of *cards*. A card is a logical key; it may hold two steps.

### 2.1 Creation flow — `AddKeyOnChainData` (`membership/model/AddKeyData.kt`)

```kotlin
data class AddKeyOnChainData(
    val steps: List<MembershipStep>,                     // [timelockStep, regularStep]
    val stepDataMap: Map<MembershipStep, StepData> = emptyMap()
)
data class StepData(signer: SignerModel?, verifyType: VerifyType, timelock: TimelockExtra?)
```

`GroupWalletType.toSteps(isPersonalWallet, walletType = MINISCRIPT)` emits each hardware step
**twice** — once plain, once with the `_TIMELOCK` suffix — plus `ADD_SEVER_KEY` and `TIMELOCK`:

```
HONEY_ADD_INHERITANCE_KEY, HONEY_ADD_INHERITANCE_KEY_TIMELOCK,
HONEY_ADD_HARDWARE_KEY_1,  HONEY_ADD_HARDWARE_KEY_1_TIMELOCK,
HONEY_ADD_HARDWARE_KEY_2,  HONEY_ADD_HARDWARE_KEY_2_TIMELOCK,
ADD_SEVER_KEY, TIMELOCK                                    // personal 2-of-4 + inheritance
```

`List<MembershipStep>.toAddKeyOnChainDataList()` pairs them into cards ordered
`[timelockStep, regularStep]`. Consequences:

* `getNextStepToAdd()` = first step whose `StepData.isComplete` is false → **the `_TIMELOCK` step is
  filled first (device account 0), the regular step second (account 1)**. The replace flow's
  `OnChainReplaceKeyStep.toIndex()` mapping agrees (`*_TIMELOCK → 0`, plain → 1).
* The card UI shows two account chips: `Acct X` (greyed until slot 0 is filled) and `Acct Y`
  (slot 1). Real labels come from `signer.index`.
* A card is "done" only when **both** slots are filled — the Continue button checks
  `data.steps.all { stepDataMap[it]?.isComplete == true }`.

`ADD_SEVER_KEY` and `TIMELOCK` are single-step cards and never show account chips
(`shouldShowAcctXBadge()`).

### 2.2 Replace flow — `AddReplaceKeyOnChainData` (`membership/model/AddReplaceKeyOnChainData.kt`)

Same shape, but keyed by `OnChainReplaceKeyStep` (`nunchuk-domain/model/OnChainReplaceKeyStep.kt`)
and carrying the *existing* wallet state alongside the pending replacement:

```kotlin
data class AddReplaceKeyOnChainData(
    val steps: List<OnChainReplaceKeyStep>,              // [timelockStep, regularStep]
    val stepDataMap: Map<OnChainReplaceKeyStep, ReplaceStepData>,  // the NEW keys
    val fingerPrint: String = "",                        // xfp of the key being replaced
    val originalSigners: List<SignerModel> = emptyList(),// what is in the wallet today
    val originalTimelock: WalletTimelock? = null,
    val newTimelock: WalletTimelock? = null
)
```

Step sets come from `getReplaceKeySteps(isGroupWallet)`; server signer `index` ⇄ step conversion is
`Int.toOnChainReplaceKeyStep(isGroupWallet)` / `OnChainReplaceKeyStep.toIndex(isGroupWallet)`.

### 2.3 `OnChainAddSignerParam` (`nunchuk-core/core/signer/OnChainAddSignerParam.kt`)

The parcelable that every add-key sub-activity receives. It is how a generic add-key screen
(TapSigner setup, Mk4, air-gap, signer intro) learns it is running inside an on-chain flow.

| Field | Meaning |
|---|---|
| `flags` | bit set: `FLAG_ADD_INHERITANCE_SIGNER` (0x01), `FLAG_VERIFY_BACKUP_SEED_PHRASE` (0x02), `FLAG_ADD_SIGNER` (0x04), `FLAG_ADD_INHERITANCE_OFF_CHAIN_SIGNER` (0x08) |
| `keyIndex` | which slot of the card is being filled (0 or 1) |
| `currentSigner` | slot 0's signer — the second account must come off the same device |
| `existingSigners` | every signer already used by the wallet, so pickers can filter them out |
| `replaceInfo` | `{ replacedXfp, step }` — non-null ⇒ replace flow (`isReplaceKeyFlow()`) |
| `magic` | non-empty ⇒ inheritance *claiming* flow (`isClaiming`) |

---

## 3. Flow A — Add keys while creating the wallet

### 3.1 Entry

```
AddKeyStepFragment / AddGroupKeyStepFragment
  └─ AddKeyStepViewModel: inheritanceType == ON_CHAIN
       → AddKeyStepEvent.OpenOnChainTimelockExplanation
            → onChainTimelockExplanationFragment            (args: role, config)
                 → onChainTimelockAddKeyListFragment        (args: group_id, is_add_only, role, config)
```

`AddKeyStepFragment` can also jump straight to `onChainTimelockAddKeyListFragment` (passing
`config`) when the explanation has already been seen.

### 3.2 The key list — `OnChainTimelockAddKeyListViewModel`

State assembly, in `init` + `refresh()`:

1. `SyncDraftWalletUseCase(groupId)`.
   * `draft.walletType == MULTI_SIG` ⇒ `AddKeyListEvent.RequireReopenWallet` → the activity
     finishes (the draft is no longer an on-chain wallet).
   * otherwise store `draft.isCustomized` and build the cards from `draft.config`
     (`initializeKeysFromConfig` → `toGroupWalletType().toSteps(...).toAddKeyOnChainDataList()`).
2. `GetMembershipStepUseCase` (combined with the card list) hydrates each step from the server:
   `masterSignerId` + `SignerExtra` JSON → the local `SignerModel`, `derivationPath`, `index`
   (`GetIndexFromPathUseCase`) and `verifyType`. TapSigner steps whose `SignerExtra.userKeyFileName`
   is empty are collected into `missingBackupKeys` (the card then shows **Upload backup** instead of
   **Verify**).
3. `TIMELOCK` / `ADD_SEVER_KEY` steps are hydrated with `info.parseTimelockExtra()`.
4. Push events `DraftWalletTimelockSet` and `DraftWalletCustomizationChanged` re-trigger `refresh()`;
   `KeyAddedToGroup` (only when `isAddOnly`) sets `shouldShowKeyAdded` → `keyAddedToGroupWalletFragment`.
5. Pull-to-refresh and `onResume()` both call `refresh()`.

Role handling in the UI: `KEYHOLDER_LIMITED` sees invisible keys, the server key and the timelock
card blurred (`BlurView`) and gets no Continue button; `isFacilitatorAdmin` gets an info dialog on
entry and disabled buttons.

### 3.3 "Add" — the dispatcher chain

```
onAddClicked(card)  →  savedStateHandle[current_step] = card.getNextStepToAdd()
                    →  AddKeyListEvent.OnAddKey
                    →  handleOnAddKey(data)
```

`handleOnAddKey` splits on the *next* step:

| next step | destination |
|---|---|
| `ADD_SEVER_KEY` | `openConfigServerKeyActivity` / `openConfigGroupServerKeyActivity` |
| anything else | `handleHardwareKeyAdd(data)` |

`handleHardwareKeyAdd(data)` — this is the heart of the flow:

1. **Customized draft, non-inheritance step** (`state.isCustomized && !nextStep.isAddInheritanceKey`)
   → `SignerIntroActivity` with `FLAG_ADD_SIGNER`, `walletType = MINISCRIPT`.
2. **Slot 0 empty**
   * inheritance step → `inheritanceKeyIntroFragment(inheritanceType = ON_CHAIN)`
     → `importantNoticePassphraseFragment` → `SignerIntroActivity` with
       `FLAG_ADD_INHERITANCE_SIGNER`.
   * otherwise → `SignerIntroActivity` with `FLAG_ADD_SIGNER`, `keyIndex = 0`.
3. **Slot 0 filled** (adding the second account of the same device)
   * TapSigner (`SignerType.NFC`) → `viewModel.handleTapSignerAcct1Addition(...)` (§3.6).
   * everything else → `viewModel.handleSignerIndexCheck(...)`:
     `GetRemoteSignerUseCase` at `m/48h/{0|1}h/1h/2h`
     * found → `onSelectedExistingHardwareSigner(signer, data)` — reuse the already-known account 1;
     * not found / error → `AddKeyListEvent.HandleSignerTypeLogic(type, tag)` → re-run the add flow
       for that device type so the user reads account 1 off it.

### 3.4 `handleSignerTypeLogic(type, tag)` — the per-key-type table

| `SignerType` / `SignerTag` | Destination |
|---|---|
| `NFC` (TAPSIGNER) | `NfcSetupActivity(SETUP_TAP_SIGNER)` via `addTapSignerLauncher` |
| `COLDCARD_NFC`, or `AIRGAP`+`COLDCARD` | `openSetupMk4` (`SetupMk4Args` + `OnChainAddSignerParam`) |
| `AIRGAP` (other tags) | `openAddAirSignerScreen` (`AddAirSignerArgs` + param) |
| `HARDWARE` + `LEDGER` | **in-app** `LedgerActivity` (BLE/USB) |
| `HARDWARE` + `BITBOX` | **in-app** `BitBoxActivity` (BLE/USB) |
| `HARDWARE` + `TREZOR` / `COLDCARD` / `JADE` | `addDesktopKeyFragment` (finish in the desktop app) |
| anything else | silently dropped |

`openLedgerFlow` / `openBitBoxFlow` pass the two-accounts-of-one-device contract explicitly:

```kotlin
accountIndex = currentKeyData.getAllSigners().size          // 0 for slot 0, 1 for slot 1
expectedXfp  = currentKeyData.getAllSigners().firstOrNull()?.fingerPrint.orEmpty()
```

`LedgerActivity` / `BitBoxActivity` may also return
`EXTRA_RESULT_ACTION = RESULT_ACTION_OPEN_DESKTOP_FLOW` (the user picked "Add via desktop app" on the
device intro) → `openRequestAddDesktopKey(tag)`.

If matching hardware signers already exist locally, `handleHardwareSignerTag(tag)` first offers them
in `TapSignerListBottomSheetFragment` (`getHardwareSigners(tag)` filters by
`type == HARDWARE && tags.contains(tag)`; for personal wallets it also drops keys already used by
the membership). Picking one → `onSelectedExistingHardwareSigner`; picking "add new" → the table above.

### 3.5 Result plumbing back into the list

| Source | Carrier | Handler |
|---|---|---|
| `SignerIntroActivity` | `signerIntroLauncher` (`EXTRA_SIGNER` and/or `EXTRA_SIGNER_TAG`) | `handleSignerIntroResult` |
| `ImportantNoticePassphraseFragment` | fragment result `REQUEST_KEY` | `handleSignerIntroResult` |
| `ColdCardIntroFragment` | fragment result (`EXTRA_SIGNER_TAG`) | `handleHardwareSignerTag` |
| `TapSignerListBottomSheetFragment` | fragment result | reuse / `handleSignerTypeLogic` |
| `LedgerActivity` / `BitBoxActivity` | dedicated launchers | `onSelectedExistingHardwareSigner` |
| `NfcSetupActivity` | `addTapSignerLauncher` | `addExistingTapSignerKey` |
| `BackUpSeedPhraseActivity` | `verifyBackUpSeedPhraseLauncher` (`EXTRA_VERIFIED_XFP`) | `setKeyVerified` |

`handleSignerIntroResult` normalises the signer: anything Coldcard-ish becomes `COLDCARD_NFC`; an
`AIRGAP` signer with no tags plus a remembered `selectedSignerTag` goes through
`onUpdateSignerTag` (`UpdateRemoteSignerUseCase`) first.

Persisting a key — `onSelectedExistingHardwareSigner` / `processTapSignerWithCompleteData`:

```
SyncKeyUseCase(step, signer, walletType = MINISCRIPT, groupId)
SaveMembershipStepUseCase(MembershipStepInfo(step, masterSignerId, verifyType,
                          extraData = SignerExtra(derivationPath, isAddNew = false, signerType, "")))
updateCardForStep(step, signerModel, verifyType)
```

`getVerificationTypeForSigner` decides `verifyType`: **`NONE` for inheritance cards and TapSigner**
(they still need an explicit verification step), `APP_VERIFIED` for everything else.

### 3.6 TapSigner two-account special case

TapSigner cannot derive a new account without an NFC tap, so the flow is interrupt-driven:

1. `addExistingTapSignerKey` → `GetUnusedSignerFromMasterSignerV2UseCase`.
2. Native error `-1009` (xpub not cached) ⇒ stash `{master id, path, context}` in `SavedStateHandle`
   and set `requestCacheTapSignerXpubEvent`.
3. The fragment observes that flag and calls `MembershipActivity.requestTapSignerCaching()` →
   `startNfcFlow(REQUEST_NFC_TOPUP_XPUBS)`.
4. The activity's NFC observer calls back into `viewModel.cacheTapSignerXpub(isoDep, cvc)` →
   `GetSignerFromTapsignerMasterSignerByPathUseCase` → resume by `TapSignerCachingContext`
   (`ADD_TAPSIGNER_KEY` = account 0, `HANDLE_ACCT1_ADDITION` = account 1,
   `HANDLE_CUSTOM_KEY_ACCOUNT_RESULT`).
5. `processTapSignerWithCompleteData` saves the step, then — if the card still has a next step —
   advances `current_step` and calls `handleTapSignerAcct1Addition`, which asks for account 1 via
   `GetSignerFromMasterSignerByIndexUseCase(index = 1)` and loops back to step 2 on `-1009`.

Paths are built locally: `m/48h/0h/{index}h/2h` (mainnet) or `m/48h/1h/{index}h/2h` (testnet).

### 3.7 Verification

`onVerifyClicked(card)` picks the first step that has a signer but `verifyType == NONE`, then emits
`OnVerifySigner(signer, filePath, backUpFileName, isBackupNfc)`:

| signer | destination |
|---|---|
| `NFC` + missing backup | `openCreateBackUpTapSigner(isOnChainBackUp = true)` |
| `NFC` | `openVerifyBackupTapSigner(isOnChainBackUp = true)` |
| anything else | `BackUpSeedPhraseActivity` (§5) via `verifyBackUpSeedPhraseLauncher` |

`setKeyVerified(xfp)` → `SetKeyVerifiedUseCase(verifyType = APP_VERIFIED)` → `refresh()` →
`OnKeyVerified` → the success screen (`BackUpSeedPhraseType.SUCCESS`).

### 3.8 Continue

The Continue button is enabled only when every card's every step is complete **and** every
inheritance card and every TapSigner card has `verifyType != NONE`. `onContinueClicked()` refreshes
and emits `OnAddAllKey`, which pops back to `addKeyStepFragment` (personal) or
`addGroupKeyStepFragment` (group).

---

## 4. Flow B — Setting the timelock

`onChainTimelockAddKeyListFragment` → **Configure** (or **Change**) on the `TIMELOCK` card →
`onChainSetUpTimelockFragment(groupId, timelockExtra, isReplaceKeyFlow, walletId)`.
`KEYHOLDER_LIMITED` gets the facilitator dialog instead.

`OnChainSetUpTimelockViewModel` — the screen defaults to *now + 5 years*, lets the user pick a
timezone/date/time, and validates on **Save**:

| condition | behaviour |
|---|---|
| date in the past | `showInvalidDateDialog` |
| more than `maxTimelockYears` ahead (`GetUserWalletConfigsSetupFromCacheUseCase`) | `showConfirmTimelockDateDialog`, then `onConfirmTimelockDate()` proceeds |
| unix seconds ≥ `2147483648` (Y2K38, Jan 19 2038) | `showBlockBasedTimelockDialog`; confirming runs `ConvertTimelockUseCase` and switches the lock to `TimelockBased.HEIGHT_LOCK` with the returned `blockHeight` |
| date moved back below the threshold | `isBlockBased`/`blockHeight` reset to time-based |

Commit:

| flow | use case | push event |
|---|---|---|
| creation (`isReplaceKeyFlow = false`) | `CreateTimelockUseCase(groupId, value, timezone, plan, based, blockHeight)` | `DraftWalletTimelockSet(groupId)` |
| replace (`isReplaceKeyFlow = true`, `walletId != null`) | `ReplaceTimelockUseCase(groupId, walletId, value, timezone, based, blockHeight)` | `ReplaceKeyChange(walletId)` |

On success the screen simply pops (`onBackPressedDispatcher.onBackPressed()`); the list screen picks
up the change through the push event. Errors surface as `NCToastMessage`.

A `HEIGHT_LOCK` card renders `Est. MM/dd/yyyy HH:mm` plus the block height; a `TIME_LOCK` card
renders the date in the stored timezone. Copy on the screen is explicit that **the lock is
immutable** — changing it later means creating a new wallet and migrating funds.

---

## 5. Flow C — Verifying the inheritance key's seed-phrase backup

`BackUpSeedPhraseActivity` (`nav.args.BackUpSeedPhraseArgs`: `type`, `signer`, `groupId`, `walletId`,
`replacedXfp`) hosts a Compose `NavHost` with four destinations. Start destination is `INTRO` or
`SUCCESS` depending on `args.type`.

```
BackUpSeedPhraseIntro          "Back up your inheritance key seed phrase" → [I have backed it up]
   └→ BackUpSeedPhraseOption   "Verify now" | "Verify later"
        ├ skip  → BackUpSeedPhraseSharedViewModel.skipVerification(...)
        │           replacedXfp empty → SetKeyVerifiedUseCase(SKIPPED_VERIFICATION)
        │           replacedXfp set   → SetReplaceKeyVerifiedUseCase(SKIPPED_VERIFICATION)
        │         → navigator.returnMembershipScreen()
        └ verify → BackUpSeedPhraseVerify   (4 numbered instructions: wipe/temp seed →
                     restore from BIP39 → enter the phrase → re-add the restored key)
                     └→ SignerIntroActivity, OnChainAddSignerParam(
                            flags = FLAG_VERIFY_BACKUP_SEED_PHRASE,
                            currentSigner = args.signer,
                            replaceInfo = ReplaceInfo(replacedXfp, step = null))
BackUpSeedPhraseVerifySuccess  "You have backed up the seed phrase correctly"
```

**The activity stays alive** while `SignerIntroActivity` runs and relays its result verbatim
(`setResult(result.resultCode, result.data); finish()`). That is how `EXTRA_VERIFIED_XFP` reaches
`OnChainTimelockAddKeyListFragment.verifyBackUpSeedPhraseLauncher` →
`viewModel.setKeyVerified(xfp)`. Both intermediate hops must use `setResult` + `finish`; switching
either to `startActivity` breaks verification silently.

Inside `SignerIntroActivity`, `FLAG_VERIFY_BACKUP_SEED_PHRASE` changes what each key type does:

| key type | verify-backup behaviour |
|---|---|
| Ledger | `LedgerActivity(verifyXfpOnly = true, expectedXfp = currentSigner.fingerPrint)` — creates nothing, just reports the fingerprint |
| BitBox | `BitBoxActivity(verifyXfpOnly = true, expectedXfp = …)` — same contract |
| Coldcard | straight to `openSetupMk4()` (the firmware gate is skipped) |
| Jade | straight to `handleSelectAddAirgapType(JADE)` |
| air-gap / NFC | their own add-key screens mark the key verified themselves |

Both hardware branches bail out to the normal add flow when `expectedXfp` is empty — there is
nothing to verify against. Backing out of the device screen (`resultCode != RESULT_OK`) leaves the
user on the key-type picker rather than finishing the flow.

The overflow menu on every screen offers **Restart wizard** (`RestartWizardUseCase` +
`membershipStepManager.restart()`) and **Exit wizard**.

---

## 6. Flow D — Replace key / change timelock on a live wallet

### 6.1 Entry

```
ServicesTabFragment (ReplaceKey) | GroupDashboardFragment (TargetAction.REPLACE_KEYS)
  → navigator.openMembershipActivity(groupStep = REPLACE_KEY,
        walletType = if (isOnChainWallet) MINISCRIPT else null)
      → MembershipActivity: stage == REPLACE_KEY && walletType == MINISCRIPT
          → onChainReplaceKeyIntroFragment(walletId, groupId)
```

`OnChainReplaceKeyIntroFragment` waits for `uiState.isDataLoaded`: if
`pendingReplaceXfps.isNotEmpty()` a replacement is already in progress and it skips straight to
`onChainReplaceKeysFragment`; otherwise it shows the intro once. Both fragments share
`OnChainReplaceKeysViewModel` via `activityViewModels()`, and the intro pops itself off the back
stack (`popUpToInclusive`).

### 6.2 Building the list — `OnChainReplaceKeysViewModel`

1. `GetWalletDetail2UseCase(walletId)` → `walletSigners` (excluding `SERVER`), `addressType`.
2. `SyncGroupWalletUseCase(groupId)` or `GetServerWalletUseCase(walletId)` →
   `buildAddReplaceKeySteps(wallet.signers, wallet.timelock)`:
   * `getReplaceKeySteps(isGroupWallet).toAddReplaceKeyOnChainDataList()` gives the empty cards;
   * each server signer is placed by `signer.index.toOnChainReplaceKeyStep(...)` into
     `originalSigners`, and `card.fingerPrint` = slot 0's xfp (**this xfp is the card's identity for
     the whole replace flow**);
   * the `SERVER_KEY` card is marked `APP_VERIFIED`, the `TIMELOCK` card gets `originalTimelock`.
3. `GetReplaceWalletStatusUseCase(groupId, walletId)` (also re-run on `PushEvent.ReplaceKeyChange`,
   `onResume`, and pull-to-refresh) merges the server's pending state:
   `status.replacements[xfp]` → `stepDataMap` (new keys), `status.timelock` → `newTimelock`,
   `status.pendingReplaceXfps`, `verifiedSigners`, and TapSigner replacements missing
   `userBackUpFileName` → `missingBackupKeys`.

Cards render `originalSigners` until a replacement exists, then the new signers with the same
Acct X / Acct Y chips as the creation flow.

### 6.3 Replacing

```
onReplaceClicked(card) → handleOnAddKey(data)
    viewModel.setCurrentStep(data.getNextStepToAdd() ?: data.type)
    viewModel.setReplacingXfp(data.fingerPrint)          // saved as REPLACE_XFP
    viewModel.initReplaceKey()                           // InitReplaceKeyUseCase(groupId, walletId, xfp)
    → handleHardwareKeyAdd(data)   // only for INHERITANCE_KEY*/HARDWARE_KEY* steps
```

`handleHardwareKeyAdd` mirrors the creation flow one-for-one, with `replaceInfo` attached to every
`OnChainAddSignerParam` and `replacedXfp` on every `AddAirSignerArgs` / `SetupMk4Args` /
`NfcSetupActivity` intent:

* slot 0 empty, inheritance step → `inheritanceKeyIntroFragment(ON_CHAIN, param)`;
* slot 0 empty, hardware step → `SignerIntroActivity` (`FLAG_ADD_SIGNER`, `keyIndex = 0`);
* slot 0 filled, TapSigner → `handleTapSignerAcct1Addition`;
* slot 0 filled, otherwise → `handleSignerIndexCheck` → `handleSignerNewIndex` (reuse) or
  `HandleSignerTypeLogic` (re-read the device).

Each new key is committed with `ReplaceKeyUseCase(groupId, walletId, xfp = REPLACE_XFP, signer,
keyIndex = getCurrentStep().toIndex(isGroupWallet))`, then the status is re-fetched.

> **On-chain replace has no desktop path.** `handleSignerTypeLogic`'s `HARDWARE` branch routes
> `LEDGER`, `TREZOR`, `BITBOX`, `COLDCARD` and `JADE` all to `openRequestAddDesktopKey`, which here
> is just an `NCInfoDialog(nc_info_hardware_key_not_supported)`. Only NFC/Coldcard-NFC/air-gap keys —
> and hardware keys **already present locally** (offered through the bottom sheet) — can actually be
> used. The nav graph does declare an `addDesktopKeyFragment` action, but nothing navigates to it.

Other actions on the screen:

| action | effect |
|---|---|
| **Change** on the timelock card | `onChainSetUpTimelockFragment(isReplaceKeyFlow = true, walletId, timelockExtra from newTimelock ?: originalTimelock)` |
| **Verify** | same three-way split as creation (`openCreateBackUpTapSigner` / `openVerifyBackupTapSigner` / `BackUpSeedPhraseActivity` with `replacedXfp`) |
| **Remove** (per card, with confirm dialog) | `RemoveKeyReplacementUseCase(groupId, walletId, xfp)` then clear that card's `stepDataMap` |
| overflow → **Cancel key replacement** (with confirm dialog) | `ResetReplaceKeyUseCase(groupId, walletId)` |
| `UpdateSignerTag` event | `customKeyAccountFragmentFragment(signer, groupId, walletId)` |

### 6.4 Finalizing

`isEnableContinueButton()` = at least one replacement or a new timelock **and** every touched card
has all its steps complete **and** every inheritance / TapSigner replacement is verified.

```
onCreateWallet() → FinalizeReplaceKeyUseCase(groupId, walletId)
                 → SyncPersonalWallets / SyncGroupWalletsUseCase
                 → result.requiresRegistration
                      ? OpenUploadConfigurationScreen(newWalletId)   // navigator, isOnChainFlow = true
                      : createWalletSuccess → createWalletSuccessFragment(walletId, replacedWalletId)
```

---

## 7. Flow E — Switching timelock type (on-chain ⇄ off-chain)

Launched by opening `MembershipActivity` with `MembershipArgs.CHANGE_TIMELOCK_FLOW` set, which forces
the start destination to `inheritancePlanTypeFragment`; its Continue goes to `changeTimeLockFragment`
carrying `walletId`, `groupId`, `slug`, `walletType`, `isPersonal`, `setupPreference` and
`changeTimelockFlow`.

`changeTimelockFlow` selects the copy and illustration: **`0` = change to on-chain**, **`1` = change
to off-chain** (`nc_change_to_{on,off}_chain_timelock_*` strings, three numbered steps and a hint with
a *Read more* link — the click handler is currently empty).

`ChangeTimelockViewModel.onContinueClicked()` → `ChangeTimelockTypeUseCase(groupId, walletId)`
→ on success the returned `DraftWallet` is pushed into the host activity
(`setOnChainReplaceWalletId(draftWallet.replaceWallet?.localId)`, `setGroupId(draftWallet.groupId)`)
and the flow continues at `introAssistedWalletFragment` — i.e. the switch is performed by creating a
new draft wallet, not by mutating the existing one.

`AddKeyStepViewModel` also uses this flag: with `changeTimelockFlow == 1` the key step opens
`inheritancePlanTypeFragment` instead of the normal key list.

---

## 8. The firmware gate — `checkfirmware/CheckFirmwareViewModel`

The Compose screen lives in `membership/signer/CheckFirmwareNavigation.kt`
(`CheckFirmwareDestination(signerTagName, walletId, groupId)`); `SignerIntroActivity` navigates to it
for **Coldcard and Jade only**, and only when an `OnChainAddSignerParam` is present that is not a
verify-backup and not an off-chain inheritance add.

The view model:

* reads the required version from `GetUserWalletConfigsSetupFromCacheUseCase(...).miniscriptSupportedFirmwares`
  matched on the signer tag — miniscript policies need recent firmware;
* lists candidate local signers: matching type/tag **and** derivation index 0, minus
  `param.existingSigners` (compared on fingerprint + derivation path) and keeping only
  `isRecommendedMultiSigPath`;
* on Continue emits `ShowFilteredSigners` (→ `SelectSignerBottomSheet`: reuse an existing key or add
  a new one) or `OpenNextScreen` (→ `openSetupMk4()` / `openAddAirSignerForJade()`).
  A verify-backup param always goes straight to `OpenNextScreen`.

---

## 9. Invariants — read this before adding a key type or a screen

1. **Every hardware slot is two accounts of one device.** Anything that adds a key here must thread
   `keyIndex` / `accountIndex` (= `getAllSigners().size`) and `expectedXfp` /
   `currentSigner` (= slot 0) through. Dropping either lets the user pair two different devices into
   one card.
2. **Four dispatchers must stay in sync.** A new key type has to be added to
   `OnChainTimelockAddKeyListFragment.handleSignerTypeLogic` + `openInAppHardwareOrDesktopFlow`,
   `OnChainReplaceKeysFragment.handleSignerTypeLogic`, and `SignerIntroActivity`'s `KeyType` switch.
   A missing branch is a silent no-op (`else -> {}`), not a crash.
3. **In-app vs desktop differs per flow.** Creation: Ledger and BitBox pair in-app, Trezor /
   Coldcard-over-USB / Jade go to the desktop-app screen. Replace: *no* hardware key can be added
   fresh — everything hits the "not supported" dialog.
4. **Verification results travel by activity result.** `SignerIntroActivity` →
   `BackUpSeedPhraseActivity` → key-list launcher, all via `setResult` + `finish` and
   `EXTRA_VERIFIED_XFP`. Never convert a hop to `startActivity`.
5. `verifyType` starts as `NONE` for inheritance cards and TapSigner only — that is what makes the
   Verify button appear and what gates Continue.
6. **TapSigner needs the host activity.** The caching handshake goes through
   `MembershipActivity.setTapSignerCachingCallback` / `requestTapSignerCaching`; both key-list
   fragments register it in `onViewCreated` and clear it in `onDestroyView`. A screen that hosts
   TapSigner adds outside `MembershipActivity` has no way to top up the xpub.
7. Derivation paths are built by hand (`m/48h/{0|1}h/{index}h/2h`) in
   `OnChainTimelockAddKeyListViewModel`, `OnChainReplaceKeysViewModel` and `CheckFirmwareViewModel`.
   Keep the three copies identical, and note the index-1 lookups are hardcoded to account 1.

### Known warts

* `OnChainReplaceKeysViewModel.getStepInfo()` returns a stub `MembershipStepInfo(step = TIMELOCK)`
  because `MembershipStepInfo` cannot express `OnChainReplaceKeyStep`. Consequently the `filePath`
  and `backUpFileName` on `OnVerifySigner` are meaningless in the replace flow.
* `ImportantNoticePassphraseFragment.handleContinueClick` hardcodes `keyIndex = 0`, so it is only
  correct for the first slot of an inheritance card.
* `AddKeyListEvent.SelectAirgapType` / `OnChainReplaceKeyEvent.SelectAirgapType` are emitted nowhere
  and handled with empty bodies.
* Several strings on both key-list screens are hardcoded English ("Pull to refresh the key
  statuses.", "Before the timelock…", "Change") rather than `stringResource`.
* `OnChainReplaceKeysContent` always renders the personal-wallet copy ("two signatures from a 2-of-4
  multisig" / "one signature from a 1-of-3"), even for group wallets.
