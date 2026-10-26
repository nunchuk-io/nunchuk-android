package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import com.nunchuk.android.model.InheritanceKey
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy

/**
 * One fallback of an on-chain plan, as Review your plan lists it (REV-01 / REV-05 / REV-06): its
 * M-of-N, the keys the server marks for it and its own unlock time.
 */
data class FallbackInheritanceInfo(
    /** Zero-based, as the server keys it (`notify_fallbacks`, `fallback_index`). */
    val index: Int,
    val requiredSignatures: Int,
    val totalKeys: Int,
    /** The keys `inheritance_keys` lists for this fallback (section FALLBACK), in server order. */
    val keys: List<InheritanceKey>,
    val timelock: WalletTimelock?,
) {
    /** "Fallback 1": what the screens call it. */
    val number: Int
        get() = index + 1

    val isSingleKey: Boolean
        get() = totalKeys == 1
}

/**
 * Empty for a legacy plan. Each fallback's keys come only from `inheritance_keys` (section FALLBACK
 * with a matching `fallback_index`); nothing is inferred from wallet signers.
 */
internal fun OnChainInheritancePolicy.toFallbackInheritanceInfos(
    planKeys: List<InheritanceKey>,
): List<FallbackInheritanceInfo> {
    if (!isConfigurable) return emptyList()
    return fallbacks.sortedBy { it.index }.map { fallback ->
        FallbackInheritanceInfo(
            index = fallback.index,
            requiredSignatures = fallback.m,
            totalKeys = fallback.n,
            keys = planKeys.filter {
                it.section == InheritanceSection.FALLBACK && it.fallbackIndex == fallback.index
            },
            timelock = fallback.timelock,
        )
    }
}
