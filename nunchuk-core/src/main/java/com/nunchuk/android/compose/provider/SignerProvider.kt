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
                derivationPath = "",
                isMasterSigner = false,
                index = 10
            ),
            SignerModel(
                "123",
                "Tom’s TAPSIGNER 2",
                fingerPrint = "79EB35F4",
                derivationPath = "",
                isMasterSigner = false,
                index = 10
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
                derivationPath = "",
                isMasterSigner = false,
                index = 10
            ),
    )
)