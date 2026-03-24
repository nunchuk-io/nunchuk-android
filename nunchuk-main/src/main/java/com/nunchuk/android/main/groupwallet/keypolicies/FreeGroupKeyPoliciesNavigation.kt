package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.groupwallet.FreeGroupWalletViewModel
import com.nunchuk.android.model.GroupSandbox
import kotlinx.serialization.Serializable

@Serializable
data object FreeGroupKeyPoliciesRoute

fun NavGraphBuilder.freeGroupKeyPolicies(
    onBackClicked: () -> Unit = {},
    onSaveSuccess: (GroupSandbox) -> Unit = {},
) {
    composable<FreeGroupKeyPoliciesRoute> {
        val activity = LocalContext.current as ComponentActivity
        val activityViewModel: FreeGroupWalletViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val activityState by activityViewModel.uiState.collectAsStateWithLifecycle()

        val signers = activityState.signers
            .filterIndexed { index, _ ->
                index != (activityState.group?.platformKeyIndex ?: -1)
            }
            .filterNotNull()

        val platformKeyPolicies = activityState.group?.platformKey?.policies

        FreeGroupKeyPoliciesScreen(
            groupId = activityViewModel.groupId,
            signers = signers,
            platformKeyPolicies = platformKeyPolicies,
            onBackClicked = onBackClicked,
            onSaveSuccess = { groupSandbox ->
                onSaveSuccess(groupSandbox)
                onBackClicked()
            },
        )
    }
}

fun NavController.navigateToFreeGroupKeyPolicies() {
    navigate(FreeGroupKeyPoliciesRoute)
}
