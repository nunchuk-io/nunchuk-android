package com.nunchuk.android.signer.tapsigner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.util.NFC_DEFAULT_NAME
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentAddNameKeyBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddNfcNameFragment : BaseChangeTapSignerNameFragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<AddNfcNameViewModel>()

    private var _binding: FragmentAddNameKeyBinding? = null
    val binding: FragmentAddNameKeyBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddNameKeyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        registerEvents()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onUpdateNameSuccess(signer: MasterSigner) {
        navigator.openSignerInfoScreen(
            activityContext = requireActivity(),
            masterFingerprint = signer.device.masterFingerprint,
            id = signer.id,
            name = signer.name,
            type = signer.type,
            justAdded = true
        )
        requireActivity().finish()
    }

    override val signerName: String
        get() = binding.signerName.getEditText()

    override val isMembershipFlow: Boolean = false

    private fun initViews() {
        binding.signerName.getEditTextView()
            .setText(nfcViewModel.masterSigner?.name ?: NFC_DEFAULT_NAME)
        binding.signerName.setMaxLength(20)
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.signerName.addTextChangedCallback {
            binding.nameCounter.text = "${it.length}/$MAX_LENGTH"
        }
        binding.btnContinue.setOnClickListener {
            if (binding.signerName.getEditText().isEmpty()) {
                binding.signerName.setError(getString(R.string.nc_text_required))
                return@setOnClickListener
            }
            binding.signerName.hideError()
            nfcViewModel.masterSigner?.let { masterSigner ->
                viewModel.updateName(masterSigner, binding.signerName.getEditText())
            } ?: run {
                startNfcAddKeyFlow()
            }
        }
    }

    private fun startNfcAddKeyFlow() {
        (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_ADD_KEY)
    }

    companion object {
        private const val MAX_LENGTH = 20
    }
}