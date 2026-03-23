package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.core.signer.SignerModel
import kotlinx.serialization.Serializable

@Serializable
data object FreeGroupKeyPoliciesRoute

fun NavGraphBuilder.freeGroupKeyPolicies(
    signers: List<SignerModel> = emptyList(),
    onBackClicked: () -> Unit = {},
) {
    composable<FreeGroupKeyPoliciesRoute> {
        FreeGroupKeyPoliciesScreen(
            signers = signers,
            onBackClicked = onBackClicked,
        )
    }
}

fun NavController.navigateToFreeGroupKeyPolicies() {
    navigate(FreeGroupKeyPoliciesRoute)
}
