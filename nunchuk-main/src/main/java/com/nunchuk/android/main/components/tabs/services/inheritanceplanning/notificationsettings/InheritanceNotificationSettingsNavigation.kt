package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings

import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.model.inheritance.EmailNotificationSettings
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceNotificationSettingsRoute(val isUpdateRequest: Boolean = false)

fun NavGraphBuilder.inheritanceNotificationSettings(
    onContinueClick: (isUpdateRequest: Boolean, List<EmailNotificationSettings>, Boolean) -> Unit,
) {
    composable<InheritanceNotificationSettingsRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceNotificationSettingsRoute>()
        InheritanceNotificationSettingsScreen(
            isUpdateRequest = route.isUpdateRequest,
            inheritanceViewModel = activityViewModel,
            membershipStepManager = activity.membershipStepManager,
            onContinueClick = { emailSettings, emailMeWalletConfig ->
                onContinueClick(route.isUpdateRequest, emailSettings, emailMeWalletConfig)
            },
        )
    }
}

fun NavController.navigateToInheritanceNotificationSettings(isUpdateRequest: Boolean = false) {
    navigate(InheritanceNotificationSettingsRoute(isUpdateRequest = isUpdateRequest))
}
