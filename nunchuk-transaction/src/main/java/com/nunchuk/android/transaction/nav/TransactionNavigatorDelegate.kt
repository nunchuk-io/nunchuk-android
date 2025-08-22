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

package com.nunchuk.android.transaction.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.nfc.RbfType
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.InheritanceClaimTxDetailInfo
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.TransactionNavigator
import com.nunchuk.android.nav.args.AddReceiptType
import com.nunchuk.android.transaction.components.address.SavedAddressActivity
import com.nunchuk.android.transaction.components.details.TransactionDetailsArgs
import com.nunchuk.android.transaction.components.details.fee.ReplaceFeeActivity
import com.nunchuk.android.transaction.components.export.ExportTransactionActivity
import com.nunchuk.android.transaction.components.imports.ImportTransactionActivity
import com.nunchuk.android.transaction.components.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.components.receive.address.details.AddressDetailsActivity
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.components.send.batchtransaction.BatchTransactionActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmActivity
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeActivity
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptActivity

interface TransactionNavigatorDelegate : TransactionNavigator {

    override fun openReceiveTransactionScreen(
        activityContext: Activity,
        walletId: String
    ) {
        ReceiveTransactionActivity.start(
            activityContext = activityContext,
            walletId = walletId
        )
    }

    override fun openAddressDetailsScreen(
        activityContext: Activity,
        address: String,
        balance: String,
        walletId: String
    ) {
        AddressDetailsActivity.start(
            activityContext = activityContext,
            address = address,
            balance = balance,
            walletId = walletId
        )
    }

    override fun openInputAmountScreen(
        activityContext: Activity,
        walletId: String,
        availableAmount: Double,
        inputs: List<UnspentOutput>,
        claimInheritanceTxParam: ClaimInheritanceTxParam?,
        btcUri: BtcUri?
    ) {
        InputAmountActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            availableAmount = availableAmount,
            inputs = inputs,
            claimInheritanceTxParam = claimInheritanceTxParam,
            btcUri = btcUri
        )
    }

    override fun openAddReceiptScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String,
        subtractFeeFromAmount: Boolean,
        slots: List<SatsCardSlot>,
        sweepType: SweepType,
        inputs: List<UnspentOutput>,
        claimInheritanceTxParam: ClaimInheritanceTxParam?,
        type: AddReceiptType,
    ) {
        AddReceiptActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            outputAmount = outputAmount,
            availableAmount = availableAmount,
            subtractFeeFromAmount = subtractFeeFromAmount,
            address = address,
            privateNote = privateNote,
            slots = slots,
            sweepType = sweepType,
            claimInheritanceTxParam = claimInheritanceTxParam,
            inputs = inputs,
            receiptType = type
        )
    }

    override fun openEstimatedFeeScreen(
        activityContext: Activity,
        walletId: String,
        availableAmount: Double,
        txReceipts: List<TxReceipt>,
        privateNote: String,
        subtractFeeFromAmount: Boolean,
        sweepType: SweepType,
        slots: List<SatsCardSlot>,
        inputs: List<UnspentOutput>,
        claimInheritanceTxParam: ClaimInheritanceTxParam?,
        isConsolidateFlow: Boolean,
        title: String,
        rollOverWalletParam: RollOverWalletParam?,
        confirmTxActionButtonText: String,
        signingPath: SigningPath?
    ) {
        EstimatedFeeActivity.start(
            activityContext = activityContext,
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
    }

    override fun openTransactionConfirmScreen(
        activityContext: Activity,
        walletId: String,
        availableAmount: Double,
        txReceipts: List<TxReceipt>,
        privateNote: String,
        subtractFeeFromAmount: Boolean,
        manualFeeRate: Int,
        sweepType: SweepType,
        slots: List<SatsCardSlot>,
        inputs: List<UnspentOutput>,
        claimInheritanceTxParam: ClaimInheritanceTxParam?,
        actionButtonText: String,
        signingPath: SigningPath?,
        antiFeeSniping: Boolean
    ) {
        TransactionConfirmActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            availableAmount = availableAmount,
            txReceipts = txReceipts,
            privateNote = privateNote,
            subtractFeeFromAmount = subtractFeeFromAmount,
            manualFeeRate = manualFeeRate,
            sweepType = sweepType,
            slots = slots,
            claimInheritanceTxParam = claimInheritanceTxParam,
            inputs = inputs,
            actionButtonText = actionButtonText,
            antiFeeSniping = antiFeeSniping,
            signingPath = signingPath
        )
    }

    override fun openTransactionDetailsScreen(
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String,
        roomId: String,
        transaction: Transaction?,
        inheritanceClaimTxDetailInfo: InheritanceClaimTxDetailInfo?,
        isRequestSignatureFlow: Boolean
    ) {
        activityContext.startActivity(
            TransactionDetailsArgs(
                walletId = walletId,
                txId = txId,
                initEventId = initEventId,
                roomId = roomId,
                transaction = transaction,
                inheritanceClaimTxDetailInfo = inheritanceClaimTxDetailInfo,
                isRequestSignatureFlow = isRequestSignatureFlow
            ).buildIntent(activityContext)
        )
    }

    override fun openTransactionDetailsScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String,
        roomId: String,
        transaction: Transaction?
    ) {
        launcher.launch(
            TransactionDetailsArgs(
                walletId = walletId,
                txId = txId,
                initEventId = initEventId,
                roomId = roomId,
                transaction = transaction
            ).buildIntent(activityContext)
        )
    }

    override fun openImportTransactionScreen(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Activity,
        walletId: String,
        masterFingerPrint: String,
        initEventId: String,
        isDummyTx: Boolean,
        isFinishWhenError: Boolean,
        isSignInFlow: Boolean
    ) {
        val intent = ImportTransactionActivity.buildIntent(
            activityContext = activityContext,
            walletId = walletId,
            masterFingerPrint = masterFingerPrint,
            initEventId = initEventId,
            isDummyTx = isDummyTx,
            isFinishWhenError = isFinishWhenError,
            isSignInFlow = isSignInFlow
        )
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            activityContext.startActivity(intent)
        }
    }

    override fun openExportTransactionScreen(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Activity,
        walletId: String,
        txId: String,
        txToSign: String,
        isDummyTx: Boolean,
        isBBQR: Boolean,
        isSignInFlow: Boolean
    ) {
        val intent = ExportTransactionActivity.buildIntent(
            activityContext = activityContext,
            walletId = walletId,
            txId = txId,
            txToSign = txToSign,
            isDummyTx = isDummyTx,
            isBBQR = isBBQR,
            isSignInFlow = isSignInFlow
        )
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            activityContext.startActivity(intent)
        }
    }

    override fun openReplaceTransactionFee(
        launcher: ActivityResultLauncher<Intent>,
        context: Context,
        walletId: String,
        transaction: Transaction,
        type: RbfType,
        signingPath: SigningPath?,
        isUseScriptPath: Boolean,
    ) {
        ReplaceFeeActivity.start(
            launcher = launcher,
            context = context,
            walletId = walletId,
            transaction = transaction,
            type = type,
            signingPath = signingPath,
            isUseScriptPath = isUseScriptPath
        )
    }

    override fun openBatchTransactionScreen(
        activityContext: Activity,
        walletId: String,
        availableAmount: Double,
        inputs: List<UnspentOutput>
    ) {
        activityContext.startActivity(
            BatchTransactionActivity.buildIntent(
                activityContext,
                walletId = walletId,
                availableAmount = availableAmount,
                inputs = inputs,
            )
        )
    }

    override fun openSavedAddressScreen(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Activity,
        flow: Int,
        address: SavedAddress?
    ) {
        SavedAddressActivity.navigate(
            launcher = launcher,
            activity = activityContext,
            address = address,
            flow = flow
        )
    }

    override fun selectMiniscriptSigningPath(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        walletId: String,
        txId: String?
    ) {
        AddReceiptActivity.selectMiniscriptSigningPath(
            activityContext = activityContext,
            launcher = launcher,
            walletId = walletId,
            txId = txId
        )
    }
}