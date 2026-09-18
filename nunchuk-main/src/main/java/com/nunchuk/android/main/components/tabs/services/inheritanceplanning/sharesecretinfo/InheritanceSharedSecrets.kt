package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretType
import com.nunchuk.android.model.inheritance.ClaimOption

/**
 * What "Share your secrets" has to list, derived from the inheritance key's claim options.
 *
 * The plan used to have exactly two secrets — the Magic Phrase and the Backup Password. With BYOH the
 * key can reach the Beneficiary by two routes, so it is still two secrets — the Magic Phrase, then the
 * inheritance key — and the routes the owner chose during wallet setup are bullets inside the second.
 *
 * The Backup Password comes first when both are chosen, which is the order the design lists them in.
 */
private val ROUTE_DISPLAY_ORDER = listOf(ClaimOption.ENCRYPTED_BACKUP, ClaimOption.SEED_PHRASE)

/**
 * The key routes to list, in display order.
 *
 * An empty list is a legacy plan (or an on-chain one), which predates the choice and always had an
 * encrypted backup — it reads as [ClaimOption.ENCRYPTED_BACKUP], not as "nothing to share".
 *
 * **Hazard:** empty also means "the wallet has not arrived yet".
 * `InheritancePlanningViewModel` fills `inheritanceClaimOptions` from `getWallet`, which is a plain
 * network call with no cache read first, so a screen that renders before the response — the review
 * plan can be the first screen of the flow — briefly reads a seed-only plan as backup-only, and stays
 * wrong if the call fails. `walletType` has the identical race (it defaults to `MULTI_SIG`, so
 * `isMiniscriptWallet` is false until the fetch lands); fix the two together with a loaded flag
 * rather than one alone.
 */
internal fun List<ClaimOption>.toInheritanceKeyRoutes(): List<ClaimOption> =
    if (isEmpty()) listOf(ClaimOption.ENCRYPTED_BACKUP)
    else ROUTE_DISPLAY_ORDER.filter { it in this }

/**
 * The header illustration, which follows the routes the key can be claimed through: the encrypted
 * backup is a password in the cloud, the seed phrase is the key written out as words, and a plan
 * that has both routes shows both.
 */
@get:DrawableRes
internal val List<ClaimOption>.shareSecretIllustrationRes: Int
    get() = when {
        ClaimOption.SEED_PHRASE !in this -> R.drawable.nc_bg_backup_password_share_secret
        ClaimOption.ENCRYPTED_BACKUP in this -> R.drawable.nc_bg_backup_password_seed_phrase_share_secret
        else -> R.drawable.nc_bg_seed_phrase_share_secret
    }

@get:StringRes
internal val ClaimOption.shareSecretLabelRes: Int
    get() = when (this) {
        ClaimOption.ENCRYPTED_BACKUP -> R.string.nc_inheritance_share_secret_info_2
        ClaimOption.SEED_PHRASE -> R.string.nc_inheritance_share_secret_seed_phrase
    }

/**
 * "Please share these two secrets with the <party>:" — the Magic Phrase, then the inheritance key.
 *
 * The count does not follow the key's routes: however many it has, they all unlock that one key, so
 * the screen lists two secrets and the routes are bullets inside the second.
 */
@StringRes
internal fun shareSecretTitleRes(type: Int): Int = when (type) {
    InheritanceShareSecretType.INDIRECT.ordinal ->
        R.string.nc_inheritance_share_secret_info_title_indirect

    InheritanceShareSecretType.JOINT_CONTROL.ordinal ->
        R.string.nc_inheritance_share_secret_info_title_joint_control

    else ->
        R.string.nc_inheritance_share_secret_info_title_direct
}

/** The party the secrets go to, for the warning that names them. */
@StringRes
internal fun shareSecretPartyRes(type: Int): Int = when (type) {
    InheritanceShareSecretType.INDIRECT.ordinal -> R.string.nc_trustee
    InheritanceShareSecretType.JOINT_CONTROL.ordinal -> R.string.nc_beneficiary_trustee
    else -> R.string.nc_beneficiary
}

/**
 * The warning under the list. The shipped copy names the Magic Phrase and the Backup Password, so it
 * only stays correct while those are the two secrets; the other cases get copy that matches what is
 * actually on screen. `null` [party] means the string takes no argument.
 */
internal data class ShareSecretWarning(@StringRes val textRes: Int, @StringRes val partyRes: Int?)

internal fun shareSecretWarning(type: Int, routes: List<ClaimOption>): ShareSecretWarning = when {
    routes == listOf(ClaimOption.SEED_PHRASE) ->
        ShareSecretWarning(R.string.nc_inheritance_share_secret_warning_seed_only, null)

    routes.size > 1 && type == InheritanceShareSecretType.JOINT_CONTROL.ordinal ->
        ShareSecretWarning(R.string.nc_inheritance_share_secret_warning_joint, null)

    routes.size > 1 ->
        ShareSecretWarning(R.string.nc_inheritance_share_secret_warning_all, shareSecretPartyRes(type))

    else ->
        ShareSecretWarning(R.string.nc_inheritance_share_secret_info_warning, shareSecretPartyRes(type))
}
