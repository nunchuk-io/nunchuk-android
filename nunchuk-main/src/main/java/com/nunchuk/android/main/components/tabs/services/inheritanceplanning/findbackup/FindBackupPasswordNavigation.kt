package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

/**
 * @param keyIndex which inheritance key this screen is about, 1-based within the plan. It is the
 * key's own position, not a step counter: a plan whose first key has no encrypted backup shows this
 * screen once, for key 2.
 */
@Serializable
data class FindBackupPasswordRoute(val keyIndex: Int = 1)

fun NavGraphBuilder.findBackupPassword(
    onContinueClicked: (keyIndex: Int) -> Unit,
) {
    composable<FindBackupPasswordRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<FindBackupPasswordRoute>()
        val viewModel = hiltViewModel<FindBackupPasswordViewModel>()
        val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
        val uiState by activityViewModel.state.collectAsStateWithLifecycle()

        FindBackupPasswordContent(
            remainTime = remainTime,
            inheritanceKeyType = uiState.inheritanceKeyTypeAt(route.keyIndex),
            keyIndex = route.keyIndex,
            numOfKeys = uiState.inheritanceKeyCount,
        ) {
            onContinueClicked(route.keyIndex)
        }
    }
}

fun NavController.navigateToFindBackupPassword(keyIndex: Int = 1) {
    navigate(FindBackupPasswordRoute(keyIndex = keyIndex))
}
