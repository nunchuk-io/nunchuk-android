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

/** The sentence under the key cards on Review your plan; null when there is nothing to add (1 of 1). */
@Composable
internal fun primaryKeysNeededSentence(info: PrimaryInheritanceInfo): String? = when {
    !info.isCustomized -> stringResource(
        R.string.nc_default_primary_keys_needed,
        info.requiredSignatures,
        info.totalKeys,
        info.ownerKeyCount,
    )

    info.isSingleKey -> null
    info.requiredSignatures == info.totalKeys ->
        stringResource(R.string.nc_all_of_these_keys_needed, info.totalKeys)

    else -> pluralStringResource(
        R.plurals.nc_any_of_these_keys_needed,
        info.requiredSignatures,
        info.requiredSignatures,
        info.totalKeys,
    )
}

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
