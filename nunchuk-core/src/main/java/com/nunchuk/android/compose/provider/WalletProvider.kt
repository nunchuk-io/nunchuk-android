package com.nunchuk.android.compose.provider

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType

class WalletExtendedListProvider : CollectionPreviewParameterProvider<List<WalletExtended>>(
    listOf(
        listOf(
            WalletExtended(
                wallet = Wallet(
                    id = "h0tj2hzp",
                    name = "555AndroidHoney",
                    totalRequireSigns = 2,
                    signers = listOf(
                        SingleSigner(
                            name = "TAPSIGNER #2",
                            xpub = "xpub6BemYiVNp19a2WVjnfsSs9f1hS2QSHknazBuUHYem11NPHZ4qHXbAxEaS5BQVWiB49XbotxC2Jrkz32ooJtHQtWTHrdZT7xPfSkYZzFsCcD",
                            publicKey = "",
                            derivationPath = "m/48h/0h/0h",
                            masterFingerprint = "56a80fae",
                            lastHealthCheck = 0,
                            masterSignerId = "56a80fae",
                            used = false,
                            type = SignerType.NFC,
                            hasMasterSigner = true,
                            descriptor = "[56a80fae/48'/0'/0']xpub6BemYiVNp19a2WVjnfsSs9f1hS2QSHknazBuUHYem11NPHZ4qHXbAxEaS5BQVWiB49XbotxC2Jrkz32ooJtHQtWTHrdZT7xPfSkYZzFsCcD",
                            tags = listOf()
                        )
                    ),
                    addressType = AddressType.NATIVE_SEGWIT,
                    escrow = false,
                    balance = Amount(value = 22992849, formattedValue = "0.22992849"),
                    createDate = 0,
                    description = "",
                    gapLimit = 20
                ), isShared = false, roomWallet = null
            )
        )
    )
)

class WalletExtendedProvider : CollectionPreviewParameterProvider<WalletExtended>(
    listOf(
        WalletExtended(
            wallet = Wallet(
                id = "h0tj2hzp",
                name = "555AndroidHoney",
                totalRequireSigns = 2,
                signers = listOf(
                    SingleSigner(
                        name = "TAPSIGNER #2",
                        xpub = "xpub6BemYiVNp19a2WVjnfsSs9f1hS2QSHknazBuUHYem11NPHZ4qHXbAxEaS5BQVWiB49XbotxC2Jrkz32ooJtHQtWTHrdZT7xPfSkYZzFsCcD",
                        publicKey = "",
                        derivationPath = "m/48h/0h/0h",
                        masterFingerprint = "56a80fae",
                        lastHealthCheck = 0,
                        masterSignerId = "56a80fae",
                        used = false,
                        type = SignerType.NFC,
                        hasMasterSigner = true,
                        descriptor = "[56a80fae/48'/0'/0']xpub6BemYiVNp19a2WVjnfsSs9f1hS2QSHknazBuUHYem11NPHZ4qHXbAxEaS5BQVWiB49XbotxC2Jrkz32ooJtHQtWTHrdZT7xPfSkYZzFsCcD",
                        tags = listOf()
                    )
                ),
                addressType = AddressType.NATIVE_SEGWIT,
                escrow = false,
                balance = Amount(value = 22992849, formattedValue = "0.22992849"),
                createDate = 0,
                description = "",
                gapLimit = 20
            ), isShared = false, roomWallet = null
        )
    )
)