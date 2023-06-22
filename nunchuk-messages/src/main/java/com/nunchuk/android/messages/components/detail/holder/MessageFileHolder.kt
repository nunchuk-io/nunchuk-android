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

package com.nunchuk.android.messages.components.detail.holder

import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.messages.components.detail.NunchukFileMessage
import com.nunchuk.android.messages.databinding.ItemFileAttachmentBinding

class MessageFileHolder(
    private val binding: ItemFileAttachmentBinding,
    private val onDownloadOrOpen: (media: NunchukFileMessage) -> Unit
) : ViewHolder(binding.root) {
    init {
        binding.root.setOnClickListener {
            onDownloadOrOpen(it.tag as NunchukFileMessage)
        }
    }
    fun bind(message: NunchukFileMessage) {
        binding.root.tag = message
        binding.containerAttachment.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = if (message.isMine) Gravity.END else Gravity.START
        }
        binding.tvFileName.text = message.filename
        binding.tvError.isGone = message.error.isNullOrEmpty()
        binding.tvError.text = message.error
    }
}