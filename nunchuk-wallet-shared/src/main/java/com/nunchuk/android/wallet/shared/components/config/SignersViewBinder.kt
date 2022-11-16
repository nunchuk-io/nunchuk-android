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

package com.nunchuk.android.wallet.shared.components.config

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.wallet.core.databinding.ItemAssignSignerBinding
import com.nunchuk.android.widget.util.AbsViewBinder

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SignerModel>,
) : AbsViewBinder<SignerModel, ItemAssignSignerBinding>(container, signers) {

    override fun initializeBinding() = ItemAssignSignerBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SignerModel) {
        val binding = ItemAssignSignerBinding.bind(container.getChildAt(position))
        binding.signerType.text = model.toReadableSignerType(context, isIgnorePrimary = true)
        binding.ivSignerType.isVisible = false
        binding.avatar.isVisible = true
        binding.avatar.text = model.name.shorten()

        binding.signerName.text = model.name
        val xfpValue = "XFP: ${model.fingerPrint}"
        binding.xpf.text = xfpValue
        binding.checkbox.isVisible = false

        val isEmptyXFP = model.fingerPrint.isEmpty()
        binding.xpf.isVisible = !isEmptyXFP
        binding.signerType.isVisible = !isEmptyXFP
        binding.signerPrimaryKeyType.isVisible = model.isPrimaryKey
        binding.tvBip32Path.isVisible = model.derivationPath.isNotEmpty()
        binding.tvBip32Path.text = "BIP32 path: ${model.derivationPath}"
    }
}