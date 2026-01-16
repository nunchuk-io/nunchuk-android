package com.nunchuk.android.main.membership.signer

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.signer.KeyType
import com.nunchuk.android.signer.SignerIntroScreen
import com.nunchuk.android.signer.SignerIntroViewModel
import kotlinx.serialization.Serializable

@Serializable
object SignerIntroDestination

fun NavGraphBuilder.signerIntroDestination(
    viewModel: SignerIntroViewModel,
    keyFlow: Int,
    supportedSigners: List<SupportedSigner>,
    onChainAddSignerParam: OnChainAddSignerParam?,
    onClick: (KeyType) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    composable<SignerIntroDestination> {
        SignerIntroNavigationScreen(
            viewModel = viewModel,
            keyFlow = keyFlow,
            supportedSigners = supportedSigners,
            onChainAddSignerParam = onChainAddSignerParam,
            onClick = onClick,
            onMoreClicked = onMoreClicked,
        )
    }
}

@Composable
private fun SignerIntroNavigationScreen(
    viewModel: SignerIntroViewModel,
    keyFlow: Int = 0,
    supportedSigners: List<SupportedSigner> = emptyList(),
    onChainAddSignerParam: OnChainAddSignerParam? = null,
    onClick: (KeyType) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    SignerIntroScreen(
        keyFlow = keyFlow,
        supportedSigners = supportedSigners,
        viewModel = viewModel,
        onChainAddSignerParam = onChainAddSignerParam,
        onClick = onClick,
        onMoreClicked = onMoreClicked
    )
}
