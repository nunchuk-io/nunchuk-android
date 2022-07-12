package com.nunchuk.android.signer.components.details

import android.content.Context
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.core.util.toReadableDrawable
import com.nunchuk.android.core.util.toReadableString
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.toSpec
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.details.SignerInfoEvent.*
import com.nunchuk.android.signer.components.details.model.SingerOption
import com.nunchuk.android.signer.databinding.ActivitySignerInfoBinding
import com.nunchuk.android.signer.nfc.NfcSetupActivity
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SignerInfoActivity : BaseNfcActivity<ActivitySignerInfoBinding>(),
    SingerInfoOptionBottomSheet.OptionClickListener {

    private val viewModel: SignerInfoViewModel by viewModels()

    override fun initializeBinding() = ActivitySignerInfoBinding.inflate(layoutInflater)

    private val args: SignerInfoArgs by lazy { SignerInfoArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()
        observeEvent()
        viewModel.init(args.id, args.signerType)
    }

    override fun onOptionClickListener(option: SingerOption) {
        when (option) {
            SingerOption.TOP_UP -> startNfcFlow(REQUEST_NFC_TOPUP_XPUBS)
            SingerOption.CHANGE_CVC -> NfcSetupActivity.navigate(this, NfcSetupActivity.CHANGE_CVC)
            SingerOption.BACKUP_KEY -> startNfcFlow(REQUEST_NFC_VIEW_BACKUP_KEY)
            SingerOption.REMOVE_KEY -> handleRemoveKey()
        }
    }

    private fun handleRemoveKey() {
        if (args.isInWallet) {
            NCInfoDialog(this).showDialog(
                message = getString(R.string.nc_warning_key_use_in_wallet),
            )
        } else if (args.signerType == SignerType.FOREIGN_SOFTWARE) {
            NCInfoDialog(this).showDialog(
                message = getString(R.string.nc_please_remove_on_added_device),
            )
        } else {
            NCWarningDialog(this).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_delete_key_msg),
                onYesClick = {
                    viewModel.handleRemoveSigner()
                }
            )
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)

        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_VIEW_BACKUP_KEY }.collect {
                    requestViewBackupKey(it)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_HEALTH_CHECK }.collect {
                    val isoDep = IsoDep.get(it.tag) ?: return@collect
                    viewModel.healthCheckTapSigner(isoDep, nfcViewModel.inputCvc.orEmpty(), viewModel.state.value?.masterSigner ?: return@collect)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_TOPUP_XPUBS }.collect {
                    topUpXPubs(it)
                }
            }
        }
    }

    private fun requestViewBackupKey(nfcScanInfo: NfcScanInfo) {
        viewModel.getTapSignerBackup(IsoDep.get(nfcScanInfo.tag) ?: return, nfcViewModel.inputCvc.orEmpty())
    }

    private fun topUpXPubs(nfcScanInfo: NfcScanInfo) {
        viewModel.topUpXpubTapSigner(IsoDep.get(nfcScanInfo.tag) ?: return, nfcViewModel.inputCvc.orEmpty(), args.id)
    }

    private fun handleState(state: SignerInfoState) {
        state.remoteSigner?.let(::bindRemoteSigner)
        state.masterSigner?.let(::bindMasterSigner)
        binding.tvCardId.isVisible = state.nfcCardId != null
        binding.tvCardIdLabel.isVisible = binding.tvCardId.isVisible
        state.nfcCardId?.let { cardId ->
            binding.tvCardId.text = cardId
        }
    }

    private fun bindMasterSigner(signer: MasterSigner) {
        binding.signerTypeIcon.setImageDrawable(signer.type.toReadableDrawable(this))
        binding.fingerprint.isVisible = true
        binding.fingerprint.text = signer.device.masterFingerprint
        binding.signerSpec.isVisible = false
        binding.signerType.text = signer.type.toReadableString(this)
    }

    private fun bindRemoteSigner(signer: SingleSigner) {
        binding.signerTypeIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_air_signer_big))
        binding.signerSpec.isVisible = true
        binding.signerSpec.text = signer.toSpec()
        binding.fingerprint.isVisible = false
        binding.signerType.text = signer.type.toReadableString(this)
    }

    private fun handleEvent(event: SignerInfoEvent) {
        showOrHideLoading(event is Loading)
        when (event) {
            is UpdateNameSuccessEvent -> {
                binding.signerName.text = event.signerName
                showEditSignerNameSuccess()
            }
            RemoveSignerCompletedEvent -> openMainScreen()
            is RemoveSignerErrorEvent -> showToast(event.message)
            is UpdateNameErrorEvent -> showToast(event.message)
            is HealthCheckErrorEvent -> {
                if (nfcViewModel.handleNfcError(event.e).not()) showHealthCheckError(event)
            }
            is HealthCheckSuccessEvent -> NCToastMessage(this).showMessage(
                message = getString(R.string.nc_txt_run_health_check_success_event, args.name),
                icon = R.drawable.ic_check_circle_outline
            )
            is GetTapSignerBackupKeyEvent -> IntentSharingController.from(this).shareFile(event.backupKeyPath)
            is GetTapSignerBackupKeyError -> nfcViewModel.handleNfcError(event.e)
            TopUpXpubSuccess -> NCToastMessage(this).showMessage(
                message = getString(R.string.nc_xpub_topped_up),
                icon = R.drawable.ic_check_circle_outline
            )
            is TopUpXpubFailed -> {
                if (!event.e?.message.isNullOrEmpty()) {
                    NCToastMessage(this).showError(event.e?.message.orEmpty())
                }
            }
        }
    }

    private fun showHealthCheckError(event: HealthCheckErrorEvent) {
        if (event.message.isNullOrEmpty()) {
            val errorMessage = if (event.e?.message.isNullOrEmpty()) {
                getString(
                    R.string.nc_txt_run_health_check_error_event,
                    args.name
                )
            } else {
                event.e?.message.orEmpty()
            }
            NCToastMessage(this).showError(errorMessage)
        } else {
            NCToastMessage(this).showWarning(event.message)
        }
    }

    private fun setupViews() {
        binding.signerName.text = args.name
        if (args.justAdded) {
            NCToastMessage(this).showMessage(
                message = getString(R.string.nc_text_add_signer_success, args.name),
                icon = R.drawable.ic_check_circle_outline
            )
            if (args.setPassphrase) {
                NCToastMessage(this).showMessage(
                    message = getString(R.string.nc_text_set_passphrase_success),
                    offset = R.dimen.nc_padding_44,
                    dismissTime = 4000L
                )
            }
        }
        binding.btnDone.isVisible = args.justAdded
        binding.signerType.text = args.signerType.toReadableString(this)
        binding.toolbar.setNavigationOnClickListener { openMainScreen() }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_more) {
                val type = viewModel.state.value?.masterSigner?.type
                    ?: viewModel.state.value?.remoteSigner?.type
                type?.let { signerType ->
                    SingerInfoOptionBottomSheet.newInstance(signerType)
                        .show(supportFragmentManager, "SingerInfoOptionBottomSheet")
                }
            }
            false
        }
        binding.btnDone.setOnClickListener { openMainScreen() }
        binding.signerName.setOnClickListener { onEditClicked() }
        binding.btnHealthCheck.setOnClickListener { handleRunHealthCheck() }
    }

    private fun handleRunHealthCheck() {
        val masterSigner = viewModel.state.value?.masterSigner
        if (masterSigner != null) {
            if (masterSigner.type == SignerType.NFC) {
                startNfcFlow(REQUEST_NFC_HEALTH_CHECK)
            } else if (masterSigner.software) {
                if (masterSigner.device.needPassPhraseSent) {
                    NCInputDialog(this).showDialog(
                        title = getString(R.string.nc_transaction_enter_passphrase),
                        onConfirmed = { viewModel.handleHealthCheck(masterSigner, it) }
                    )
                } else {
                    viewModel.handleHealthCheck(masterSigner)
                }
            }
        }
    }

    private fun openMainScreen() {
        if (args.justAdded) {
            navigator.openMainScreen(this)
        } else {
            finish()
        }
    }

    private fun onEditClicked() {
        val bottomSheet = SignerUpdateBottomSheet.show(
            fragmentManager = supportFragmentManager,
            signerName = binding.signerName.text.toString()
        )
        bottomSheet.setListener(viewModel::handleEditCompletedEvent)
    }

    private fun showEditSignerNameSuccess() {
        binding.signerName.post {
            NCToastMessage(this).showMessage(
                message = getString(R.string.nc_text_change_signer_success),
                icon = R.drawable.ic_check_circle_outline
            )
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            id: String,
            name: String,
            type: SignerType,
            justAdded: Boolean = false,
            setPassphrase: Boolean = false,
            isInWallet: Boolean
        ) {
            activityContext.startActivity(
                SignerInfoArgs(
                    id = id,
                    name = name,
                    justAdded = justAdded,
                    signerType = type,
                    setPassphrase = setPassphrase,
                    isInWallet = isInWallet
                ).buildIntent(activityContext)
            )
        }
    }

}