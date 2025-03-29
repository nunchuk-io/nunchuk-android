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

package com.nunchuk.android.transaction.components.details.fee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcNumberInputField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.util.CurrencyFormatter
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.fee.toFeeRate
import com.nunchuk.android.transaction.components.send.fee.toFeeRateInBtc
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class ReplaceFeeFragment : Fragment() {
    private val viewModel: ReplaceFeeViewModel by viewModels()
    private val args: ReplaceFeeArgs by lazy { ReplaceFeeArgs.deserializeFrom(requireActivity().intent) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ReplaceFeeScreen(
                    viewModel = viewModel,
                    onContinueClick = { newFeeRate ->
                        viewModel.draftTransaction(
                            oldTx = args.transaction,
                            walletId = args.walletId,
                            newFee = newFeeRate
                        )
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setPreviousFeeRate(args.transaction.feeRate.value.toInt())
        viewModel.initDraftTransaction(args.transaction, args.walletId)
        flowObserver(viewModel.event) {
            when (it) {
                is ReplaceFeeEvent.ShowError -> showError(it.e?.message.orUnknownError())
                is ReplaceFeeEvent.DraftTransactionSuccess -> {
                    findNavController().navigate(
                        ReplaceFeeFragmentDirections.actionReplaceFeeFragmentToConfirmReplaceTransactionFragment(
                            it.newFee
                        )
                    )
                }

                is ReplaceFeeEvent.Loading -> showOrHideLoading(it.isLoading)
                else -> Unit
            }
        }
    }

    companion object {
        fun start(
            launcher: ActivityResultLauncher<Intent>,
            context: Context,
            args: ReplaceFeeArgs,
        ) {
            launcher.launch(args.buildIntent(context))
        }
    }
}


@Composable
private fun ReplaceFeeScreen(
    viewModel: ReplaceFeeViewModel = viewModel(),
    onContinueClick: (Int) -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    ReplaceFeeContent(
        uiState = uiState,
        onContinueClick = onContinueClick,
    )
}

@Composable
private fun ReplaceFeeContent(
    uiState: ReplaceFeeState = ReplaceFeeState(),
    onContinueClick: (Int) -> Unit = {},
) {
    var newFeeRate by rememberSaveable {
        mutableStateOf("")
    }
    var showWarning by rememberSaveable {
        mutableStateOf(false)
    }
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                isBack = false, title = stringResource(id = R.string.nc_replace_by_fee),
                textStyle = NunchukTheme.typography.title
            )
        }, bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = newFeeRate.isNotEmpty(),
                    onClick = {
                        val newFee = newFeeRate.toDouble().times(1000).roundToInt()
                        if (newFee > uiState.previousFeeRate) {
                            onContinueClick(newFee)
                        } else {
                            showWarning = true
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.nc_create_transaction_new_fee_rate))
                }
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.nc_fee_rate),
                    style = NunchukTheme.typography.title,
                    modifier = Modifier.padding(top = 24.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = NunchukTheme.shape.medium
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.nc_old_fee_rate),
                            style = NunchukTheme.typography.body
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = uiState.previousFeeRate.toFeeRate(),
                                style = NunchukTheme.typography.title
                            )

                            Text(
                                text = uiState.previousFeeRate.toFeeRateInBtc(),
                                style = NunchukTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .height(1.dp)
                            .background(color = MaterialTheme.colorScheme.whisper),
                    )

                    Text(
                        text = stringResource(R.string.nc_new_fee_rate),
                        style = NunchukTheme.typography.body
                    )

                    Text(
                        text = stringResource(R.string.nc_new_fee_rate_desc),
                        style = NunchukTheme.typography.bodySmall
                    )

                    Row(
                        Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NcNumberInputField(
                            modifier = Modifier.weight(1f),
                            title = "",
                            value = newFeeRate,
                            onValueChange = {
                                showWarning = false
                                val format = CurrencyFormatter.format(it, 3)
                                newFeeRate = format
                            },
                            error = stringResource(R.string.nc_new_fee_rate_invalid).takeIf { showWarning },
                        )

                        Text(
                            text = stringResource(R.string.nc_transaction_fee_rate_unit),
                            style = NunchukTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    if (uiState.scriptPathFee.value > 0) {
                        NcHighlightText(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(
                                R.string.nc_transaction_taproot_effective_fee_rate,
                                uiState.scriptPathFee.value.toDouble().div(1000.0)
                                    .formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)
                            ).replace("<b>", "[B]")
                                .replace("</b>", "[/B]"),
                            style = NunchukTheme.typography.bodySmall
                        )
                    }

                    if (uiState.cpfpFee.value > 0) {
                        NcHighlightText(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(
                                R.string.nc_transaction_effective_fee_rate,
                                uiState.scriptPathFee.value.toDouble().div(1000.0)
                                    .formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)
                            ).replace("<b>", "[B]")
                                .replace("</b>", "[/B]"),
                            style = NunchukTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.nc_transaction_processing_speed),
                        style = NunchukTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        FeeRateView(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.nc_transaction_priority_rate),
                            value = uiState.fee.priorityRate
                        )
                        FeeRateView(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.nc_transaction_standard_rate),
                            value = uiState.fee.standardRate
                        )
                        FeeRateView(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.nc_transaction_economical_rate),
                            value = uiState.fee.economicRate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeRateView(modifier: Modifier, title: String, value: Int) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = NunchukTheme.typography.bodySmall
        )

        Text(
            text = value.toFeeRate(),
            style = NunchukTheme.typography.bodySmall
        )

    }
}

@Preview
@Composable
private fun ReplaceFeeScreenPreview() {
    ReplaceFeeContent(
        uiState = ReplaceFeeState(
            fee = EstimateFeeRates(
                priorityRate = 100,
                standardRate = 50,
                economicRate = 10
            ),
            previousFeeRate = 20,
            scriptPathFee = Amount(1000),
            cpfpFee = Amount(2000)
        )
    )
}