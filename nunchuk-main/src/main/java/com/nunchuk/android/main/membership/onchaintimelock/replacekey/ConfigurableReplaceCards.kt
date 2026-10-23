package com.nunchuk.android.main.membership.onchaintimelock.replacekey

import android.os.Parcelable
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.inheritance.InheritanceValidationField
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.inheritance.bip32AccountIndex
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.wallet.ReplaceWalletStatus
import com.nunchuk.android.type.SignerTag
import kotlinx.parcelize.Parcelize

/**
 * One account slot of a configurable wallet's card (NUN-10320 replacement `cards[].keys[]`), joined
 * by `key_index` to the wallet key it holds today and to the pending replacement, if any.
 *
 * @property accountIndex the account the slot uses; the replacement must supply the same one.
 */
data class ReplaceSlot(
    val keyIndex: Int,
    val accountIndex: Int,
    val section: InheritanceSection?,
    val original: SignerModel?,
    val replacement: SignerModel? = null,
    val replacementVerifyType: VerifyType = VerifyType.NONE,
) {
    val originalXfp: String get() = original?.fingerPrint.orEmpty()
}

/**
 * Every account an original key supplies to the wallet, across all cards. A replacement has to
 * supply each of them from one new key, then be verified once (REP-02 / REP-03 / REP-03b).
 */
data class ReplaceTarget(val originalXfp: String, val slots: List<ReplaceSlot>) {
    val replacements: List<SignerModel> get() = slots.mapNotNull { it.replacement }
    val isReplacing: Boolean get() = replacements.isNotEmpty()
    val nextMissingSlot: ReplaceSlot? get() = slots.firstOrNull { it.replacement == null }
    val isComplete: Boolean get() = isReplacing && nextMissingSlot == null
    val isVerified: Boolean get() = isComplete && slots.all { it.replacementVerifyType != VerifyType.NONE }

    /** The new device, once its first account is in. */
    val replacementDevice: SignerModel? get() = replacements.firstOrNull()
}

/** One device on a card: the accounts of the same original key this card holds, by account. */
data class ReplaceKeyRow(val slots: List<ReplaceSlot>) {
    val originalXfp: String get() = slots.first().originalXfp
    val original: SignerModel? get() = slots.firstNotNullOfOrNull { it.original }
    val keyIndices: Set<Int> get() = slots.map { it.keyIndex }.toSet()
}

data class ReplaceCard(val card: DraftInheritanceCard, val rows: List<ReplaceKeyRow>) {
    val section: InheritanceSection get() = card.section
    val isServerKey: Boolean get() = card.isServerKey
}

/** A timelock as the wallet has it and as the pending replacement would change it. */
data class ReplaceTimelock(val original: WalletTimelock?, val pending: WalletTimelock?) {
    val effective: WalletTimelock? get() = pending ?: original
    val isChanged: Boolean get() = pending != null
}

/**
 * "Replace key or change timelock" for a configurable on-chain wallet (REP-01): spending, primary
 * and fallback cards in the server's order, the primary and fallback timelocks, and the issues the
 * server found with the pending replacement.
 */
data class ConfigurableReplaceState(
    val policy: OnChainInheritancePolicy?,
    val cards: List<ReplaceCard>,
    val primaryTimelock: ReplaceTimelock,
    val fallbackTimelocks: Map<Int, ReplaceTimelock>,
    val validationErrors: List<InheritanceValidationError>,
) {
    val targets: Map<String, ReplaceTarget> by lazy {
        cards.flatMap { it.rows }
            .flatMap { it.slots }
            .filter { it.originalXfp.isNotEmpty() }
            .groupBy { it.originalXfp.lowercase() }
            .mapValues { (_, slots) ->
                ReplaceTarget(
                    originalXfp = slots.first().originalXfp,
                    slots = slots.distinctBy { it.keyIndex }.sortedWith(compareBy({ it.accountIndex }, { it.keyIndex })),
                )
            }
    }

    fun targetOf(row: ReplaceKeyRow): ReplaceTarget? = targets[row.originalXfp.lowercase()]

    val hasChanges: Boolean
        get() = targets.values.any { it.isReplacing } ||
            primaryTimelock.isChanged || fallbackTimelocks.values.any { it.isChanged }

    /**
     * Continue (REP design note): something changed, every key being replaced has all its accounts
     * and is verified, and the server reports no issue with the whole candidate.
     */
    val canContinue: Boolean
        get() = hasChanges && validationErrors.isEmpty() &&
            targets.values.filter { it.isReplacing }.all { it.isVerified }
}

/**
 * Where the next replacement import goes, kept across process death: a device flow can outlive the
 * app process.
 *
 * @property sameDevice the new key's device once one of its accounts is in; the rest must come off it.
 */
@Parcelize
data class ConfigurableReplaceRequest(
    val originalXfp: String,
    val keyIndex: Int,
    val accountIndex: Int,
    val isInheritance: Boolean,
    val sameDevice: SignerModel? = null,
) : Parcelable

/**
 * Joins [status]'s cards with the wallet's keys ([walletSigners], by `key_index`) and the pending
 * replacements. A slot that serves no condition and holds no key is a retained, unused account: it
 * is neither shown nor required.
 */
internal fun buildConfigurableReplaceState(
    policy: OnChainInheritancePolicy?,
    walletSigners: List<SignerServer>,
    walletTimelock: WalletTimelock?,
    status: ReplaceWalletStatus,
): ConfigurableReplaceState {
    val signerByIndex = walletSigners.associateBy { it.index }
    val replacementByIndex = status.replacements.values.flatten().associateBy { it.index }
    val cards = status.inheritanceCards.map { card ->
        if (card.isServerKey) return@map ReplaceCard(card, emptyList())
        val slots = card.keys.mapNotNull { key ->
            val original = signerByIndex[key.keyIndex]
            if (key.section == null && original == null) return@mapNotNull null
            val account = key.accountIndex
                ?: original?.derivationPath?.let(::bip32AccountIndex)
                ?: 0
            val replacement = replacementByIndex[key.keyIndex]
            ReplaceSlot(
                keyIndex = key.keyIndex,
                accountIndex = account,
                section = key.section,
                original = original?.toReplaceSigner(account),
                replacement = replacement?.toReplaceSigner(
                    replacement.derivationPath?.let(::bip32AccountIndex) ?: account
                ),
                replacementVerifyType = replacement?.verifyType ?: VerifyType.NONE,
            )
        }
        val rows = slots.groupBy { it.originalXfp.lowercase() }.values.map { ReplaceKeyRow(it) }
        ReplaceCard(card, rows)
    }
    val originalPrimary = policy?.primary?.timelock ?: walletTimelock
    return ConfigurableReplaceState(
        policy = policy,
        cards = cards,
        primaryTimelock = ReplaceTimelock(
            original = originalPrimary,
            pending = status.timelock?.takeUnless { it.isSameTime(originalPrimary) },
        ),
        fallbackTimelocks = policy?.fallbacks.orEmpty().associate { fallback ->
            val pending = status.fallbackTimelocks.getOrNull(fallback.index)
            fallback.index to ReplaceTimelock(
                original = fallback.timelock,
                pending = pending?.takeUnless { it.isSameTime(fallback.timelock) },
            )
        },
        validationErrors = status.validationErrors,
    )
}

/** A wallet key as a [SignerModel]; tags this app doesn't know are dropped rather than failing the screen. */
private fun SignerServer.toReplaceSigner(account: Int) = SignerModel(
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

/** The server echoes the effective time; it is only a change when it differs from the wallet's. */
private fun WalletTimelock.isSameTime(other: WalletTimelock?): Boolean =
    other != null && based == other.based && timelockValue == other.timelockValue && blockHeight == other.blockHeight

/** Key indices a `keys` issue circles in [section] (and, for a fallback, [fallbackIndex]). */
internal fun List<InheritanceValidationError>.highlightedKeys(section: InheritanceSection, fallbackIndex: Int?): Set<Int> =
    filter { it.section == section && it.field == InheritanceValidationField.KEYS && it.fallbackIndex == fallbackIndex }
        .flatMap { it.keyIndices }
        .toSet()

internal fun List<InheritanceValidationError>.isTimelockHighlighted(section: InheritanceSection, fallbackIndex: Int?): Boolean =
    any { it.section == section && it.field == InheritanceValidationField.TIMELOCK && it.fallbackIndex == fallbackIndex }
