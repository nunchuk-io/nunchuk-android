package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.groupwallet.FreeGroupWalletViewModel
import com.nunchuk.android.model.GroupSandbox
import kotlinx.serialization.Serializable

@Serializable
data class FreeGroupKeyPoliciesRoute(val walletId: String? = null)

fun NavGraphBuilder.freeGroupKeyPolicies(
    onBackClicked: () -> Unit = {},
    onSaveSuccess: (GroupSandbox) -> Unit = {},
    onUpdatePolicySuccess: () -> Unit = {},
) {
    composable<FreeGroupKeyPoliciesRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<FreeGroupKeyPoliciesRoute>()
        val walletId = route.walletId.orEmpty()
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: FreeGroupWalletViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val activityState by activityViewModel.uiState.collectAsStateWithLifecycle()

        val allSigners = activityState.signers
            .filterNotNull()

        val platformKeyPolicies = activityState.group?.platformKey?.policies

        FreeGroupKeyPoliciesScreen(
            groupId = activityViewModel.groupId,
            walletId = walletId,
            allSigners = allSigners,
            platformKeyPolicies = platformKeyPolicies,
            onBackClicked = onBackClicked,
            onSaveSuccess = { groupSandbox ->
                onSaveSuccess(groupSandbox)
                onBackClicked()
            },
            onUpdatePolicySuccess = {
                onUpdatePolicySuccess()
                onBackClicked()
            },
        )
    }
}

fun NavController.navigateToFreeGroupKeyPolicies() {
    navigate(FreeGroupKeyPoliciesRoute())
}
