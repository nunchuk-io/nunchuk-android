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

**Both ways in are metered — budget them.** MCP counts tool calls **per month** (6 or 20 depending on
seat) and says so plainly when exhausted. The REST PAT in `.mcp.json` is the one to prefer, but it is
not unlimited either: `/v1/files/.../nodes` and `/v1/images` sit behind the API paywall
(`x-figma-rate-limit-type: low`, `x-figma-plan-tier: pro`) and return `429` with a `retry-after` in
the **tens of thousands of seconds** — 13.7 h on 2026-09-14. `/v1/me` keeps working, so a 200 there
only proves the token is valid, not that you have budget. The limit is per account, so minting a
fresh PAT does not reset it.

```
curl -H "X-Figma-Token: $TOKEN" \
  "https://api.figma.com/v1/files/c1J2lsV2uVgb8XBtQa4m03/nodes?ids=1:2"
# check the budget before assuming a fetch will land:
curl -sD - -o /dev/null -H "X-Figma-Token: $TOKEN" \
  "https://api.figma.com/v1/files/c1J2lsV2uVgb8XBtQa4m03/nodes?ids=1:2&depth=1" | grep -i retry-after
```

So **fetch the whole page once and keep the dump**, rather than pulling frame by frame. When the
budget is gone, a screenshot pasted into the conversation is the fastest substitute for everything
except asset export.

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
- **The re-add picker is scoped to the wallet type it is building.** The server advertises an
  inheritance entry per wallet type, so an unscoped picker lists the same device once for each —
  Coldcard, Jade, Ledger and BitBox each appeared twice. `BackUpSeedPhraseArgs.walletType` carries
  it: `MULTI_SIG` off-chain, `MINISCRIPT` from the on-chain timelock and replace flows. The four
  add-key pickers already passed it; only the re-add ones had no way to.
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

- **"Share your secrets" follows the sharing method** (post-plan setup, `inheritanceplanning/`). The
  step used to assume exactly two secrets — Magic Phrase + Backup Password — which is wrong as soon
  as the key can reach the Beneficiary by two routes. The list is now driven by the inheritance
  key's `claim_options`, read off the wallet's server signers in `InheritancePlanningViewModel`
  (`inheritanceClaimOptions`, the **union** across keys: every route any key uses has to be shared):
  - The screen is **two cards in every case** — Secret 1 the Magic Phrase, Secret 2 the inheritance
    key with a bullet per route — which is what the 2026-09-15 frames
    (`Downloads/inheritance/Post 02–05`) draw for Direct, Indirect and Joint alike. The routes are
    not secrets of their own, so the title stays "these two secrets" however many there are, and the
    old numbered `NCLabelWithIndex` list and its "three secrets" titles are gone.
  - seed phrase only → Magic Phrase + seed phrase backup, with the "no encrypted backup exists"
    warning; encrypted backup only → the same two cards, one bullet; both → two bullets in Secret 2.
  - `toInheritanceKeyRoutes()` (`sharesecretinfo/InheritanceSharedSecrets.kt`) is the one place that
    reads an **empty** `claim_options` as `ENCRYPTED_BACKUP`: a legacy plan predates the choice and
    always had one, so empty must not read as "nothing to share". On-chain never reaches it.
  - Joint control is the same two cards, plus the grey note under Secret 2 when the key has both
    routes: they unlock the same key, so handing one to each party would give both a working copy
    and leave the Magic Phrase unmatched. The note is the only joint-only piece — the grouping
    itself is now how every party type renders, so there is no `SEED_PHRASE in routes` branch left
    in `InheritanceShareSecretInfoContent` and joint + backup-only no longer falls back to a
    numbered list. Card icons: Secret 2's `key-dark` turned out to be the existing `ic_key` scaled
    24→20 (same path, same `#031F2B`), so only Secret 1's `security-answer-distribution` needed
    importing — monochrome, tinted by `NcIcon`, so no dark-mode plate to remap.
  - Multi-beneficiary is **not** redrawn: its per-beneficiary card keeps the numbered list, because
    the frames never covered that flow. With both routes it therefore still reads as three numbered
    items inside one beneficiary card, which contradicts the two-secret framing — raise it with
    design before changing it.
  - "Learn more" is now two destinations: the Backup Password screen (Keystone + a generic "Other
    devices" added — it listed only TAPSIGNER and COLDCARD, which no longer covers the hardware, and
    its copy was hard-coded English) and a new seed-phrase screen (`seedphrasebackupinfo/`).
  - **With both methods the explanation is one two-step sequence** — Backup Password (**Continue**)
    → seed phrase (**Got it**) — reached from the single **Info** on the review plan's inheritance
    key card. `InheritanceBackUpDownloadRoute.continueToSeedPhrase` picks the CTA, and step 2 pops
    `popBackStack<InheritanceBackUpDownloadRoute>(inclusive = true)` so Got it closes the pair rather
    than landing back on step 1. The share-secrets list keeps a separate "Learn more" per item, so
    each of those still opens its one screen. Step 2 also swaps its closing note for the
    joint-control one and its sharing advice for the passphrase case — the seed-only screen keeps
    its own wording for both; checked against the frames, they genuinely differ.
  - **Review your plan** lists the same secrets for the same reason — it was showing a Backup
    Password card for plans that have no encrypted backup. With **both** methods it collapses to a
    single **Inheritance key (XFP: …)** card, because the two methods unlock the same key and are
    one thing to hand over; seed-only shows the same card with the seed phrase description; and
    backup-only keeps today's Backup Password card. All three carry the **Info** link, and
    `onInheritanceKeyInfoClick(routes)` picks its destination: both → the two-step pair, seed only →
    the seed phrase screen, backup only → the Backup Password screen.

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
- Ledger step 1 copy. Krux and Jade each gained their "Export XPUB from <device>" third step,
  gated to this flow — `SignerTag.exportXpubStepRes()` in `AirgapIntroFragment`, one line per
  device. Steps 1 and 2 stay the generic copy, which is what the Krux and Jade frames show.

---

## 5. Not done

1. **Screen 16 has no QR import — deferred by the owner.** The design draws QR / file / Desktop;
   the shared screen offers file and Desktop. The scanner this app has (`ScanDynamicQRActivity` →
   `parsePassportSigners`) decodes air-gapped *signers* and hands back a `SingleSigner`, i.e.
   xpubs; nothing turns a scanned QR into raw bytes, which is what an encrypted backup is. Whoever
   picks this up has to settle what the QR carries first — uploading the scanned xpub as the
   "backup" would leave the Beneficiary unable to claim, since it is public data and no Backup
   Password protects it. Coldcard's import screen has never offered QR either, so this is a gap in
   the shared flow rather than a Keystone one.
2. **"Verify the backup via the app" is Coldcard-only.** It decrypts with
   `nunchukNativeSdk.verifyColdCardBackup`; another vendor's backup would fail it for the wrong
   reason, so the option is filtered out. Needs native SDK support to come back.
3. **The existing backup flow still has bugs** (owner's note) — audit before extending it.

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
- **`inheritanceClaimOptions` and `walletType` share a race.** `InheritancePlanningViewModel` fills
  both from `getWallet`, a plain network call with no cache read first. Until it lands, `claimOptions`
  is empty (reads as a legacy backup-only plan) and `walletType` is `MULTI_SIG` (reads as off-chain).
  The review plan can be the first screen of the flow, so a seed-only plan briefly shows the Backup
  Password card — and stays wrong if the call fails. Pre-existing for `walletType`; the new field
  joins it. Fix with one "loaded" flag covering both, not a nullable on one field.
- **`m/48h` message signing** is unconfirmed for BitBox, Trezor and Krux (Krux signs over SD card
  only). An inheritance key that cannot sign at the wallet's `m/48h` path leaves the Beneficiary
  unable to claim. The server list is the throttle: drop a device there rather than in the client.
