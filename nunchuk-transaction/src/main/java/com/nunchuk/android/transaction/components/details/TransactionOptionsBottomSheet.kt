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

package com.nunchuk.android.transaction.components.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.model.TransactionOption.CANCEL
import com.nunchuk.android.share.model.TransactionOption.COPY_RAW_TRANSACTION_HEX
import com.nunchuk.android.share.model.TransactionOption.COPY_TRANSACTION_ID
import com.nunchuk.android.share.model.TransactionOption.EXPORT_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.IMPORT_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.REMOVE_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.REPLACE_BY_FEE
import com.nunchuk.android.share.model.TransactionOption.REQUEST_SIGNATURE
import com.nunchuk.android.share.model.TransactionOption.SCHEDULE_BROADCAST
import com.nunchuk.android.share.model.TransactionOption.SHOW_INVOICE
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.DialogTransactionSignBottomSheetBinding
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionOptionsBottomSheet : BaseBottomSheet<DialogTransactionSignBottomSheetBinding>() {

    private lateinit var listener: (TransactionOption) -> Unit

    private val args: TransactionOptionsArgs by lazy {
        TransactionOptionsArgs.deserializeFrom(
            arguments
        )
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogTransactionSignBottomSheetBinding {
        return DialogTransactionSignBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.btnCancel.isVisible = !args.isReceive && (args.isPending || args.isPendingConfirm)
        binding.btnCancel.setOnClickListener {
            listener(CANCEL)
            dismiss()
        }

        binding.btnExport.isVisible = args.isPending
        binding.btnExport.setOnClickListener {
            listener(EXPORT_TRANSACTION)
            dismiss()
        }

        binding.btnImport.isVisible = args.isPending
        binding.btnImport.setOnClickListener {
            listener(IMPORT_TRANSACTION)
            dismiss()
        }

        binding.btnRequestSignature.isVisible = args.isPending && args.isShowRequestSignature
        binding.btnRequestSignature.setOnClickListener {
            listener(REQUEST_SIGNATURE)
            dismiss()
        }

        binding.btnReplaceFee.isVisible = !args.isReceive && args.isPendingConfirm
        binding.btnReplaceFee.setOnClickListener {
            listener(REPLACE_BY_FEE)
            dismiss()
        }

        binding.btnCopyRawTransactionHex.isVisible = args.canBroadcast
        binding.btnCopyRawTransactionHex.setOnClickListener {
            listener(COPY_RAW_TRANSACTION_HEX)
            dismiss()
        }

        binding.btnCopyTxId.setOnDebounceClickListener {
            listener(COPY_TRANSACTION_ID)
            dismiss()
        }

        binding.btnRemoveTransaction.isVisible = args.isRejected
        binding.btnRemoveTransaction.setOnDebounceClickListener {
            listener(REMOVE_TRANSACTION)
            dismiss()
        }

        binding.btnScheduleBroadcast.isVisible = args.isPending
                && !args.isReceive
                && args.isSupportScheduleBroadcast
        binding.btnScheduleBroadcast.text = if (args.isScheduleBroadcast) {
            getString(R.string.nc_cancel_scheduled_broadcast)
        } else {
            getString(R.string.nc_schedule_broadcast)
        }
        binding.btnScheduleBroadcast.setOnDebounceClickListener {
            listener(SCHEDULE_BROADCAST)
            dismiss()
        }

        binding.btnShowAsAnInvoice.isVisible = args.txStatus == TransactionStatus.CONFIRMED.name
        binding.btnShowAsAnInvoice.setOnDebounceClickListener {
            listener(SHOW_INVOICE)
            dismiss()
        }

        hideOptionsIfRoleIsKeyHolderLimited()
    }

    private fun hideOptionsIfRoleIsKeyHolderLimited() {
        val bottomSheet = binding.bottomSheet
        if (args.userRole.toRole.isKeyHolderLimited) {
            for (i in 0 until bottomSheet.childCount) {
                val child = bottomSheet.getChildAt(i)
                if (child.id != R.id.btnExport && child.id != R.id.btnImport) {
                    child.visibility = View.GONE
                }
            }
        }
    }

    fun setListener(listener: (TransactionOption) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "TransactionOptionsBottomSheet"

        fun show(
            fragmentManager: FragmentManager,
            isPending: Boolean,
            isPendingConfirm: Boolean,
            isRejected: Boolean,
            isSupportScheduleBroadcast: Boolean,
            isScheduleBroadcast: Boolean,
            canBroadcast: Boolean,
            isShowRequestSignature: Boolean,
            userRole: String,
            isReceive: Boolean,
            plan: MembershipPlan,
            txStatus: String
        ): TransactionOptionsBottomSheet {
            return TransactionOptionsBottomSheet().apply {
                arguments =
                    TransactionOptionsArgs(
                        isPending = isPending,
                        isPendingConfirm = isPendingConfirm,
                        isRejected = isRejected,
                        isSupportScheduleBroadcast = isSupportScheduleBroadcast,
                        isScheduleBroadcast = isScheduleBroadcast,
                        canBroadcast = canBroadcast,
                        isShowRequestSignature = isShowRequestSignature,
                        userRole = userRole,
                        isReceive = isReceive,
                        plan = plan,
                        txStatus = txStatus
                    ).buildBundle()
                show(fragmentManager, TAG)
            }
        }
    }

}

data class TransactionOptionsArgs(
    val isPending: Boolean,
    val isPendingConfirm: Boolean,
    val isRejected: Boolean,
    val isSupportScheduleBroadcast: Boolean,
    val isScheduleBroadcast: Boolean,
    val canBroadcast: Boolean,
    val isShowRequestSignature: Boolean,
    val userRole: String,
    val isReceive: Boolean,
    val plan: MembershipPlan,
    val txStatus: String
) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putBoolean(EXTRA_IS_PENDING, isPending)
        putBoolean(EXTRA_IS_PENDING_CONFIRM, isPendingConfirm)
        putBoolean(EXTRA_IS_REJECTED, isRejected)
        putBoolean(EXTRA_IS_SUPPORT_SCHEDULE_BROADCAST, isSupportScheduleBroadcast)
        putBoolean(EXTRA_IS_SCHEDULE_BROADCAST, isScheduleBroadcast)
        putBoolean(EXTRA_CAN_BROADCAST, canBroadcast)
        putBoolean(EXTRA_SHOW_REQUEST_SIGNATURE, isShowRequestSignature)
        putString(EXTRA_USER_ROLE, userRole)
        putBoolean(EXTRA_IS_RECEIVE, isReceive)
        putSerializable(EXTRA_PLAN, plan)
        putString(EXTRA_TX_STATUS, txStatus)
    }

    companion object {
        private const val EXTRA_IS_PENDING = "EXTRA_IS_PENDING"
        private const val EXTRA_IS_PENDING_CONFIRM = "EXTRA_IS_PENDING_CONFIRM"
        private const val EXTRA_IS_REJECTED = "EXTRA_IS_REJECTED"
        private const val EXTRA_IS_SUPPORT_SCHEDULE_BROADCAST = "EXTRA_IS_SUPPORT_SCHEDULE_BROADCAST"
        private const val EXTRA_IS_SCHEDULE_BROADCAST = "EXTRA_IS_SCHEDULE_BROADCAST"
        private const val EXTRA_CAN_BROADCAST = "EXTRA_CAN_BROADCAST"
        private const val EXTRA_SHOW_REQUEST_SIGNATURE = "EXTRA_SHOW_REQUEST_SIGNATURE"
        private const val EXTRA_USER_ROLE = "EXTRA_USER_ROLE"
        private const val EXTRA_IS_RECEIVE = "EXTRA_IS_RECEIVE"
        private const val EXTRA_PLAN = "EXTRA_PLAN"
        private const val EXTRA_TX_STATUS = "EXTRA_TX_STATUS"

        fun deserializeFrom(data: Bundle?) = TransactionOptionsArgs(
            data?.getBooleanValue(EXTRA_IS_PENDING).orFalse(),
            data?.getBooleanValue(EXTRA_IS_PENDING_CONFIRM).orFalse(),
            data?.getBooleanValue(EXTRA_IS_REJECTED).orFalse(),
            data?.getBooleanValue(EXTRA_IS_SUPPORT_SCHEDULE_BROADCAST).orFalse(),
            data?.getBooleanValue(EXTRA_IS_SCHEDULE_BROADCAST).orFalse(),
            data?.getBooleanValue(EXTRA_CAN_BROADCAST).orFalse(),
            data?.getBooleanValue(EXTRA_SHOW_REQUEST_SIGNATURE).orFalse(),
            data?.getString(EXTRA_USER_ROLE).orEmpty(),
            data?.getBooleanValue(EXTRA_IS_RECEIVE).orFalse(),
            data?.serializable<MembershipPlan>(EXTRA_PLAN) ?: MembershipPlan.NONE,
            data?.getString(EXTRA_TX_STATUS).orEmpty()
        )
    }
}
