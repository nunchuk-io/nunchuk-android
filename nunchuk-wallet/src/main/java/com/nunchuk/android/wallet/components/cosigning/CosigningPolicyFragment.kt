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
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.wallet.R
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
                viewModel.updateState(args.keyPolicy)
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
                    }
                }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun CosigningPolicyScreen(viewModel: CosigningPolicyViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CosigningPolicyContent(
        isAutoBroadcast = state.keyPolicy.autoBroadcastTransaction,
        delayCosigningInHour = state.keyPolicy.signingDelayInHour,
        spendingPolicy = state.keyPolicy.spendingPolicy,
        onEditSingingDelayClicked = viewModel::onEditSigningDelayClicked,
        onEditSpendingLimitClicked = viewModel::onEditSpendingLimitClicked,
    )
}

@Composable
private fun CosigningPolicyContent(
    isAutoBroadcast: Boolean = true,
    delayCosigningInHour: Int = 0,
    spendingPolicy: SpendingPolicy? = null,
    onEditSpendingLimitClicked: () -> Unit = {},
    onEditSingingDelayClicked: () -> Unit = {},
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
                                text = "${spendingPolicy.limit} ${spendingPolicy.currencyUnit.name}/${
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
                            text = "$delayCosigningInHour hours",
                            style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
                        )
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
        delayCosigningInHour = 2,
        spendingPolicy = SpendingPolicy(5000, SpendingTimeUnit.DAILY, SpendingCurrencyUnit.USD)
    )
}