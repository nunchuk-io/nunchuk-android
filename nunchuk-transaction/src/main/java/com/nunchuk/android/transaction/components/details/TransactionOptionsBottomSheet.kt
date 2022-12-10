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

package com.nunchuk.android.transaction.components.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.checkCameraPermission
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.model.TransactionOption.*
import com.nunchuk.android.transaction.databinding.DialogTransactionSignBottomSheetBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransactionOptionsBottomSheet : BaseBottomSheet<DialogTransactionSignBottomSheetBinding>() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

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
        binding.btnCancel.isVisible = args.isPending
        binding.btnCancel.setOnClickListener {
            listener(CANCEL)
            dismiss()
        }
        binding.btnExportPSBT.isVisible = args.isPending
        binding.btnExportPSBT.setOnClickListener {
            listener(EXPORT_PSBT)
            dismiss()
        }

        binding.btnExport.isVisible = args.isPending
        binding.btnExport.setOnClickListener {
            listener(EXPORT_KEYSTONE)
            dismiss()
        }

        binding.btnExportPassport.isVisible = args.isPending
        binding.btnExportPassport.setOnClickListener {
            listener(EXPORT_PASSPORT)
            dismiss()
        }

        binding.btnImport.isVisible = args.isPending
        binding.btnImport.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(IMPORT_KEYSTONE)
                dismiss()
            }
        }

        binding.btnImportPassport.isVisible = args.isPending
        binding.btnImportPassport.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(IMPORT_PASSPORT)
                dismiss()
            }
        }

        binding.btnReplaceFee.isVisible = args.isPendingConfirm
        binding.btnReplaceFee.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(REPLACE_BY_FEE)
                dismiss()
            }
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

        binding.btnScheduleBroadcast.isVisible =
            args.isPending && membershipStepManager.plan == MembershipPlan.HONEY_BADGER
        binding.btnScheduleBroadcast.setOnDebounceClickListener {
            listener(SCHEDULE_BROADCAST)
            dismiss()
        }
    }

    fun setListener(listener: (TransactionOption) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "TransactionOptionsBottomSheet"

        private fun newInstance(
            isPending: Boolean,
            isPendingConfirm: Boolean,
            isRejected: Boolean,
            isAssistedWallet: Boolean
        ) = TransactionOptionsBottomSheet().apply {
            arguments =
                TransactionOptionsArgs(
                    isPending,
                    isPendingConfirm,
                    isRejected,
                    isAssistedWallet
                ).buildBundle()
        }


        fun show(
            fragmentManager: FragmentManager,
            isPending: Boolean,
            isPendingConfirm: Boolean,
            isRejected: Boolean,
            isAssistedWallet: Boolean,
        ): TransactionOptionsBottomSheet {
            return newInstance(
                isPending,
                isPendingConfirm,
                isRejected,
                isAssistedWallet,
            ).apply { show(fragmentManager, TAG) }
        }
    }

}

data class TransactionOptionsArgs(
    val isPending: Boolean,
    val isPendingConfirm: Boolean,
    val isRejected: Boolean,
    val isAssistedWallet: Boolean,
) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putBoolean(EXTRA_IS_PENDING, isPending)
        putBoolean(EXTRA_IS_PENDING_CONFIRM, isPendingConfirm)
        putBoolean(EXTRA_IS_REJECTED, isRejected)
        putBoolean(EXTRA_IS_ASSISTED_WALLET, isAssistedWallet)
    }

    companion object {
        private const val EXTRA_IS_PENDING = "EXTRA_IS_PENDING"
        private const val EXTRA_IS_PENDING_CONFIRM = "EXTRA_IS_PENDING_CONFIRM"
        private const val EXTRA_IS_REJECTED = "EXTRA_IS_REJECTED"
        private const val EXTRA_IS_ASSISTED_WALLET = "EXTRA_IS_ASSISTED_WALLET"

        fun deserializeFrom(data: Bundle?) = TransactionOptionsArgs(
            data?.getBooleanValue(EXTRA_IS_PENDING).orFalse(),
            data?.getBooleanValue(EXTRA_IS_PENDING_CONFIRM).orFalse(),
            data?.getBooleanValue(EXTRA_IS_REJECTED).orFalse(),
            data?.getBooleanValue(EXTRA_IS_ASSISTED_WALLET).orFalse(),
        )
    }
}
