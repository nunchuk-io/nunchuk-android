package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceReviewPlanGroupRoute

fun NavGraphBuilder.inheritanceReviewPlanGroup() {
    composable<InheritanceReviewPlanGroupRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val viewModel = hiltViewModel<InheritanceReviewPlanGroupViewModel>()
        val groupId = activityViewModel.state.value.groupId
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(Unit) {
            viewModel.init(activityViewModel.setupOrReviewParam)
        }

        LaunchedEffect(viewModel, lifecycleOwner) {
            viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is InheritanceReviewPlanGroupEvent.OnContinue -> {
                            activity.setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(GlobalResultKey.DUMMY_TX_ID, event.dummyTransactionId)
                                putExtra(
                                    GlobalResultKey.REQUIRED_SIGNATURES,
                                    event.requiredSignatures.requiredSignatures,
                                )
                            })
                            activity.finish()
                        }
                        is InheritanceReviewPlanGroupEvent.Loading -> activity.showOrHideLoading(event.loading)
                        is InheritanceReviewPlanGroupEvent.ProcessFailure -> NCToastMessage(activity).showError(event.message)
                        InheritanceReviewPlanGroupEvent.CancelInheritanceSuccess -> Unit
                        InheritanceReviewPlanGroupEvent.CreateOrUpdateInheritanceSuccess -> Unit
                    }
                }
        }

        InheritanceReviewPlanGroupScreen(
            viewModel = viewModel,
            sharedViewModel = activityViewModel,
            groupId = groupId,
        )
    }
}
