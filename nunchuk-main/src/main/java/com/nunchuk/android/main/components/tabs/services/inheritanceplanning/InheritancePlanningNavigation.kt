package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.main.BuildConfig
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.inheritanceActivationDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.navigateToInheritanceActivationDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.assetallocation.inheritanceAssetAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.assetallocation.navigateToInheritanceAssetAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload.inheritanceBackUpDownload
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload.navigateToInheritanceBackUpDownload
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.InheritanceBeneficiaryScheduleConfig
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.InheritanceBeneficiarySchedulesRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.inheritanceBeneficiarySchedules
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.navigateToInheritanceBeneficiarySchedules
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod.inheritanceBufferPeriod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod.navigateToInheritanceBufferPeriod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod.BufferPeriodMethodOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod.inheritanceBufferPeriodMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod.navigateToInheritanceBufferPeriodMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.changetimezone.inheritanceChangeTimezone
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.changetimezone.navigateToInheritanceChangeTimezone
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.createsuccess.InheritanceCreateSuccessRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.createsuccess.inheritanceCreateSuccess
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.createsuccess.navigateToInheritanceCreateSuccess
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution.BeneficiaryType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution.inheritanceCustomizedDistribution
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution.navigateToInheritanceCustomizedDistribution
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod.InheritanceDistributionMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod.inheritanceDistributionMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod.navigateToInheritanceDistributionMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.FallbackTriggerUnit
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.InheritanceFallbackOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.InheritanceFallbackSettingsValue
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.inheritanceFallbackSettings
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.navigateToInheritanceFallbackSettings
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup.findBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup.navigateToFindBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.howitworks.inheritanceHowItWorks
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.howitworks.navigateToInheritanceHowItWorks
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.intro.InheritanceSetupIntroRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.intro.inheritanceSetupIntro
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip.inheritanceKeyTip
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip.navigateToInheritanceKeyTip
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase.magicalPhraseIntro
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase.navigateToMagicalPhraseIntro
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note.inheritanceNote
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note.navigateToInheritanceNote
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings.inheritanceNotificationSettings
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings.navigateToInheritanceNotificationSettings
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref.inheritanceNotifyPref
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref.navigateToInheritanceNotifyPref
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.planoverview.inheritancePlanOverview
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.planoverview.navigateToInheritancePlanOverview
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.InheritanceReleaseMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.inheritanceReleaseMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.navigateToInheritanceReleaseMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedule.inheritanceReleaseSchedule
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedule.navigateToInheritanceReleaseSchedule
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.InheritanceReleaseScheduleDetailRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.inheritanceReleaseScheduleDetail
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.navigateToInheritanceReleaseScheduleDetail
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.withTimezone
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit.inheritanceReleaseScheduleStageEdit
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit.navigateToInheritanceReleaseScheduleStageEdit
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm.InheritanceRequestPlanningConfirmRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm.inheritanceRequestPlanningConfirm
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.sent.inheritanceRequestPlanningSentSuccess
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.sent.navigateToInheritanceRequestPlanningSentSuccess
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceAlertReviewRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.inheritanceReviewPlan
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.inheritanceReviewPlanGroup
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.navigateToInheritanceReviewPlan
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.inheritanceShareSecret
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.navigateToInheritanceShareSecret
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo.inheritanceShareSecretInfo
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo.navigateToInheritanceShareSecretInfo
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.timelockinfo.inheritanceTimelockInfo
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.timelockinfo.navigateToInheritanceTimelockInfo
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.widget.NCWarningDialog
import java.util.Calendar

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
        InheritancePlanFlow.SIGN_DUMMY_TX -> InheritanceAlertReviewRoute
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
    val planningState by activityViewModel.state.collectAsStateWithLifecycle()
    val setupOrReviewParam = planningState.setupOrReviewParam
    val releaseScheduleFlowViewModel: InheritanceReleaseScheduleFlowViewModel =
        hiltViewModel(viewModelStoreOwner = activity)
    val navController = rememberNavController()
    val startRoute = remember(
        planFlow,
        startDestination,
        setupOrReviewParam.walletId,
        setupOrReviewParam.groupId,
        setupOrReviewParam.magicalPhrase,
        setupOrReviewParam.sourceFlow,
    ) {
        getInheritancePlanningStartRoute(
            planFlow = planFlow,
            param = setupOrReviewParam,
            startDestination = startDestination,
        )
    }

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
                    InheritanceSetupFlowType.MULTI_BENEFICIARY ->
                        navController.navigateToInheritanceAssetAllocation(isUpdateRequest = false)
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
                    navController.navigateToInheritancePlanOverview(setupFlowType = InheritanceSetupFlowType.OLD_FLOW)
                }
            },
        )

        inheritanceFallbackSettings(
            initialValueProvider = { activityViewModel.setupOrReviewParam.fallbackSettings },
            finalScheduledPayoutTimeMillisProvider = {
                activityViewModel.setupOrReviewParam.finalScheduledPayoutTimeMillis()
            },
            onBackClicked = { navController.popBackStack() },
            onContinueClicked = { value ->
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(
                        fallbackSettings = value
                    )
                )
                val didPopToBeneficiarySchedules =
                    navController.popBackStack<InheritanceBeneficiarySchedulesRoute>(inclusive = false)
                if (!didPopToBeneficiarySchedules) {
                    navController.popBackStack()
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
                navController.navigateToInheritancePlanOverview(setupFlowType = flowType)
            },
        )

        inheritanceAssetAllocation(
            onBackClicked = { navController.popBackStack() },
            onContinueClicked = { allocations, isUpdateRequest ->
                activityViewModel.setOrUpdate(
                    activityViewModel.setupOrReviewParam.copy(beneficiaryAllocations = allocations)
                )
                if (isUpdateRequest) {
                    navController.popBackStack()
                } else {
                    navController.navigateToInheritanceReleaseMethod()
                }
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
                    navController.navigateToFindBackupPassword()
                }
            },
        )

        findBackupPassword(
            onContinueClicked = { stepNumber ->
                val uiState = activityViewModel.state.value
                if (uiState.keyTypes.size == 2 && stepNumber == 1) {
                    navController.navigateToFindBackupPassword(stepNumber = 2)
                } else if (activityViewModel.getGroupWalletType() == com.nunchuk.android.model.byzantine.GroupWalletType.THREE_OF_FIVE_INHERITANCE) {
                    navController.navigateToInheritanceActivationDate()
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
                    navController.navigateToInheritanceNote()
                } else if (activityViewModel.isMiniscriptWallet()) {
                    navController.navigateToInheritanceTimelockInfo()
                } else {
                    navController.navigateToInheritanceActivationDate()
                }
            },
        )

        inheritanceReleaseMethod(
            onBackClicked = { navController.popBackStack() },
            onContinueClicked = { method, isUpdateRequest ->
                val currentParam = activityViewModel.setupOrReviewParam
                val newReleaseMethodType = method.toReleaseMethodType()
                val isMethodChanged = currentParam.releaseMethodType != newReleaseMethodType
                val previousDestination = navController.previousBackStackEntry?.destination
                val openedFromBeneficiarySchedules =
                    previousDestination?.hasRoute<InheritanceBeneficiarySchedulesRoute>() == true
                if (isUpdateRequest && !isMethodChanged) {
                    navController.popBackStack()
                    return@inheritanceReleaseMethod
                }
                activityViewModel.setOrUpdate(
                    currentParam.copy(
                        releaseMethodType = newReleaseMethodType,
                        beneficiaryAllocations = currentParam.beneficiaryAllocations,
                        sharedScheduleConfig = if (isMethodChanged) null else currentParam.sharedScheduleConfig,
                        individualScheduleConfigs = if (isMethodChanged) {
                            emptyMap()
                        } else {
                            currentParam.individualScheduleConfigs
                        },
                        isSharedScheduleConfigured = if (isMethodChanged) false else currentParam.isSharedScheduleConfigured,
                        fallbackSettings = if (isMethodChanged) null else currentParam.fallbackSettings,
                    )
                )
                if (openedFromBeneficiarySchedules) {
                    navController.popBackStack()
                } else if (isUpdateRequest) {
                    navController.popBackStack()
                    navController.navigateToInheritanceBeneficiarySchedules()
                } else {
                    navController.navigateToInheritanceBeneficiarySchedules()
                }
            },
        )

        inheritanceBeneficiarySchedules(
            onBackClicked = { navController.popBackStack() },
            onEditReleaseMethodClicked = {
                navController.navigateToInheritanceReleaseMethod(
                    isUpdateRequest = activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW
                )
            },
            onEditFallbackSettingsClicked = { navController.navigateToInheritanceFallbackSettings() },
            onEditAssetAllocationClicked = {
                navController.navigateToInheritanceAssetAllocation(isUpdateRequest = true)
            },
            onAddReleaseScheduleClicked = {
                val returnToReviewPlan =
                    navController.previousBackStackEntry?.destination?.hasRoute<InheritanceReviewPlanRoute>() == true
                val draftId = releaseScheduleFlowViewModel.createDraft(
                    releaseScheduleUiState = ReleaseScheduleUiState(),
                    hasBufferPeriodSelection = false,
                )
                navController.navigateToInheritanceReleaseScheduleDetail(
                    draftId = draftId,
                    fromBeneficiarySchedules = true,
                    returnToReviewPlan = returnToReviewPlan,
                )
            },
            onEditSharedScheduleClicked = {
                val returnToReviewPlan =
                    navController.previousBackStackEntry?.destination?.hasRoute<InheritanceReviewPlanRoute>() == true
                val setupOrReviewParam = activityViewModel.setupOrReviewParam
                val sharedConfig = setupOrReviewParam.sharedScheduleConfig
                val hasBufferSelection = sharedConfig != null
                val draftId = releaseScheduleFlowViewModel.createDraft(
                    releaseScheduleUiState = sharedConfig?.releaseScheduleUiState ?: ReleaseScheduleUiState(),
                    bufferPeriod = sharedConfig?.bufferPeriod ?: setupOrReviewParam.bufferPeriod,
                    bufferPeriodApplyType = sharedConfig?.bufferPeriodApplyType ?: setupOrReviewParam.bufferPeriodApplyType,
                    hasBufferPeriodSelection = hasBufferSelection,
                )
                navController.navigateToInheritanceReleaseScheduleDetail(
                    draftId = draftId,
                    fromBeneficiarySchedules = true,
                    isPostBufferPeriodMethod = hasBufferSelection,
                    returnToReviewPlan = returnToReviewPlan,
                )
            },
            onEditBeneficiaryScheduleClicked = { beneficiaryEmail ->
                val returnToReviewPlan =
                    navController.previousBackStackEntry?.destination?.hasRoute<InheritanceReviewPlanRoute>() == true
                val beneficiaryKey = beneficiaryScheduleKey(beneficiaryEmail)
                val scheduleConfig =
                    activityViewModel.setupOrReviewParam.individualScheduleConfigs[beneficiaryKey]
                        ?: activityViewModel.setupOrReviewParam.individualScheduleConfigs.entries
                            .firstOrNull { it.key.trim().lowercase() == beneficiaryKey }
                            ?.value
                val draftId = releaseScheduleFlowViewModel.createDraft(
                    releaseScheduleUiState = scheduleConfig?.releaseScheduleUiState
                        ?: ReleaseScheduleUiState(),
                    bufferPeriod = scheduleConfig?.bufferPeriod,
                    bufferPeriodApplyType = scheduleConfig?.bufferPeriodApplyType,
                    hasBufferPeriodSelection = scheduleConfig != null,
                )
                navController.navigate(
                    InheritanceReleaseScheduleDetailRoute(
                        draftId = draftId,
                        isPostBufferPeriodMethod = scheduleConfig != null,
                        fromBeneficiarySchedules = true,
                        beneficiaryEmail = beneficiaryKey,
                        returnToReviewPlan = returnToReviewPlan,
                    )
                )
            },
            onContinueClicked = {
                if (activityViewModel.setupOrReviewParam.fallbackSettings == null) {
                    navController.navigateToInheritanceFallbackSettings()
                } else {
                    navController.navigateToMagicalPhraseIntro()
                }
            },
        )

        inheritanceReleaseSchedule(
            onContinueClicked = {
                val setupOrReviewParam = activityViewModel.setupOrReviewParam
                val sharedConfig = setupOrReviewParam.sharedScheduleConfig
                val hasBufferSelection = sharedConfig != null
                val draftId = releaseScheduleFlowViewModel.createDraft(
                    releaseScheduleUiState = sharedConfig?.releaseScheduleUiState ?: ReleaseScheduleUiState(),
                    bufferPeriod = sharedConfig?.bufferPeriod ?: setupOrReviewParam.bufferPeriod,
                    bufferPeriodApplyType = sharedConfig?.bufferPeriodApplyType ?: setupOrReviewParam.bufferPeriodApplyType,
                    hasBufferPeriodSelection = hasBufferSelection,
                )
                navController.navigateToInheritanceReleaseScheduleDetail(
                    draftId = draftId,
                    isPostBufferPeriodMethod = hasBufferSelection,
                )
            },
        )

        inheritanceReleaseScheduleDetail(
            onEditStage = { stage ->
                val draftId = releaseScheduleFlowViewModel.state.value.activeDraftId
                navController.navigateToInheritanceReleaseScheduleStageEdit(
                    draftId = draftId,
                    stageId = stage.id,
                    isNewStage = false
                )
            },
            onEditBufferPeriodClicked = { route ->
                navController.navigateToInheritanceBufferPeriod(
                    draftId = route.draftId,
                    isUpdateRequest = true,
                    fromBeneficiarySchedules = route.fromBeneficiarySchedules,
                    beneficiaryEmail = route.beneficiaryEmail,
                    returnToReviewPlan = route.returnToReviewPlan,
                )
            },
            onAddStageRequested = {
                val draftId = releaseScheduleFlowViewModel.state.value.activeDraftId
                val newStage = releaseScheduleFlowViewModel.getDraft(draftId).releaseScheduleUiState.buildNewStage()
                releaseScheduleFlowViewModel.setPendingNewStage(draftId, newStage)
                navController.navigateToInheritanceReleaseScheduleStageEdit(
                    draftId = draftId,
                    stageId = newStage.id,
                    isNewStage = true
                )
            },
            onContinueClicked = { route ->
                val setupOrReviewParam = activityViewModel.setupOrReviewParam
                val draftId = route.draftId.ifBlank { releaseScheduleFlowViewModel.state.value.activeDraftId }
                val draft = releaseScheduleFlowViewModel.getDraft(draftId)
                val releaseScheduleUiState = draft.releaseScheduleUiState
                val activeBeneficiaryEmail = beneficiaryScheduleKey(route.beneficiaryEmail)
                val isBeneficiaryScheduleContext =
                    route.fromBeneficiarySchedules || activeBeneficiaryEmail.isNotBlank()
                val isSingleBeneficiaryContext =
                    setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY &&
                        !route.fromBeneficiarySchedules &&
                        activeBeneficiaryEmail.isBlank()
                val firstStageTimeZoneId =
                    releaseScheduleUiState.stages.firstOrNull()?.timeZoneId.orEmpty()
                if (firstStageTimeZoneId.isNotEmpty() && firstStageTimeZoneId != setupOrReviewParam.selectedZoneId) {
                    activityViewModel.setOrUpdate(
                        setupOrReviewParam.copy(selectedZoneId = firstStageTimeZoneId)
                    )
                }
                if ((isBeneficiaryScheduleContext || isSingleBeneficiaryContext) && route.isPostBufferPeriodMethod) {
                    var updatedParam = activityViewModel.setupOrReviewParam
                    if (activeBeneficiaryEmail.isNotBlank()) {
                        val updatedConfigs = updatedParam.individualScheduleConfigs + mapOf(
                            activeBeneficiaryEmail to InheritanceBeneficiaryScheduleConfig(
                                releaseScheduleUiState = releaseScheduleUiState,
                                bufferPeriod = draft.bufferPeriod,
                                bufferPeriodApplyType = draft.bufferPeriodApplyType,
                            )
                        )
                        updatedParam = updatedParam.copy(
                            individualScheduleConfigs = updatedConfigs,
                        )
                    } else {
                        updatedParam = updatedParam.copy(
                            bufferPeriod = draft.bufferPeriod,
                            bufferPeriodApplyType = draft.bufferPeriodApplyType,
                            sharedScheduleConfig = InheritanceBeneficiaryScheduleConfig(
                                releaseScheduleUiState = releaseScheduleUiState,
                                bufferPeriod = draft.bufferPeriod,
                                bufferPeriodApplyType = draft.bufferPeriodApplyType,
                            ),
                            isSharedScheduleConfigured = true,
                        )
                    }
                    activityViewModel.setOrUpdate(updatedParam)
                    releaseScheduleFlowViewModel.discardDraft(draftId)
                    if (route.returnToReviewPlan) {
                        val didPopBeneficiarySchedules = navController.popBackStack<InheritanceBeneficiarySchedulesRoute>(
                            inclusive = true
                        )
                        if (!didPopBeneficiarySchedules) {
                            val didPopToReview =
                                navController.popBackStack<InheritanceReviewPlanRoute>(inclusive = false)
                            if (!didPopToReview) {
                                navController.navigateToInheritanceReviewPlan()
                            }
                        }
                    } else if (isSingleBeneficiaryContext) {
                        navController.navigateToInheritanceNote()
                    } else {
                        val didPop =
                            navController.popBackStack<InheritanceBeneficiarySchedulesRoute>(inclusive = false)
                        if (!didPop) {
                            navController.navigateToInheritanceBeneficiarySchedules()
                        }
                    }
                } else if (isBeneficiaryScheduleContext || isSingleBeneficiaryContext) {
                    val bufferDraftId = releaseScheduleFlowViewModel.cloneDraft(draftId)
                    navController.navigateToInheritanceBufferPeriod(
                        draftId = bufferDraftId,
                        fromBeneficiarySchedules = isBeneficiaryScheduleContext,
                        beneficiaryEmail = activeBeneficiaryEmail,
                        returnToReviewPlan = route.returnToReviewPlan,
                    )
                } else if (route.returnToReviewPlan) {
                    val didPopToReview =
                        navController.popBackStack<InheritanceReviewPlanRoute>(inclusive = false)
                    if (!didPopToReview) {
                        navController.navigateToInheritanceReviewPlan()
                    }
                } else if (activityViewModel.isMiniscriptWallet()) {
                    navController.navigateToInheritanceTimelockInfo()
                } else {
                    navController.navigateToInheritanceActivationDate()
                }
            },
        )

        inheritanceReleaseScheduleStageEdit(
            onBackClicked = { isNewStage, draftId ->
                returnToReleaseScheduleDetail(navController)
                if (isNewStage) releaseScheduleFlowViewModel.setPendingNewStage(draftId, null)
            },
            onStageNotFound = { _ -> returnToReleaseScheduleDetail(navController) },
            onDeleteStage = { stageId, isNewStage, draftId ->
                if (isNewStage) {
                    returnToReleaseScheduleDetail(navController)
                    releaseScheduleFlowViewModel.setPendingNewStage(draftId, null)
                } else {
                    returnToReleaseScheduleDetail(navController)
                    val updatedUiState =
                        releaseScheduleFlowViewModel.getDraft(draftId).releaseScheduleUiState.deleteStage(stageId)
                    releaseScheduleFlowViewModel.setReleaseScheduleUiState(draftId, updatedUiState)
                    val firstStageTimeZoneId =
                        updatedUiState.stages.firstOrNull()?.timeZoneId
                            ?: activityViewModel.setupOrReviewParam.selectedZoneId
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(selectedZoneId = firstStageTimeZoneId)
                    )
                }
            },
            onConfirmStage = { updatedStage, isNewStage, draftId ->
                val nextUiState = if (isNewStage) {
                    releaseScheduleFlowViewModel.getDraft(draftId).releaseScheduleUiState.appendStage(updatedStage)
                } else {
                    releaseScheduleFlowViewModel.getDraft(draftId).releaseScheduleUiState.updateStage(updatedStage)
                }
                releaseScheduleFlowViewModel.setReleaseScheduleUiState(draftId, nextUiState)
                if (updatedStage.stageNumber == 1) {
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(selectedZoneId = updatedStage.timeZoneId)
                    )
                }
                returnToReleaseScheduleDetail(navController)
                if (isNewStage) releaseScheduleFlowViewModel.setPendingNewStage(draftId, null)
            },
        )

        inheritanceTimelockInfo(
            onContinueClicked = {
                navController.navigateToInheritanceNote()
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
                    navController.navigateToInheritanceNote()
                }
            },
        )

        inheritanceChangeTimezone(
            onBackClicked = { navController.popBackStack() },
            onSaveClicked = { isUpdateRequest, selectedZoneId ->
                val currentParam = activityViewModel.setupOrReviewParam
                val previousZoneId = currentParam.selectedZoneId
                val updatedSharedScheduleConfig = currentParam.sharedScheduleConfig?.let { config ->
                    config.copy(
                        releaseScheduleUiState = config.releaseScheduleUiState.withTimezone(
                            newZoneId = selectedZoneId,
                            fallbackZoneId = previousZoneId,
                        )
                    )
                }
                val updatedIndividualScheduleConfigs =
                    currentParam.individualScheduleConfigs.mapValues { (_, config) ->
                        config.copy(
                            releaseScheduleUiState = config.releaseScheduleUiState.withTimezone(
                                newZoneId = selectedZoneId,
                                fallbackZoneId = previousZoneId,
                            )
                        )
                    }
                activityViewModel.setOrUpdate(
                    currentParam.copy(
                        selectedZoneId = selectedZoneId,
                        sharedScheduleConfig = updatedSharedScheduleConfig,
                        individualScheduleConfigs = updatedIndividualScheduleConfigs,
                    )
                )
                if (isUpdateRequest || currentParam.planFlow == InheritancePlanFlow.VIEW) {
                    navController.popBackStack()
                } else {
                    navController.navigateToInheritanceNote()
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
                    navController.navigateToInheritanceNotifyPref(isUpdateRequest = isUpdateRequest)
                } else if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY) {
                    navController.navigateToInheritanceNotifyPref(isUpdateRequest = isUpdateRequest)
                } else if (activityViewModel.isMiniscriptWallet()) {
                    navController.navigateToInheritanceNotifyPref(isUpdateRequest = isUpdateRequest)
                } else {
                    navController.navigateToInheritanceBufferPeriod()
                }
            },
        )

        inheritanceBufferPeriod(
            onContinueClick = { draftId, isUpdateRequest, fromBeneficiarySchedules, beneficiaryEmail, returnToReviewPlan, period ->
                val activeBeneficiaryEmail = beneficiaryScheduleKey(beneficiaryEmail)
                val isDraftScheduleFlow = draftId.isNotBlank()
                if (draftId.isNotBlank()) {
                    releaseScheduleFlowViewModel.setBufferPeriodSelection(
                        draftId = draftId,
                        period = period,
                    )
                } else {
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
                }
                if (isUpdateRequest || (
                        activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW &&
                            !isDraftScheduleFlow
                        )
                ) {
                    navController.popBackStack()
                } else if (fromBeneficiarySchedules) {
                    if (period == null) {
                        navController.navigateToInheritanceReleaseScheduleDetail(
                            draftId = draftId,
                            isPostBufferPeriodMethod = true,
                            fromBeneficiarySchedules = true,
                            beneficiaryEmail = activeBeneficiaryEmail,
                            returnToReviewPlan = returnToReviewPlan,
                        )
                    } else {
                        navController.navigateToInheritanceBufferPeriodMethod(
                            draftId = draftId,
                            fromBeneficiarySchedules = true,
                            beneficiaryEmail = activeBeneficiaryEmail,
                            returnToReviewPlan = returnToReviewPlan,
                        )
                    }
                } else if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                    if (period == null) {
                        navController.navigateToInheritanceReleaseScheduleDetail(
                            draftId = draftId,
                            isPostBufferPeriodMethod = true,
                        )
                    } else {
                        navController.navigateToInheritanceBufferPeriodMethod(draftId = draftId)
                    }
                } else {
                    navController.navigateToInheritanceNotifyPref()
                }
            },
        )

        inheritanceBufferPeriodMethod(
            onContinueClicked = { option, draftId, fromBeneficiarySchedules, beneficiaryEmail, returnToReviewPlan ->
                val activeBeneficiaryEmail = beneficiaryScheduleKey(beneficiaryEmail)
                if (draftId.isNotBlank()) {
                    releaseScheduleFlowViewModel.setBufferPeriodApplyType(
                        draftId = draftId,
                        applyType = option.toBufferPeriodApplyType(),
                    )
                } else {
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(
                            bufferPeriodApplyType = option.toBufferPeriodApplyType()
                        )
                    )
                }
                navController.navigateToInheritanceReleaseScheduleDetail(
                    draftId = draftId,
                    isPostBufferPeriodMethod = true,
                    fromBeneficiarySchedules = fromBeneficiarySchedules,
                    beneficiaryEmail = activeBeneficiaryEmail,
                    returnToReviewPlan = returnToReviewPlan,
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
            onCreateOrUpdateSuccess = {
                val param = activityViewModel.setupOrReviewParam
                navController.navigateToInheritanceCreateSuccess(
                    magicalPhrase = param.magicalPhrase,
                    planFlow = param.planFlow,
                    walletId = param.walletId,
                    sourceFlow = param.sourceFlow,
                )
            },
            onCancelSuccess = {},
            onMarkSetupInheritance = {},
            onEditActivationDateClick = {
                if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.OLD_FLOW) {
                    navController.navigateToInheritanceActivationDate(isUpdateRequest = true)
                } else {
                    navController.navigateToInheritanceChangeTimezone(isUpdateRequest = true)
                }
            },
            onEditNoteClick = {
                navController.navigateToInheritanceNote(isUpdateRequest = true)
            },
            onNotifyPrefClick = {
                navController.navigateToInheritanceNotifyPref(isUpdateRequest = true)
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
                navController.navigateToInheritanceShareSecret(
                    magicalPhrase = param.magicalPhrase,
                    planFlow = param.planFlow,
                    walletId = param.walletId,
                    sourceFlow = param.sourceFlow,
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
                if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                    val setupOrReviewParam = activityViewModel.setupOrReviewParam
                    val sharedConfig = setupOrReviewParam.sharedScheduleConfig
                    val hasBufferSelection = sharedConfig != null
                    val draftId = releaseScheduleFlowViewModel.createDraft(
                        releaseScheduleUiState = sharedConfig?.releaseScheduleUiState ?: ReleaseScheduleUiState(),
                        bufferPeriod = sharedConfig?.bufferPeriod ?: setupOrReviewParam.bufferPeriod,
                        bufferPeriodApplyType = sharedConfig?.bufferPeriodApplyType ?: setupOrReviewParam.bufferPeriodApplyType,
                        hasBufferPeriodSelection = hasBufferSelection,
                    )
                    navController.navigateToInheritanceReleaseScheduleDetail(
                        draftId = draftId,
                        isPostBufferPeriodMethod = hasBufferSelection,
                        returnToReviewPlan = true,
                    )
                } else {
                    navController.navigateToInheritanceBufferPeriod(isUpdateRequest = true)
                }
            },
            onBackUpPasswordInfoClick = {
                navController.navigateToInheritanceBackUpDownload()
            },
            onEditAssetAllocationClick = {
                navController.navigateToInheritanceAssetAllocation(isUpdateRequest = true)
            },
            onEditReleaseMethodClick = {
                navController.navigateToInheritanceReleaseMethod(isUpdateRequest = true)
            },
            onEditBeneficiarySchedulesClick = {
                navController.navigateToInheritanceBeneficiarySchedules()
            },
            onEditFallbackSettingsClick = {
                navController.navigateToInheritanceFallbackSettings()
            },
        )

        inheritanceReviewPlanGroup()

        inheritanceCreateSuccess(
            onContinueClick = { route ->
                navController.navigateToInheritanceShareSecret(
                    magicalPhrase = route.magicalPhrase,
                    planFlow = route.planFlow,
                    walletId = route.walletId,
                    sourceFlow = route.sourceFlow,
                )
            },
        )

        inheritanceShareSecret(
            onContinueClick = { route, type ->
                navController.navigateToInheritanceShareSecretInfo(
                    magicalPhrase = route.magicalPhrase,
                    type = type,
                    planFlow = route.planFlow,
                    walletId = route.walletId,
                    sourceFlow = route.sourceFlow,
                )
            },
        )

        inheritanceShareSecretInfo(
            navigator = navigator,
            onNavigateToHowItWorks = { type ->
                navController.navigateToInheritanceHowItWorks(type = type)
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
                navController.navigateToInheritanceRequestPlanningSentSuccess()
            },
        )

        inheritanceRequestPlanningSentSuccess(
            onGotItClick = { activity.finish() },
        )
    }
}

@Composable
internal fun MembershipStepEffect(membershipStepManager: MembershipStepManager) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val trackerId = remember(lifecycleOwner) { System.identityHashCode(lifecycleOwner) }

    DisposableEffect(lifecycleOwner, membershipStepManager, trackerId) {
        membershipStepManager.attachStepTracker(trackerId)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                membershipStepManager.detachStepTracker(trackerId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                membershipStepManager.detachStepTracker(trackerId)
            }
        }
    }
}

fun returnToReleaseScheduleDetail(navController: NavController) {
    val isShowingReleaseScheduleDetail =
        navController.currentDestination?.hasRoute<InheritanceReleaseScheduleDetailRoute>() == true
    if (isShowingReleaseScheduleDetail) return
    val didPop =
        navController.popBackStack<InheritanceReleaseScheduleDetailRoute>(inclusive = false)
    if (!didPop) {
        navController.popBackStack()
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
            navController.navigateToInheritanceNotificationSettings(isUpdateRequest = isUpdateRequest)
        } else {
            navController.popBackStack()
        }
    } else if (activityViewModel.isMiniscriptWallet()) {
        navController.navigateToInheritanceNotificationSettings()
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

private fun beneficiaryScheduleKey(email: String): String = email.trim().lowercase()

private fun InheritancePlanningParam.SetupOrReview.finalScheduledPayoutTimeMillis(): Long? {
    val scheduleStates = when (releaseMethodType) {
        InheritanceReleaseMethodType.SHARED_SCHEDULE -> {
            listOfNotNull(sharedScheduleConfig?.releaseScheduleUiState)
        }

        InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES -> {
            individualScheduleConfigs.values.map { it.releaseScheduleUiState }
        }
    }

    return scheduleStates
        .flatMap { uiState -> uiState.stages }
        .map { stage -> stage.finalWithdrawalDate().toStartOfDayEpochMillis() }
        .maxOrNull()
}

private fun ReleaseScheduleDate.toStartOfDayEpochMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@Composable
internal fun releaseScheduleBufferPeriodSummaryText(
    period: Period?,
    applyType: InheritanceBufferPeriodApplyType?,
): String? {
    if (period == null) {
        return stringResource(id = R.string.nc_release_schedule_buffer_period_summary_no_buffer)
    }
    if (applyType == null) return period.displayName
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

@Composable
internal fun fallbackSettingsSummaryText(
    fallbackSettings: InheritanceFallbackSettingsValue?,
): String? {
    if (fallbackSettings == null) return null
    return when (fallbackSettings.selectedOption) {
        InheritanceFallbackOption.NO_FALLBACK -> {
            stringResource(id = R.string.nc_fallback_summary_no_fallback)
        }

        InheritanceFallbackOption.INACTIVITY_FALLBACK -> {
            val count = fallbackSettings.triggerValue.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val unitText = when (fallbackSettings.triggerUnit) {
                FallbackTriggerUnit.YEAR -> {
                    if (count == 1) {
                        stringResource(id = R.string.nc_fallback_unit_year)
                    } else {
                        stringResource(id = R.string.nc_fallback_unit_years)
                    }
                }

                FallbackTriggerUnit.MONTH -> {
                    if (count == 1) {
                        stringResource(id = R.string.nc_fallback_unit_month)
                    } else {
                        stringResource(id = R.string.nc_fallback_unit_months)
                    }
                }

                FallbackTriggerUnit.WEEK -> {
                    if (count == 1) {
                        stringResource(id = R.string.nc_fallback_unit_week)
                    } else {
                        stringResource(id = R.string.nc_fallback_unit_weeks)
                    }
                }

                FallbackTriggerUnit.DAY -> {
                    if (count == 1) {
                        stringResource(id = R.string.nc_fallback_unit_day)
                    } else {
                        stringResource(id = R.string.nc_fallback_unit_days)
                    }
                }
            }
            stringResource(
                id = R.string.nc_fallback_summary_inactivity,
                count,
                unitText,
            )
        }

        InheritanceFallbackOption.DATE_BASED_FALLBACK -> {
            stringResource(
                id = R.string.nc_fallback_summary_date_based,
                fallbackSettings.fallbackDate,
            )
        }
    }
}
