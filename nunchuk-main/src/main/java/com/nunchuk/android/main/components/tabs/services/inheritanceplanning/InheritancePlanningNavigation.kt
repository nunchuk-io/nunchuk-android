package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.main.BuildConfig
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.InheritanceActivationDateRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.inheritanceActivationDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.assetallocation.inheritanceAssetAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.assetallocation.navigateToInheritanceAssetAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload.inheritanceBackUpDownload
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload.navigateToInheritanceBackUpDownload
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.InheritanceBeneficiarySchedulesRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.inheritanceBeneficiarySchedules
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.navigateToInheritanceBeneficiarySchedules
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod.InheritanceBufferPeriodRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod.inheritanceBufferPeriod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod.BufferPeriodMethodOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod.InheritanceBufferPeriodMethodRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod.inheritanceBufferPeriodMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.createsuccess.InheritanceCreateSuccessRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.createsuccess.inheritanceCreateSuccess
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution.BeneficiaryType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution.inheritanceCustomizedDistribution
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution.navigateToInheritanceCustomizedDistribution
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod.InheritanceDistributionMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod.inheritanceDistributionMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod.navigateToInheritanceDistributionMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup.FindBackupPasswordRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup.findBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.howitworks.InheritanceHowItWorksRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.howitworks.inheritanceHowItWorks
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.intro.InheritanceSetupIntroRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.intro.inheritanceSetupIntro
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip.inheritanceKeyTip
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip.navigateToInheritanceKeyTip
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase.magicalPhraseIntro
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase.navigateToMagicalPhraseIntro
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note.InheritanceNoteRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note.inheritanceNote
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings.InheritanceNotificationSettingsRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings.inheritanceNotificationSettings
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref.InheritanceNotifyPrefRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref.inheritanceNotifyPref
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.planoverview.InheritancePlanOverviewRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.planoverview.inheritancePlanOverview
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.InheritanceReleaseMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.inheritanceReleaseMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.navigateToInheritanceReleaseMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedule.inheritanceReleaseSchedule
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedule.navigateToInheritanceReleaseSchedule
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.InheritanceReleaseScheduleDetailRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.inheritanceReleaseScheduleDetail
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit.InheritanceReleaseScheduleStageEditRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit.inheritanceReleaseScheduleStageEdit
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm.InheritanceRequestPlanningConfirmRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm.inheritanceRequestPlanningConfirm
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.sent.InheritanceRequestPlanningSentSuccessRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.sent.inheritanceRequestPlanningSentSuccess
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanGroupRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.inheritanceReviewPlan
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.inheritanceReviewPlanGroup
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.navigateToInheritanceReviewPlan
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.inheritanceShareSecret
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo.InheritanceShareSecretInfoRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo.inheritanceShareSecretInfo
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.timelockinfo.inheritanceTimelockInfo
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.timelockinfo.navigateToInheritanceTimelockInfo
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.widget.NCWarningDialog

fun getInheritancePlanningStartRoute(
    @InheritancePlanFlow.InheritancePlanFlowInfo planFlow: Int,
    param: InheritancePlanningParam.SetupOrReview,
    startDestination: Int = InheritancePlanningActivity.START_DESTINATION_DEFAULT,
): Any {
    if (startDestination == InheritancePlanningActivity.START_DESTINATION_CREATE_SUCCESS) {
        return InheritanceCreateSuccessRoute(
            sourceFlow = param.sourceFlow,
            magicalPhrase = param.magicalPhrase,
            planFlow = param.planFlow,
            walletId = param.walletId,
        )
    }

    return when (planFlow) {
        InheritancePlanFlow.SETUP -> InheritanceSetupIntroRoute
        InheritancePlanFlow.VIEW -> InheritanceReviewPlanRoute
        InheritancePlanFlow.SIGN_DUMMY_TX -> InheritanceReviewPlanGroupRoute
        InheritancePlanFlow.REQUEST -> InheritanceRequestPlanningConfirmRoute(
            walletId = param.walletId,
            groupId = param.groupId,
        )
        else -> InheritanceReviewPlanRoute
    }
}

@Composable
fun InheritancePlanningGraph(
    activity: InheritancePlanningActivity,
    navigator: NunchukNavigator,
    activityViewModel: InheritancePlanningViewModel,
    @InheritancePlanFlow.InheritancePlanFlowInfo planFlow: Int,
    startDestination: Int = InheritancePlanningActivity.START_DESTINATION_DEFAULT,
) {
    val navController = rememberNavController()
    val startRoute = remember(
        planFlow,
        startDestination,
        activityViewModel.setupOrReviewParam.walletId,
        activityViewModel.setupOrReviewParam.groupId,
        activityViewModel.setupOrReviewParam.magicalPhrase,
        activityViewModel.setupOrReviewParam.sourceFlow,
    ) {
        getInheritancePlanningStartRoute(
            planFlow = planFlow,
            param = activityViewModel.setupOrReviewParam,
            startDestination = startDestination,
        )
    }
    var releaseScheduleUiState by remember { mutableStateOf(ReleaseScheduleUiState()) }
    var pendingNewStage by remember { mutableStateOf<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage?>(null) }

    NavHost(
        navController = navController,
        startDestination = startRoute,
    ) {
        inheritanceSetupIntro(
            onContinueClicked = {
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(
                        setupFlowType = InheritanceSetupFlowType.OLD_FLOW
                    )
                )
                navController.navigateToInheritanceDistributionMethod()
            },
        )

        inheritancePlanOverview(
            onContinueClicked = { setupFlowType ->
                when (setupFlowType) {
                    InheritanceSetupFlowType.MULTI_BENEFICIARY -> navController.navigateToInheritanceAssetAllocation()
                    InheritanceSetupFlowType.SINGLE_BENEFICIARY -> navController.navigateToMagicalPhraseIntro()
                    else -> navController.navigateToMagicalPhraseIntro()
                }
            },
        )

        inheritanceDistributionMethod(
            onContinueClicked = { method ->
                if (method == InheritanceDistributionMethod.CUSTOMIZED) {
                    navController.navigateToInheritanceCustomizedDistribution()
                } else {
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(
                            setupFlowType = InheritanceSetupFlowType.OLD_FLOW
                        )
                    )
                    navController.navigate(
                        InheritancePlanOverviewRoute(setupFlowType = InheritanceSetupFlowType.OLD_FLOW)
                    )
                }
            },
        )

        inheritanceCustomizedDistribution(
            onContinueClicked = { type ->
                val flowType = when (type) {
                    BeneficiaryType.SINGLE -> InheritanceSetupFlowType.SINGLE_BENEFICIARY
                    BeneficiaryType.MULTI -> InheritanceSetupFlowType.MULTI_BENEFICIARY
                }
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(setupFlowType = flowType)
                )
                navController.navigate(InheritancePlanOverviewRoute(setupFlowType = flowType))
            },
        )

        inheritanceAssetAllocation(
            onBackClicked = { navController.popBackStack() },
            onContinueClicked = { allocations ->
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(beneficiaryAllocations = allocations)
                )
                navController.navigateToInheritanceReleaseMethod()
            },
        )

        magicalPhraseIntro(
            onContinueClicked = { magicalPhrase, inheritanceKeys, beneficiaryAllocations ->
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(
                        magicalPhrase = magicalPhrase,
                        inheritanceKeys = inheritanceKeys,
                        beneficiaryAllocations = beneficiaryAllocations.ifEmpty {
                            activityViewModel.setupOrReviewParam.beneficiaryAllocations
                        },
                    )
                )
                if (activityViewModel.isMiniscriptWallet()) {
                    navController.navigateToInheritanceKeyTip()
                } else {
                    navController.navigate(FindBackupPasswordRoute())
                }
            },
        )

        findBackupPassword(
            onContinueClicked = { stepNumber ->
                val uiState = activityViewModel.state.value
                if (uiState.keyTypes.size == 2 && stepNumber == 1) {
                    navController.navigate(FindBackupPasswordRoute(stepNumber = 2))
                } else if (activityViewModel.getGroupWalletType() == com.nunchuk.android.model.byzantine.GroupWalletType.THREE_OF_FIVE_INHERITANCE) {
                    navController.navigate(InheritanceActivationDateRoute())
                } else {
                    navController.navigateToInheritanceKeyTip()
                }
            },
        )

        inheritanceKeyTip(
            onContinueClicked = {
                if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                    navController.navigateToInheritanceReleaseSchedule()
                } else if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY) {
                    navController.navigateToInheritanceReleaseMethod()
                } else if (activityViewModel.isMiniscriptWallet()) {
                    navController.navigateToInheritanceTimelockInfo()
                } else {
                    navController.navigate(InheritanceActivationDateRoute())
                }
            },
        )

        inheritanceReleaseMethod(
            onBackClicked = { navController.popBackStack() },
            onContinueClicked = { method ->
                val previousDestination = navController.previousBackStackEntry?.destination
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(
                        releaseMethodType = method.toReleaseMethodType(),
                        beneficiaryAllocations = activityViewModel.setupOrReviewParam.beneficiaryAllocations.ifEmpty {
                            defaultBeneficiaryAllocations()
                        },
                    )
                )
                if (previousDestination?.hasRoute<InheritanceBeneficiarySchedulesRoute>() == true) {
                    navController.popBackStack()
                } else {
                    navController.navigateToInheritanceBeneficiarySchedules()
                }
            },
        )

        inheritanceBeneficiarySchedules(
            releaseScheduleUiState = releaseScheduleUiState,
            onBackClicked = { navController.popBackStack() },
            onEditReleaseMethodClicked = { navController.navigateToInheritanceReleaseMethod() },
            onAddReleaseScheduleClicked = {
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(isSharedScheduleConfigured = false)
                )
                navController.navigate(InheritanceReleaseScheduleDetailRoute(fromBeneficiarySchedules = true))
            },
            onEditSharedScheduleClicked = {
                navController.navigate(InheritanceReleaseScheduleDetailRoute(fromBeneficiarySchedules = true))
            },
            onEditBeneficiaryScheduleClicked = {
                // TODO: navigate to beneficiary schedule setup screen.
            },
        )

        inheritanceReleaseSchedule(
            onContinueClicked = {
                navController.navigate(InheritanceReleaseScheduleDetailRoute())
            },
        )

        inheritanceReleaseScheduleDetail(
            releaseScheduleUiState = releaseScheduleUiState,
            onUiStateChanged = { releaseScheduleUiState = it },
            onEditStage = { stage ->
                navController.navigate(
                    InheritanceReleaseScheduleStageEditRoute(stageId = stage.id, isNewStage = false)
                )
            },
            onEditBufferPeriodClicked = { route ->
                navController.navigate(
                    InheritanceBufferPeriodRoute(
                        isUpdateRequest = true,
                        fromBeneficiarySchedules = route.fromBeneficiarySchedules,
                    )
                )
            },
            onAddStageRequested = {
                val newStage = releaseScheduleUiState.buildNewStage()
                pendingNewStage = newStage
                navController.navigate(
                    InheritanceReleaseScheduleStageEditRoute(stageId = newStage.id, isNewStage = true)
                )
            },
            onContinueClicked = { route ->
                val setupOrReviewParam = activityViewModel.setupOrReviewParam
                val firstStageTimeZoneId = releaseScheduleUiState.stages.firstOrNull()?.timeZoneId.orEmpty()
                if (firstStageTimeZoneId.isNotEmpty() && firstStageTimeZoneId != setupOrReviewParam.selectedZoneId) {
                    activityViewModel.setOrUpdate(
                        setupOrReviewParam.copy(selectedZoneId = firstStageTimeZoneId)
                    )
                }
                if (route.fromBeneficiarySchedules && route.isPostBufferPeriodMethod) {
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(isSharedScheduleConfigured = true)
                    )
                    val didPop = navController.popBackStack<InheritanceBeneficiarySchedulesRoute>(inclusive = false)
                    if (!didPop) {
                        navController.navigateToInheritanceBeneficiarySchedules()
                    }
                } else if (route.fromBeneficiarySchedules) {
                    navController.navigate(InheritanceBufferPeriodRoute(fromBeneficiarySchedules = true))
                } else if (setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY &&
                    route.isPostBufferPeriodMethod &&
                    setupOrReviewParam.bufferPeriod != null &&
                    setupOrReviewParam.bufferPeriodApplyType != null
                ) {
                    navController.navigate(InheritanceNoteRoute())
                } else if (setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                    navController.navigate(InheritanceBufferPeriodRoute())
                } else if (activityViewModel.isMiniscriptWallet()) {
                    navController.navigateToInheritanceTimelockInfo()
                } else {
                    navController.navigate(InheritanceActivationDateRoute())
                }
            },
        )

        inheritanceReleaseScheduleStageEdit(
            releaseScheduleUiState = releaseScheduleUiState,
            pendingNewStage = pendingNewStage,
            onBackClicked = { isNewStage ->
                returnToReleaseScheduleDetail(navController)
                if (isNewStage) pendingNewStage = null
            },
            onStageNotFound = { returnToReleaseScheduleDetail(navController) },
            onDeleteStage = { stageId, isNewStage ->
                if (isNewStage) {
                    returnToReleaseScheduleDetail(navController)
                    pendingNewStage = null
                } else {
                    returnToReleaseScheduleDetail(navController)
                    releaseScheduleUiState = releaseScheduleUiState.deleteStage(stageId)
                    val firstStageTimeZoneId =
                        releaseScheduleUiState.stages.firstOrNull()?.timeZoneId
                            ?: activityViewModel.setupOrReviewParam.selectedZoneId
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(selectedZoneId = firstStageTimeZoneId)
                    )
                }
            },
            onConfirmStage = { updatedStage, isNewStage ->
                releaseScheduleUiState = if (isNewStage) {
                    releaseScheduleUiState.appendStage(updatedStage)
                } else {
                    releaseScheduleUiState.updateStage(updatedStage)
                }
                if (updatedStage.stageNumber == 1) {
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(selectedZoneId = updatedStage.timeZoneId)
                    )
                }
                returnToReleaseScheduleDetail(navController)
                if (isNewStage) pendingNewStage = null
            },
        )

        inheritanceTimelockInfo(
            onContinueClicked = {
                navController.navigate(InheritanceNoteRoute())
            },
        )

        inheritanceActivationDate(
            onContinueClick = { isUpdateRequest, date, selectedZoneId ->
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(
                        activationDate = date,
                        selectedZoneId = selectedZoneId,
                    )
                )
                if (isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                    navController.popBackStack()
                } else {
                    navController.navigate(InheritanceNoteRoute())
                }
            },
        )

        inheritanceNote(
            onContinueClick = { isUpdateRequest, note, beneficiaryAllocations ->
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(
                        note = note,
                        beneficiaryAllocations = beneficiaryAllocations.ifEmpty {
                            activityViewModel.setupOrReviewParam.beneficiaryAllocations
                        },
                    )
                )
                if (isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                    navController.popBackStack()
                } else if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                    navController.navigate(InheritanceNotifyPrefRoute(isUpdateRequest = isUpdateRequest))
                } else if (activityViewModel.isMiniscriptWallet()) {
                    navController.navigate(InheritanceNotifyPrefRoute(isUpdateRequest = isUpdateRequest))
                } else {
                    navController.navigate(InheritanceBufferPeriodRoute())
                }
            },
        )

        inheritanceBufferPeriod(
            onContinueClick = { isUpdateRequest, fromBeneficiarySchedules, period ->
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(
                        bufferPeriod = period,
                        bufferPeriodApplyType = if (period == null) {
                            null
                        } else {
                            activityViewModel.setupOrReviewParam.bufferPeriodApplyType
                        },
                    )
                )
                if (isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                    navController.popBackStack()
                } else if (fromBeneficiarySchedules) {
                    if (period == null) {
                        navController.navigate(
                            InheritanceReleaseScheduleDetailRoute(
                                isPostBufferPeriodMethod = true,
                                fromBeneficiarySchedules = true,
                            )
                        )
                    } else {
                        navController.navigate(
                            InheritanceBufferPeriodMethodRoute(fromBeneficiarySchedules = true)
                        )
                    }
                } else if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                    if (period == null) {
                        navController.navigate(InheritanceNoteRoute())
                    } else {
                        navController.navigate(InheritanceBufferPeriodMethodRoute())
                    }
                } else {
                    navController.navigate(InheritanceNotifyPrefRoute())
                }
            },
        )

        inheritanceBufferPeriodMethod(
            onContinueClicked = { option, fromBeneficiarySchedules ->
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(
                        bufferPeriodApplyType = option.toBufferPeriodApplyType()
                    )
                )
                navController.navigate(
                    InheritanceReleaseScheduleDetailRoute(
                        isPostBufferPeriodMethod = true,
                        fromBeneficiarySchedules = fromBeneficiarySchedules,
                    )
                )
            },
        )

        inheritanceNotifyPref(
            onSkipClick = { isUpdateRequest ->
                openReviewPlanScreen(
                    navController = navController,
                    isUpdateRequest = isUpdateRequest,
                    activityViewModel = activityViewModel,
                    isDiscard = true,
                    emails = emptyList(),
                    isNotify = false,
                )
            },
            onContinueClick = { isUpdateRequest, emails, isNotify ->
                openReviewPlanScreen(
                    navController = navController,
                    isUpdateRequest = isUpdateRequest,
                    activityViewModel = activityViewModel,
                    isDiscard = false,
                    emails = emails,
                    isNotify = isNotify,
                )
            },
        )

        inheritanceNotificationSettings(
            onContinueClick = { isUpdateRequest, emailSettings, emailMeWalletConfig ->
                val notificationSettings = InheritanceNotificationSettings(
                    emailMeWalletConfig = emailMeWalletConfig,
                    perEmailSettings = emailSettings,
                )
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(notificationSettings = notificationSettings)
                )
                if (isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                    navController.popBackStack<InheritanceReviewPlanRoute>(inclusive = false)
                } else {
                    navController.navigateToInheritanceReviewPlan()
                }
            },
        )

        inheritanceReviewPlan(
            navigator = navigator,
            releaseScheduleUiState = releaseScheduleUiState,
            onCreateOrUpdateSuccess = {
                val param = activityViewModel.setupOrReviewParam
                navController.navigate(
                    InheritanceCreateSuccessRoute(
                        magicalPhrase = param.magicalPhrase,
                        planFlow = param.planFlow,
                        walletId = param.walletId,
                        sourceFlow = param.sourceFlow,
                    )
                )
            },
            onCancelSuccess = {},
            onMarkSetupInheritance = {},
            onEditActivationDateClick = {
                navController.navigate(InheritanceActivationDateRoute(isUpdateRequest = true))
            },
            onEditNoteClick = {
                navController.navigate(InheritanceNoteRoute(isUpdateRequest = true))
            },
            onNotifyPrefClick = {
                navController.navigate(InheritanceNotifyPrefRoute(isUpdateRequest = true))
            },
            onDiscardChange = {
                NCWarningDialog(activity).showDialog(
                    title = activity.getString(com.nunchuk.android.wallet.R.string.nc_confirmation),
                    message = activity.getString(R.string.nc_are_you_sure_discard_the_change),
                    onYesClick = { activity.finish() },
                )
            },
            onShareSecretClicked = {
                val param = activityViewModel.setupOrReviewParam
                navController.navigate(
                    InheritanceShareSecretRoute(
                        magicalPhrase = param.magicalPhrase,
                        planFlow = param.planFlow,
                        walletId = param.walletId,
                        sourceFlow = param.sourceFlow,
                    )
                )
            },
            onViewClaimingInstruction = {
                val link = if (BuildConfig.DEBUG) {
                    "https://stg-www.nunchuk.io/howtoclaim"
                } else {
                    "https://www.nunchuk.io/howtoclaim"
                }
                activity.openExternalLink(link)
            },
            onEditBufferPeriodClick = {
                navController.navigate(InheritanceBufferPeriodRoute(isUpdateRequest = true))
            },
            onBackUpPasswordInfoClick = {
                navController.navigateToInheritanceBackUpDownload()
            },
        )

        inheritanceReviewPlanGroup()

        inheritanceCreateSuccess(
            onContinueClick = { route ->
                navController.navigate(
                    InheritanceShareSecretRoute(
                        magicalPhrase = route.magicalPhrase,
                        planFlow = route.planFlow,
                        walletId = route.walletId,
                        sourceFlow = route.sourceFlow,
                    )
                )
            },
        )

        inheritanceShareSecret(
            onContinueClick = { route, type ->
                navController.navigate(
                    InheritanceShareSecretInfoRoute(
                        magicalPhrase = route.magicalPhrase,
                        type = type,
                        planFlow = route.planFlow,
                        walletId = route.walletId,
                        sourceFlow = route.sourceFlow,
                    )
                )
            },
        )

        inheritanceShareSecretInfo(
            navigator = navigator,
            onNavigateToHowItWorks = { type ->
                navController.navigate(InheritanceHowItWorksRoute(type = type))
            },
            onNavigateToBackUpDownload = {
                navController.navigateToInheritanceBackUpDownload()
            },
        )

        inheritanceBackUpDownload(
            onContinueClicked = { navController.popBackStack() },
        )

        inheritanceHowItWorks(
            onDoneClick = { activity.finish() },
        )

        inheritanceRequestPlanningConfirm(
            onRequestSuccess = {
                navController.navigate(InheritanceRequestPlanningSentSuccessRoute)
            },
        )

        inheritanceRequestPlanningSentSuccess(
            onGotItClick = { activity.finish() },
        )
    }
}

@Composable
internal fun MembershipStepEffect(membershipStepManager: MembershipStepManager) {
    DisposableEffect(membershipStepManager) {
        membershipStepManager.updateStep(true)
        onDispose {
            membershipStepManager.updateStep(false)
        }
    }
}

fun returnToReleaseScheduleDetail(navController: NavController) {
    val isShowingReleaseScheduleDetail =
        navController.currentDestination?.hasRoute<InheritanceReleaseScheduleDetailRoute>() == true
    if (isShowingReleaseScheduleDetail) return
    val didPop = navController.popBackStack<InheritanceReleaseScheduleDetailRoute>(inclusive = false)
    if (!didPop) {
        navController.navigate(InheritanceReleaseScheduleDetailRoute())
    }
}

private fun openReviewPlanScreen(
    navController: NavController,
    isUpdateRequest: Boolean,
    activityViewModel: InheritancePlanningViewModel,
    isDiscard: Boolean,
    emails: List<String>,
    isNotify: Boolean,
) {
    if (!isDiscard) {
        activityViewModel.setOrUpdate(
            activityViewModel.setupOrReviewParam.copy(
                isNotify = isNotify,
                emails = emails,
            )
        )
    }
    if (isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
        if (activityViewModel.isMiniscriptWallet()) {
            navController.navigate(InheritanceNotificationSettingsRoute(isUpdateRequest = isUpdateRequest))
        } else {
            navController.popBackStack()
        }
    } else if (activityViewModel.isMiniscriptWallet()) {
        navController.navigate(InheritanceNotificationSettingsRoute())
    } else {
        navController.navigateToInheritanceReviewPlan()
    }
}

internal fun BufferPeriodMethodOption.toBufferPeriodApplyType(): InheritanceBufferPeriodApplyType {
    return when (this) {
        BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY -> InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY
        BufferPeriodMethodOption.EVERY_WITHDRAWAL -> InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL
    }
}

internal fun InheritanceBufferPeriodApplyType.toBufferPeriodMethodOption(): BufferPeriodMethodOption {
    return when (this) {
        InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY -> BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY
        InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL -> BufferPeriodMethodOption.EVERY_WITHDRAWAL
    }
}

internal fun InheritanceReleaseMethod.toReleaseMethodType(): InheritanceReleaseMethodType {
    return when (this) {
        InheritanceReleaseMethod.SHARED_SCHEDULE -> InheritanceReleaseMethodType.SHARED_SCHEDULE
        InheritanceReleaseMethod.INDIVIDUAL_SCHEDULES -> InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES
    }
}

internal fun InheritanceReleaseMethodType.toReleaseMethodOption(): InheritanceReleaseMethod {
    return when (this) {
        InheritanceReleaseMethodType.SHARED_SCHEDULE -> InheritanceReleaseMethod.SHARED_SCHEDULE
        InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES -> InheritanceReleaseMethod.INDIVIDUAL_SCHEDULES
    }
}

internal fun defaultBeneficiaryAllocations(): List<InheritanceBeneficiaryAllocation> {
    return listOf(
        InheritanceBeneficiaryAllocation(
            email = "Wife@gmail.com",
            allocationPercent = 50,
            magic = "dolphin concert apple mirror",
        ),
        InheritanceBeneficiaryAllocation(
            email = "Son@gmail.com",
            allocationPercent = 25,
            magic = "galaxy piano silver ocean",
        ),
        InheritanceBeneficiaryAllocation(
            email = "Daughter@gmail.com",
            allocationPercent = 25,
            magic = "nebula violin pearl forest",
        ),
    )
}

@Composable
internal fun releaseScheduleBufferPeriodSummaryText(
    period: Period?,
    applyType: InheritanceBufferPeriodApplyType?,
): String? {
    if (period == null || applyType == null) return null
    val applyTypeText = when (applyType) {
        InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY -> {
            stringResource(id = R.string.nc_release_schedule_buffer_period_first_withdrawal_only)
        }
        InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL -> {
            stringResource(id = R.string.nc_release_schedule_buffer_period_every_withdrawal)
        }
    }
    return stringResource(
        id = R.string.nc_release_schedule_buffer_period_summary,
        period.intervalCount,
        applyTypeText,
    )
}
