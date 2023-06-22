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

package com.nunchuk.android.main.membership.authentication.dummytx

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.hadBroadcast
import com.nunchuk.android.core.util.toReadableDrawable
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.widget.databinding.ItemTransactionSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder
import com.nunchuk.android.widget.util.setOnDebounceClickListener

internal class TransactionSignersViewBinder(
    container: ViewGroup,
    private val signerMap: Map<String, Boolean>,
    signers: List<SignerModel>,
    private val txStatus: TransactionStatus,
    val listener: (SignerModel) -> Unit = {}
) : AbsViewBinder<SignerModel, ItemTransactionSignerBinding>(container, signers) {

    override fun initializeBinding() =
        ItemTransactionSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemTransactionSignerBinding.bind(container[position])
        binding.avatar.isVisible = false
        binding.ivSignerType.isVisible = true
        binding.ivSignerType.setImageDrawable(model.toReadableDrawable(context))
        binding.signerName.text = model.name
        binding.xpf.text = model.getXfpOrCardIdLabel()
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
            binding.btnSign.isVisible = true
            binding.signed.isVisible = false
            binding.signNotAvailable.isVisible = false
        }
    }

    private fun SignerModel.isSigned() = signerMap[fingerPrint] ?: false

}