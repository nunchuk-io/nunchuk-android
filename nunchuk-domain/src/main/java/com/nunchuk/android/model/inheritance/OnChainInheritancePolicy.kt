package com.nunchuk.android.model.inheritance

import android.os.Parcelable
import com.nunchuk.android.model.WalletTimelock
import kotlinx.parcelize.Parcelize

/**
 * `onchain_inheritance_policy` of an on-chain (miniscript) wallet, draft or plan (NUN-10320).
 *
 * Holds the conditions only — which keys belong to each one comes from the draft's cards or the
 * plan's `inheritance_keys` ([com.nunchuk.android.model.InheritanceKey.section]).
 *
 * "Fallback" here is a later inheritance condition; unrelated to [InheritancePlanFallbackPolicy]
 * (multi-beneficiary redistribution) and the BSMS "fallback option".
 */
@Parcelize
data class OnChainInheritancePolicy(
    val type: OnChainInheritancePolicyType,
    val primary: PrimaryInheritanceCondition?,
    val fallbacks: List<FallbackInheritanceCondition> = emptyList(),
) : Parcelable {
    val isConfigurable: Boolean
        get() = type == OnChainInheritancePolicyType.CONFIGURABLE
}

enum class OnChainInheritancePolicyType {
    /** An existing plan, including the HB 1-of-3 inheritance. */
    LEGACY,

    /** Primary / fallback editing and backend key verification are enabled. */
    CONFIGURABLE,
}

/**
 * Any [m] of [n] keys after [timelock]. [isCustomized] is false for the guided default (HB: the 3
 * owner keys plus one separate inheritance key, 2 of 4). [timelock] may be unset during setup.
 */
@Parcelize
data class PrimaryInheritanceCondition(
    val m: Int,
    val n: Int,
    val isCustomized: Boolean,
    val timelock: WalletTimelock?,
) : Parcelable

/** Zero-based [index] is the edit/delete target; show `index + 1` ("Fallback 1"). */
@Parcelize
data class FallbackInheritanceCondition(
    val index: Int,
    val m: Int,
    val n: Int,
    val timelock: WalletTimelock?,
) : Parcelable

/** Where a card, key or validation issue sits: before the timelock, primary inheritance, or a fallback. */
enum class InheritanceSection {
    SPENDING,
    PRIMARY,
    FALLBACK,
}
