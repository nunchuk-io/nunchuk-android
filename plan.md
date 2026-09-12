# Off-chain inheritance key (BYOH) — plan & progress

**Branch:** `NUN-10192-off-chain-inheritance-byoh`

Lets the owner put the inheritance key of an **off-chain timelock (assisted / `MULTI_SIG`) wallet** on
any supported hardware device — "bring your own hardware" — instead of only TAPSIGNER/COLDCARD, and
choose how that key reaches their Beneficiary.

**Scope guard:** off-chain only. The on-chain timelock flow
(`membership/onchaintimelock/`, `WalletType.MINISCRIPT`) must not change behaviour. Every new branch
is gated behind `OnChainAddSignerParam.FLAG_ADD_INHERITANCE_OFF_CHAIN_SIGNER`, which on-chain never
passes. The claim flow is a separate scope and is untouched.

---

## 1. Sources of truth

| What | Where |
|---|---|
| Design | Figma `NDS – Inheritance BYOH`, file key `c1J2lsV2uVgb8XBtQa4m03`, page node `1:2` |
| API | Jira **NUN-10192** — "BE - Support seed phrase and encrypted backup options for off-chain inheritance" |
| Discussion | Slack `C017CLBP6P3`, thread_ts `1788944391.059549` |

### Reading the Figma reliably

The design is numbered `01`…`20` **and the numbering has already changed once** — do not hard-code
screen numbers into code comments, describe the screen instead.

Two traps when extracting it:

1. **Honour the `visible` flag.** Hidden layers still carry text. A text dump that ignores
   `visible: false` reported a third Ledger step that the designer had hidden, and a stale note.
2. **Frames beat notes.** The note *"Add-key copy — source of truth"* still says *"Jade uses the
   generic air-gapped copy"* and *"Step counts: Ledger 3, everything else 2"*. Both are out of date:
   the Ledger frames hide step 3 (so Ledger is 2 steps) and the Jade/Krux frames have 3 visible
   steps. Trust the frames.

MCP access is rate-limited hard (Figma counts tool calls **per month** — 6 or 20 depending on seat).
Prefer the REST API with the PAT already in `.mcp.json`:

```
curl -H "X-Figma-Token: $TOKEN" \
  "https://api.figma.com/v1/files/c1J2lsV2uVgb8XBtQa4m03/nodes?ids=1:2"
```

---

## 2. Flow as implemented

```
key list  ──Add on the inheritance slot──►  inheritance intro
                                              │
                                              ▼
                                         passphrase notice
                                              │
                                              ▼
                                     key-type picker (server-driven)
                                              │
                                              ▼
                                   "add an existing key?" sheet
                                        │            │
                                  reuse │            │ add new
                                        │            ▼
                                        │      method choice (BT / USB / desktop)
                                        │            │
                                        │            ▼
                                        │      device guide  ──►  connect / scan
                                        │            │
                                        └────────────┴──►  key saved
                                                            │
                                                            ▼
                                              "Inheritance key added"
                                                            │
                                                            ▼
                                             sharing-method choice  ──►  PUT claim-options
```

Re-entering the key list never reopens this by itself; the key row carries the entry point.

**No device runs its backup at add time.** Adding the key only adds the key; the sharing method is
chosen afterwards and decides which artifact is made. Coldcard used to be pushed straight from the
picker into its passphrase question and encrypted backup — it is now added like any other key
(`keyTypesOwnedByKeyList` in `SignerIntroActivity`). TAPSIGNER is the one exception left: the server
registers it through the upload of its encrypted backup, so it still backs up first (see §5).

### Key row states

| Condition | Status line | Action | Opens |
|---|---|---|---|
| `claim_options` empty | *Sharing method not set* | **Set up** | sharing-method choice |
| chosen, nothing verified | *Not verified* | **Backup** | the chosen branch |
| chose both, one verified | *1 of 2 verified* | **Verify backup** | the checklist |
| verified | *Backup uploaded · Seed shared* | ✓ Added | — |

The action dispatches on `claim_options`, not on the device
(`AddKeyData.inheritanceBackupBranch()`): seed phrase → `BackUpSeedPhraseActivity`, encrypted backup
→ the device's own backup flow, both → the checklist.

---

## 3. API (NUN-10192) — deployed on testnet

Gated by `x-nc-os-name` + `x-nc-app-version` **≥ 2.8.6**. Older versions get the legacy payload.

**`GET /v1.1/user-wallets/configs/setup`** — `supported_signers[]` gains:
- `claim_options`: `["SEED_PHRASE"]` and/or `["ENCRYPTED_BACKUP"]` — what the device supports
- `claim_note`: display-only sentence explaining a limitation

Today the server advertises 13 `MULTI_SIG` inheritance entries. Only TAPSIGNER, COLDCARD (3 forms)
and Keystone support `ENCRYPTED_BACKUP`; everything else is `SEED_PHRASE` only. **SeedSigner is not
advertised at all**, so it is absent from the picker without any client-side rule.

**Draft / replacement signers** gain `claim_options` and `verifications[]`
(`verification_method` + `verification_type`). `[]` means legacy / not configured.

**`PUT .../{xfp}/claim-options`** — new, four variants (draft & replacement × personal & group).
No `BOTH` value: "do both" is both entries. Empty lists rejected. Dropping `ENCRYPTED_BACKUP`
**deletes the uploaded backup server-side and cannot be undone** — confirm in the app first.

**`POST .../{xfp}/verify`** — now takes `verification_method`, plus `key_checksum` for the encrypted
backup.

---

## 4. Done

**Data layer**
- `ClaimOption`, `InheritanceKeyVerification` (`nunchuk-domain/model/inheritance/`)
- `claim_options` / `claim_note` on `SupportedSignerConfig`; `claim_options` / `verifications` on
  `SignerServerDto` → `SignerServer`
- `ClaimOptionsRequest`; `verification_method` on `KeyVerifiedRequest`
- 4 × `PUT .../claim-options` in `UserWalletsApi` / `GroupWalletApi`
- `KeyRepository.setInheritanceClaimOptions` + `SetInheritanceClaimOptionsUseCase`

**UI / flow**
- Picker is server-driven (`is_inheritance_key` + `MULTI_SIG`); the static list in
  `SignerIntroViewModel.offChainInheritanceSetupKeyTypes` is only a fallback for a legacy response
- Inheritance intro + passphrase notice inside `SignerIntroActivity`, before the picker
- Existing-key sheet after the key type is picked
- `KeyDistributionActivity` — "key added" + sharing-method choice + the "do both" checklist,
  entered at any of the three by `KeyDistributionEntry`
- Key row status line + **Set up** action, both key lists
- No client-side fallback for `claim_options`: nothing from the server ⇒ empty state + toast
- **Backup/Verify on the key row dispatches on the sharing method.** `inheritanceBackupBranch()` on
  `AddKeyData`; the row's completeness (`isRowComplete`, `needsClaimVerification`) now comes from
  `claim_options` + `verifications[]` rather than the single local `verifyType`, which goes green
  after one artifact and would hide the second half of a "do both" key.
- **Branch (a), seed phrase** — reuses `BackUpSeedPhraseActivity` (11 → 12 → 13 → re-add → 13b/13c).
  Ledger/BitBox hand back `EXTRA_VERIFIED_XFP` and the key list records it; Coldcard and air-gap
  record themselves inside their own screens.
- **13b / 13c.** `InheritanceSeedPhraseVerified` names the key whose backup was proven;
  `BackUpSeedPhraseVerifyMismatch` shows the derived XFP against the expected one, with **Try
  again** and **Back to seed phrase steps**. The mismatch was silent before — air-gap hands the
  re-added key back and nobody looked at it — so the activity now answers it itself rather than
  relaying it, which fixes the on-chain flow at the same time. What each device hands back differs
  — Ledger/BitBox return only a verified fingerprint, Coldcard and air-gap compare themselves and
  mark a match on their own screens, **TAPSIGNER compares nothing and returns whatever it read** —
  so `BackUpSeedPhraseActivity` does the comparison rather than reading a returned key as proof of
  failure. In practice 13c shows for air-gap and TAPSIGNER; Coldcard and Ledger/BitBox report
  their own mismatch inline and never return.
- **The seed-phrase flow moved out of `onchaintimelock/`** into `membership/backupseedphrase/`: it
  now serves the on-chain timelock, on-chain replace and off-chain inheritance flows alike.
  Which confirmation screen a caller wants is explicit in `BackUpSeedPhraseType`
  (`SUCCESS` vs `INHERITANCE_VERIFIED`), chosen through `BackUpSeedPhraseArgs.verified()`.
- **The backup upload no longer re-adds the key.** `uploadBackupKey` asks the server to add the key
  when `isRequestAddKey` is set, which is how a TAPSIGNER used to get onto the draft. The key is
  now added before the sharing method is chosen, so that request came back
  `400 Duplicate key xfp`. `UploadBackUpTapSignerFragment` sets the flag from
  `NfcSetupActivity.claimOption`: non-null means the off-chain inheritance backup, which by
  construction runs after the key is on the draft. Coldcard already did this — `Mk4Activity` sets
  `isRequestAddOrReplaceKey = action != UPLOAD_BACKUP` — so this only brings TAPSIGNER in line.
- **The key row's action and its colour come from one rule** (`AddKeyData.isRowComplete`). Reading
  the local `verifyType` for the tick while the card colour read the server's `verifications[]`
  left a finished, green inheritance row still offering "Verify backup" whenever the local step
  lagged the server.
- **Both key lists share one set of entry points** (`honey/distribution/InheritanceBackupNavigation.kt`)
  instead of mirroring the same navigation methods each.
- **The branch opens as soon as the sharing method is saved**, for every device — no key backs up
  before the method is chosen any more. "Do both" goes to the checklist in place; a single option
  is handed back to the key list, which owns the step state those flows need.
- **Continue on the key list is blocked until the inheritance key is settled**
  (`AddKeyData.isInheritanceIncomplete`): a sharing method recorded, and each chosen method
  verified or deliberately skipped.
- **Skipping a verification is recorded**, so it settles the artifact rather than leaving the row
  asking forever. It already was on the seed-phrase branch; the encrypted one now writes it too
  (`TapSignerVerifyBackUpOptionViewModel`, new `ColdCardVerifyBackUpOptionViewModel`).
- **The encrypted-backup branch is the flow Coldcard already had.** `openVerifyColdCard` sends
  every non-TAPSIGNER key into `Mk4Activity` with `ColdcardAction.UPLOAD_BACKUP`, which is screens
  15 → 18 end to end: intro, on-device steps, import, upload, verify (by app / myself / skip).
  Keystone needed no new screens, only the vendor to travel with the request —
  `SetupMk4Args.signerTag`, from `SignerModel.backupVendorTag` — so the copy names the right
  device (`nc_back_up_device`, `nc_backing_up_device`, `nc_encrypt_backup_follow_on_device`,
  `nc_recovered_key_on_device`), the on-device steps are Keystone's own, and **verify via the app
  is hidden for anything but Coldcard**: that check runs `verifyColdCardBackup`, which decrypts
  Coldcard's format only.
- **Branch (c), "do both"** — `VerifyBackupsContent` (12c/12c-ii) inside `KeyDistributionActivity`,
  with "Change how you share this key" → the distribution choice, and `RemoveEncryptedBackupSheet`
  (12c-iii) confirming the one irreversible downgrade. The checklist only *chooses* which half to
  verify and hands it back to the key list, which owns the step state those flows need.
- **Per-method verification is written, on both halves.** `verification_method` travels through
  `SetKeyVerifiedUseCase` → `KeyRepository.setKeyVerified`. Two carriers, because the two branches
  reach different screens and none of them know what a claim option is:
  - seed phrase → `BackUpSeedPhraseArgs.claimOption` → `OnChainAddSignerParam.claimOption`, read by
    `Mk4IntroFragment`, `ColdcardRecoverFragment` and `AddAirgapSignerFragment`
  - encrypted backup → `AddKeyData.encryptedBackupClaimOption()` on `OnVerifySigner` →
    `SetupMk4Args.claimOption` / `NfcSetupActivity.claimOption`, read by the four verify view
    models (`CheckBackUpByApp`, `CheckBackUpBySelf`, `ColdCardVerifyRecoveredKey`,
    `ColdCardVerifyBackupViaApp`)

  Null everywhere else, so on-chain and legacy plans keep the single verification the server
  infers.
- **No device backs up at add time any more.** The off-chain inheritance picker adds every key
  type itself and hands back the signer, so the owner always sees "Inheritance key added" and the
  sharing-method choice first; the backup runs only if they asked for one. Coldcard needed only to
  leave `keyTypesOwnedByKeyList` (now gone entirely); TAPSIGNER needed the key derived from the
  card without the upload that used to register it — `TapSignerKeyResolver`, shared by both key
  lists, which asks for a card tap when the xpub at the wallet's path is not cached
  (`MembershipActivity.requestTapSignerCaching`). A TAPSIGNER row with no backup yet opens
  create-backup instead of verify, the same way Coldcard already chose between `UPLOAD_BACKUP`
  and `VERIFY_KEY`.
- **`missingBackupKeys` no longer blocks Continue for a BYOH key.** The old rule flagged every
  non-NFC inheritance key with no `userKeyFileName`, i.e. every device that has no encrypted backup
  to make, which disabled Continue forever. It now only applies once the owner has actually asked
  for an encrypted backup (legacy plans, which record no `claim_options`, keep the old rule).

**Fixes made along the way**
- `HeaderProviderImpl.getAppVersion()` strips the build-type suffix. `versionNameSuffix = ".DEV"`
  made every debug build send `2.8.5.DEV`, which the server cannot parse, so it **silently** served
  the legacy payload — this affects *every* version-gated feature, not just this one.
  `versionName` bumped 2.8.5 → 2.8.6 in the same change.
- `filterSignerByType` used `type == x || tags.contains(tag)`; picking BitBox offered a Trezor.
  The tag now narrows the match except for COLDCARD, where it legitimately widens it.
- Insets: inheritance intro, passphrase notice and the air-gap guide had their CTA inside a
  `verticalScroll` column with `Spacer(weight(1f))` — the weight resolves to zero there, so the
  button sat under the nav bar and long content pushed the title off-screen. All three now use
  `Scaffold(bottomBar)` + `navigationBarsPadding()`.
- Assets re-exported for Ledger + Trezor. Their `#D0E2FF` plate is mapped back to
  `@color/nc_fill_denim` — the converter hard-codes it and dark mode breaks otherwise. BitBox was
  **not** re-exported (design only flagged a copy change there).
- Ledger step 1 copy; Krux gained its "Export XPUB from Krux" step (gated to this flow).

---

## 5. Not done

1. **Jade guide** — the design gives Jade its own three steps (`Initialize Jade` / `Unlock Jade` /
   `Export XPUB from Jade`). The code currently forces Jade onto the generic two-step air-gap
   screen, which was correct for the older design and is now wrong. Fix the same way Krux was done
   (`AirgapIntroFragment`, gated to off-chain inheritance), and drop the override in
   `AddAirgapSignerActivity` that keeps Jade off `airgapActionIntroFragment`.
2. **Screen 16 has no QR import — deferred by the owner.** The design draws QR / file / Desktop;
   the shared screen offers file and Desktop. The scanner this app has (`ScanDynamicQRActivity` →
   `parsePassportSigners`) decodes air-gapped *signers* and hands back a `SingleSigner`, i.e.
   xpubs; nothing turns a scanned QR into raw bytes, which is what an encrypted backup is. Whoever
   picks this up has to settle what the QR carries first — uploading the scanned xpub as the
   "backup" would leave the Beneficiary unable to claim, since it is public data and no Backup
   Password protects it. Coldcard's import screen has never offered QR either, so this is a gap in
   the shared flow rather than a Keystone one.
3. **The on-chain re-add picker is not scoped to a wallet type.** `openReAddKeyForVerification`
   now passes `BackUpSeedPhraseArgs.walletType`, and the off-chain flow sets `MULTI_SIG`; the
   on-chain callers still leave it null, so their picker keeps listing a vendor once per wallet
   type the server advertises it for. Left alone deliberately — on-chain is out of scope — but it
   is the same bug, fixed by passing `MINISCRIPT` from those two call sites.
4. **"Verify the backup via the app" is Coldcard-only.** It decrypts with
   `nunchukNativeSdk.verifyColdCardBackup`; another vendor's backup would fail it for the wrong
   reason, so the option is filtered out. Needs native SDK support to come back.
5. **The existing backup flow still has bugs** (owner's note) — audit before extending it.

---

## 6. Traps

- **The draft wallet arrives late.** A key is saved while the add-key screen is still on top; the
  draft carrying `claim_options` lands afterwards. Anything that infers "a key was just added" from
  comparing state across recomputations is wrong — the first recomputation has no signer yet, so
  entering the screen looks identical to adding a key. Use the explicit
  `onInheritanceKeyAdded()` signal, raised where the key is saved; it waits for the data.
- **One-shot events get dropped.** `flowObserver` collects at `STARTED`, and the key list is
  STOPPED while the add-key screen is up, so a `MutableSharedFlow` emission is lost and the screen
  only appears after the user resumes the app by hand. Anything raised during an add must live in
  the `StateFlow`.
- **A fresh draft has `signers: []`.** Identify the inheritance slot from the membership step
  (`type.isAddInheritanceKey`); use the draft only to enrich it with `claim_options` /
  `verifications`.
- **`AirgapIntroFragment`, `ImportantNoticePassphraseContent` and `LedgerInstructionScreen` are
  shared** with the ordinary add-key and on-chain flows. Copy and layout fixes were applied
  globally; behavioural additions (Krux's third step) are gated to this flow.
- **Four dispatchers must agree** on a key type — see §9 of
  `membership/onchaintimelock/ONCHAIN_TIMELOCK_FLOWS.md`. A missing branch is a silent no-op, not a
  crash: Ledger/Trezor/BitBox hand back `EXTRA_SIGNER_TAG` while everything else hands back
  `EXTRA_SIGNER`, and reading only one of them loses the key with no error.

---

## 7. Open

- **Ledger/BitBox screen order.** The design note reads *"device guides after method selection"*,
  which is what ships today (method choice → guide). Confirmed to keep as-is; revisit only if
  design says otherwise.
- **`versionName` 2.8.5 → 2.8.6** is in this branch. It is a release-management change riding along
  with a feature; drop it if the team bumps versions separately — but the feature cannot be tested
  without it.
- **Passport / Jade encrypted backup** is TBD in the design, and the Keystone vendor copy is marked
  *"Exact steps to be finalized with Keystone"*.
- **Legacy plans and the "Set up" row.** `needsClaimOptions` is true whenever the slot is an
  inheritance key with no `claim_options`, and a legacy plan never returns any. An already-added,
  already-verified inheritance key on such a plan therefore shows *Sharing method not set* + **Set
  up** where the verified tick used to be. That matches the design's "sharing method not set"
  state, but it is a visible regression for existing wallets — decide whether a legacy plan should
  keep its verified row instead.
- **`m/48h` message signing** is unconfirmed for BitBox, Trezor and Krux (Krux signs over SD card
  only). An inheritance key that cannot sign at the wallet's `m/48h` path leaves the Beneficiary
  unable to claim. The server list is the throttle: drop a device there rather than in the client.
