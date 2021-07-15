package com.nunchuk.android.messages.contact

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseBottomSheetDialogFragment
import com.nunchuk.android.messages.contact.AddContactsEvent.*
import com.nunchuk.android.messages.databinding.BottomSheetAddContactsBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddContactsBottomSheet : BaseBottomSheetDialogFragment<BottomSheetAddContactsBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    var listener: () -> Unit = {}

    private val viewModel: AddContactsViewModel by activityViewModels { factory }

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
            InvalidEmailEvent -> showErrorMessage(true)
            AllEmailValidEvent -> showErrorMessage(false)
            AddContactSuccessEvent -> showAddContactSuccess()
            is AddContactsErrorEvent -> showAddContactError(event.message)
        }
    }

    private fun showAddContactError(message: String) {
        NCToastMessage(requireActivity()).showError(message)
        cleanUp()
    }

    private fun showAddContactSuccess() {
        NCToastMessage(requireActivity()).showMessage("Add contact success")
        cleanUp()
    }

    private fun showErrorMessage(show: Boolean) {
        binding.errorText.isVisible = show
    }

    private fun setupViews() {
        binding.input.setOnEnterListener {
            viewModel.handleAddEmail(binding.input.text.toString())
            binding.input.setText("")
        }
        binding.closeBtn.setOnClickListener {
            cleanUp()
        }

        binding.sendBtn.setOnClickListener {
            viewModel.handleSend()
        }
    }

    private fun cleanUp() {
        viewModel.cleanUp()
        binding.input.setText("")
        binding.emails.removeAllViews()
        listener()
        dismiss()
    }

    companion object {
        private const val TAG = "AddContactsBottomSheet"
        fun show(fragmentManager: FragmentManager) = AddContactsBottomSheet().apply {
            show(fragmentManager, TAG)
        }
    }

}

fun EditText.setOnEnterListener(callback: () -> Unit) {
    setOnKeyListener(object : View.OnKeyListener {
        override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE)) {
                callback()
                return true
            }
            return false
        }
    })
}