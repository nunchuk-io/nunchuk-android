package com.nunchuk.android.main.membership.byzantine.addKey

import android.content.Context
import android.content.Intent
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.main.R
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.KeyType
import com.nunchuk.android.type.SignerType

fun getKeyOptions(context: Context,
                  isKeyHolderLimited: Boolean,
                  isStandard: Boolean,
) = if (isKeyHolderLimited) {
        listOfNotNull(
            SheetOption(
                type = SignerType.NFC.ordinal,
                label = context.getString(R.string.nc_tapsigner),
                showDivider = true
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_LEDGER,
                label = context.getString(R.string.nc_ledger)
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_SOFTWARE_KEY,
                label = context.getString(R.string.nc_signer_type_software),
            ).takeIf { isStandard },
            SheetOption(
                type = SheetOptionType.TYPE_ADD_TREZOR,
                label = context.getString(R.string.nc_trezor)
            ),
        )
    } else {
        listOfNotNull(
            SheetOption(
                type = SignerType.NFC.ordinal,
                label = context.getString(R.string.nc_tapsigner),
                showDivider = true
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_BITBOX,
                label = context.getString(R.string.nc_bitbox)
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_AIRGAP_JADE,
                label = context.getString(R.string.nc_blockstream_jade),
            ),
            SheetOption(
                type = SignerType.COLDCARD_NFC.ordinal,
                label = context.getString(R.string.nc_coldcard)
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
                label = context.getString(R.string.nc_foudation_passport),
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_AIRGAP_OTHER,
                label = context.getString(R.string.nc_signer_generic_air_gapped)
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
                label = context.getString(R.string.nc_keystone),
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_AIRGAP_KRUX,
                label = context.getString(R.string.nc_krux),
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_LEDGER,
                label = context.getString(R.string.nc_ledger)
            ),
            SheetOption(
                type = SignerType.PORTAL_NFC.ordinal,
                label = context.getString(R.string.nc_portal),
                applyTint = false
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
                label = context.getString(R.string.nc_seedsigner),
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_SOFTWARE_KEY,
                label = context.getString(R.string.nc_signer_type_software),
            ).takeIf { isStandard },
            SheetOption(
                type = SheetOptionType.TYPE_ADD_TREZOR,
                label = context.getString(R.string.nc_trezor)
            ),
        )
    }
/**
 * The option-sheet entry a key type picked on the signer intro screen stands for, read from its
 * result. The assisted key lists open that screen in place of their old option sheet and hand the
 * pick to the same `onOptionClicked` branch, so each key type keeps one membership setup path.
 */
fun Intent.pickedKeySheetOptionType(): Int? {
    val name = getStringExtra(GlobalResultKey.EXTRA_KEY_TYPE) ?: return null
    return when (KeyType.entries.firstOrNull { it.name == name } ?: return null) {
        KeyType.TAPSIGNER -> SignerType.NFC.ordinal
        KeyType.COLDCARD -> SignerType.COLDCARD_NFC.ordinal
        KeyType.PORTAL -> SignerType.PORTAL_NFC.ordinal
        KeyType.JADE -> SheetOptionType.TYPE_ADD_AIRGAP_JADE
        KeyType.SEEDSIGNER -> SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER
        KeyType.KEYSTONE -> SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE
        KeyType.FOUNDATION -> SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT
        KeyType.KRUX -> SheetOptionType.TYPE_ADD_AIRGAP_KRUX
        KeyType.GENERIC_AIRGAP -> SheetOptionType.TYPE_ADD_AIRGAP_OTHER
        KeyType.LEDGER -> SheetOptionType.TYPE_ADD_LEDGER
        KeyType.BITBOX -> SheetOptionType.TYPE_ADD_BITBOX
        KeyType.TREZOR -> SheetOptionType.TYPE_ADD_TREZOR
        KeyType.SOFTWARE -> SheetOptionType.TYPE_ADD_SOFTWARE_KEY
        KeyType.PLATFORM_KEY -> null
    }
}
