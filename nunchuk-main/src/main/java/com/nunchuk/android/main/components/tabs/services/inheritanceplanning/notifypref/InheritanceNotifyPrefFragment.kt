package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.contact.components.add.EmailWithState
import com.nunchuk.android.contact.components.add.EmailsViewBinder
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.databinding.FragmentInheritanceNotifyPrefBinding
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.widget.util.setOnEnterOrSpaceListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceNotifyPrefFragment : BaseFragment<FragmentInheritanceNotifyPrefBinding>() {

    private val viewModel: InheritanceNotifyPrefViewModel by viewModels()
    private val args: InheritanceNotifyPrefFragmentArgs by navArgs()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentInheritanceNotifyPrefBinding =
        FragmentInheritanceNotifyPrefBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceNotifyPrefEvent.ContinueClick -> {
                    openReviewPlanScreen(event.emails, event.isNotify)
                }
                InheritanceNotifyPrefEvent.AllEmailValidEvent -> showErrorMessage(false)
                InheritanceNotifyPrefEvent.InvalidEmailEvent -> showErrorMessage(true)
            }
        }
        flowObserver(viewModel.state) { state ->
            bindEmailList(state.emails)
        }
    }

    private fun openReviewPlanScreen(emails: List<String>, isNotify: Boolean) {
        if (args.isUpdateRequest || args.planFlow == InheritancePlanFlow.VIEW) {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    EXTRA_IS_NOTIFY to isNotify,
                    EXTRA_EMAILS to emails
                )
            )
            findNavController().popBackStack()
        } else {
            findNavController().navigate(
                InheritanceNotifyPrefFragmentDirections.actionInheritanceNotifyPrefFragmentToInheritanceReviewPlanFragment(
                    activationDate = args.activationDate,
                    note = args.note,
                    verifyToken = args.verifyToken,
                    emails = emails.toTypedArray(),
                    planFlow = args.planFlow,
                    isNotify = isNotify,
                    magicalPhrase = args.magicalPhrase
                )
            )
        }
    }

    private fun showErrorMessage(show: Boolean) {
        binding.tvErrorEmail.isVisible = show
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
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
        binding.notifyCheck.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateIsNotify(isChecked)
        }
        binding.btnNotification.setOnClickListener {
            openReviewPlanScreen(emptyList(), false)
        }
        binding.btnContinue.setOnClickListener {
            viewModel.onContinueClicked()
        }
    }

    private fun bindEmailList(emails: List<EmailWithState>) {
        if (emails.isEmpty()) {
            binding.emails.removeAllViews()
        } else {
            EmailsViewBinder(binding.emails, emails, viewModel::handleRemove).bindItems()
        }
        val isHasErrorEmail = emails.any { it.valid.not() && EmailValidator.valid(it.email) }
        val isHasErrorUserName = emails.any { it.valid.not() && !EmailValidator.valid(it.email) }
        binding.tvErrorEmail.isVisible = isHasErrorEmail
        binding.tvErrorUserName.isVisible = isHasErrorUserName
    }

    companion object {
        const val REQUEST_KEY = "InheritanceNotifyPrefFragment"
        const val EXTRA_IS_NOTIFY = "EXTRA_IS_NOTIFY"
        const val EXTRA_EMAILS = "EXTRA_EMAILS"
    }
}
