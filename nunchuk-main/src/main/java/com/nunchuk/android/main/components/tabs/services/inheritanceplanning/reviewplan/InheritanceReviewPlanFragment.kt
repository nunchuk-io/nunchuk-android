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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.InheritanceActivationDateFragment
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod.InheritanceBufferPeriodFragment
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note.InheritanceNoteFragment
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref.InheritanceNotifyPrefFragment
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.isInheritanceFlow
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.utils.simpleGlobalDateFormat
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class InheritanceReviewPlanFragment : MembershipFragment(), BottomSheetOptionListener {

    @Inject
    lateinit var navigator: NunchukNavigator

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceReviewPlanScreen(
                    viewModel,
                    inheritanceViewModel,
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
                    })
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
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
        setFragmentResultListener(InheritanceActivationDateFragment.REQUEST_KEY) { _, bundle ->
            val date = bundle.getLong(InheritanceActivationDateFragment.EXTRA_ACTIVATION_DATE)
            viewModel.updateActivationDate(date)
        }
        setFragmentResultListener(InheritanceNoteFragment.REQUEST_KEY) { _, bundle ->
            val note = bundle.getString(InheritanceNoteFragment.EXTRA_NOTE)
            viewModel.updateNote(note.orEmpty())
        }
        setFragmentResultListener(InheritanceNotifyPrefFragment.REQUEST_KEY) { _, bundle ->
            val isNotify = bundle.getBoolean(InheritanceNotifyPrefFragment.EXTRA_IS_NOTIFY)
            val emails = bundle.getStringArrayList(InheritanceNotifyPrefFragment.EXTRA_EMAILS)
            viewModel.updateNotifyPref(isNotify, emails.orEmpty())
        }
        setFragmentResultListener(InheritanceBufferPeriodFragment.REQUEST_KEY) { _, bundle ->
            val period =
                bundle.parcelable<Period>(InheritanceBufferPeriodFragment.EXTRA_BUFFER_PERIOD)
            viewModel.updateBufferPeriod(period)
        }
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
                }

                is InheritanceReviewPlanEvent.CreateOrUpdateInheritanceSuccess -> handleFlow()
                is InheritanceReviewPlanEvent.Loading -> showOrHideLoading(loading = event.loading)
                is InheritanceReviewPlanEvent.ProcessFailure -> showError(message = event.message)
                is InheritanceReviewPlanEvent.CancelInheritanceSuccess, InheritanceReviewPlanEvent.MarkSetupInheritance -> handleFlow()
            }
        }
    }

    private fun handleFlow() {
        if (viewModel.isCreateOrUpdateFlow().not()) {
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
            viewModel.calculateRequiredSignatures(isCreateOrUpdateFlow = false)
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
    onEditBufferPeriodClick: (bufferPeriod: Period?) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()

    InheritanceReviewPlanScreenContent(
        remainTime = remainTime,
        planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
        magicalPhrase = inheritanceViewModel.setupOrReviewParam.magicalPhrase,
        groupId = inheritanceViewModel.setupOrReviewParam.groupId,
        state = state,
        onContinueClicked = {
            viewModel.calculateRequiredSignatures(isCreateOrUpdateFlow = true)
        },
        onEditActivationDateClick = {
            onEditActivationDateClick(state.activationDate)
        },
        onEditNoteClick = {
            onEditNoteClick(state.note)
        },
        onNotifyPrefClick = {
            onNotifyPrefClick(state.isNotifyToday, state.emails)
        },
        onDiscardChange = onDiscardChange,
        onShareSecretClicked = onShareSecretClicked,
        onActionTopBarClick = onActionTopBarClick,
        onViewClaimingInstruction = onViewClaimingInstruction,
        onEditBufferPeriodClick = onEditBufferPeriodClick
    )
}

@Composable
fun InheritanceReviewPlanScreenContent(
    remainTime: Int = 0,
    planFlow: Int = InheritancePlanFlow.VIEW,
    magicalPhrase: String = "",
    groupId: String = "",
    state: InheritanceReviewPlanState = InheritanceReviewPlanState(),
    onContinueClicked: () -> Unit = {},
    onShareSecretClicked: () -> Unit = {},
    onDiscardChange: () -> Unit = {},
    onEditActivationDateClick: () -> Unit = {},
    onEditNoteClick: () -> Unit = {},
    onNotifyPrefClick: () -> Unit = {},
    onActionTopBarClick: () -> Unit = {},
    onViewClaimingInstruction: () -> Unit = {},
    onEditBufferPeriodClick: (bufferPeriod: Period?) -> Unit = {}
) {
    val isAccountSetupInheritance = groupId.isEmpty() || magicalPhrase.isEmpty().not()
    val magicalPhraseMask = if (groupId.isNotEmpty() && magicalPhrase.isEmpty()) {
        Utils.maskValue("", isMask = true)
    } else { magicalPhrase.ifBlank { stringResource(id = R.string.nc_no_listed) } }

    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .background(colorResource(id = R.color.nc_denim_tint_color))
                        .statusBarsPadding()
                ) {
                    val title = if (planFlow == InheritancePlanFlow.SETUP) stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ) else ""
                    NcTopAppBar(
                        backgroundColor = colorResource(id = R.color.nc_denim_tint_color),
                        title = title,
                        elevation = 0.dp,
                        isBack = planFlow != InheritancePlanFlow.VIEW,
                        actions = {
                            IconButton(onClick = {
                                onActionTopBarClick()
                            }) {
                                var showMoreIcon = false
                                if (planFlow == InheritancePlanFlow.SETUP) {
                                    showMoreIcon = false
                                } else {
                                    if (groupId.isNotEmpty()) {
                                        if (state.currentUserRole.toRole.isMasterOrAdmin) showMoreIcon = true
                                    } else {
                                        showMoreIcon = true
                                    }
                                }
                                if (showMoreIcon && isAccountSetupInheritance) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_more_horizontal),
                                        contentDescription = "More"
                                    )
                                }
                            }
                        }
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .background(color = colorResource(id = R.color.nc_denim_tint_color))
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.nc_review_your_plan),
                                style = NunchukTheme.typography.heading,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(
                                            color = colorResource(id = R.color.nc_primary_light_color),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
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

                                    Spacer(modifier = Modifier.height(24.dp))
                                    DetailPlanItem(
                                        iconId = R.drawable.ic_calendar_light,
                                        titleId = R.string.nc_activation_date,
                                        content = Date(state.activationDate).simpleGlobalDateFormat(),
                                        editable = isAccountSetupInheritance,
                                        onClick = {
                                            onEditActivationDateClick()
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    DetailPlanItem(
                                        iconId = R.drawable.ic_star_light,
                                        titleId = R.string.nc_magical_phrase,
                                        content = magicalPhraseMask,
                                        editable = false
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    DetailPlanItem(
                                        iconId = R.drawable.ic_password_light,
                                        titleId = R.string.nc_backup_password,
                                        content = stringResource(id = R.string.nc_backup_password_desc),
                                        editable = false
                                    )
                                    if (isAccountSetupInheritance && planFlow == InheritancePlanFlow.VIEW) {
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
                                if (isAccountSetupInheritance) {
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
                                    color = NcColor.greyLight, shape = RoundedCornerShape(8.dp)
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = state.note.ifBlank { stringResource(id = R.string.nc_no_note) },
                                    style = NunchukTheme.typography.body,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                            thickness = 1.dp,
                            color = NcColor.whisper
                        )
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
                                if (isAccountSetupInheritance) {
                                    Text(
                                        text = stringResource(id = R.string.nc_edit),
                                        style = NunchukTheme.typography.title,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable {
                                            onEditBufferPeriodClick(state.bufferPeriod)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.background(
                                    color = NcColor.greyLight, shape = RoundedCornerShape(8.dp)
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = state.bufferPeriod?.displayName.orEmpty()
                                        .ifBlank { stringResource(id = R.string.nc_no_buffer) },
                                    style = NunchukTheme.typography.body,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                            thickness = 1.dp,
                            color = NcColor.whisper
                        )

                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp, top = 24.dp
                            )
                        ) {
                            Row(horizontalArrangement = Arrangement.Center) {
                                Text(
                                    text = stringResource(id = R.string.nc_notification_preferences),
                                    style = NunchukTheme.typography.title,
                                )
                                Spacer(modifier = Modifier.weight(weight = 1f))
                                if (isAccountSetupInheritance) {
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

                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = NcColor.greyLight, shape = RoundedCornerShape(8.dp)
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
                                            text = state.emails.joinToString("\n")
                                                .ifEmpty { "(${stringResource(id = R.string.nc_none)})" },
                                            style = NunchukTheme.typography.title
                                        )
                                    }

                                    Divider(
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 24.dp,
                                            bottom = 24.dp
                                        ),
                                        thickness = 1.dp,
                                        color = NcColor.whisper
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(id = R.string.nc_notify_them_today),
                                            style = NunchukTheme.typography.body,
                                            modifier = Modifier.fillMaxWidth(0.3f),
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = if (state.isNotifyToday) stringResource(id = R.string.nc_text_yes) else stringResource(
                                                id = R.string.nc_text_no
                                            ), style = NunchukTheme.typography.title
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val continueText = if (planFlow == InheritancePlanFlow.SETUP) {
                    stringResource(id = R.string.nc_text_continue)
                } else {
                    stringResource(id = R.string.nc_continue_to_finalize_changes)
                }
                if (isAccountSetupInheritance) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp), onContinueClicked
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
    }
}

@Composable
fun DetailPlanItem(
    iconId: Int = R.drawable.ic_nc_star_dark,
    titleId: Int = R.string.nc_text_continue,
    content: String = "dolphin concert apple",
    editable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column {
        Row(horizontalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(id = iconId),
                tint = Color.White,
                contentDescription = ""
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = titleId),
                color = colorResource(id = R.color.nc_white_color),
                style = NunchukTheme.typography.title
            )
            Spacer(modifier = Modifier.weight(weight = 1f))
            if (editable) {
                Text(
                    modifier = Modifier.clickable {
                        onClick()
                    },
                    text = stringResource(id = R.string.nc_edit),
                    color = colorResource(id = R.color.nc_white_color),
                    style = NunchukTheme.typography.title,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.background(
                color = NcColor.greyLight, shape = RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = content,
                style = NunchukTheme.typography.body,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun DetailPlanItemPreview() {
    DetailPlanItem()
}


@Preview
@Composable
private fun InheritanceReviewPlanScreenPreview() {
    InheritanceReviewPlanScreenContent()
}