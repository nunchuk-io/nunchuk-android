package com.nunchuk.android.core.miniscript

import com.nunchuk.android.core.R

enum class MultisignType(
    val title: String,
    val description: String,
    val icon: Int
) {
    EXPANDING(
        "Expanding multisig",
        "Number of required signatures stays the same, but more possible signers can be added over time",
        icon = R.drawable.ic_expanding_multisign
    ),
    DECAYING(
        "Decaying multisig",
        "Total number of possible signers stays the same, but fewer signatures are needed over time",
        icon = R.drawable.ic_decaying_multisign
    ),
    FLEXIBLE(
        "Flexible multisig", "Both the number of signers and required signatures can change over time",
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