/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.wallet.components.cosigning

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.formatRoundDecimal
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.*
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CosigningPolicyFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: CosigningPolicyViewModel by viewModels()
    private val args: CosigningPolicyFragmentArgs by navArgs()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val args = CosigningPolicyFragmentArgs.fromBundle(data)
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
                viewModel.updateServerConfig(signatures, token)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CosigningPolicyScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        CosigningPolicyEvent.OnEditSingingDelayClicked -> navigator.openMembershipActivity(
                            launcher = launcher,
                            activityContext = requireActivity(),
                            groupStep = MembershipStage.CONFIG_SERVER_KEY,
                            keyPolicy = viewModel.state.value.keyPolicy,
                            xfp = args.xfp
                        )
                        CosigningPolicyEvent.OnEditSpendingLimitClicked -> navigator.openMembershipActivity(
                            launcher = launcher,
                            activityContext = requireActivity(),
                            groupStep = MembershipStage.CONFIG_SPENDING_LIMIT,
                            keyPolicy = viewModel.state.value.keyPolicy,
                            xfp = args.xfp
                        )
                        CosigningPolicyEvent.OnDiscardChange -> NCWarningDialog(requireActivity()).showDialog(
                            title = getString(R.string.nc_confirmation),
                            message = getString(R.string.nc_are_you_sure_discard_the_change),
                            onYesClick = {
                                requireActivity().finish()
                            }
                        )
                        is CosigningPolicyEvent.OnSaveChange -> openWalletAuthentication(event)
                        is CosigningPolicyEvent.Loading -> showOrHideLoading(event.isLoading)
                        is CosigningPolicyEvent.ShowError -> showError(event.error)
                        CosigningPolicyEvent.UpdateKeyPolicySuccess -> NCToastMessage(
                            requireActivity()
                        ).showMessage(
                            getString(
                                R.string.nc_policy_updated
                            )
                        )
                    }
                }
        }
    }

    private fun openWalletAuthentication(event: CosigningPolicyEvent.OnSaveChange) {
        if (event.required.type == "NONE") {
            viewModel.updateServerConfig()
        } else {
            navigator.openWalletAuthentication(
                walletId = args.walletId,
                userData = event.data,
                requiredSignatures = event.required.requiredSignatures,
                type = event.required.type,
                launcher = signLauncher,
                activityContext = requireActivity()
            )
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun CosigningPolicyScreen(viewModel: CosigningPolicyViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CosigningPolicyContent(
        isAutoBroadcast = state.keyPolicy.autoBroadcastTransaction,
        keyPolicy = state.keyPolicy,
        spendingPolicy = state.keyPolicy.spendingPolicy,
        isUpdateFlow = state.isUpdateFlow,
        onEditSingingDelayClicked = viewModel::onEditSigningDelayClicked,
        onEditSpendingLimitClicked = viewModel::onEditSpendingLimitClicked,
        onSaveChangeClicked = viewModel::onSaveChangeClicked,
        onDiscardChangeClicked = viewModel::onDiscardChangeClicked
    )
}

@Composable
private fun CosigningPolicyContent(
    isAutoBroadcast: Boolean = true,
    keyPolicy: KeyPolicy = KeyPolicy(),
    spendingPolicy: SpendingPolicy? = null,
    isUpdateFlow: Boolean = false,
    onEditSpendingLimitClicked: () -> Unit = {},
    onEditSingingDelayClicked: () -> Unit = {},
    onSaveChangeClicked: () -> Unit = {},
    onDiscardChangeClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(title = "")
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_cosigning_policies),
                    style = NunchukTheme.typography.heading
                )
                if (spendingPolicy != null) {
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
                        Text(
                            modifier = Modifier.clickable(onClick = onEditSpendingLimitClicked),
                            text = stringResource(R.string.nc_edit),
                            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                        )
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
                                text = stringResource(R.string.nc_cosigning_spending_limit),
                                style = NunchukTheme.typography.body
                            )
                            Text(
                                modifier = Modifier.weight(1.0f),
                                textAlign = TextAlign.End,
                                text = "${spendingPolicy.limit.formatRoundDecimal()} ${spendingPolicy.currencyUnit.name}/${
                                    spendingPolicy.timeUnit.name.lowercase()
                                        .capitalize(Locale.current)
                                }",
                                style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
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
                    Text(
                        modifier = Modifier.clickable(onClick = onEditSingingDelayClicked),
                        text = stringResource(R.string.nc_edit),
                        style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                    )
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
                        Text(
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.End,
                            text = "${keyPolicy.getSigningDelayInHours()} hours ${keyPolicy.getSigningDelayInMinutes()} minutes",
                            style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                if (isUpdateFlow) {
                    Spacer(modifier = Modifier.weight(1.0f))
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = onSaveChangeClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_continue_save_changes))
                    }
                    TextButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = onDiscardChangeClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_discard_changes))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CosigningPolicyScreenPreview() {
    CosigningPolicyContent(
        isAutoBroadcast = true,
        keyPolicy = KeyPolicy(),
        isUpdateFlow = true,
        spendingPolicy = SpendingPolicy(5000.0, SpendingTimeUnit.DAILY, SpendingCurrencyUnit.USD)
    )
}