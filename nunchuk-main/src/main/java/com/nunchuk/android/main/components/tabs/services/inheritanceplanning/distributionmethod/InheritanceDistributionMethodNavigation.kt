package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceDistributionMethodRoute

internal fun NavGraphBuilder.inheritanceDistributionMethod(
    onContinueClicked: (InheritanceDistributionMethod) -> Unit,
) {
    composable<InheritanceDistributionMethodRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        InheritanceDistributionMethodScreen(
            remainTime = remainTime,
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToInheritanceDistributionMethod() {
    navigate(InheritanceDistributionMethodRoute)
}
