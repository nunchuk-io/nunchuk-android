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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claimnote

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Amount
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.satscard.wallets.SelectWalletFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InheritanceClaimNoteFragment : MembershipFragment(), BottomSheetOptionListener {

    @Inject
    lateinit var navigator: NunchukNavigator
    private val viewModel: InheritanceClaimNoteViewModel by viewModels()
    private val args: InheritanceClaimNoteFragmentArgs by navArgs()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                openSelectWallet()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                InheritanceClaimNoteScreen(viewModel, onDoneClick = {
                    requireActivity().finish()
                })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceClaimNoteEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is InheritanceClaimNoteEvent.WithdrawClick -> showSweepOptions()
                is InheritanceClaimNoteEvent.Error -> showError(message = event.message)
                is InheritanceClaimNoteEvent.CheckHasWallet -> {
                    if (event.isHasWallet) {
                        openSelectWallet()
                    } else {
                        navigator.openQuickWalletScreen(launcher, requireActivity())
                    }
                }
            }
        }
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
                    R.string.nc_sweep_to_an_address
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
        val totalBalance = viewModel.getBalance() * BTC_SATOSHI_EXCHANGE_RATE
        val totalInBtc = Amount(value = totalBalance.toLong()).pureBTC()
        navigator.openAddReceiptScreen(
            activityContext = requireActivity(),
            walletId = "",
            outputAmount = totalInBtc,
            availableAmount = totalInBtc,
            subtractFeeFromAmount = true,
            slots = emptyList(),
            sweepType = sweepType,
            masterSignerId = args.signer.id,
            magicalPhrase = args.magic
        )
    }

    private fun openSelectWallet() {
        findNavController().navigate(
            InheritanceClaimNoteFragmentDirections.actionInheritanceClaimNoteFragmentToSelectWalletFragment(
                slots = emptyArray(),
                type = SelectWalletFragment.TYPE_INHERITANCE_WALLET,
                walletBalance = viewModel.getBalance().toFloat(),
                masterSignerId = args.signer.id,
                magicalPhrase = args.magic
            )
        )
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun InheritanceClaimNoteScreen(
    viewModel: InheritanceClaimNoteViewModel = viewModel(),
    onDoneClick: () -> Unit

) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    InheritanceClaimNoteContent(
        note = state.inheritanceAdditional?.inheritance?.note.orEmpty(),
        balance = state.inheritanceAdditional?.balance ?: 0.0,
        onWithdrawClick = viewModel::onWithdrawClick,
        onDoneClick = onDoneClick
    )
}

@ExperimentalLifecycleComposeApi
@Composable
private fun InheritanceClaimNoteContent(
    note: String = "",
    balance: Double = 0.0,
    onWithdrawClick: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
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
                    NcTopAppBar(
                        backgroundColor = colorResource(id = R.color.nc_denim_tint_color),
                        title = "",
                    )
                }
                LazyColumn(modifier = Modifier.weight(1.0f)) {
                    item {
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
                                        id = R.color.nc_primary_color
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
                            text = stringResource(R.string.nc_congratulation_unlocked_your_inheritance),
                            style = NunchukTheme.typography.heading
                        )
                        if (note.isBlank().not()) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 12.dp
                                ),
                                text = stringResource(R.string.nc_you_have_a_message_below),
                                style = NunchukTheme.typography.body
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                                    .background(
                                        color = colorResource(
                                            id = R.color.nc_grey_light
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Text(
                                    modifier = Modifier.padding(12.dp),
                                    style = NunchukTheme.typography.body,
                                    text = note
                                )
                            }
                        }
                    }
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onWithdrawClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_withdraw_inheritance))
                }
                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .height(48.dp),
                    onClick = onDoneClick,
                ) {
                    Text(text = stringResource(R.string.nc_text_done))
                }
            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Preview
@Composable
private fun InheritanceClaimNotePreview() {
    InheritanceClaimNoteContent(

    )
}