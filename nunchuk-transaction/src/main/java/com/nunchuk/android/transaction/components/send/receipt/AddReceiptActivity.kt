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

package com.nunchuk.android.transaction.components.send.receipt

import android.content.Context
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.share.satscard.SweepSatscardViewModel
import com.nunchuk.android.share.satscard.observerSweepSatscard
import com.nunchuk.android.transaction.components.send.confirmation.TaprootDraftTransaction
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmViewModel
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeViewModel
import com.nunchuk.android.transaction.databinding.ActivityTransactionAddReceiptBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject


@AndroidEntryPoint
class AddReceiptActivity : BaseNfcActivity<ActivityTransactionAddReceiptBinding>() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val args: AddReceiptArgs by lazy { AddReceiptArgs.deserializeFrom(intent) }

    private val viewModel: AddReceiptViewModel by viewModels()
    private val estimateFeeViewModel: EstimatedFeeViewModel by viewModels()
    private val sweepSatscardViewModel: SweepSatscardViewModel by viewModels()
    private val transactionConfirmViewModel: TransactionConfirmViewModel by viewModels()

    override fun initializeBinding() = ActivityTransactionAddReceiptBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observerSweepSatscard(sweepSatscardViewModel, nfcViewModel) { args.walletId }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_SATSCARD_SWEEP_SLOT }) {
            sweepSatscardViewModel.init(
                viewModel.getAddReceiptState().address,
                estimateFeeViewModel.defaultRate
            )
            sweepSatscardViewModel.handleSweepBalance(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
                args.slots.toList(),
                args.sweepType
            )
            nfcViewModel.clearScanInfo()
        }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var draftTx by remember { mutableStateOf<TaprootDraftTransaction?>(null) }

            LaunchedEffect(Unit) {
                transactionConfirmViewModel.event.collect {
                    if (it is TransactionConfirmEvent.DraftTaprootTransactionSuccess) {
                        draftTx = it.draftTransaction
                        navController.navigate(ReceiptNavigation.TaprootFeeSelection)
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = ReceiptNavigation.Main
            ) {
                composable<ReceiptNavigation.Main> {
                    AndroidFragment(
                        AddReceiptFragment::class.java,
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize(),
                        arguments = intent.extras!!
                    )
                }
                composable<ReceiptNavigation.TaprootFeeSelection> {
                    val state by viewModel.state.asFlow().collectAsStateWithLifecycle(AddReceiptState())
                    val tx = draftTx
                    if (tx != null) {
                        FeeSelectionScreen(
                            draftTx = tx,
                            signers = state.signers,
                            onFeeSettingsClick = {
                                navigator.openFeeSettingsScreen(this@AddReceiptActivity)
                            },
                            onContinue = { keySetIndex ->
                                transactionConfirmViewModel.handleConfirmEvent(keySetIndex = keySetIndex)
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            walletId: String,
            outputAmount: Double,
            availableAmount: Double,
            address: String = "",
            privateNote: String = "",
            subtractFeeFromAmount: Boolean = false,
            slots: List<SatsCardSlot> = emptyList(),
            sweepType: SweepType = SweepType.NONE,
            inputs: List<UnspentOutput> = emptyList(),
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
        ) {
            activityContext.startActivity(
                AddReceiptArgs(
                    walletId = walletId,
                    outputAmount = outputAmount,
                    availableAmount = availableAmount,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    slots = slots,
                    address = address,
                    privateNote = privateNote,
                    sweepType = sweepType,
                    inputs = inputs,
                    claimInheritanceTxParam = claimInheritanceTxParam
                ).buildIntent(activityContext)
            )
        }

    }

}