package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.utils.simpleGlobalDateFormat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class InheritanceReviewPlanGroupGroupFragment : MembershipFragment(), BottomSheetOptionListener {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: InheritanceReviewPlanGroupViewModel by viewModels()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceReviewPlanGroupScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceReviewPlanGroupEvent.OnContinue -> {
                    requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(GlobalResultKey.DUMMY_TX_ID, event.dummyTransactionId)
                        putExtra(GlobalResultKey.REQUIRED_SIGNATURES, event.requiredSignatures.requiredSignatures)
                    })
                   requireActivity().finish()
                }

                is InheritanceReviewPlanGroupEvent.Loading -> showOrHideLoading(loading = event.loading)
                is InheritanceReviewPlanGroupEvent.ProcessFailure -> showError(message = event.message)
                InheritanceReviewPlanGroupEvent.CancelInheritanceSuccess -> {}
                InheritanceReviewPlanGroupEvent.CreateOrUpdateInheritanceSuccess -> {}
            }
        }
    }
}

@Composable
fun InheritanceReviewPlanGroupScreen(
    viewModel: InheritanceReviewPlanGroupViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InheritanceReviewPlanGroupScreenContent(
        uiState = state,
        onContinueClicked = viewModel::onContinueClick
    )
}

@Composable
fun InheritanceReviewPlanGroupScreenContent(
    uiState: InheritanceReviewPlanGroupState = InheritanceReviewPlanGroupState(),
    onContinueClicked: () -> Unit = {},
) {
    val newData = uiState.payload.newData
    val oldData = uiState.payload.oldData

    if (newData == null && oldData == null && uiState.type != DummyTransactionType.CANCEL_INHERITANCE_PLAN) {
        return
    }

    val requester by remember(uiState.members, uiState.requestByUserId) {
        derivedStateOf {
            uiState.members.find { it.userId == uiState.requestByUserId }
        }
    }

    val onTextColor: (isChanged: Boolean) -> Color = {
        if (oldData != null && it) Color(0xffCF4018) else Color(0xff031F2B)
    }

    val title =
        when (uiState.type) {
            DummyTransactionType.CREATE_INHERITANCE_PLAN -> stringResource(
                id = R.string.nc_inheritance_plan_group_create,
                uiState.walletName
            )

            DummyTransactionType.UPDATE_INHERITANCE_PLAN, DummyTransactionType.CANCEL_INHERITANCE_PLAN -> stringResource(
                id = R.string.nc_inheritance_plan_group_change,
                uiState.walletName
            )

            else -> ""
        }

    val desc = when (uiState.type) {
        DummyTransactionType.CREATE_INHERITANCE_PLAN -> stringResource(
            id = R.string.nc_create_inheritance_plan_group_change_by,
            requester?.name ?: "Someone",
            uiState.walletName
        )

        DummyTransactionType.UPDATE_INHERITANCE_PLAN -> stringResource(
            id = R.string.nc_update_inheritance_plan_group_change_by,
            requester?.name ?: "Someone",
            uiState.walletName
        )

        DummyTransactionType.CANCEL_INHERITANCE_PLAN -> stringResource(
            id = R.string.nc_cancel_inheritance_plan_group_change_by,
            requester?.name ?: "Someone",
            uiState.walletName
        )

        else -> ""
    }

    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .fillMaxSize()
            ) {
                NcTopAppBar(title = "", elevation = 0.dp)
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                ) {
                    item {
                        if (uiState.dummyTransactionId.isNotEmpty() && uiState.walletName.isNotEmpty()) {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = title,
                                style = NunchukTheme.typography.heading
                            )

                            Text(
                                text = desc,
                                style = NunchukTheme.typography.body,
                                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            )
                            if (uiState.type != DummyTransactionType.CANCEL_INHERITANCE_PLAN) {
                                Column(
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp, top = 24.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.nc_activation_date),
                                        style = NunchukTheme.typography.title
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .background(
                                                color = NcColor.greyLight,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            text = Date(newData?.activationTimeMilis.orDefault(0L)).simpleGlobalDateFormat(),
                                            style = NunchukTheme.typography.body.copy(
                                                color = onTextColor(
                                                    newData?.activationTimeMilis != oldData?.activationTimeMilis
                                                )
                                            ),
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp, top = 24.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.nc_note_to_beneficiary_trustee),
                                        style = NunchukTheme.typography.title
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .background(
                                                color = NcColor.greyLight,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            text = newData?.note.orEmpty()
                                                .ifBlank { stringResource(id = R.string.nc_no_note) },
                                            style = NunchukTheme.typography.body.copy(
                                                color = onTextColor(
                                                    newData?.note != oldData?.note
                                                )
                                            ),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp, top = 24.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.nc_buffer_period),
                                        style = NunchukTheme.typography.title
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .background(
                                                color = NcColor.greyLight,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            text = newData?.bufferPeriod?.displayName.orEmpty()
                                                .ifBlank { stringResource(id = R.string.nc_no_note) },
                                            style = NunchukTheme.typography.body.copy(
                                                color = onTextColor(
                                                    newData?.bufferPeriod?.id != oldData?.bufferPeriod?.id
                                                )
                                            ),
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp, top = 24.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.nc_notification_preferences),
                                        style = NunchukTheme.typography.title,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .background(
                                                color = NcColor.greyLight,
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
                                                    text = newData?.notificationEmails.orEmpty()
                                                        .joinToString("\n")
                                                        .ifEmpty { "(${stringResource(id = R.string.nc_none)})" },
                                                    style = NunchukTheme.typography.title.copy(
                                                        color = onTextColor(
                                                            newData?.notificationEmails != oldData?.notificationEmails
                                                        )
                                                    )
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
                                                    text = if (newData?.notifyToday.orFalse()) stringResource(
                                                        id = R.string.nc_text_yes
                                                    ) else stringResource(
                                                        id = R.string.nc_text_no
                                                    ), style = NunchukTheme.typography.title.copy(
                                                        color = onTextColor(
                                                            newData?.notifyToday != oldData?.notifyToday
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), onContinueClicked
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.nc_text_continue_signature_pending,
                            uiState.pendingSignatures
                        )
                    )
                }
            }
        }
    }


}

@Preview
@Composable
private fun InheritanceReviewPlanGroupScreenPreview() {
    InheritanceReviewPlanGroupScreenContent()
}