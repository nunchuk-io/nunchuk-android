package com.nunchuk.android.core.miniscript

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nunchuk.android.core.R

enum class MultisignType(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val icon: Int
) {
    EXPANDING(
        R.string.nc_miniscript_expanding_multisig,
        R.string.nc_miniscript_expanding_multisig_desc,
        icon = R.drawable.ic_expanding_multisign
    ),
    DECAYING(
        R.string.nc_miniscript_decaying_multisig,
        R.string.nc_miniscript_decaying_multisig_desc,
        icon = R.drawable.ic_decaying_multisign
    ),
    FLEXIBLE(
        R.string.nc_miniscript_flexible_multisig,
        R.string.nc_miniscript_flexible_multisig_desc,
        icon = R.drawable.ic_flexible_multisign
    ),
    ZEN_HODL(
        R.string.nc_multisign_zen_hodl,
        R.string.nc_multisign_zen_hodl_desc,
        icon = R.drawable.ic_zen_hodl
    ),
    CUSTOM(
        R.string.nc_miniscript_enter_miniscript,
        R.string.nc_miniscript_enter_miniscript_desc,
        icon = R.drawable.ic_enter_miniscript
    ),
    IMPORT(
        R.string.nc_miniscript_import_from_file,
        R.string.nc_miniscript_import_from_file_desc,
        icon = R.drawable.ic_import
    ),
}
