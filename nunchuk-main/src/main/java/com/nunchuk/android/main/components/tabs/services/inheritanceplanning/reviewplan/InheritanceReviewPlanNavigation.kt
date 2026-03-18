package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.model.Period
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceReviewPlanRoute

fun NavGraphBuilder.inheritanceReviewPlan(
    navigator: NunchukNavigator,
    onCreateOrUpdateSuccess: () -> Unit,
    onCancelSuccess: () -> Unit,
    onMarkSetupInheritance: () -> Unit,
    onEditActivationDateClick: (Long) -> Unit,
    onEditNoteClick: (String) -> Unit,
    onNotifyPrefClick: () -> Unit,
    onDiscardChange: () -> Unit,
    onShareSecretClicked: () -> Unit,
    onViewClaimingInstruction: () -> Unit,
    onEditBufferPeriodClick: (Period?) -> Unit,
    onBackUpPasswordInfoClick: () -> Unit,
    onEditAssetAllocationClick: () -> Unit,
    onEditReleaseMethodClick: () -> Unit,
    onEditBeneficiarySchedulesClick: () -> Unit,
    onEditFallbackSettingsClick: () -> Unit,
) {
    composable<InheritanceReviewPlanRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val viewModel = hiltViewModel<InheritanceReviewPlanViewModel>()
        val lifecycleOwner = LocalLifecycleOwner.current
        val releaseScheduleUiState =
            activityViewModel.setupOrReviewParam.sharedScheduleConfig?.releaseScheduleUiState
                ?: com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState()

        DisposableEffect(activity, viewModel) {
            activity.setBottomSheetOptionListener { option ->
                if (option.type == SheetOptionType.TYPE_CANCEL) {
                    viewModel.calculateRequiredSignatures(InheritanceReviewPlanViewModel.ReviewFlow.CANCEL)
                }
            }
            onDispose { activity.setBottomSheetOptionListener(null) }
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val isDoLater = data.getBoolean(GlobalResultKey.DUMMY_TX_INTRO_DO_LATER, false)
                if (isDoLater) {
                    activity.finish()
                } else {
                    val signatureMap = data.serializable<HashMap<String, String>>(GlobalResultKey.SIGNATURE_EXTRA)
                        ?: return@rememberLauncherForActivityResult
                    val securityQuestionToken =
                        data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                    if (signatureMap.isNotEmpty() || securityQuestionToken.isNotEmpty()) {
                        viewModel.handleFlow(signatureMap, securityQuestionToken)
                    } else if (activityViewModel.setupOrReviewParam.groupId.isNotEmpty()) {
                        viewModel.markSetupInheritance()
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.init(activityViewModel.setupOrReviewParam)
        }

        LaunchedEffect(viewModel, lifecycleOwner) {
            viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess -> {
                            navigator.openWalletAuthentication(
                                walletId = event.walletId,
                                userData = event.userData,
                                requiredSignatures = event.requiredSignatures,
                                type = event.type,
                                groupId = activityViewModel.setupOrReviewParam.groupId,
                                dummyTransactionId = event.dummyTransactionId,
                                launcher = launcher,
                                activityContext = activity,
                            )
                            if (event.dummyTransactionId.isNotEmpty() &&
                                activityViewModel.setupOrReviewParam.planFlow == com.nunchuk.android.core.util.InheritancePlanFlow.VIEW
                            ) {
                                activity.finish()
                            }
                        }
                        is InheritanceReviewPlanEvent.CreateOrUpdateInheritanceSuccess,
                        is InheritanceReviewPlanEvent.CancelInheritanceSuccess,
                        InheritanceReviewPlanEvent.MarkSetupInheritance -> {
                            handleReviewFlow(
                                activity = activity,
                                viewModel = viewModel,
                                activityViewModel = activityViewModel,
                                onCreateOrUpdateSuccess = onCreateOrUpdateSuccess,
                                onCancelSuccess = onCancelSuccess,
                                onMarkSetupInheritance = onMarkSetupInheritance,
                            )
                        }
                        is InheritanceReviewPlanEvent.Loading -> activity.showOrHideLoading(event.loading)
                        is InheritanceReviewPlanEvent.ProcessFailure -> NCToastMessage(activity).showError(event.message)
                    }
                }
        }

        InheritanceReviewPlanScreen(
            viewModel = viewModel,
            inheritanceViewModel = activityViewModel,
            releaseScheduleUiState = releaseScheduleUiState,
            onEditActivationDateClick = onEditActivationDateClick,
            onEditNoteClick = onEditNoteClick,
            onNotifyPrefClick = { _, _ -> onNotifyPrefClick() },
            onDiscardChange = onDiscardChange,
            onShareSecretClicked = onShareSecretClicked,
            onActionTopBarClick = {
                if (activityViewModel.setupOrReviewParam.planFlow == com.nunchuk.android.core.util.InheritancePlanFlow.VIEW) {
                    showReviewActionOptions(activity)
                }
            },
            onViewClaimingInstruction = onViewClaimingInstruction,
            onEditBufferPeriodClick = onEditBufferPeriodClick,
            onBackUpPasswordInfoClick = onBackUpPasswordInfoClick,
            onEditAssetAllocationClick = onEditAssetAllocationClick,
            onEditReleaseMethodClick = onEditReleaseMethodClick,
            onEditBeneficiarySchedulesClick = onEditBeneficiarySchedulesClick,
            onEditFallbackSettingsClick = onEditFallbackSettingsClick,
        )
    }
}

private fun showReviewActionOptions(activity: InheritancePlanningActivity) {
    (activity.supportFragmentManager.findFragmentByTag("BottomSheetOption") as? androidx.fragment.app.DialogFragment)?.dismiss()
    val dialog = BottomSheetOption.newInstance(
        listOf(
            SheetOption(
                SheetOptionType.TYPE_CANCEL,
                R.drawable.ic_close_red,
                R.string.nc_cancel_inheritance_plan,
                isDeleted = true,
            ),
        )
    )
    dialog.show(activity.supportFragmentManager, "BottomSheetOption")
}

private fun handleReviewFlow(
    activity: InheritancePlanningActivity,
    viewModel: InheritanceReviewPlanViewModel,
    activityViewModel: InheritancePlanningViewModel,
    onCreateOrUpdateSuccess: () -> Unit,
    onCancelSuccess: () -> Unit,
    onMarkSetupInheritance: () -> Unit,
) {
    if (viewModel.reviewFlow == InheritanceReviewPlanViewModel.ReviewFlow.CANCEL) {
        com.nunchuk.android.core.manager.NcToastManager.scheduleShowMessage(
            message = activity.getString(R.string.nc_inheritance_plan_cancelled_notify)
        )
        activity.setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(GlobalResultKey.UPDATE_INHERITANCE, viewModel.isDataChanged())
            putExtra(GlobalResultKey.WALLET_ID, activityViewModel.setupOrReviewParam.walletId)
        })
        activity.finish()
    } else if (activityViewModel.setupOrReviewParam.planFlow == com.nunchuk.android.core.util.InheritancePlanFlow.SETUP) {
        onCreateOrUpdateSuccess()
    } else if (activityViewModel.setupOrReviewParam.planFlow == com.nunchuk.android.core.util.InheritancePlanFlow.VIEW) {
        com.nunchuk.android.core.manager.NcToastManager.scheduleShowMessage(
            message = activity.getString(R.string.nc_inheritance_plan_updated_notify)
        )
        activity.setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(GlobalResultKey.UPDATE_INHERITANCE, viewModel.isDataChanged())
            putExtra(GlobalResultKey.WALLET_ID, activityViewModel.setupOrReviewParam.walletId)
        })
        activity.finish()
    }
}

fun NavController.navigateToInheritanceReviewPlan() { navigate(InheritanceReviewPlanRoute) }
