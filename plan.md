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

### Key row states (owner picks these up later)

| Condition | Status line | Action |
|---|---|---|
| `claim_options` empty | *Sharing method not set* | **Set up** → sharing-method choice directly |
| chosen, nothing verified | *Not verified* | Verify backup |
| chose both, one verified | *1 of 2 verified* | Verify backup |
| verified | *Backup uploaded · Seed shared* | ✓ Added |

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
- `KeyDistributionActivity` — "key added" + sharing-method choice, with `skipKeyAdded` for the
  key-row entry
- Key row status line + **Set up** action, both key lists
- No client-side fallback for `claim_options`: nothing from the server ⇒ empty state + toast

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
2. **Seed-phrase branch** — back up seed phrase → verify now / skip → restore-on-device steps →
   re-add the key → public keys match / do not match. The on-chain
   `membership/onchaintimelock/backupseedphrase/` covers most of it; the match / no-match screens
   are new.
3. **Encrypted-backup branch** — generic intro → vendor steps (Keystone first; Passport & Jade are
   TBD placeholders in the design) → import via QR/file → upload → verify.
4. **"Do both" checklist** — the two artifacts are verified separately. Switching back to
   seed-phrase-only deletes the encrypted backup, so that path needs a confirm sheet.
5. **Per-method verification** — `verifications[]` is modelled but nothing writes it.
   `MembershipStepInfo.verifyType` is a single value and cannot express "1 of 2".
6. **The existing backup flow still has bugs** (owner's note) — audit before extending it.

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
