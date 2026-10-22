package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.howitworks

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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretType
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceHowItWorksRoute(val type: Int)

fun NavGraphBuilder.inheritanceHowItWorks(
    onDoneClick: () -> Unit,
) {
    composable<InheritanceHowItWorksRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceHowItWorksRoute>()
        val activityViewModel: InheritancePlanningViewModel = hiltViewModel(viewModelStoreOwner = activity)
        val sharedUiState by activityViewModel.state.collectAsStateWithLifecycle()
        InheritanceHowItWorksScreen(
            type = InheritanceShareSecretType.entries.getOrElse(route.type) {
                InheritanceShareSecretType.DIRECT
            },
            primaryInheritance = sharedUiState.primaryInheritance,
            onDoneClick = onDoneClick,
        )
    }
}

fun NavController.navigateToInheritanceHowItWorks(type: Int) {
    navigate(InheritanceHowItWorksRoute(type = type))
}
