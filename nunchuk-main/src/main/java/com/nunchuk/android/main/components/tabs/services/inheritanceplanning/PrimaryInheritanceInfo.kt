package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import com.nunchuk.android.model.InheritanceKey
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy

/**
 * What an on-chain plan's primary inheritance asks of the Beneficiary, in the shape the Review /
 * Share / How it works screens talk about it.
 *
 * Guided default: the separate inheritance key plus any of the owner keys ("the inheritance key and
 * one of your 3 owner keys"). Customized: every primary key is an inheritance key.
 */
data class PrimaryInheritanceInfo(
    val isCustomized: Boolean,
    val requiredSignatures: Int,
    val totalKeys: Int,
    /** The keys the server marks for primary inheritance, listed one by one, in server order. */
    val inheritanceKeys: List<InheritanceKey>,
) {
    /** Guided default only — the owner keys the Beneficiary receives one of. */
    val ownerKeyCount: Int
        get() = if (isCustomized) 0 else totalKeys - inheritanceKeys.size

    val isSingleKey: Boolean
        get() = totalKeys == 1
}

/**
 * Null — so the screens keep the legacy copy — unless the plan is configurable and the server lists
 * its primary inheritance keys ([InheritanceSection.PRIMARY] in `inheritance_keys`). The keys are
 * never inferred from wallet signers or list order: naming an owner key as the inheritance key
 * would have the owner hand out enough owner keys to spend before the timelock.
 */
internal fun OnChainInheritancePolicy.toPrimaryInheritanceInfo(
    planKeys: List<InheritanceKey>,
): PrimaryInheritanceInfo? {
    val policy = toPrimaryInheritancePolicyInfo() ?: return null
    val primaryKeys = planKeys.filter { it.section == InheritanceSection.PRIMARY }
    if (primaryKeys.isEmpty()) return null
    return PrimaryInheritanceInfo(
        isCustomized = policy.isCustomized,
        requiredSignatures = policy.requiredSignatures,
        totalKeys = policy.totalKeys,
        inheritanceKeys = primaryKeys,
    )
}

/**
 * The primary route's M-of-N as the wallet's policy states it, for the plan setup screens that run
 * before the plan lists its keys (Plan overview, Inheritance keys, About the timelock). Names no key.
 */
data class PrimaryInheritancePolicyInfo(
    val isCustomized: Boolean,
    val requiredSignatures: Int,
    val totalKeys: Int,
) {
    /** Guided default only: every key but the one separate inheritance key is an owner key. */
    val ownerKeyCount: Int
        get() = if (isCustomized) 0 else (totalKeys - 1).coerceAtLeast(0)

    val isSingleKey: Boolean
        get() = totalKeys == 1

    /** Seed phrases handed over: the guided default shares the inheritance key and one owner key. */
    val sharedSeedPhraseCount: Int
        get() = if (isCustomized) totalKeys else 2
}

/** Null for a legacy plan (or one with no primary condition), which keeps the legacy copy. */
internal fun OnChainInheritancePolicy.toPrimaryInheritancePolicyInfo(): PrimaryInheritancePolicyInfo? {
    if (!isConfigurable) return null
    val primary = primary ?: return null
    return PrimaryInheritancePolicyInfo(
        isCustomized = primary.isCustomized,
        requiredSignatures = primary.m,
        totalKeys = primary.n,
    )
}
