package com.nunchuk.android.main.membership.onchaintimelock.addkey

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.DraftKeySlot
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.inheritance.InheritanceValidationField
import com.nunchuk.android.model.inheritance.bip32AccountIndex
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

/**
 * One account slot of a configurable on-chain draft card (NUN-10320 `cards[].keys[]`), joined to the
 * draft signer with the same `key_index`. [signer] is null while the account still has to be imported.
 *
 * @property accountIndex the account to import; null when it depends on the key the user picks.
 */
data class DraftSetupSlot(
    val keyIndex: Int,
    val accountIndex: Int?,
    val section: InheritanceSection?,
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE,
) {
    val isFilled: Boolean get() = signer != null
    val isVerified: Boolean get() = isFilled && verifyType != VerifyType.NONE
}

/**
 * A card of the configurable setup screen, in the server's order. Only the slots that serve a
 * condition are kept, plus any already-imported account: a retained, unused account
 * (`section: null`) is neither shown nor required.
 */
data class DraftSetupCard(
    val card: DraftInheritanceCard,
    val slots: List<DraftSetupSlot>,
) {
    val section: InheritanceSection get() = card.section
    val isServerKey: Boolean get() = card.isServerKey
    val isComplete: Boolean get() = slots.all { it.isFilled }
    val nextEmptySlot: DraftSetupSlot? get() = slots.firstOrNull { !it.isFilled }

    /** The device behind a spending card: every account of it comes off the same key. */
    val device: SignerModel? get() = slots.firstNotNullOfOrNull { it.signer }

    /** Where the next import of this card goes; a spending card asks the desktop app for every empty account at once. */
    fun slotToAdd(groupId: String, slot: DraftSetupSlot): DraftKeySlot = DraftKeySlot(
        groupId = groupId,
        keyIndex = slot.keyIndex,
        keyIndices = if (section == InheritanceSection.SPENDING) {
            slots.filterNot { it.isFilled }.map { it.keyIndex }
        } else {
            listOf(slot.keyIndex)
        },
        tags = card.tags,
        accountIndex = slot.accountIndex,
        accountIndexByXfp = card.accountIndexByXfp,
    )
}

/**
 * Joins the draft's [cards] with its [signers] (by key index). [localSigner] resolves the app's own
 * copy of a key by XFP, so names and device icons match the rest of the app.
 */
internal fun buildDraftSetupCards(
    cards: List<DraftInheritanceCard>,
    signers: List<SignerServer>,
    localSigner: (xfp: String) -> SignerModel? = { null },
): List<DraftSetupCard> {
    val signerByIndex = signers.associateBy { it.index }
    return cards.map { card ->
        val slots = card.keys.mapNotNull { key ->
            val server = signerByIndex[key.keyIndex]
            if (key.section == null && server == null) return@mapNotNull null
            val account = server?.derivationPath?.let(::bip32AccountIndex) ?: key.accountIndex ?: 0
            val signer = server?.let {
                val xfp = it.xfp.orEmpty()
                localSigner(xfp)?.copy(
                    derivationPath = it.derivationPath.orEmpty(),
                    index = account,
                ) ?: it.toSlotSigner(account)
            }
            DraftSetupSlot(
                keyIndex = key.keyIndex,
                accountIndex = key.accountIndex,
                section = key.section,
                signer = signer,
                verifyType = server?.verifyType ?: VerifyType.NONE,
            )
        }
        DraftSetupCard(card = card, slots = slots)
    }
}

/**
 * Keys already in this setup, one per device, that the choose-key sheet (KEY-01a) offers for a slot
 * on [card]. The Platform key, TAPSIGNER and any key already in that route are left out; keys of
 * other routes are offered, because routes may share keys. A route is every card of the same section
 * and fallback (a customized primary comes as one card per key).
 *
 * TAPSIGNER has no BIP39 seed phrase to hand over, so it is never an on-chain inheritance key,
 * primary or fallback (design, 2026-11-02); "Add key" already hides it through the server's list.
 */
internal fun List<DraftSetupCard>.chooseKeyOptions(card: DraftInheritanceCard): List<SignerModel> {
    val inThisRoute = filter { it.card.section == card.section && it.card.fallbackIndex == card.fallbackIndex }
        .flatMap { it.slots }
        .mapNotNull { it.signer?.fingerPrint?.lowercase() }
        .toSet()
    return filterNot { it.isServerKey }
        .flatMap { it.slots.mapNotNull { slot -> slot.signer } }
        .filterNot { it.type == SignerType.NFC || it.fingerPrint.lowercase() in inThisRoute }
        .distinctBy { it.fingerPrint.lowercase() }
}

/**
 * KEY-10: indices of the fallbacks holding Account 0 of [xfp] that an import of [account] into
 * [card] takes away from them. A fallback keeps Account 0 only while no primary-inheritance account
 * uses it (Note to dev 4355:18152), so once the import lands the backend drops that key from the
 * fallback. A spending card imports every empty account at once, so it takes Account 0 when one of
 * those is Account 0. A fallback import never takes another fallback's account.
 */
internal fun List<DraftSetupCard>.fallbacksLosingKey(card: DraftSetupCard, xfp: String, account: Int): Set<Int> {
    if (card.section == InheritanceSection.FALLBACK) return emptySet()
    val takesAccountZero = account == 0 ||
        (card.section == InheritanceSection.SPENDING && card.slots.any { !it.isFilled && it.accountIndex == 0 })
    if (!takesAccountZero) return emptySet()
    return filter { it.section == InheritanceSection.FALLBACK }
        .filter { fallback ->
            fallback.slots.any { slot ->
                val signer = slot.signer ?: return@any false
                signer.fingerPrint.equals(xfp, ignoreCase = true) && signer.index == 0
            }
        }
        .mapNotNull { it.card.fallbackIndex }
        .toSet()
}

/** A draft key the app has no copy of; unknown tags are dropped rather than failing the screen. */
private fun SignerServer.toSlotSigner(account: Int) = SignerModel(
    id = xfp.orEmpty(),
    name = name.orEmpty(),
    derivationPath = derivationPath.orEmpty(),
    fingerPrint = xfp.orEmpty(),
    type = type,
    tags = tags.mapNotNull { tag -> SignerTag.entries.firstOrNull { it.name == tag } },
    isVisible = isVisible,
    isMasterSigner = false,
    index = account,
    cardId = tapsigner?.cardId.orEmpty(),
    xpub = xpub.orEmpty(),
)

/**
 * Key indices a `keys` validation error circles in [section] (never on spending cards); for a
 * fallback, only those of [fallbackIndex] when it is given.
 */
internal fun List<InheritanceValidationError>.highlightedKeyIndices(
    section: InheritanceSection,
    fallbackIndex: Int? = null,
): Set<Int> =
    filter { it.isFor(section, fallbackIndex) && it.field == InheritanceValidationField.KEYS }
        .flatMap { it.keyIndices }
        .toSet()

internal fun List<InheritanceValidationError>.isTimelockHighlighted(
    section: InheritanceSection,
    fallbackIndex: Int? = null,
): Boolean =
    any { it.isFor(section, fallbackIndex) && it.field == InheritanceValidationField.TIMELOCK }

private fun InheritanceValidationError.isFor(section: InheritanceSection, fallbackIndex: Int?): Boolean =
    this.section == section && (fallbackIndex == null || this.fallbackIndex == fallbackIndex)
