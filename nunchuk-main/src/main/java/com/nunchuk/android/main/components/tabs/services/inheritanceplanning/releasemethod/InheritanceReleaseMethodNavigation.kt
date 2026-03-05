package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod

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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.toReleaseMethodOption
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceReleaseMethodRoute

internal fun NavGraphBuilder.inheritanceReleaseMethod(
    onBackClicked: () -> Unit,
    onContinueClicked: (InheritanceReleaseMethod) -> Unit,
) {
    composable<InheritanceReleaseMethodRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val setupOrReviewParam = activityViewModel.setupOrReviewParam
        InheritanceReleaseMethodScreen(
            remainTime = remainTime,
            selectedMethod = setupOrReviewParam.releaseMethodType.toReleaseMethodOption(),
            onBackClicked = onBackClicked,
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToInheritanceReleaseMethod() { navigate(InheritanceReleaseMethodRoute) }
