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

package com.nunchuk.android.messages.components.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.messages.components.group.ChatGroupInfoOption.*
import com.nunchuk.android.messages.databinding.BottomSheetChatGroupInfoBinding

class ChatGroupInfoBottomSheet : BaseBottomSheet<BottomSheetChatGroupInfoBinding>() {

    lateinit var listener: (ChatGroupInfoOption) -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetChatGroupInfoBinding {
        return BottomSheetChatGroupInfoBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
    }

    private fun setupViews() {
        binding.btnEdit.setOnClickListener { onFinishWithAction(EDIT) }
        binding.btnAddMembers.setOnClickListener { onFinishWithAction(ADD) }
        binding.btnDelete.setOnClickListener { onFinishWithAction(LEAVE) }
    }

    private fun onFinishWithAction(option: ChatGroupInfoOption) {
        listener(option)
        dismiss()
    }

    companion object {
        private const val TAG = "ChatGroupInfoBottomSheet"
        fun show(fragmentManager: FragmentManager) = ChatGroupInfoBottomSheet().apply {
            show(fragmentManager, TAG)
        }
    }

}


enum class ChatGroupInfoOption {
    EDIT, ADD, LEAVE
}