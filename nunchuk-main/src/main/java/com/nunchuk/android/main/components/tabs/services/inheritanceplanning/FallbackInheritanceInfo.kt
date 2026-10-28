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

/**
 * The numbers Share your secrets lists for each fallback's keys ("Fallback keys 2, 3 and 4"),
 * numbered across all fallbacks in order. When the server has named every key of a fallback
 * ("Fallback key 3"), those numbers are used so the screen matches Review your plan; otherwise —
 * during setup, before the plan lists its keys — they continue from the previous fallback.
 */
internal fun List<FallbackInheritanceInfo>.fallbackKeyNumbers(): List<List<Int>> {
    var next = 1
    return map { fallback ->
        val named = fallback.keys.mapNotNull { TRAILING_NUMBER.find(it.name)?.value?.toIntOrNull() }
        val numbers = if (fallback.keys.size == fallback.totalKeys && named.size == fallback.totalKeys) {
            named
        } else {
            (next until next + fallback.totalKeys).toList()
        }
        next = (numbers.maxOrNull() ?: (next - 1)) + 1
        numbers
    }
}

private val TRAILING_NUMBER = Regex("\\d+$")
