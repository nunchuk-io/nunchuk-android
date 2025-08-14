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

package com.nunchuk.android.transaction.components.send.fee

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EstimatedFeeActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = EstimatedFeeNavigation.Main
            ) {
                composable<EstimatedFeeNavigation.Main> {
                    AndroidFragment(
                        EstimatedFeeFragment::class.java,
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize(),
                        arguments = intent.extras!!
                    )
                }
            }
        }
    }

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            availableAmount: Double,
            txReceipts: List<TxReceipt>,
            privateNote: String,
            subtractFeeFromAmount: Boolean = false,
            sweepType: SweepType = SweepType.NONE,
            slots: List<SatsCardSlot>,
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
            inputs: List<UnspentOutput> = emptyList(),
            isConsolidateFlow: Boolean = false,
            title: String = "",
            rollOverWalletParam: RollOverWalletParam? = null,
            confirmTxActionButtonText: String = "",
            signingPath: SigningPath? = null
        ) {
            val args = EstimatedFeeArgs(
                walletId = walletId,
                txReceipts = txReceipts,
                availableAmount = availableAmount,
                privateNote = privateNote,
                subtractFeeFromAmount = subtractFeeFromAmount,
                sweepType = sweepType,
                slots = slots,
                claimInheritanceTxParam = claimInheritanceTxParam,
                inputs = inputs,
                isConsolidateFlow = isConsolidateFlow,
                title = title,
                rollOverWalletParam = rollOverWalletParam,
                confirmTxActionButtonText = confirmTxActionButtonText,
                signingPath = signingPath
            )
            
            activityContext.startActivity(args.buildIntent(activityContext))
        }
    }
}
