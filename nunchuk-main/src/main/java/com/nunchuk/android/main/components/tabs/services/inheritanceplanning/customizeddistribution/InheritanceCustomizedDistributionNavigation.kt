package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution

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
data object InheritanceCustomizedDistributionRoute

internal fun NavGraphBuilder.inheritanceCustomizedDistribution(
    onContinueClicked: (BeneficiaryType) -> Unit,
) {
    composable<InheritanceCustomizedDistributionRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        InheritanceCustomizedDistributionScreen(
            remainTime = remainTime,
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToInheritanceCustomizedDistribution() {
    navigate(InheritanceCustomizedDistributionRoute)
}
