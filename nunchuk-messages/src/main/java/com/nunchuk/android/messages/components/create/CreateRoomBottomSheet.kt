package com.nunchuk.android.messages.components.create

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.messages.components.create.CreateRoomEvent.*
import com.nunchuk.android.messages.databinding.BottomSheetCreateRoomBinding
import com.nunchuk.android.model.Contact
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class CreateRoomBottomSheet : BaseBottomSheet<BottomSheetCreateRoomBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: CreateRoomViewModel by activityViewModels { factory }

    private lateinit var adapter: ContactsAdapter

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetCreateRoomBinding {
        return BottomSheetCreateRoomBinding.inflate(inflater, container, false)
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
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: CreateRoomState) {
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

    private fun handleEvent(event: CreateRoomEvent) {
        when (event) {
            NoContactsEvent -> showNoContactsError()
            is CreateRoomSuccessEvent -> onRoomCreated(event.roomId)
            is CreateRoomErrorEvent -> showRoomCreateError(event.message)
        }
    }

    private fun onRoomCreated(roomId: String) {
        hideLoading()
        cleanUp()
        navigator.openRoomDetailActivity(requireActivity(), roomId = roomId)
    }

    private fun showRoomCreateError(message: String) {
        hideLoading()
        NCToastMessage(requireActivity()).show(message)
    }

    private fun showNoContactsError() {
        NCToastMessage(requireActivity()).show("You don't have any contacts")
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
        private const val TAG = "CreateRoomBottomSheet"
        fun show(fragmentManager: FragmentManager) = CreateRoomBottomSheet().apply {
            show(fragmentManager, TAG)
        }
    }

}