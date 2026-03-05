package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.intro

import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceSetupIntroRoute

fun NavGraphBuilder.inheritanceSetupIntro(
    onContinueClicked: () -> Unit,
) {
    composable<InheritanceSetupIntroRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val viewModel = hiltViewModel<InheritanceSetupIntroViewModel>()
        InheritanceSetupIntroScreen(
            viewModel = viewModel,
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToInheritanceSetupIntro() {
    navigate(InheritanceSetupIntroRoute)
}
