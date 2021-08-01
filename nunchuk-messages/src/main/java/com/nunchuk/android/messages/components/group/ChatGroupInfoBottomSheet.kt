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