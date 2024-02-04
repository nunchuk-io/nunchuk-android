package com.nunchuk.android.wallet.components.cosigning

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CosigningGroupPolicyFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: CosigningGroupPolicyViewModel by viewModels()
    private val args: CosigningGroupPolicyFragmentArgs by navArgs()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val args = CosigningGroupPolicyFragmentArgs.fromBundle(data)
                viewModel.updateState(args.keyPolicy, true)
            }
        }

    private val signLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val signatures =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.SIGNATURE_EXTRA)
                        .orEmpty()
                val token = data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                if (signatures.isNotEmpty() || token.isNotEmpty()) {
                    viewModel.updateServerConfig(signatures, token)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                CosigningGroupPolicyScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        CosigningGroupPolicyEvent.OnEditSingingDelayClicked -> navigator.openConfigGroupServerKeyActivity(
                            launcher = launcher,
                            activityContext = requireActivity(),
                            groupStep = MembershipStage.CONFIG_SERVER_KEY,
                            keyPolicy = viewModel.state.value.keyPolicy,
                            groupId = args.groupId,
                            xfp = args.signer?.fingerPrint.orEmpty(),
                        )

                        CosigningGroupPolicyEvent.OnEditSpendingLimitClicked -> navigator.openConfigGroupServerKeyActivity(
                            launcher = launcher,
                            activityContext = requireActivity(),
                            groupStep = MembershipStage.CONFIG_SPENDING_LIMIT,
                            keyPolicy = viewModel.state.value.keyPolicy,
                            groupId = args.groupId,
                            xfp = args.signer?.fingerPrint.orEmpty(),
                        )

                        is CosigningGroupPolicyEvent.OnDiscardChange -> NCWarningDialog(
                            requireActivity()
                        ).showDialog(
                            title = getString(R.string.nc_confirmation),
                            message = if (event.isCancel) getString(R.string.nc_are_you_sure_cancel_the_change) else getString(
                                R.string.nc_are_you_sure_discard_the_change
                            ),
                            onYesClick = {
                                if (args.dummyTransactionId.isNotEmpty()) {
                                    viewModel.cancelChange()
                                } else {
                                    requireActivity().finish()
                                }
                            }
                        )

                        is CosigningGroupPolicyEvent.OnSaveChange -> openWalletAuthentication(event)
                        is CosigningGroupPolicyEvent.Loading -> showOrHideLoading(event.isLoading)
                        is CosigningGroupPolicyEvent.ShowError -> showError(event.error)
                        CosigningGroupPolicyEvent.UpdateKeyPolicySuccess -> NCToastMessage(
                            requireActivity()
                        ).showMessage(
                            getString(
                                R.string.nc_policy_updated
                            )
                        )

                        CosigningGroupPolicyEvent.CancelChangeSuccess -> requireActivity().finish()
                    }
                }
        }
    }

    private fun openWalletAuthentication(event: CosigningGroupPolicyEvent.OnSaveChange) {
        if (event.required.type == "NONE") {
            viewModel.updateServerConfig()
        } else {
            navigator.openWalletAuthentication(
                walletId = args.walletId,
                userData = event.data,
                requiredSignatures = event.required.requiredSignatures,
                type = event.required.type,
                launcher = signLauncher,
                activityContext = requireActivity(),
                groupId = args.groupId,
                dummyTransactionId = event.dummyTransactionId
            )
        }
    }
}


@Composable
private fun CosigningGroupPolicyScreen(viewModel: CosigningGroupPolicyViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CosigningGroupPolicyContent(
        isAutoBroadcast = state.keyPolicy.autoBroadcastTransaction,
        uiState = state,
        isUpdateFlow = state.isUpdateFlow,
        onEditSingingDelayClicked = viewModel::onEditSigningDelayClicked,
        onEditSpendingLimitClicked = viewModel::onEditSpendingLimitClicked,
        onSaveChangeClicked = viewModel::onSaveChangeClicked,
        onDiscardChangeClicked = viewModel::onDiscardChangeClicked
    )
}

@Composable
private fun CosigningGroupPolicyContent(
    isAutoBroadcast: Boolean = true,
    uiState: CosigningGroupPolicyState = CosigningGroupPolicyState(),
    isUpdateFlow: Boolean = false,
    onEditSpendingLimitClicked: () -> Unit = {},
    onEditSingingDelayClicked: () -> Unit = {},
    onSaveChangeClicked: () -> Unit = {},
    onDiscardChangeClicked: () -> Unit = {},
) {
    val requester by remember(uiState.members, uiState.requestByUserId) {
        derivedStateOf {
            uiState.members.find { it.userId == uiState.requestByUserId }
        }
    }

    NunchukTheme {
        Scaffold(
            topBar = { NcTopAppBar(title = "") }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.dummyTransactionId.isNotEmpty() && uiState.walletName.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(
                            R.string.nc_cosigning_policies_change,
                            uiState.walletName
                        ),
                        style = NunchukTheme.typography.heading
                    )

                    Text(
                        text = stringResource(
                            R.string.nc_request_cosigning_policies_change_by,
                            requester?.name ?: "Someone"
                        ),
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.nc_cosigning_policies),
                        style = NunchukTheme.typography.heading
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.nc_spending_limit),
                        style = NunchukTheme.typography.title
                    )
                    if (uiState.dummyTransactionId.isEmpty() && uiState.myRole.isMasterOrAdmin) {
                        Text(
                            modifier = Modifier.clickable(onClick = onEditSpendingLimitClicked),
                            text = stringResource(R.string.nc_edit),
                            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                        )
                    }
                }
                if (uiState.keyPolicy.isApplyAll && uiState.keyPolicy.spendingPolicies.isNotEmpty()) {
                    uiState.members.forEach {
                        SpendingLimitAccountView(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            member = it,
                            policy = uiState.keyPolicy.spendingPolicies.values.first()
                        )
                    }
                } else {
                    uiState.members.forEach { member ->
                        val keyPolicy = uiState.keyPolicy.spendingPolicies[member.membershipId]
                        if (keyPolicy != null) {
                            SpendingLimitAccountView(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp
                                ),
                                member = member,
                                policy = keyPolicy
                            )
                        }
                    }
                }

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .padding(horizontal = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.nc_co_signing_delay),
                        style = NunchukTheme.typography.title
                    )
                    if (uiState.dummyTransactionId.isEmpty() && uiState.myRole.isMasterOrAdmin) {
                        Text(
                            modifier = Modifier.clickable(onClick = onEditSingingDelayClicked),
                            text = stringResource(R.string.nc_edit),
                            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            color = colorResource(id = R.color.nc_grey_light),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1.0f),
                            text = stringResource(R.string.nc_automation_broadcast_transaction),
                            style = NunchukTheme.typography.body
                        )
                        Text(
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.End,
                            text = if (isAutoBroadcast)
                                stringResource(R.string.nc_on)
                            else
                                stringResource(R.string.nc_off),
                            style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1.0f),
                            text = stringResource(R.string.nc_enable_co_signing_delay),
                            style = NunchukTheme.typography.body
                        )
                        val delayTime = if (uiState.keyPolicy.getSigningDelayInHours() == 0 && uiState.keyPolicy.getSigningDelayInMinutes() == 0) {
                            stringResource(R.string.nc_off)
                        } else if (uiState.keyPolicy.getSigningDelayInHours() == 0) {
                            pluralStringResource(
                                R.plurals.nc_plural_minute,
                                uiState.keyPolicy.getSigningDelayInMinutes(),
                                uiState.keyPolicy.getSigningDelayInMinutes()
                            )
                        } else if (uiState.keyPolicy.getSigningDelayInMinutes() == 0) {
                            pluralStringResource(
                                R.plurals.nc_plural_hour,
                                uiState.keyPolicy.getSigningDelayInHours(),
                                uiState.keyPolicy.getSigningDelayInHours()
                            )
                        } else {
                            "${
                                pluralStringResource(
                                    R.plurals.nc_plural_hour,
                                    uiState.keyPolicy.getSigningDelayInHours(),
                                    uiState.keyPolicy.getSigningDelayInHours()
                                )
                            } ${
                                pluralStringResource(
                                    R.plurals.nc_plural_minute,
                                    uiState.keyPolicy.getSigningDelayInMinutes(),
                                    uiState.keyPolicy.getSigningDelayInMinutes()
                                )
                            }"
                        }
                        Text(
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.End,
                            text = delayTime,
                            style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                if (isUpdateFlow || uiState.myRole.isMasterOrAdmin) {
                    Spacer(modifier = Modifier.weight(1.0f))
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = onSaveChangeClicked,
                        enabled = uiState.keyPolicy.spendingPolicies.isNotEmpty()
                    ) {
                        if (uiState.pendingSignature > 0) {
                            Text(
                                text = stringResource(
                                    R.string.nc_text_continue_signature_pending,
                                    uiState.pendingSignature
                                )
                            )
                        } else {
                            Text(text = stringResource(R.string.nc_continue_save_changes))
                        }
                    }
                    if (uiState.myRole.isMasterOrAdmin) {
                        TextButton(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            onClick = onDiscardChangeClicked,
                            enabled = uiState.keyPolicy.spendingPolicies.isNotEmpty()
                        ) {
                            Text(
                                text = if (uiState.dummyTransactionId.isNotEmpty()) stringResource(R.string.nc_cancel_change)
                                else stringResource(R.string.nc_discard_changes)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CosigningGroupPolicyContentPreview() {
    CosigningGroupPolicyContent(
        isAutoBroadcast = true,
        uiState = CosigningGroupPolicyState(
            members = listOf(
                AssistedMember(
                    role = AssistedWalletRole.MASTER.name,
                    name = "Bob Lee",
                    email = "khoapham@gmail.com",
                    membershipId = "1"
                ),
                AssistedMember(
                    role = AssistedWalletRole.ADMIN.name,
                    name = "Bob Lee",
                    email = "khoapham@gmail.com"
                ),
            ),
            keyPolicy = GroupKeyPolicy(
                isApplyAll = true,
                signingDelayInSeconds = 100,
                autoBroadcastTransaction = true,
                spendingPolicies = mapOf(
                    "" to SpendingPolicy(
                        100.0,
                        SpendingTimeUnit.DAILY,
                        currencyUnit = "USD"
                    )
                ),
            ),
            requestByUserId = "1"
        ),
        isUpdateFlow = true,
    )
}