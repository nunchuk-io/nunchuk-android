package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod

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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.toBufferPeriodMethodOption
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceBufferPeriodMethodRoute(
    val fromBeneficiarySchedules: Boolean = false,
    val beneficiaryEmail: String = "",
)

internal fun NavGraphBuilder.inheritanceBufferPeriodMethod(
    onContinueClicked: (BufferPeriodMethodOption, Boolean, String) -> Unit,
) {
    composable<InheritanceBufferPeriodMethodRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceBufferPeriodMethodRoute>()
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val selectedOption = activityViewModel.setupOrReviewParam.bufferPeriodApplyType
            ?.toBufferPeriodMethodOption()
            ?: BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY
        InheritanceBufferPeriodMethodScreen(
            remainTime = remainTime,
            selectedOption = selectedOption,
            onBackClicked = { activity.onBackPressedDispatcher.onBackPressed() },
            onContinueClicked = { option ->
                onContinueClicked(option, route.fromBeneficiarySchedules, route.beneficiaryEmail)
            },
        )
    }
}

fun NavController.navigateToInheritanceBufferPeriodMethod(
    fromBeneficiarySchedules: Boolean = false,
    beneficiaryEmail: String = "",
) {
    navigate(
        InheritanceBufferPeriodMethodRoute(
            fromBeneficiarySchedules = fromBeneficiarySchedules,
            beneficiaryEmail = beneficiaryEmail,
        )
    )
}
