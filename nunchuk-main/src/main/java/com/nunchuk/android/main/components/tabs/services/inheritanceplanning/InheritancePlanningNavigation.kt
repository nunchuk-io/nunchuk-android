package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.BuildConfig
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.InheritanceActivationDateEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.InheritanceActivationDateScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload.InheritanceBackUpDownloadContent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.InheritanceBeneficiarySchedulesScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod.InheritanceBufferPeriodEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod.InheritanceBufferPeriodScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod.BufferPeriodMethodOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod.InheritanceBufferPeriodMethodScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.createsuccess.InheritanceCreateSuccessScreenContent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution.BeneficiaryType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution.InheritanceCustomizedDistributionScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod.InheritanceDistributionMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.distributionmethod.InheritanceDistributionMethodScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup.FindBackupPasswordContent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.howitworks.InheritanceHowItWorksScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.intro.InheritanceSetupIntroScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip.InheritanceKeyTipContent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase.MagicalPhraseIntroEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase.MagicalPhraseIntroScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note.InheritanceNoteEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note.InheritanceNoteScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref.InheritanceNotifyPrefScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings.InheritanceNotificationSettingsScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.planoverview.InheritancePlanOverviewScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm.InheritanceRequestPlanningConfirmEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm.InheritanceRequestPlanningConfirmScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm.InheritanceRequestPlanningConfirmViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.sent.InheritanceRequestPlanningSentSuccessScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanFragment
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanGroupEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanGroupScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanGroupViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.InheritanceReleaseMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.InheritanceReleaseMethodScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedule.InheritanceReleaseScheduleScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.InheritanceReleaseScheduleDetailScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit.InheritanceReleaseScheduleStageEditScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo.InheritanceShareSecretInfoScreen
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.timelockinfo.InheritanceTimelockInfoContent
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardActivity
import com.nunchuk.android.model.Period
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceSetupIntroRoute

@Serializable
data object InheritancePlanOverviewRoute

@Serializable
data object InheritanceDistributionMethodRoute

@Serializable
data object InheritanceCustomizedDistributionRoute

@Serializable
data object InheritanceReleaseMethodRoute

@Serializable
data object InheritanceBeneficiarySchedulesRoute

@Serializable
data object MagicalPhraseIntroRoute

@Serializable
data class FindBackupPasswordRoute(val stepNumber: Int = 1)

@Serializable
data object InheritanceKeyTipRoute

@Serializable
data object InheritanceReleaseScheduleRoute

@Serializable
data class InheritanceReleaseScheduleDetailRoute(
    val isPostBufferPeriodMethod: Boolean = false,
    val fromBeneficiarySchedules: Boolean = false,
)

@Serializable
data class InheritanceReleaseScheduleStageEditRoute(
    val stageId: Int,
    val isNewStage: Boolean = false,
)

@Serializable
data object InheritanceTimelockInfoRoute

@Serializable
data class InheritanceActivationDateRoute(val isUpdateRequest: Boolean = false)

@Serializable
data class InheritanceNoteRoute(val isUpdateRequest: Boolean = false)

@Serializable
data class InheritanceBufferPeriodRoute(
    val isUpdateRequest: Boolean = false,
    val fromBeneficiarySchedules: Boolean = false,
)

@Serializable
data class InheritanceBufferPeriodMethodRoute(
    val fromBeneficiarySchedules: Boolean = false,
)

@Serializable
data class InheritanceNotifyPrefRoute(val isUpdateRequest: Boolean = false)

@Serializable
data class InheritanceNotificationSettingsRoute(val isUpdateRequest: Boolean = false)

@Serializable
data object InheritanceReviewPlanRoute

@Serializable
data object InheritanceReviewPlanGroupRoute

@Serializable
data class InheritanceCreateSuccessRoute(
    val sourceFlow: Int = InheritanceSourceFlow.NONE,
    val magicalPhrase: String = "",
    val planFlow: Int = InheritancePlanFlow.NONE,
    val walletId: String = "",
)

@Serializable
data class InheritanceShareSecretRoute(
    val sourceFlow: Int = InheritanceSourceFlow.NONE,
    val magicalPhrase: String = "",
    val planFlow: Int = InheritancePlanFlow.NONE,
    val walletId: String = "",
)

@Serializable
data class InheritanceShareSecretInfoRoute(
    val sourceFlow: Int = InheritanceSourceFlow.NONE,
    val magicalPhrase: String = "",
    val type: Int = 0,
    val planFlow: Int = InheritancePlanFlow.NONE,
    val walletId: String = "",
)

@Serializable
data class InheritanceHowItWorksRoute(val type: Int)

@Serializable
data object InheritanceBackUpDownloadRoute

@Serializable
data class InheritanceRequestPlanningConfirmRoute(
    val walletId: String,
    val groupId: String
)

@Serializable
data object InheritanceRequestPlanningSentSuccessRoute

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
            groupId = param.groupId
        )
        else -> InheritanceReviewPlanRoute
    }
}

@Composable
fun InheritancePlanningGraph(
    activity: InheritancePlanningActivity,
    navigator: NunchukNavigator,
    membershipStepManager: MembershipStepManager,
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var releaseScheduleUiState by remember { mutableStateOf(ReleaseScheduleUiState()) }
    var pendingNewStage by remember { mutableStateOf<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage?>(null) }

    NavHost(
        navController = navController,
        startDestination = startRoute,
    ) {
        composable<InheritanceSetupIntroRoute> {
            MembershipStepEffect(membershipStepManager)
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.intro.InheritanceSetupIntroViewModel>()
            InheritanceSetupIntroScreen(
                viewModel = viewModel,
                onContinueClicked = {
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(
                            setupFlowType = InheritanceSetupFlowType.OLD_FLOW
                        )
                    )
                    navController.navigate(InheritanceDistributionMethodRoute)
                }
            )
        }
        composable<InheritancePlanOverviewRoute> {
            MembershipStepEffect(membershipStepManager)
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.planoverview.InheritancePlanOverviewViewModel>()
            val sharedState by activityViewModel.state.collectAsStateWithLifecycle()
            InheritancePlanOverviewScreen(
                viewModel = viewModel,
                groupWalletType = activityViewModel.getGroupWalletType(),
                isMiniscriptWallet = sharedState.isMiniscriptWallet,
                onContinueClicked = {
                    if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY) {
                        navController.navigate(InheritanceReleaseMethodRoute)
                    } else {
                        navController.navigate(MagicalPhraseIntroRoute)
                    }
                }
            )
        }
        composable<InheritanceDistributionMethodRoute> {
            MembershipStepEffect(membershipStepManager)
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            InheritanceDistributionMethodScreen(
                remainTime = remainTime,
                onContinueClicked = { method ->
                    if (method == InheritanceDistributionMethod.CUSTOMIZED) {
                        navController.navigate(InheritanceCustomizedDistributionRoute)
                    } else {
                        activityViewModel.setOrUpdate(
                            activityViewModel.setupOrReviewParam.copy(
                                setupFlowType = InheritanceSetupFlowType.OLD_FLOW
                            )
                        )
                        navController.navigate(InheritancePlanOverviewRoute)
                    }
                }
            )
        }
        composable<InheritanceCustomizedDistributionRoute> {
            MembershipStepEffect(membershipStepManager)
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            InheritanceCustomizedDistributionScreen(
                remainTime = remainTime,
                onContinueClicked = { type ->
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(
                            setupFlowType = when (type) {
                                BeneficiaryType.SINGLE -> InheritanceSetupFlowType.SINGLE_BENEFICIARY
                                BeneficiaryType.MULTI -> InheritanceSetupFlowType.MULTI_BENEFICIARY
                            }
                        )
                    )
                    navController.navigate(InheritancePlanOverviewRoute)
                }
            )
        }
        composable<MagicalPhraseIntroRoute> {
            MembershipStepEffect(membershipStepManager)
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase.MagicalPhraseIntroViewModel>()
            val sharedState by activityViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.init(activityViewModel.setupOrReviewParam)
            }

            LaunchedEffect(viewModel, lifecycleOwner) {
                viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { event ->
                        when (event) {
                            is MagicalPhraseIntroEvent.OnContinueClicked -> {
                                activityViewModel.setOrUpdate(
                                    activityViewModel.setupOrReviewParam.copy(
                                        magicalPhrase = event.magicalPhrase,
                                        inheritanceKeys = event.inheritanceKeys
                                    )
                                )
                                if (activityViewModel.isMiniscriptWallet()) {
                                    navController.navigate(InheritanceKeyTipRoute)
                                } else {
                                    navController.navigate(FindBackupPasswordRoute())
                                }
                            }

                            is MagicalPhraseIntroEvent.Error -> NCToastMessage(activity).showError(event.message)
                            is MagicalPhraseIntroEvent.Loading -> activity.showOrHideLoading(event.loading)
                        }
                    }
            }
            MagicalPhraseIntroScreen(
                viewModel = viewModel,
                isMiniscriptWallet = sharedState.isMiniscriptWallet
            )
        }
        composable<FindBackupPasswordRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<FindBackupPasswordRoute>()
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup.FindBackupPasswordViewModel>()
            val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
            val uiState by activityViewModel.state.collectAsStateWithLifecycle()

            FindBackupPasswordContent(
                remainTime = remainTime,
                inheritanceKeyType = if (uiState.keyTypes.isNotEmpty()) {
                    uiState.keyTypes[route.stepNumber - 1]
                } else {
                    InheritanceKeyType.TAPSIGNER
                },
                numOfKeys = uiState.keyTypes.size,
                keyTypes = uiState.keyTypes,
                stepNumber = route.stepNumber
            ) {
                if (uiState.keyTypes.size == 2 && route.stepNumber == 1) {
                    navController.navigate(FindBackupPasswordRoute(stepNumber = 2))
                } else if (activityViewModel.getGroupWalletType() == com.nunchuk.android.model.byzantine.GroupWalletType.THREE_OF_FIVE_INHERITANCE) {
                    navController.navigate(InheritanceActivationDateRoute())
                } else {
                    navController.navigate(InheritanceKeyTipRoute)
                }
            }
        }
        composable<InheritanceKeyTipRoute> {
            MembershipStepEffect(membershipStepManager)
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            val sharedState by activityViewModel.state.collectAsStateWithLifecycle()
            InheritanceKeyTipContent(
                remainTime = remainTime,
                numberOfKey = sharedState.keyTypes.size,
                isMiniscriptWallet = sharedState.isMiniscriptWallet,
                onContinueClicked = {
                    if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                        navController.navigate(InheritanceReleaseScheduleRoute)
                    } else if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY) {
                        navController.navigate(InheritanceReleaseMethodRoute)
                    } else if (activityViewModel.isMiniscriptWallet()) {
                        navController.navigate(InheritanceTimelockInfoRoute)
                    } else {
                        navController.navigate(InheritanceActivationDateRoute())
                    }
                }
            )
        }
        composable<InheritanceReleaseMethodRoute> {
            MembershipStepEffect(membershipStepManager)
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            val setupOrReviewParam = activityViewModel.setupOrReviewParam
            InheritanceReleaseMethodScreen(
                remainTime = remainTime,
                selectedMethod = setupOrReviewParam.releaseMethodType.toReleaseMethodOption(),
                onBackClicked = {
                    navController.popBackStack()
                },
                onContinueClicked = { method ->
                    val previousDestination = navController.previousBackStackEntry?.destination
                    activityViewModel.setOrUpdate(
                        setupOrReviewParam.copy(
                            releaseMethodType = method.toReleaseMethodType(),
                            beneficiaryAllocations = setupOrReviewParam.beneficiaryAllocations.ifEmpty {
                                defaultBeneficiaryAllocations()
                            }
                        )
                    )
                    if (previousDestination?.hasRoute<InheritanceBeneficiarySchedulesRoute>() == true) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(InheritanceBeneficiarySchedulesRoute)
                    }
                }
            )
        }
        composable<InheritanceBeneficiarySchedulesRoute> {
            MembershipStepEffect(membershipStepManager)
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            val setupOrReviewParam = activityViewModel.setupOrReviewParam
            InheritanceBeneficiarySchedulesScreen(
                remainTime = remainTime,
                releaseMethod = setupOrReviewParam.releaseMethodType.toReleaseMethodOption(),
                beneficiaries = setupOrReviewParam.beneficiaryAllocations.ifEmpty {
                    defaultBeneficiaryAllocations()
                },
                releaseScheduleUiState = releaseScheduleUiState,
                sharedBufferPeriodSummaryText = releaseScheduleBufferPeriodSummaryText(
                    period = setupOrReviewParam.bufferPeriod,
                    applyType = setupOrReviewParam.bufferPeriodApplyType
                ),
                isSharedScheduleConfigured = setupOrReviewParam.isSharedScheduleConfigured,
                onBackClicked = { navController.popBackStack() },
                onEditReleaseMethodClicked = { navController.navigate(InheritanceReleaseMethodRoute) },
                onAddReleaseScheduleClicked = {
                    activityViewModel.setOrUpdate(
                        setupOrReviewParam.copy(isSharedScheduleConfigured = false)
                    )
                    navController.navigate(
                        InheritanceReleaseScheduleDetailRoute(fromBeneficiarySchedules = true)
                    )
                },
                onEditSharedScheduleClicked = {
                    navController.navigate(
                        InheritanceReleaseScheduleDetailRoute(fromBeneficiarySchedules = true)
                    )
                },
                onEditBeneficiaryScheduleClicked = {
                    // TODO: navigate to beneficiary schedule setup screen.
                }
            )
        }
        composable<InheritanceReleaseScheduleRoute> {
            MembershipStepEffect(membershipStepManager)
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            InheritanceReleaseScheduleScreen(
                remainTime = remainTime,
                onContinueClicked = {
                    navController.navigate(InheritanceReleaseScheduleDetailRoute())
                }
            )
        }
        composable<InheritanceReleaseScheduleDetailRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            val setupOrReviewParam = activityViewModel.setupOrReviewParam
            val route = backStackEntry.toRoute<InheritanceReleaseScheduleDetailRoute>()
            InheritanceReleaseScheduleDetailScreen(
                remainTime = remainTime,
                uiState = releaseScheduleUiState,
                descriptionText = if (route.fromBeneficiarySchedules) {
                    stringResource(id = R.string.nc_release_schedule_shared_beneficiary_desc)
                } else {
                    null
                },
                bufferPeriodSummaryText = releaseScheduleBufferPeriodSummaryText(
                    period = setupOrReviewParam.bufferPeriod,
                    applyType = setupOrReviewParam.bufferPeriodApplyType
                ),
                onUiStateChanged = { releaseScheduleUiState = it },
                onEditStage = { stage ->
                    navController.navigate(
                        InheritanceReleaseScheduleStageEditRoute(
                            stageId = stage.id,
                            isNewStage = false,
                        )
                    )
                },
                onEditBufferPeriodClicked = {
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
                        InheritanceReleaseScheduleStageEditRoute(
                            stageId = newStage.id,
                            isNewStage = true,
                        )
                    )
                },
                onContinueClicked = {
                    val firstStageTimeZoneId = releaseScheduleUiState.stages.firstOrNull()?.timeZoneId.orEmpty()
                    if (firstStageTimeZoneId.isNotEmpty() && firstStageTimeZoneId != setupOrReviewParam.selectedZoneId) {
                        activityViewModel.setOrUpdate(
                            setupOrReviewParam.copy(selectedZoneId = firstStageTimeZoneId)
                        )
                    }
                    if (route.fromBeneficiarySchedules && route.isPostBufferPeriodMethod) {
                        activityViewModel.setOrUpdate(
                            activityViewModel.setupOrReviewParam.copy(
                                isSharedScheduleConfigured = true
                            )
                        )
                        val didPopToBeneficiarySchedule = navController.popBackStack<InheritanceBeneficiarySchedulesRoute>(
                            inclusive = false
                        )
                        if (!didPopToBeneficiarySchedule) {
                            navController.navigate(InheritanceBeneficiarySchedulesRoute)
                        }
                    } else if (route.fromBeneficiarySchedules) {
                        navController.navigate(
                            InheritanceBufferPeriodRoute(fromBeneficiarySchedules = true)
                        )
                    } else if (setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY &&
                        route.isPostBufferPeriodMethod &&
                        setupOrReviewParam.bufferPeriod != null &&
                        setupOrReviewParam.bufferPeriodApplyType != null
                    ) {
                        navController.navigate(InheritanceNoteRoute())
                    } else if (setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                        navController.navigate(InheritanceBufferPeriodRoute())
                    } else if (activityViewModel.isMiniscriptWallet()) {
                        navController.navigate(InheritanceTimelockInfoRoute)
                    } else {
                        navController.navigate(InheritanceActivationDateRoute())
                    }
                }
            )
        }
        composable<InheritanceReleaseScheduleStageEditRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceReleaseScheduleStageEditRoute>()
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            fun returnToReleaseScheduleDetail() {
                val isShowingReleaseScheduleDetail =
                    navController.currentDestination?.hasRoute<InheritanceReleaseScheduleDetailRoute>() == true
                if (isShowingReleaseScheduleDetail) return

                val didPopToReleaseScheduleDetail =
                    navController.popBackStack<InheritanceReleaseScheduleDetailRoute>(inclusive = false)
                if (!didPopToReleaseScheduleDetail) {
                    navController.navigate(InheritanceReleaseScheduleDetailRoute())
                }
            }
            val stage = if (route.isNewStage) {
                pendingNewStage
            } else {
                releaseScheduleUiState.getStage(route.stageId)
            }

            if (stage == null && route.isNewStage.not()) {
                LaunchedEffect(route.stageId) {
                    returnToReleaseScheduleDetail()
                }
            } else if (stage != null) {
                val previousStageDate = releaseScheduleUiState.previousStageDate(stage.stageNumber)
                InheritanceReleaseScheduleStageEditScreen(
                    remainTime = remainTime,
                    stage = stage,
                    previousStageDate = previousStageDate,
                    isNewStage = route.isNewStage,
                    onBackClicked = {
                        returnToReleaseScheduleDetail()
                        if (route.isNewStage) pendingNewStage = null
                    },
                    onDeleteClicked = { stageId ->
                        if (route.isNewStage) {
                            returnToReleaseScheduleDetail()
                            pendingNewStage = null
                        } else {
                            returnToReleaseScheduleDetail()
                            releaseScheduleUiState = releaseScheduleUiState.deleteStage(stageId)
                            val firstStageTimeZoneId =
                                releaseScheduleUiState.stages.firstOrNull()?.timeZoneId
                                    ?: activityViewModel.setupOrReviewParam.selectedZoneId
                            activityViewModel.setOrUpdate(
                                activityViewModel.setupOrReviewParam.copy(
                                    selectedZoneId = firstStageTimeZoneId
                                )
                            )
                        }
                    },
                    onConfirmClicked = { updatedStage ->
                        releaseScheduleUiState = if (route.isNewStage) {
                            releaseScheduleUiState.appendStage(updatedStage)
                        } else {
                            releaseScheduleUiState.updateStage(updatedStage)
                        }
                        if (updatedStage.stageNumber == 1) {
                            activityViewModel.setOrUpdate(
                                activityViewModel.setupOrReviewParam.copy(
                                    selectedZoneId = updatedStage.timeZoneId
                                )
                            )
                        }
                        returnToReleaseScheduleDetail()
                        if (route.isNewStage) pendingNewStage = null
                    }
                )
            }
        }
        composable<InheritanceTimelockInfoRoute> {
            MembershipStepEffect(membershipStepManager)
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            InheritanceTimelockInfoContent(
                remainTime = remainTime,
                onContinueClicked = {
                    navController.navigate(InheritanceNoteRoute())
                }
            )
        }
        composable<InheritanceActivationDateRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceActivationDateRoute>()
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.InheritanceActivationDateViewModel>()

            LaunchedEffect(viewModel, lifecycleOwner, route) {
                viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { event ->
                        when (event) {
                            is InheritanceActivationDateEvent.ContinueClick -> {
                                activityViewModel.setOrUpdate(
                                    activityViewModel.setupOrReviewParam.copy(
                                        activationDate = event.date,
                                        selectedZoneId = event.selectedZoneId
                                    )
                                )
                                if (route.isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                                    navController.popBackStack()
                                } else {
                                    navController.navigate(InheritanceNoteRoute())
                                }
                            }
                        }
                    }
            }

            InheritanceActivationDateScreen(
                viewModel = viewModel,
                isUpdateRequest = route.isUpdateRequest,
                inheritanceViewModel = activityViewModel
            )
        }
        composable<InheritanceNoteRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceNoteRoute>()
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note.InheritanceNoteViewModel>()

            LaunchedEffect(Unit) {
                viewModel.init(
                    param = activityViewModel.setupOrReviewParam,
                    isUpdateRequest = route.isUpdateRequest
                )
            }

            LaunchedEffect(viewModel, lifecycleOwner, route) {
                viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { event ->
                        when (event) {
                            is InheritanceNoteEvent.ContinueClick -> {
                                activityViewModel.setOrUpdate(
                                    activityViewModel.setupOrReviewParam.copy(
                                        note = event.note
                                    )
                                )
                                if (route.isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                                    navController.popBackStack()
                                } else if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                                    navController.navigate(InheritanceNotifyPrefRoute(isUpdateRequest = route.isUpdateRequest))
                                } else if (activityViewModel.isMiniscriptWallet()) {
                                    navController.navigate(InheritanceNotifyPrefRoute(isUpdateRequest = route.isUpdateRequest))
                                } else {
                                    navController.navigate(InheritanceBufferPeriodRoute())
                                }
                            }
                        }
                    }
            }

            InheritanceNoteScreen(
                viewModel = viewModel,
                isUpdateRequest = route.isUpdateRequest,
                inheritanceViewModel = activityViewModel
            )
        }
        composable<InheritanceBufferPeriodRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceBufferPeriodRoute>()
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod.InheritanceBufferPeriodViewModel>()
            fun returnToReleaseScheduleDetail() {
                val isShowingReleaseScheduleDetail =
                    navController.currentDestination?.hasRoute<InheritanceReleaseScheduleDetailRoute>() == true
                if (isShowingReleaseScheduleDetail) return

                val didPopToReleaseScheduleDetail =
                    navController.popBackStack<InheritanceReleaseScheduleDetailRoute>(inclusive = false)
                if (!didPopToReleaseScheduleDetail) {
                    navController.navigate(InheritanceReleaseScheduleDetailRoute())
                }
            }

            LaunchedEffect(Unit) {
                viewModel.init(
                    param = activityViewModel.setupOrReviewParam,
                    isUpdateRequest = route.isUpdateRequest
                )
            }

            LaunchedEffect(viewModel, lifecycleOwner, route) {
                viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { event ->
                        when (event) {
                            is InheritanceBufferPeriodEvent.Loading -> activity.showOrHideLoading(event.isLoading)
                            is InheritanceBufferPeriodEvent.Error -> NCToastMessage(activity).showError(event.message)
                            is InheritanceBufferPeriodEvent.OnContinueClick -> {
                                activityViewModel.setOrUpdate(
                                    activityViewModel.setupOrReviewParam.copy(
                                        bufferPeriod = event.period,
                                        bufferPeriodApplyType = if (event.period == null) {
                                            null
                                        } else {
                                            activityViewModel.setupOrReviewParam.bufferPeriodApplyType
                                        }
                                    )
                                )
                                if (route.isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                                    navController.popBackStack()
                                } else if (route.fromBeneficiarySchedules) {
                                    if (event.period == null) {
                                        navController.navigate(
                                            InheritanceReleaseScheduleDetailRoute(
                                                isPostBufferPeriodMethod = true,
                                                fromBeneficiarySchedules = true,
                                            )
                                        )
                                    } else {
                                        navController.navigate(
                                            InheritanceBufferPeriodMethodRoute(
                                                fromBeneficiarySchedules = true
                                            )
                                        )
                                    }
                                } else if (activityViewModel.setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY) {
                                    if (event.period == null) {
                                        navController.navigate(InheritanceNoteRoute())
                                    } else {
                                        navController.navigate(InheritanceBufferPeriodMethodRoute())
                                    }
                                } else {
                                    navController.navigate(InheritanceNotifyPrefRoute())
                                }
                            }
                        }
                    }
            }

            InheritanceBufferPeriodScreen(
                viewModel = viewModel,
                isUpdateRequest = route.isUpdateRequest,
                inheritanceViewModel = activityViewModel
            )
        }
        composable<InheritanceBufferPeriodMethodRoute> {
            MembershipStepEffect(membershipStepManager)
            val route = it.toRoute<InheritanceBufferPeriodMethodRoute>()
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            val selectedOption = activityViewModel.setupOrReviewParam.bufferPeriodApplyType
                ?.toBufferPeriodMethodOption()
                ?: BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY
            InheritanceBufferPeriodMethodScreen(
                remainTime = remainTime,
                selectedOption = selectedOption,
                onBackClicked = { navController.popBackStack() },
                onContinueClicked = { option ->
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(
                            bufferPeriodApplyType = option.toBufferPeriodApplyType()
                        )
                    )
                    navController.navigate(
                        InheritanceReleaseScheduleDetailRoute(
                            isPostBufferPeriodMethod = true,
                            fromBeneficiarySchedules = route.fromBeneficiarySchedules,
                        )
                    )
                }
            )
        }
        composable<InheritanceNotifyPrefRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceNotifyPrefRoute>()
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref.InheritanceNotifyPrefViewModel>()

            LaunchedEffect(Unit) {
                viewModel.init(
                    param = activityViewModel.setupOrReviewParam,
                    isUpdateRequest = route.isUpdateRequest
                )
            }

            InheritanceNotifyPrefScreen(
                viewModel = viewModel,
                isUpdateRequest = route.isUpdateRequest,
                inheritanceViewModel = activityViewModel,
                onSkipClick = {
                    openReviewPlanScreen(
                        navController = navController,
                        route = route,
                        activityViewModel = activityViewModel,
                        isDiscard = true,
                        emails = emptyList(),
                        isNotify = false,
                    )
                },
                onContinueClick = { emails, isNotify ->
                    openReviewPlanScreen(
                        navController = navController,
                        route = route,
                        activityViewModel = activityViewModel,
                        isDiscard = false,
                        emails = emails,
                        isNotify = isNotify,
                    )
                }
            )
        }
        composable<InheritanceNotificationSettingsRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceNotificationSettingsRoute>()
            InheritanceNotificationSettingsScreen(
                isUpdateRequest = route.isUpdateRequest,
                inheritanceViewModel = activityViewModel,
                membershipStepManager = membershipStepManager,
                onContinueClick = { emailSettings, emailMeWalletConfig ->
                    val notificationSettings = com.nunchuk.android.model.inheritance.InheritanceNotificationSettings(
                        emailMeWalletConfig = emailMeWalletConfig,
                        perEmailSettings = emailSettings
                    )
                    activityViewModel.setOrUpdate(
                        activityViewModel.setupOrReviewParam.copy(
                            notificationSettings = notificationSettings
                        )
                    )
                    if (route.isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                        navController.popBackStack<InheritanceReviewPlanRoute>(inclusive = false)
                    } else {
                        navController.navigate(InheritanceReviewPlanRoute)
                    }
                }
            )
        }
        composable<InheritanceReviewPlanRoute> {
            MembershipStepEffect(membershipStepManager)
            val viewModel = hiltViewModel<InheritanceReviewPlanViewModel>()

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
                                    activityContext = activity
                                )
                                if (event.dummyTransactionId.isNotEmpty() &&
                                    activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW
                                ) {
                                    activity.finish()
                                }
                            }

                            is InheritanceReviewPlanEvent.CreateOrUpdateInheritanceSuccess,
                            is InheritanceReviewPlanEvent.CancelInheritanceSuccess,
                            InheritanceReviewPlanEvent.MarkSetupInheritance -> {
                                handleReviewFlow(
                                    activity = activity,
                                    navController = navController,
                                    viewModel = viewModel,
                                    activityViewModel = activityViewModel
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
                onEditActivationDateClick = {
                    navController.navigate(InheritanceActivationDateRoute(isUpdateRequest = true))
                },
                onEditNoteClick = {
                    navController.navigate(InheritanceNoteRoute(isUpdateRequest = true))
                },
                onNotifyPrefClick = { _, _ ->
                    navController.navigate(InheritanceNotifyPrefRoute(isUpdateRequest = true))
                },
                onDiscardChange = {
                    NCWarningDialog(activity).showDialog(
                        title = activity.getString(com.nunchuk.android.wallet.R.string.nc_confirmation),
                        message = activity.getString(R.string.nc_are_you_sure_discard_the_change),
                        onYesClick = { activity.finish() }
                    )
                },
                onShareSecretClicked = {
                    val param = activityViewModel.setupOrReviewParam
                    navController.navigate(
                        InheritanceShareSecretRoute(
                            magicalPhrase = param.magicalPhrase,
                            planFlow = param.planFlow,
                            walletId = param.walletId,
                            sourceFlow = param.sourceFlow
                        )
                    )
                },
                onActionTopBarClick = {
                    if (activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                        showReviewActionOptions(activity)
                    }
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
                    navController.navigate(InheritanceBackUpDownloadRoute)
                },
            )
        }
        composable<InheritanceReviewPlanGroupRoute> {
            MembershipStepEffect(membershipStepManager)
            val viewModel = hiltViewModel<InheritanceReviewPlanGroupViewModel>()
            val groupId = activityViewModel.state.value.groupId

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
                                        event.requiredSignatures.requiredSignatures
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
                groupId = groupId
            )
        }
        composable<InheritanceCreateSuccessRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<InheritanceCreateSuccessRoute>()
            InheritanceCreateSuccessScreenContent(
                onContinueClick = {
                    navController.navigate(
                        InheritanceShareSecretRoute(
                            magicalPhrase = route.magicalPhrase,
                            planFlow = route.planFlow,
                            walletId = route.walletId,
                            sourceFlow = route.sourceFlow
                        )
                    )
                }
            )
        }
        composable<InheritanceShareSecretRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceShareSecretRoute>()
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretViewModel>()
            LaunchedEffect(viewModel, lifecycleOwner, route) {
                viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { event ->
                        when (event) {
                            is InheritanceShareSecretEvent.ContinueClick -> {
                                navController.navigate(
                                    InheritanceShareSecretInfoRoute(
                                        magicalPhrase = route.magicalPhrase,
                                        type = event.type,
                                        planFlow = route.planFlow,
                                        walletId = route.walletId,
                                        sourceFlow = route.sourceFlow
                                    )
                                )
                            }
                        }
                    }
            }

            InheritanceShareSecretScreen(
                viewModel = viewModel,
                planFlow = route.planFlow
            )
        }
        composable<InheritanceShareSecretInfoRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceShareSecretInfoRoute>()
            val viewModel = hiltViewModel<com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo.InheritanceShareSecretInfoViewModel>()
            InheritanceShareSecretInfoScreen(
                viewModel = viewModel,
                sharedViewModel = activityViewModel,
                type = route.type,
                magicalPhrase = route.magicalPhrase,
                planFlow = route.planFlow,
                onContinue = {
                    if (activityViewModel.isMiniscriptWallet()) {
                        navController.navigate(InheritanceHowItWorksRoute(type = route.type))
                    } else if (route.planFlow == InheritancePlanFlow.SETUP) {
                        NCInfoDialog(activity).showDialog(
                            message = activity.getString(R.string.nc_inheritance_share_secret_info_dialog_desc),
                            onYesClick = {
                                handleInheritanceShareSecretBack(
                                    activity = activity,
                                    sourceFlow = route.sourceFlow,
                                    planFlow = route.planFlow,
                                    walletId = route.walletId,
                                    navigator = navigator
                                )
                            }
                        )
                    } else {
                        handleInheritanceShareSecretBack(
                            activity = activity,
                            sourceFlow = route.sourceFlow,
                            planFlow = route.planFlow,
                            walletId = route.walletId,
                            navigator = navigator
                        )
                    }
                },
                onLearnMoreClicked = {
                    navController.navigate(InheritanceBackUpDownloadRoute)
                },
                onSaveBsms = {
                    activity.showSaveShareOption()
                }
            )
        }
        composable<InheritanceBackUpDownloadRoute> {
            MembershipStepEffect(membershipStepManager)
            InheritanceBackUpDownloadContent(
                onContinueClicked = {
                    navController.popBackStack()
                }
            )
        }
        composable<InheritanceHowItWorksRoute> { backStackEntry ->
            MembershipStepEffect(membershipStepManager)
            val route = backStackEntry.toRoute<InheritanceHowItWorksRoute>()
            InheritanceHowItWorksScreen(
                type = InheritanceShareSecretType.entries.getOrElse(route.type) {
                    InheritanceShareSecretType.DIRECT
                },
                onDoneClick = {
                    activity.finish()
                }
            )
        }
        composable<InheritanceRequestPlanningConfirmRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<InheritanceRequestPlanningConfirmRoute>()
            val viewModel = hiltViewModel<InheritanceRequestPlanningConfirmViewModel>()

            LaunchedEffect(viewModel, lifecycleOwner) {
                viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { event ->
                        when (event) {
                            is InheritanceRequestPlanningConfirmEvent.Loading -> {
                                activity.showOrHideLoading(event.isLoading)
                            }

                            is InheritanceRequestPlanningConfirmEvent.Error -> {
                                NCToastMessage(activity).show(event.message)
                            }

                            InheritanceRequestPlanningConfirmEvent.RequestInheritanceSuccess -> {
                                activity.setResult(Activity.RESULT_OK, Intent().apply {
                                    putExtra(InheritancePlanningActivity.RESULT_REQUEST_PLANNING, true)
                                })
                                navController.navigate(InheritanceRequestPlanningSentSuccessRoute)
                            }
                        }
                    }
            }

            InheritanceRequestPlanningConfirmScreen(
                onCancel = {
                    activity.finish()
                },
                onContinue = {
                    viewModel.requestInheritancePlanning(
                        walletId = route.walletId,
                        groupId = route.groupId
                    )
                }
            )
        }
        composable<InheritanceRequestPlanningSentSuccessRoute> {
            InheritanceRequestPlanningSentSuccessScreen(
                onGotItClick = {
                    activity.finish()
                }
            )
        }
    }
}

@Composable
private fun MembershipStepEffect(membershipStepManager: MembershipStepManager) {
    DisposableEffect(membershipStepManager) {
        membershipStepManager.updateStep(true)
        onDispose {
            membershipStepManager.updateStep(false)
        }
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
                isDeleted = true
            ),
        )
    )
    dialog.show(activity.supportFragmentManager, "BottomSheetOption")
}

private fun handleReviewFlow(
    activity: InheritancePlanningActivity,
    navController: NavController,
    viewModel: InheritanceReviewPlanViewModel,
    activityViewModel: InheritancePlanningViewModel
) {
    if (viewModel.reviewFlow == InheritanceReviewPlanViewModel.ReviewFlow.CANCEL) {
        NcToastManager.scheduleShowMessage(message = activity.getString(R.string.nc_inheritance_plan_cancelled_notify))
        activity.setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(GlobalResultKey.UPDATE_INHERITANCE, viewModel.isDataChanged())
            putExtra(GlobalResultKey.WALLET_ID, activityViewModel.setupOrReviewParam.walletId)
        })
        activity.finish()
    } else if (activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.SETUP) {
        val param = activityViewModel.setupOrReviewParam
        navController.navigate(
            InheritanceCreateSuccessRoute(
                magicalPhrase = param.magicalPhrase,
                planFlow = param.planFlow,
                walletId = param.walletId,
                sourceFlow = param.sourceFlow
            )
        )
    } else if (activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
        NcToastManager.scheduleShowMessage(message = activity.getString(R.string.nc_inheritance_plan_updated_notify))
        activity.setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(GlobalResultKey.UPDATE_INHERITANCE, viewModel.isDataChanged())
            putExtra(GlobalResultKey.WALLET_ID, activityViewModel.setupOrReviewParam.walletId)
        })
        activity.finish()
    }
}

private fun openReviewPlanScreen(
    navController: NavController,
    route: InheritanceNotifyPrefRoute,
    activityViewModel: InheritancePlanningViewModel,
    isDiscard: Boolean,
    emails: List<String>,
    isNotify: Boolean,
) {
    if (!isDiscard) {
        activityViewModel.setOrUpdate(
            activityViewModel.setupOrReviewParam.copy(
                isNotify = isNotify,
                emails = emails
            )
        )
    }
    if (route.isUpdateRequest || activityViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
        if (activityViewModel.isMiniscriptWallet()) {
            navController.navigate(InheritanceNotificationSettingsRoute(isUpdateRequest = route.isUpdateRequest))
        } else {
            navController.popBackStack()
        }
    } else if (activityViewModel.isMiniscriptWallet()) {
        navController.navigate(InheritanceNotificationSettingsRoute())
    } else {
        navController.navigate(InheritanceReviewPlanRoute)
    }
}

private fun BufferPeriodMethodOption.toBufferPeriodApplyType(): InheritanceBufferPeriodApplyType {
    return when (this) {
        BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY -> InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY
        BufferPeriodMethodOption.EVERY_WITHDRAWAL -> InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL
    }
}

private fun InheritanceBufferPeriodApplyType.toBufferPeriodMethodOption(): BufferPeriodMethodOption {
    return when (this) {
        InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY -> BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY
        InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL -> BufferPeriodMethodOption.EVERY_WITHDRAWAL
    }
}

private fun InheritanceReleaseMethod.toReleaseMethodType(): InheritanceReleaseMethodType {
    return when (this) {
        InheritanceReleaseMethod.SHARED_SCHEDULE -> InheritanceReleaseMethodType.SHARED_SCHEDULE
        InheritanceReleaseMethod.INDIVIDUAL_SCHEDULES -> InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES
    }
}

private fun InheritanceReleaseMethodType.toReleaseMethodOption(): InheritanceReleaseMethod {
    return when (this) {
        InheritanceReleaseMethodType.SHARED_SCHEDULE -> InheritanceReleaseMethod.SHARED_SCHEDULE
        InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES -> InheritanceReleaseMethod.INDIVIDUAL_SCHEDULES
    }
}

private fun defaultBeneficiaryAllocations(): List<InheritanceBeneficiaryAllocation> {
    return listOf(
        InheritanceBeneficiaryAllocation(
            email = "Wife@gmail.com",
            allocationPercent = 50,
        ),
        InheritanceBeneficiaryAllocation(
            email = "Son@gmail.com",
            allocationPercent = 25,
        ),
        InheritanceBeneficiaryAllocation(
            email = "Daughter@gmail.com",
            allocationPercent = 25,
        ),
    )
}

@Composable
private fun releaseScheduleBufferPeriodSummaryText(
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
        applyTypeText
    )
}

private fun handleInheritanceShareSecretBack(
    activity: InheritancePlanningActivity,
    sourceFlow: Int,
    planFlow: Int,
    walletId: String,
    navigator: NunchukNavigator,
) {
    when (sourceFlow) {
        InheritanceSourceFlow.GROUP_DASHBOARD -> {
            ActivityManager.popUntil(GroupDashboardActivity::class.java)
        }

        InheritanceSourceFlow.SERVICE_TAB -> activity.finish()
        else -> {
            ActivityManager.popUntilRoot()
            if (planFlow == InheritancePlanFlow.SETUP && sourceFlow == InheritanceSourceFlow.WIZARD) {
                navigator.openWalletDetailsScreen(activity, walletId)
            }
        }
    }
}
