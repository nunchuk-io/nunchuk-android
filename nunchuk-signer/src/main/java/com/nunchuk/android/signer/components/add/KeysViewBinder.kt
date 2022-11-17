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

package com.nunchuk.android.signer.components.add

import android.view.ViewGroup
import androidx.core.view.get
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.databinding.ItemKeyBinding
import com.nunchuk.android.widget.util.AbsViewBinder

internal class KeysViewBinder(
    container: ViewGroup,
    signers: List<SingleSigner>,
    val callback: (SingleSigner) -> Unit = {}
) : AbsViewBinder<SingleSigner, ItemKeyBinding>(container, signers) {

    override fun initializeBinding() = ItemKeyBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: SingleSigner) {
        val binding = ItemKeyBinding.bind(container[position])
        binding.key.text = model.derivationPath
        binding.root.setOnClickListener {
            callback(model)
        }
    }

}
