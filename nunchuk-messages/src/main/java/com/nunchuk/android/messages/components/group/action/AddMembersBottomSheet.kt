package com.nunchuk.android.messages.components.group.action

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.create.ContactsAdapter
import com.nunchuk.android.messages.components.create.ReceiptsViewBinder
import com.nunchuk.android.messages.components.group.action.AddMembersEvent.*
import com.nunchuk.android.messages.databinding.BottomSheetAddMembersBinding
import com.nunchuk.android.model.Contact
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddMembersBottomSheet : BaseBottomSheet<BottomSheetAddMembersBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: AddMembersViewModel by viewModels { factory }

    private val args: AddMembersBottomSheetArgs by lazy { AddMembersBottomSheetArgs.deserializeFrom(arguments) }

    private lateinit var adapter: ContactsAdapter

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetAddMembersBinding {
        return BottomSheetAddMembersBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
        observeEvent()
        viewModel.initRoom(args.roomId)
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: AddMembersState) {
        bindReceiptList(state.receipts)
        bindContactList(state.suggestions)
    }

    private fun bindContactList(suggestions: List<Contact>) {
        adapter.items = suggestions
    }

    private fun bindReceiptList(receipts: List<Contact>) {
        if (receipts.isEmpty()) {
            binding.doneBtn.isVisible = false
            binding.receipts.removeAllViews()
        } else {
            binding.doneBtn.isVisible = true
            ReceiptsViewBinder(binding.receipts, receipts, viewModel::handleRemove).bindItems()
        }
    }

    private fun handleEvent(event: AddMembersEvent) {
        when (event) {
            NoContactsEvent -> showNoContactsError()
            is AddMembersSuccessEvent -> showMembersAdded()
            is AddMembersError -> showAddMembersError(event.message)
        }
    }

    private fun showMembersAdded() {
        NCToastMessage(requireActivity()).show(getString(R.string.nc_message_new_members_added))
        hideLoading()
        cleanUp()
    }

    private fun showAddMembersError(message: String) {
        hideLoading()
        NCToastMessage(requireActivity()).show(message)
    }

    private fun showNoContactsError() {
        NCToastMessage(requireActivity()).show(getString(R.string.nc_message_empty_contacts))
        cleanUp()
    }

    private fun setupViews() {
        adapter = ContactsAdapter {
            viewModel.handleSelectContact(it)
            binding.input.setText("")
        }

        binding.contactList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.contactList.adapter = adapter

        binding.closeBtn.setOnClickListener {
            cleanUp()
        }
        binding.input.addTextChangedCallback(viewModel::handleInput)
        binding.doneBtn.setOnClickListener {
            showLoading()
            viewModel.handleDone()
        }
    }

    private fun cleanUp() {
        viewModel.cleanUp()
        binding.input.setText("")
        adapter.items = emptyList()
        binding.receipts.removeAllViews()
        dismiss()
    }

    companion object {

        private const val TAG = "AddMembersBottomSheet"

        private fun newInstance(roomId: String) = AddMembersBottomSheet().apply {
            arguments = AddMembersBottomSheetArgs(roomId).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, roomId: String): AddMembersBottomSheet {
            return newInstance(roomId).apply { show(fragmentManager, TAG) }
        }
    }

}

data class AddMembersBottomSheetArgs(val roomId: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_ROOM_ID, roomId)
    }

    companion object {
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"

        fun deserializeFrom(data: Bundle?) = AddMembersBottomSheetArgs(
            data?.getString(EXTRA_ROOM_ID).orEmpty()
        )
    }
}
