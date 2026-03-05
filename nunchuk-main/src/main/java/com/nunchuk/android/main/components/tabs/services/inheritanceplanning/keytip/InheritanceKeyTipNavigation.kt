package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceKeyTipRoute

fun NavGraphBuilder.inheritanceKeyTip(
    onContinueClicked: () -> Unit,
) {
    composable<InheritanceKeyTipRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val sharedState by activityViewModel.state.collectAsStateWithLifecycle()
        InheritanceKeyTipContent(
            remainTime = remainTime,
            numberOfKey = sharedState.keyTypes.size,
            isMiniscriptWallet = sharedState.isMiniscriptWallet,
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToInheritanceKeyTip() { navigate(InheritanceKeyTipRoute) }
