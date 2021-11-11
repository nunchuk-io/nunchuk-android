package com.nunchuk.android.contact.components.add

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.contact.components.add.AddContactsEvent.*
import com.nunchuk.android.contact.databinding.BottomSheetAddContactsBinding
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnEnterOrSpaceListener
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddContactsBottomSheet : BaseBottomSheet<BottomSheetAddContactsBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    var listener: () -> Unit = {}

    private val viewModel: AddContactsViewModel by viewModels { factory }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetAddContactsBinding {
        return BottomSheetAddContactsBinding.inflate(inflater, container, false)
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

    private fun handleState(state: AddContactsState) {
        bindEmailList(state.emails)
    }

    private fun bindEmailList(emails: List<EmailWithState>) {
        if (emails.isEmpty()) {
            binding.emails.removeAllViews()
        } else {
            EmailsViewBinder(binding.emails, emails, viewModel::handleRemove).bindItems()
        }
    }

    private fun handleEvent(event: AddContactsEvent) {
        when (event) {
            InviteFriendSuccessEvent -> cleanUp()
            InvalidEmailEvent -> showErrorMessage(true)
            AllEmailValidEvent -> showErrorMessage(false)
            AddContactSuccessEvent -> showAddContactSuccess()
            is AddContactsErrorEvent -> showAddContactError(event.message)
            is LoadingEvent -> showOrHideLoading(event.loading)
            is FailedSendEmailsEvent -> showDialogConfirmationEmailInvitation(event.emails)
        }
    }

    private fun showAddContactError(message: String) {
        showOrHideLoading(false)
        NCToastMessage(requireActivity()).showError(message)
        cleanUp()
    }

    private fun showAddContactSuccess() {
        showOrHideLoading(false)
        NCToastMessage(requireActivity()).showMessage("Add contact success")
        cleanUp()
    }

    private fun showErrorMessage(show: Boolean) {
        showOrHideLoading(false)
        binding.errorText.isVisible = show
    }

    private fun setupViews() {
        binding.input.setOnEnterOrSpaceListener {
            viewModel.handleAddEmail(binding.input.text.toString())
            binding.input.setText("")
        }
        binding.input.addTextChangedListener { text ->
            val currentText = text.toString()
            if (currentText.isNotEmpty() && (currentText.last() == ',' || currentText.last() == ' ')) {
                viewModel.handleAddEmail(currentText.dropLast(1))
                binding.input.setText("")
            }
        }
        binding.closeBtn.setOnClickListener {
            cleanUp()
        }

        binding.sendBtn.setOnClickListener {
            val currentText = binding.input.text.toString().trim()
            if (currentText.isNotEmpty()) {
                viewModel.handleAddEmail(currentText)
                binding.input.setText("")
            }

            viewModel.handleSend()
        }
    }

    private fun cleanUp() {
        viewModel.cleanUp()
        binding.input.setText("")
        binding.emails.removeAllViews()
        listener()
        dialog?.dismiss()
        dismiss()
    }

    private fun showDialogConfirmationEmailInvitation(emails: List<String>) {
        NCInviteFriendDialog(requireActivity()).showDialog(
            inviteList = emails.joinToString(),
            onYesClick = {
                viewModel.inviteFriend(emails)
            },
            onNoClick = {
                cleanUp()
            }
        )
    }

    companion object {
        private const val TAG = "AddContactsBottomSheet"
        fun show(fragmentManager: FragmentManager) = AddContactsBottomSheet().apply {
            show(fragmentManager, TAG)
        }
    }

}
