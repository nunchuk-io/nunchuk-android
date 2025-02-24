package com.nunchuk.android.compose.provider

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.nunchuk.android.core.signer.SignerModel

class SignersModelProvider : CollectionPreviewParameterProvider<List<SignerModel>>(
    listOf(
        listOf(
            SignerModel(
                "123",
                "Tom’s TAPSIGNER",
                fingerPrint = "79EB35F4",
                derivationPath = "84h/0h/0h/0h/0h",
                isMasterSigner = false,
                index = 10
            ),
            SignerModel(
                "234",
                "Tom’s TAPSIGNER 2",
                fingerPrint = "79EB35F5",
                derivationPath = "84h/0h/0h/0h/0h",
                isMasterSigner = false,
                index = 10,
                isVisible = false
            ),
            SignerModel(
                "345",
                "Tom’s TAPSIGNER 3",
                fingerPrint = "79EB35F6",
                derivationPath = "84h/0h/0h/0h/0h",
                isMasterSigner = false,
                index = 3,
                isVisible = false
            ),
        )
    )
)

class SignerModelProvider : CollectionPreviewParameterProvider<SignerModel>(
    listOf(
            SignerModel(
                "123",
                "Tom’s TAPSIGNER",
                fingerPrint = "79EB35F4",
                derivationPath = "m/44'/0'/0'/0/0",
                isMasterSigner = false,
                index = 10
            ),
    )
)