package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy

/**
 * What an on-chain plan's primary inheritance route asks of the Beneficiary, in the shape the
 * Review / Share / How it works screens talk about it.
 *
 * Guided default: one separate inheritance key plus any of the owner keys ("the inheritance key
 * and one of your 3 owner keys"). Customized: every key in the route is an inheritance key.
 */
data class PrimaryInheritanceInfo(
    val isCustomized: Boolean,
    val requiredSignatures: Int,
    val totalKeys: Int,
    /** The keys listed one by one as "Inheritance key 1, 2, …", in wallet order. */
    val inheritanceKeyXfps: List<String>,
) {
    /** Guided default only — the owner keys the Beneficiary receives one of. */
    val ownerKeyCount: Int
        get() = if (isCustomized) 0 else totalKeys - inheritanceKeyXfps.size

    val isSingleKey: Boolean
        get() = totalKeys == 1
}

/**
 * Null when the server sends no primary route — a legacy plan, which keeps its existing copy.
 *
 * In the guided default, the separate inheritance key is the route key carrying the INHERITANCE
 * tag ([inheritanceTaggedXfps]); the others are owner keys. If no route key carries the tag this is
 * also null: guessing from list order could name an owner key as the inheritance key, and the
 * sharing instructions would then hand out enough owner keys to spend before the timelock.
 */
internal fun OnChainInheritancePolicy.toPrimaryInheritanceInfo(
    inheritanceTaggedXfps: Set<String>,
): PrimaryInheritanceInfo? {
    val primary = primary ?: return null
    val inheritanceKeyXfps = if (isCustomized) {
        primary.xfps
    } else {
        primary.xfps.filter { it in inheritanceTaggedXfps }.ifEmpty { return null }
    }
    return PrimaryInheritanceInfo(
        isCustomized = isCustomized,
        requiredSignatures = primary.requiredSignatures,
        totalKeys = primary.totalKeys,
        inheritanceKeyXfps = inheritanceKeyXfps,
    )
}
