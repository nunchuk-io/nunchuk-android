package com.nunchuk.android.main.membership.byzantine.addKey

import android.content.Context
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.main.R
import com.nunchuk.android.type.SignerType

fun getKeyOptions(context: Context,
                  isKeyHolderLimited: Boolean,
                  isStandard: Boolean,
                  shouldShowNewPortal: Boolean = false
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
                showDivider = false
            ),
            SheetOption(
                type = SignerType.PORTAL_NFC.ordinal,
                resId = if(shouldShowNewPortal) R.drawable.ic_new else 0,
                label = context.getString(R.string.nc_portal),
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
                type = SheetOptionType.TYPE_ADD_LEDGER,
                label = context.getString(R.string.nc_ledger)
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