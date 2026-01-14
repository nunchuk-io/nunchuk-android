/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.BuildConfig
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.TimelockBased
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.inheritance.EmailNotificationSettings
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceReviewPlanFragment : MembershipFragment(), BottomSheetOptionListener {

    private val viewModel: InheritanceReviewPlanViewModel by viewModels()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val isDoLater = data.getBoolean(GlobalResultKey.DUMMY_TX_INTRO_DO_LATER, false)
                if (isDoLater) {
                    requireActivity().finish()
                } else {
                    val signatureMap =
                        data.serializable<HashMap<String, String>>(GlobalResultKey.SIGNATURE_EXTRA)
                            ?: return@registerForActivityResult
                    val securityQuestionToken =
                        data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                    if (signatureMap.isNotEmpty() || securityQuestionToken.isNotEmpty()) {
                        viewModel.handleFlow(signatureMap, securityQuestionToken)
                    } else if (inheritanceViewModel.setupOrReviewParam.groupId.isNotEmpty()) {
                        viewModel.markSetupInheritance()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceReviewPlanScreen(
                    viewModel = viewModel,
                    inheritanceViewModel = inheritanceViewModel,
                    onEditActivationDateClick = {
                        findNavController().navigate(
                            InheritanceReviewPlanFragmentDirections.actionInheritanceReviewPlanFragmentToInheritanceActivationDateFragment(
                                isUpdateRequest = true,
                            )
                        )
                    },
                    onEditNoteClick = {
                        findNavController().navigate(
                            InheritanceReviewPlanFragmentDirections.actionInheritanceReviewPlanFragmentToInheritanceNoteFragment(
                                isUpdateRequest = true,
                            )
                        )
                    },
                    onNotifyPrefClick = { _, _ ->
                        findNavController().navigate(
                            InheritanceReviewPlanFragmentDirections.actionInheritanceReviewPlanFragmentToInheritanceNotifyPrefFragment(
                                isUpdateRequest = true,
                            )
                        )
                    },
                    onDiscardChange = {
                        showDiscardDialog()
                    },
                    onShareSecretClicked = {
                        findNavController().navigate(
                            InheritanceReviewPlanFragmentDirections.actionInheritanceReviewPlanFragmentToInheritanceShareSecretFragment(
                                magicalPhrase = inheritanceViewModel.setupOrReviewParam.magicalPhrase,
                                planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
                                walletId = inheritanceViewModel.setupOrReviewParam.walletId,
                                sourceFlow = inheritanceViewModel.setupOrReviewParam.sourceFlow
                            )
                        )
                    },
                    onActionTopBarClick = {
                        if (inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                            showActionOptions()
                        }
                    },
                    onViewClaimingInstruction = {
                        val link =
                            if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/howtoclaim" else "https://www.nunchuk.io/howtoclaim"
                        requireActivity().openExternalLink(link)
                    },
                    onEditBufferPeriodClick = {
                        findNavController().navigate(
                            InheritanceReviewPlanFragmentDirections.actionInheritanceReviewPlanFragmentToInheritanceBufferPeriodFragment(
                                isUpdateRequest = true,
                            )
                        )
                    },
                    onBackUpPasswordInfoClick = {
                        findNavController().navigate(
                            InheritanceReviewPlanFragmentDirections.actionInheritanceReviewPlanFragmentToInheritanceBackUpDownloadFragment()
                        )
                    },
                )
            }
        }
    }

    private fun showDiscardDialog() {
        NCWarningDialog(requireActivity()).showDialog(
            title = getString(com.nunchuk.android.wallet.R.string.nc_confirmation),
            message = getString(R.string.nc_are_you_sure_discard_the_change),
            onYesClick = {
                requireActivity().finish()
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess -> {
                    navigator.openWalletAuthentication(
                        walletId = event.walletId,
                        userData = event.userData,
                        requiredSignatures = event.requiredSignatures,
                        type = event.type,
                        groupId = inheritanceViewModel.setupOrReviewParam.groupId,
                        dummyTransactionId = event.dummyTransactionId,
                        launcher = launcher,
                        activityContext = requireActivity()
                    )
                    if (event.dummyTransactionId.isNotEmpty() && inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                        requireActivity().finish()
                    }
                }

                is InheritanceReviewPlanEvent.CreateOrUpdateInheritanceSuccess -> handleFlow()
                is InheritanceReviewPlanEvent.Loading -> showOrHideLoading(loading = event.loading)
                is InheritanceReviewPlanEvent.ProcessFailure -> showError(message = event.message)
                is InheritanceReviewPlanEvent.CancelInheritanceSuccess, InheritanceReviewPlanEvent.MarkSetupInheritance -> handleFlow()
            }
        }
    }

    private fun handleFlow() {
        if (viewModel.reviewFlow == InheritanceReviewPlanViewModel.ReviewFlow.CANCEL) {
            NcToastManager.scheduleShowMessage(message = getString(R.string.nc_inheritance_plan_cancelled_notify))
            handleResult()
        } else if (inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.SETUP) {
            findNavController().navigate(
                InheritanceReviewPlanFragmentDirections.actionInheritanceReviewPlanFragmentToInheritanceCreateSuccessFragment(
                    magicalPhrase = inheritanceViewModel.setupOrReviewParam.magicalPhrase,
                    planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
                    walletId = inheritanceViewModel.setupOrReviewParam.walletId,
                    sourceFlow = inheritanceViewModel.setupOrReviewParam.sourceFlow
                )
            )
        } else if (inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
            NcToastManager.scheduleShowMessage(message = getString(R.string.nc_inheritance_plan_updated_notify))
            handleResult()
        }
    }

    private fun showActionOptions() {
        (childFragmentManager.findFragmentByTag("BottomSheetOption") as? DialogFragment)?.dismiss()
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
        dialog.show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        if (option.type == SheetOptionType.TYPE_CANCEL) {
            viewModel.calculateRequiredSignatures(flow = InheritanceReviewPlanViewModel.ReviewFlow.CANCEL)
        }
    }

    private fun handleResult() {
        requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(GlobalResultKey.UPDATE_INHERITANCE, viewModel.isDataChanged())
            putExtra(GlobalResultKey.WALLET_ID, inheritanceViewModel.setupOrReviewParam.walletId)
        })
        requireActivity().finish()
    }
}


@Composable
fun InheritanceReviewPlanScreen(
    viewModel: InheritanceReviewPlanViewModel = viewModel(),
    inheritanceViewModel: InheritancePlanningViewModel,
    onEditActivationDateClick: (date: Long) -> Unit,
    onEditNoteClick: (note: String) -> Unit,
    onNotifyPrefClick: (isNotifyToday: Boolean, emails: List<String>) -> Unit,
    onDiscardChange: () -> Unit,
    onShareSecretClicked: () -> Unit,
    onActionTopBarClick: () -> Unit,
    onViewClaimingInstruction: () -> Unit = {},
    onEditBufferPeriodClick: (bufferPeriod: Period?) -> Unit = {},
    onBackUpPasswordInfoClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val isContinueButtonEnabled by viewModel.isContinueButtonEnabled.collectAsStateWithLifecycle()
    val sharedUiState by inheritanceViewModel.state.collectAsStateWithLifecycle()
    val setupOrReviewParam = sharedUiState.setupOrReviewParam

    LaunchedEffect(setupOrReviewParam) {
        viewModel.update(setupOrReviewParam)
    }

    InheritanceReviewPlanScreenContent(
        isMiniscriptWallet = sharedUiState.isMiniscriptWallet,
        userEmail = sharedUiState.userEmail,
        remainTime = remainTime,
        planFlow = setupOrReviewParam.planFlow,
        magicalPhrase = setupOrReviewParam.magicalPhrase,
        groupId = setupOrReviewParam.groupId,
        setupOrReviewParam = setupOrReviewParam,
        state = state,
        isContinueButtonEnabled = isContinueButtonEnabled,
        onContinueClicked = {
            viewModel.calculateRequiredSignatures(flow = InheritanceReviewPlanViewModel.ReviewFlow.CREATE_OR_UPDATE)
        },
        onEditActivationDateClick = {
            onEditActivationDateClick(setupOrReviewParam.activationDate)
        },
        onEditNoteClick = {
            onEditNoteClick(setupOrReviewParam.note)
        },
        onNotifyPrefClick = {
            onNotifyPrefClick(setupOrReviewParam.isNotify, setupOrReviewParam.emails)
        },
        onDiscardChange = onDiscardChange,
        onShareSecretClicked = onShareSecretClicked,
        onActionTopBarClick = onActionTopBarClick,
        onViewClaimingInstruction = onViewClaimingInstruction,
        onEditBufferPeriodClick = onEditBufferPeriodClick,
        onBackUpPasswordInfoClick = onBackUpPasswordInfoClick
    )
}

@Composable
fun InheritanceReviewPlanScreenContent(
    isMiniscriptWallet: Boolean = false,
    userEmail: String = "",
    remainTime: Int = 0,
    planFlow: Int = InheritancePlanFlow.VIEW,
    magicalPhrase: String = "",
    groupId: String = "",
    state: InheritanceReviewPlanState = InheritanceReviewPlanState(),
    setupOrReviewParam: InheritancePlanningParam.SetupOrReview = InheritancePlanningParam.SetupOrReview(
        walletId = ""
    ),
    isContinueButtonEnabled: Boolean = true,
    onContinueClicked: () -> Unit = {},
    onShareSecretClicked: () -> Unit = {},
    onDiscardChange: () -> Unit = {},
    onEditActivationDateClick: () -> Unit = {},
    onEditNoteClick: () -> Unit = {},
    onNotifyPrefClick: () -> Unit = {},
    onActionTopBarClick: () -> Unit = {},
    onViewClaimingInstruction: () -> Unit = {},
    onEditBufferPeriodClick: (bufferPeriod: Period?) -> Unit = {},
    onBackUpPasswordInfoClick: () -> Unit = {},
) {
    val isEditable = groupId.isEmpty() || state.currentUserRole.toRole.isMasterOrAdmin
    val magicalPhraseMask = if (groupId.isNotEmpty() && magicalPhrase.isEmpty()) {
        Utils.maskValue("", isMask = true)
    } else {
        magicalPhrase.ifBlank { stringResource(id = R.string.nc_no_listed) }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                val title = if (planFlow == InheritancePlanFlow.SETUP) stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ) else ""
                NcTopAppBar(
                    backgroundColor = colorResource(id = R.color.nc_primary_light_color),
                    title = title,
                    isBack = planFlow != InheritancePlanFlow.VIEW,
                    tintColor = Color.White,
                    actions = {
                        IconButton(onClick = {
                            onActionTopBarClick()
                        }) {
                            var showMoreIcon = false
                            if (planFlow == InheritancePlanFlow.SETUP) {
                                showMoreIcon = false
                            } else {
                                if (groupId.isNotEmpty()) {
                                    if (state.currentUserRole.toRole.isMasterOrAdmin) showMoreIcon =
                                        true
                                } else {
                                    showMoreIcon = true
                                }
                            }
                            if (showMoreIcon && isEditable) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_horizontal),
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    val continueText = if (planFlow == InheritancePlanFlow.SETUP) {
                        stringResource(id = R.string.nc_text_continue)
                    } else {
                        stringResource(id = R.string.nc_save)
                    }
                    if (isEditable) {
                        NcPrimaryDarkButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            enabled = isContinueButtonEnabled,
                            onClick = onContinueClicked
                        ) {
                            Text(text = continueText)
                        }
                        if (planFlow == InheritancePlanFlow.VIEW) {
                            NcOutlineButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                                    .height(48.dp),
                                onClick = onDiscardChange,
                            ) {
                                Text(text = stringResource(R.string.nc_discard_changes))
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                // Header Section with Wallet Info
                item {
                    Column(
                        modifier = Modifier
                            .background(color = colorResource(id = R.color.nc_primary_light_color))
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_review_your_plan),
                            style = NunchukTheme.typography.heading,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        color = colorResource(id = R.color.nc_primary_light_color),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp)
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_assisted_wallet_intro),
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(60.dp),
                                        contentDescription = null
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1.0f)
                                            .padding(start = 8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(top = 4.dp),
                                            color = colorResource(id = R.color.nc_white_color),
                                            text = stringResource(id = R.string.nc_wallet_subject_to_inheritance),
                                            style = NunchukTheme.typography.title
                                        )
                                        Text(
                                            modifier = Modifier.padding(top = 4.dp),
                                            color = colorResource(id = R.color.nc_white_color),
                                            text = state.walletName.orEmpty(),
                                            style = NunchukTheme.typography.body
                                        )
                                    }
                                }

                                Text(
                                    text = stringResource(R.string.nc_provider_need_these_info_title),
                                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                                    style = NunchukTheme.typography.title,
                                    color = Color.White
                                )
                                SpecialDetailPlanItem(
                                    iconId = R.drawable.ic_star_light,
                                    title = stringResource(R.string.nc_magical_phrase),
                                    content = magicalPhraseMask,
                                    editable = false
                                )
                                if (isMiniscriptWallet) {
                                    setupOrReviewParam.inheritanceKeys.forEachIndexed { index, key ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        SpecialDetailPlanItem(
                                            iconId = R.drawable.ic_key,
                                            title = "Inheritance Key ${index + 1}",
                                            subTitle = "XFP: ${key.uppercase()}",
                                            content = stringResource(R.string.nc_12_or_24_word_inheritance_key_backup),
                                            editable = false
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val isSingleKey = setupOrReviewParam.inheritanceKeys.size == 1
                                    SpecialDetailPlanItem(
                                        iconId = R.drawable.ic_password_light,
                                        title = stringResource(if (isSingleKey) R.string.nc_backup_password else R.string.nc_two_backup_password),
                                        actionText = stringResource(id = R.string.nc_text_info),
                                        content = if (!isSingleKey) stringResource(
                                            id = R.string.nc_backup_passwords_desc
                                        ) else stringResource(id = R.string.nc_backup_password_desc),
                                        editable = true,
                                        onClick = {
                                            onBackUpPasswordInfoClick()
                                        }
                                    )
                                }
                                Text(
                                    text = "Funds become claimable after:",
                                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                                    style = NunchukTheme.typography.title,
                                    color = Color.White
                                )
                                ActivationDateItem(
                                    activationDate = formatDateTimeInTimezone(
                                        timestamp = setupOrReviewParam.activationDate,
                                        timeZoneId = setupOrReviewParam.selectedZoneId,
                                        isOnChainTimelock = isMiniscriptWallet
                                    ),
                                    timeZoneId = setupOrReviewParam.selectedZoneId,
                                    editable = isEditable && !isMiniscriptWallet,
                                    isHeightLock = setupOrReviewParam.timelockBased == TimelockBased.HEIGHT_LOCK,
                                    blockHeight = setupOrReviewParam.blockHeight,
                                    onClick = {
                                        onEditActivationDateClick()
                                    }
                                )
                                if (isEditable && planFlow == InheritancePlanFlow.VIEW) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    NcOutlineButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .height(48.dp),
                                        borderColor = Color.White,
                                        onClick = onShareSecretClicked,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.nc_share_your_secrets),
                                            color = Color.White,
                                            style = NunchukTheme.typography.title
                                        )
                                    }
                                    Text(
                                        text = stringResource(id = R.string.nc_view_claiming_instructions),
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        style = NunchukTheme.typography.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                            .clickable {
                                                onViewClaimingInstruction()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp, top = 24.dp
                        )
                    ) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            Text(
                                text = stringResource(id = R.string.nc_note_to_beneficiary_trustee),
                                style = NunchukTheme.typography.title
                            )
                            Spacer(modifier = Modifier.weight(weight = 1f))
                            if (isEditable) {
                                Text(
                                    text = stringResource(id = R.string.nc_edit),
                                    style = NunchukTheme.typography.title,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        onEditNoteClick()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.greyLight,
                                shape = RoundedCornerShape(8.dp)
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = setupOrReviewParam.note.ifBlank { stringResource(id = R.string.nc_no_note) },
                                style = NunchukTheme.typography.body,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                if (!isMiniscriptWallet) {
                    item(key = "divider_1") {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.whisper
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp, top = 24.dp
                            )
                        ) {
                            Row(horizontalArrangement = Arrangement.Center) {
                                Text(
                                    text = stringResource(id = R.string.nc_buffer_period),
                                    style = NunchukTheme.typography.title
                                )
                                Spacer(modifier = Modifier.weight(weight = 1f))
                                if (isEditable) {
                                    Text(
                                        text = stringResource(id = R.string.nc_edit),
                                        style = NunchukTheme.typography.title,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable {
                                            onEditBufferPeriodClick(setupOrReviewParam.bufferPeriod)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.background(
                                    color = MaterialTheme.colorScheme.greyLight,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = setupOrReviewParam.bufferPeriod?.displayName.orEmpty()
                                        .ifBlank { stringResource(id = R.string.nc_no_buffer) },
                                    style = NunchukTheme.typography.body,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }

                // Divider
                item(key = "divider_2") {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.whisper
                    )
                }

                // Notification Preferences Header
                item(key = "notification_preferences_header") {
                    Row(
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp, top = 24.dp
                        ),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_notification_preferences),
                            style = NunchukTheme.typography.title,
                        )
                        Spacer(modifier = Modifier.weight(weight = 1f))
                        if (isEditable) {
                            Text(
                                text = stringResource(id = R.string.nc_edit),
                                style = NunchukTheme.typography.title,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    onNotifyPrefClick()
                                }
                            )
                        }
                    }
                }

                // Provider Notification Settings (Beneficiary/Trustee Emails)
                if (setupOrReviewParam.notificationSettings != null && setupOrReviewParam.notificationSettings.perEmailSettings.isNotEmpty()) {
                    // User Notification Settings (Owner Email)
                    item {
                        UserNotificationSettingsContent(
                            emailMeWalletConfig = setupOrReviewParam.notificationSettings.emailMeWalletConfig,
                            userEmail = userEmail
                        )
                    }

                    setupOrReviewParam.notificationSettings.perEmailSettings.forEachIndexed { index, emailSettings ->
                        item {
                            ProviderNotificationSettingsContent(emailSettings = emailSettings)
                        }
                    }
                } else {
                    item {
                        SimpleNotificationCard(
                            emails = setupOrReviewParam.emails,
                            isNotifyToday = setupOrReviewParam.isNotify
                        )
                    }
                }

                // Bottom spacing
                item(key = "notification_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SimpleNotificationCard(
    emails: List<String>,
    isNotifyToday: Boolean
) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.greyLight,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.nc_beneficiary_trustee_email_address),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.fillMaxWidth(0.3f),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = emails.joinToString("\n")
                        .ifEmpty { "(${stringResource(id = R.string.nc_none)})" },
                    style = NunchukTheme.typography.title
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 24.dp,
                    bottom = 24.dp
                ),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.whisper
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.nc_notify_them_today),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.fillMaxWidth(0.3f),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isNotifyToday) stringResource(id = R.string.nc_text_yes) else stringResource(
                        id = R.string.nc_text_no
                    ), style = NunchukTheme.typography.title
                )
            }
        }
    }
}

@Composable
fun SpecialDetailPlanItem(
    title: String,
    subTitle: String = "",
    iconId: Int = R.drawable.ic_nc_star_dark,
    content: String = "dolphin concert apple",
    editable: Boolean = false,
    actionText: String = stringResource(id = R.string.nc_edit),
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(
                color = Color.White, shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = iconId),
                tint = colorResource(id = R.color.nc_grey_g7),
                contentDescription = ""
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = title,
                    color = colorResource(id = R.color.nc_grey_g7),
                    style = NunchukTheme.typography.title
                )

                if (subTitle.isNotEmpty()) {
                    Text(
                        text = "($subTitle)",
                        color = colorResource(id = R.color.nc_grey_g7),
                        style = NunchukTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.weight(weight = 1f))
            if (editable) {
                Text(
                    modifier = Modifier.clickable {
                        onClick()
                    },
                    text = actionText,
                    color = colorResource(id = R.color.nc_grey_g7),
                    style = NunchukTheme.typography.title,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            style = NunchukTheme.typography.body,
            color = colorResource(id = R.color.nc_grey_g7),
            modifier = Modifier
                .padding(start = 32.dp)
                .fillMaxWidth()
        )
    }
}

@PreviewLightDark
@Composable
private fun DetailPlanItemPreview() {
    SpecialDetailPlanItem(
        title = "Magical Phrase",
        iconId = R.drawable.ic_star_light,
        content = "dolphin concert apple",
        editable = true,
        onClick = {}
    )
}

@PreviewLightDark
@Composable
private fun UserNotificationSettingsPreview() {
    NunchukTheme {
        UserNotificationSettingsContent(
            emailMeWalletConfig = true,
            userEmail = "user@example.com"
        )
    }
}

@PreviewLightDark
@Composable
private fun ProviderNotificationSettingsPreview() {
    NunchukTheme {
        ProviderNotificationSettingsContent(
            emailSettings = EmailNotificationSettings(
                email = "beneficiary@example.com",
                notifyOnTimelockExpiry = true,
                notifyOnWalletChanges = false,
                includeWalletConfiguration = true
            )
        )
    }
}

@PreviewLightDark
@Composable
private fun SimpleNotificationCardPreview() {
    NunchukTheme {
        SimpleNotificationCard(
            emails = listOf("email1@example.com", "email2@example.com"),
            isNotifyToday = true
        )
    }
}

@PreviewLightDark
@Composable
private fun InheritanceReviewPlanScreenPreview() {
    InheritanceReviewPlanScreenContent(
        state = InheritanceReviewPlanState(
            walletName = "My Wallet"
        ),
        setupOrReviewParam = InheritancePlanningParam.SetupOrReview(
            walletId = "wallet123",
            activationDate = System.currentTimeMillis(),
            note = "Sample note",
            emails = listOf("email1@example.com", "email2@example.com"),
            isNotify = true,
            magicalPhrase = "sample magical phrase"
        ),
    )
}