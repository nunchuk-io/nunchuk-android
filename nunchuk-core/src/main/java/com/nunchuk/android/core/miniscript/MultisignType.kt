package com.nunchuk.android.core.miniscript

import com.nunchuk.android.core.R

enum class MultisignType(
    val title: String,
    val description: String,
    val icon: Int
) {
    EXPANDING(
        "Expanding multisig",
        "Keeps the required keys unchanged but adds more keys over time",
        icon = R.drawable.ic_expanding_multisign
    ),
    DECAYING(
        "Decaying multisig",
        "Keeps the total number of keys unchanged but lowers the required keys over time",
        icon = R.drawable.ic_decaying_multisign
    ),
    FLEXIBLE(
        "Flexible multisig", "Freely change the key set over time",
        icon = R.drawable.ic_flexible_multisign
    ),
    ZEN_HODL(
        "Zen Hodl",
        "Locks coins immediately when deposited and only allows spending after the timelock expires",
        icon = R.drawable.ic_zen_hodl
    ),
    CUSTOM(
        "Enter miniscript",
        "Paste or enter your miniscript",
        icon = R.drawable.ic_enter_miniscript
    ),
    IMPORT(
        "Import from file",
        "Import miniscript from a file",
        icon = R.drawable.ic_import
    ),
}