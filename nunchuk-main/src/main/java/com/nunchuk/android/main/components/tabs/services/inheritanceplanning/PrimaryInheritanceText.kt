package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.main.R

/*
 * Copy for an on-chain plan's primary inheritance route, shared by Review your plan, Share your
 * secrets and How it works. Every count comes from the plan's M-of-N, never a fixed number.
 */

/** "Primary inheritance · any 2 of 3 keys" — the heading above the route's key cards. */
@Composable
internal fun primaryInheritanceHeading(info: PrimaryInheritanceInfo): String = when {
    info.isSingleKey -> stringResource(R.string.nc_primary_inheritance_single_key)
    info.requiredSignatures == info.totalKeys ->
        stringResource(R.string.nc_primary_inheritance_all_keys, info.requiredSignatures, info.totalKeys)

    else -> stringResource(R.string.nc_primary_inheritance_any_keys, info.requiredSignatures, info.totalKeys)
}

/**
 * The sentence under the key cards on Review your plan. Only the guided default has one — which keys
 * to hand over; the heading already states the M-of-N, so it is never repeated (REV-04 / REV-10).
 */
@Composable
internal fun primaryKeysToShareSentence(info: PrimaryInheritanceInfo): String? =
    if (info.isCustomized) null
    else stringResource(R.string.nc_share_inheritance_key_and_owner_key, info.ownerKeyCount)

/** Share your secrets, item 2: which seed phrases to hand over and how many of them a claim needs. */
@Composable
internal fun onChainSharePrimaryKeys(info: PrimaryInheritanceInfo, isJointControl: Boolean): String = when {
    !info.isCustomized -> stringResource(
        if (isJointControl) R.string.nc_onchain_share_default_primary_keys_joint else R.string.nc_onchain_share_default_primary_keys,
        info.ownerKeyCount,
        info.requiredSignatures,
        info.totalKeys,
    )

    info.isSingleKey -> stringResource(
        if (isJointControl) R.string.nc_onchain_share_primary_single_key_joint else R.string.nc_onchain_share_primary_single_key
    )

    else -> {
        val needed = if (info.requiredSignatures == info.totalKeys) {
            stringResource(R.string.nc_all_of_the_keys_needed, info.totalKeys)
        } else {
            pluralStringResource(
                R.plurals.nc_any_of_the_keys_needed,
                info.requiredSignatures,
                info.requiredSignatures,
                info.totalKeys,
            )
        }
        stringResource(
            if (isJointControl) R.string.nc_onchain_share_primary_keys_joint else R.string.nc_onchain_share_primary_keys,
            inheritanceKeysList(info.totalKeys),
            needed,
        )
    }
}

/** "Inheritance keys 1, 2 and 3". */
@Composable
private fun inheritanceKeysList(count: Int): String =
    stringResource(R.string.nc_inheritance_keys_list, (1 until count).joinToString(", "), count)

/** The keys of the primary route as a phrase inside a sentence: "any 2 of the 3 primary inheritance keys". */
@Composable
internal fun primaryKeysPhrase(info: PrimaryInheritanceInfo): String = when {
    !info.isCustomized -> stringResource(R.string.nc_primary_keys_default_phrase, info.ownerKeyCount)
    info.isSingleKey -> stringResource(R.string.nc_primary_keys_single_phrase)
    info.requiredSignatures == info.totalKeys ->
        stringResource(R.string.nc_primary_keys_all_phrase, info.totalKeys)

    else -> stringResource(R.string.nc_primary_keys_any_phrase, info.requiredSignatures, info.totalKeys)
}

/** "Fallback 2 · any 2 of 3 keys" — the heading above a fallback's key cards on Review your plan. */
@Composable
internal fun fallbackInheritanceHeading(info: FallbackInheritanceInfo): String = when {
    info.isSingleKey -> stringResource(R.string.nc_fallback_inheritance_single_key, info.number)
    info.requiredSignatures == info.totalKeys ->
        stringResource(R.string.nc_fallback_inheritance_all_keys, info.number, info.requiredSignatures, info.totalKeys)

    else -> stringResource(R.string.nc_fallback_inheritance_any_keys, info.number, info.requiredSignatures, info.totalKeys)
}

/**
 * One line of the fallback list on Share your secrets (SHARE-03 / SHARE-14): "Fallback keys 2, 3 and
 * 4 · any 2 of 3 required". [keyNumbers] come from [fallbackKeyNumbers].
 */
@Composable
internal fun fallbackShareKeysLine(info: FallbackInheritanceInfo, keyNumbers: List<Int>): String {
    val keys = if (keyNumbers.size <= 1) {
        stringResource(R.string.nc_fallback_key_number, keyNumbers.firstOrNull() ?: info.number)
    } else {
        stringResource(
            R.string.nc_fallback_keys_list,
            keyNumbers.dropLast(1).joinToString(", "),
            keyNumbers.last(),
        )
    }
    val required = when {
        info.isSingleKey -> stringResource(R.string.nc_fallback_keys_required_single)
        info.requiredSignatures == info.totalKeys ->
            stringResource(R.string.nc_fallback_keys_required_all, info.requiredSignatures, info.totalKeys)

        else -> stringResource(R.string.nc_fallback_keys_required_any, info.requiredSignatures, info.totalKeys)
    }
    return stringResource(R.string.nc_fallback_share_keys_line, keys, required)
}

/** A fallback's bullet on the Inheritance keys screen (PLAN-08 / PLAN-11): "Fallback 2: any 2 of its 3 keys." */
@Composable
internal fun fallbackKeyTipRoute(info: FallbackInheritanceInfo): String = when {
    info.isSingleKey -> stringResource(R.string.nc_inheritance_key_tip_fallback_route_single, info.number)
    info.requiredSignatures == info.totalKeys -> stringResource(
        R.string.nc_inheritance_key_tip_fallback_route_all,
        info.number,
        info.requiredSignatures,
        info.totalKeys,
    )

    else -> stringResource(
        R.string.nc_inheritance_key_tip_fallback_route_any,
        info.number,
        info.requiredSignatures,
        info.totalKeys,
    )
}
