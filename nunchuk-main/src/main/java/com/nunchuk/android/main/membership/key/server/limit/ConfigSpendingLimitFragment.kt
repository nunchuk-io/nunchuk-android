package com.nunchuk.android.main.membership.key.server.limit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.main.R
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigSpendingLimitFragment : MembershipFragment(), BottomSheetOptionListener {
    private val viewModel: ConfigSpendingLimitViewModel by viewModels()
    private val args: ConfigSpendingLimitFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ConfigSpendingLimitScreen(viewModel, args)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        ConfigSpendingLimitEvent.ShowCurrencyUnit -> showCurrencyUnitOption()
                        ConfigSpendingLimitEvent.ShowTimeUnit -> showTimeUnitOption()
                        is ConfigSpendingLimitEvent.ContinueClicked -> handleContinueClicked(event)
                    }
                }
        }
    }

    private fun handleContinueClicked(event: ConfigSpendingLimitEvent.ContinueClicked) {
        val keyPolicy = args.keyPolicy
        if (keyPolicy != null) {
            // edit mode
            val newArgs = CosigningPolicyFragmentArgs(
                keyPolicy.copy(spendingPolicy = event.spendingPolicy),
            )
            requireActivity().apply {
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtras(newArgs.toBundle())
                })
                finish()
            }
        } else {
            // create mode
            findNavController().navigate(
                ConfigSpendingLimitFragmentDirections.actionConfigSpendingLimitFragmentToConfigureServerKeySettingFragment(
                    spendingLimit = event.spendingPolicy
                )
            )
        }
    }

    private fun showCurrencyUnitOption() {
        BottomSheetOption.newInstance(
            SpendingCurrencyUnit.values().map {
                SheetOption(
                    type = OFFSET + it.ordinal,
                    label = it.toLabel(requireContext()),
                    isSelected = it == viewModel.state.value.currencyUnit
                )
            }
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showTimeUnitOption() {
        BottomSheetOption.newInstance(
            SpendingTimeUnit.values().map {
                SheetOption(
                    type = it.ordinal,
                    label = it.toLabel(requireContext()),
                    isSelected = it == viewModel.state.value.timeUnit
                )
            }
        ).show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type >= OFFSET) {
            val type = SpendingCurrencyUnit.values()[option.type - OFFSET]
            viewModel.setCurrencyUnit(type)
        } else {
            val type = SpendingTimeUnit.values()[option.type]
            viewModel.setTimeUnit(type)
        }
    }

    companion object {
        private const val OFFSET = 10000
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun ConfigSpendingLimitScreen(
    viewModel: ConfigSpendingLimitViewModel,
    args: ConfigSpendingLimitFragmentArgs
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    ConfigSpendingLimitContent(
        remainTime = remainTime,
        currencyUnit = state.currencyUnit,
        timeUnit = state.timeUnit,
        onShowCurrencyUnitOption = viewModel::showCurrencyUnitOption,
        onShowTimeUnitOption = viewModel::showTimeUnitOption,
        onContinueClicked = viewModel::onContinueClicked,
        spendingLimit = remember {
            mutableStateOf(args.keyPolicy?.spendingPolicy?.limit?.toString() ?: "5000")
        },
        isEditMode = args.keyPolicy != null
    )
}

@Composable
private fun ConfigSpendingLimitContent(
    remainTime: Int = 0,
    currencyUnit: SpendingCurrencyUnit = SpendingCurrencyUnit.USD,
    timeUnit: SpendingTimeUnit = SpendingTimeUnit.DAILY,
    onContinueClicked: (value: Long) -> Unit = {},
    onShowTimeUnitOption: () -> Unit = {},
    onShowCurrencyUnitOption: () -> Unit = {},
    spendingLimit: MutableState<String> = mutableStateOf("5000"),
    isEditMode: Boolean = false,
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(if (isEditMode) "" else stringResource(R.string.nc_estimate_remain_time, remainTime))
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_config_cosiging_spending_limit),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_cosigning_pending_limit_desc),
                    style = NunchukTheme.typography.body
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_cosigning_spending_limit),
                    style = NunchukTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .padding(horizontal = 16.dp)
                        .height(IntrinsicSize.Max)
                ) {
                    NcTextField(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(end = 16.dp),
                        title = "",
                        value = spendingLimit.value,
                        onValueChange = {
                            spendingLimit.value = it.take(15)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(
                                width = 1.dp,
                                color = Color(0xFFDEDEDE),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable { onShowCurrencyUnitOption() }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(end = 16.dp),
                            text = currencyUnit.toLabel(LocalContext.current),
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = ""
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .height(52.dp)
                        .clickable { onShowTimeUnitOption() }
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(0xFFDEDEDE),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(end = 16.dp),
                        text = timeUnit.toLabel(LocalContext.current),
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = ""
                    )
                }
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    enabled = spendingLimit.value.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        onContinueClicked(spendingLimit.value.toLongOrNull() ?: 0L)
                    },
                ) {
                    Text(text = if (isEditMode) stringResource(R.string.nc_update_spending_limit) else stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfigSpendingLimitScreenPreview() {
    ConfigSpendingLimitContent()
}