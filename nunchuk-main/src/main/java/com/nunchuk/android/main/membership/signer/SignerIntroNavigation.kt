package com.nunchuk.android.main.membership.signer

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.signer.KeyType
import com.nunchuk.android.signer.SignerIntroScreen
import com.nunchuk.android.signer.SignerIntroViewModel
import kotlinx.serialization.Serializable

@Serializable
object SignerIntroDestination

fun NavGraphBuilder.signerIntroDestination(
    viewModel: SignerIntroViewModel,
    onChainAddSignerParam: OnChainAddSignerParam?,
    onClick: (KeyType) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    composable<SignerIntroDestination> {
        SignerIntroNavigationScreen(
            viewModel = viewModel,
            onChainAddSignerParam = onChainAddSignerParam,
            onClick = onClick,
            onMoreClicked = onMoreClicked,
        )
    }
}

@Composable
private fun SignerIntroNavigationScreen(
    viewModel: SignerIntroViewModel,
    onChainAddSignerParam: OnChainAddSignerParam? = null,
    onClick: (KeyType) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    SignerIntroScreen(
        viewModel = viewModel,
        onChainAddSignerParam = onChainAddSignerParam,
        onClick = onClick,
        onMoreClicked = onMoreClicked
    )
}
