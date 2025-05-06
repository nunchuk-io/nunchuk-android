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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.withdrawbitcoin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcOptionItem
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.montserratMedium
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.SelectWalletType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Amount
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceClaimWithdrawBitcoinFragment : MembershipFragment(), BottomSheetOptionListener {

    private val viewModel: InheritanceClaimWithdrawBitcoinViewModel by viewModels()
    private val args: InheritanceClaimWithdrawBitcoinFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceClaimWithdrawScreen(
                    balance = args.walletBalance.toDouble()
                ) { selectOption ->
                    when (selectOption) {
                        0 -> {
                            navigator.openInputAmountScreen(
                                activityContext = requireActivity(),
                                walletId = "",
                                availableAmount = args.walletBalance.toDouble(),
                                claimInheritanceTxParam = ClaimInheritanceTxParam(
                                    masterSignerIds = args.signers.map { it.id },
                                    magicalPhrase = args.magic.trim(),
                                    derivationPaths = args.derivationPaths.toList(),
                                    totalAmount = args.walletBalance.toDouble()
                                )
                            )
                        }

                        1 -> showSweepOptions()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceClaimWithdrawBitcoinEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is InheritanceClaimWithdrawBitcoinEvent.Error -> showError(message = event.message)
                is InheritanceClaimWithdrawBitcoinEvent.CheckHasWallet -> {
                    if (event.isHasWallet) {
                        openSelectWallet()
                    } else {
                        navigator.openWalletIntermediaryScreen(
                            requireActivity(),
                            quickWalletParam = QuickWalletParam(
                                claimInheritanceTxParam =
                                    ClaimInheritanceTxParam(
                                        masterSignerIds = args.signers.map { it.id },
                                        magicalPhrase = args.magic.trim(),
                                        derivationPaths = args.derivationPaths.toList(),
                                        totalAmount = args.walletBalance.toDouble(),
                                        isUseWallet = true
                                    ),
                                type = SelectWalletType.TYPE_INHERITANCE_WALLET
                            )
                        )
                    }
                }

                is InheritanceClaimWithdrawBitcoinEvent.CheckNewWallet -> {
                    if (event.isNewWallet) {
                        openSelectWallet()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNewWallets()
    }

    private fun showSweepOptions() {
        (childFragmentManager.findFragmentByTag("BottomSheetOption") as? DialogFragment)?.dismiss()
        val dialog = BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_SWEEP_TO_WALLET,
                    R.drawable.ic_wallet_info,
                    R.string.nc_withdraw_nunchuk_wallet
                ),
                SheetOption(
                    SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS,
                    R.drawable.ic_sending_bitcoin,
                    R.string.nc_withdraw_to_an_address
                ),
            )
        )
        dialog.show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        if (option.type == SheetOptionType.TYPE_SWEEP_TO_WALLET) {
            viewModel.checkWallet()
        } else if (option.type == SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS) {
            openSweepRecipeScreen()
        }
    }

    private fun openSweepRecipeScreen() {
        val sweepType = SweepType.SWEEP_TO_EXTERNAL_ADDRESS
        val totalBalance = args.walletBalance * BTC_SATOSHI_EXCHANGE_RATE
        val totalInBtc = Amount(value = totalBalance.toLong()).pureBTC()
        navigator.openAddReceiptScreen(
            activityContext = requireActivity(),
            walletId = "",
            outputAmount = totalInBtc,
            availableAmount = totalInBtc,
            subtractFeeFromAmount = true,
            slots = emptyList(),
            sweepType = sweepType,
            claimInheritanceTxParam = ClaimInheritanceTxParam(
                masterSignerIds = args.signers.map { it.id },
                magicalPhrase = args.magic.trim(),
                derivationPaths = args.derivationPaths.toList(),
                totalAmount = args.walletBalance.toDouble(),
                isUseWallet = false
            )
        )
    }

    private fun openSelectWallet() {
        findNavController().navigate(
            InheritanceClaimWithdrawBitcoinFragmentDirections.actionInheritanceClaimWithdrawBitcoinFragmentToSelectWalletFragment(
                slots = emptyArray(),
                type = SelectWalletType.TYPE_INHERITANCE_WALLET,
                walletBalance = args.walletBalance.toFloat(),
                claimParam = ClaimInheritanceTxParam(
                    masterSignerIds = args.signers.map { it.id },
                    magicalPhrase = args.magic.trim(),
                    derivationPaths = args.derivationPaths.toList(),
                    totalAmount = args.walletBalance.toDouble(),
                    isUseWallet = true
                )
            )
        )
    }
}

@Composable
fun InheritanceClaimWithdrawScreen(
    balance: Double = 0.0,
    onContinue: (Int) -> Unit = {},
) {
    InheritanceClaimWithdrawContent(
        balance = balance,
        onContinue = onContinue
    )
}

@Composable
private fun InheritanceClaimWithdrawContent(
    balance: Double = 0.0,
    onContinue: (Int) -> Unit = {},
) {
    val selectOption = remember { mutableIntStateOf(0) }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(), bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        onContinue(selectOption.intValue)
                    },
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .background(colorResource(id = R.color.nc_denim_tint_color))
                        .statusBarsPadding()
                ) {
                    NcTopAppBar(
                        backgroundColor = colorResource(id = R.color.nc_denim_tint_color),
                        title = "",
                    )
                }
                Column(
                    modifier = Modifier
                        .background(color = colorResource(id = R.color.nc_denim_tint_color))
                        .height(215.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        text = stringResource(R.string.nc_your_inheritance),
                        style = NunchukTheme.typography.title,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        text = balance.toAmount().getBTCAmount(),
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = montserratMedium,
                            color = colorResource(
                                id = R.color.nc_text_primary
                            )
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        text = balance.toAmount().getCurrencyAmount(),
                        style = NunchukTheme.typography.title,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_withdraw_bitcoin),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp, top = 12.dp
                    ),
                    text = stringResource(R.string.nc_withdraw_bitcoin_desc),
                    style = NunchukTheme.typography.body
                )

                NcOptionItem(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    isSelected = selectOption.intValue == 0,
                    label = stringResource(R.string.nc_withdraw_a_custom_amount),
                    onClick = {
                        selectOption.intValue = 0
                    }
                )

                NcOptionItem(
                    modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                    isSelected = selectOption.intValue == 1,
                    label = stringResource(R.string.nc_withdraw_full_balance_now),
                    onClick = {
                        selectOption.intValue = 1
                    }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceClaimWithdrawPreview() {
    InheritanceClaimWithdrawContent(

    )
}