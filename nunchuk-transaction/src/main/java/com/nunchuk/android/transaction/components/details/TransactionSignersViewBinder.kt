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

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.StyleSpan
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.hadBroadcast
import com.nunchuk.android.core.util.isPendingSignatures
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.toReadableDrawable
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isObserver
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.transaction.R
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.formatByWeek
import com.nunchuk.android.widget.databinding.ItemTransactionSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import java.util.Date

internal class TransactionSignersViewBinder(
    container: ViewGroup,
    private val signerMap: Map<String, Boolean>,
    signers: List<SignerModel>,
    private val txStatus: TransactionStatus,
    private val listener: (SignerModel) -> Unit = {},
    private val serverTransaction: ServerTransaction?,
    private val userRole: AssistedWalletRole = AssistedWalletRole.NONE,
) : AbsViewBinder<SignerModel, ItemTransactionSignerBinding>(container, signers) {

    override fun initializeBinding() =
        ItemTransactionSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemTransactionSignerBinding.bind(container[position])
        binding.avatar.isGone = model.localKey
        binding.ivSignerType.isVisible = model.localKey
        if (model.localKey) {
            binding.ivSignerType.setImageDrawable(model.toReadableDrawable(context))
        } else {
            binding.avatar.text = model.name.shorten()
        }
        binding.signerName.text = model.name
        binding.signerType.text = model.toReadableSignerType(context)
        binding.btnSign.setOnDebounceClickListener { listener(model) }
        val isSigned = model.isSigned()

        if (txStatus.hadBroadcast()) {
            binding.btnSign.isVisible = false
            binding.signed.isVisible = false
            binding.signNotAvailable.isVisible = false
        } else if (isSigned) {
            binding.btnSign.isVisible = false
            binding.signed.isVisible = true
            binding.signNotAvailable.isVisible = false
        } else if (!model.localKey) {
            binding.btnSign.isVisible = false
            binding.signed.isVisible = false
            binding.signNotAvailable.isVisible = true
        } else {
            binding.btnSign.isVisible = model.type != SignerType.SERVER
                    && model.type != SignerType.FOREIGN_SOFTWARE
                    && txStatus.isPendingSignatures() && userRole.isObserver.not()
            binding.signed.isVisible = false
            binding.signNotAvailable.isVisible = false
        }

        binding.signerType.isVisible = model.type != SignerType.SERVER
        binding.acctX.apply {
            isVisible = model.isShowAcctX()
            text = String.format(context.getString(R.string.nc_acct_x), model.index)
        }
        if (model.type == SignerType.SERVER) {
            val spendingLimitMessage = serverTransaction?.spendingLimitMessage.orEmpty()
            val cosignedTime = serverTransaction?.signedInMilis ?: 0L
            binding.xpf.isVisible = spendingLimitMessage.isNotEmpty()
                    || (cosignedTime > 0L && isSigned.not() && txStatus.isPendingSignatures())
                    || serverTransaction?.isCosigning == true
            if (serverTransaction?.isCosigning == true) {
                binding.xpf.text = context.getString(R.string.nc_co_signing_in_progress)
            } else if (spendingLimitMessage.isNotEmpty()) {
                binding.xpf.text = serverTransaction?.spendingLimitMessage
            } else if (cosignedTime > 0L && isSigned.not() && txStatus.isPendingSignatures()) {
                val cosignDate = Date(cosignedTime)
                val spannable = if (DateUtils.isToday(cosignedTime)) {
                    SpannableString("${context.getString(R.string.nc_cosign_at)} ${cosignDate.formatByHour()}")
                } else {
                    SpannableString("${context.getString(R.string.nc_cosign_at)} ${cosignDate.formatByHour()} ${cosignDate.formatByWeek()}")
                }
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    context.getString(R.string.nc_cosign_at).length,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.xpf.text = spannable
            }
            binding.xpf.setTextColor(ContextCompat.getColor(context, R.color.nc_beeswax_dark))
        } else {
            binding.xpf.isVisible = true
            binding.xpf.setTextColor(ContextCompat.getColor(context, R.color.nc_text_secondary))
            binding.xpf.text = model.getXfpOrCardIdLabel()
        }
    }

    private fun SignerModel.isSigned() = signerMap[fingerPrint] ?: false

}