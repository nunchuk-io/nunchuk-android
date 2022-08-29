package com.nunchuk.android.wallet.components.config

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.checkReadExternalPermission
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import com.nunchuk.android.wallet.databinding.ActivityWalletConfigBinding
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.NCDeleteConfirmationDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletConfigActivity : BaseActivity<ActivityWalletConfigBinding>(), BottomSheetOptionListener {

    private val viewModel: WalletConfigViewModel by viewModels()

    private val controller: IntentSharingController by lazy(LazyThreadSafetyMode.NONE) { IntentSharingController.from(this) }

    private val args: WalletConfigArgs by lazy { WalletConfigArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityWalletConfigBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.walletId)
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_EXPORT_AS_QR -> showSubOptionsExportQr()
            SheetOptionType.TYPE_EXPORT_KEYSTONE_QR -> viewModel.handleExportWalletQR()
            SheetOptionType.TYPE_EXPORT_PASSPORT_QR -> viewModel.handleExportPassport()
            SheetOptionType.TYPE_DELETE_WALLET -> handleDeleteWallet()
            SheetOptionType.TYPE_EXPORT_TO_COLD_CARD -> handleExportColdcard()
        }
    }

    private fun handleDeleteWallet() {
        if (viewModel.isSharedWallet()) {
            NCWarningDialog(this).showDialog(
                message = getString(R.string.nc_delete_collaborative_wallet),
                onYesClick = { viewModel.handleDeleteWallet() }
            )
        } else {
            NCDeleteConfirmationDialog(this).showDialog(
                message = getString(R.string.nc_are_you_sure_to_delete_wallet),
                onConfirmed = {
                    if (it.trim() == CONFIRMATION_TEXT) {
                        viewModel.handleDeleteWallet()
                    } else {
                        NCToastMessage(this).showWarning(getString(R.string.nc_incorrect))
                    }
                }
            )
        }
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: WalletConfigEvent) {
        when (event) {
            UpdateNameSuccessEvent -> showEditWalletSuccess()
            is UpdateNameErrorEvent -> NCToastMessage(this).showWarning(event.message)
            is WalletConfigEvent.OpenDynamicQRScreen -> navigator.openDynamicQRScreen(this, event.descriptors)
            WalletConfigEvent.DeleteWalletSuccess -> walletDeleted()
            is WalletConfigEvent.UploadWalletConfigEvent -> shareConfigurationFile(event.filePath)
            is WalletConfigEvent.WalletDetailsError -> onGetWalletError(event)
        }
    }

    private fun onGetWalletError(event: WalletConfigEvent.WalletDetailsError) {
        NCToastMessage(this).showError(event.message)
    }

    private fun shareConfigurationFile(filePath: String) {
        controller.shareFile(filePath)
    }

    private fun walletDeleted() {
        NCToastMessage(this).showMessage(getString(R.string.nc_wallet_delete_wallet_success))
        setResult(Activity.RESULT_OK)
        ActivityManager.popUntilRoot()
    }

    private fun handleState(walletExtended: WalletExtended) {
        val wallet = walletExtended.wallet
        binding.walletName.text = wallet.name

        binding.configuration.bindWalletConfiguration(wallet)

        binding.walletType.text = (if (wallet.escrow) WalletType.ESCROW else WalletType.MULTI_SIG).toReadableString(this)
        binding.addressType.text = wallet.addressType.toReadableString(this)
        binding.shareIcon.isVisible = walletExtended.isShared
        SignersViewBinder(binding.signersContainer, wallet.signers.map(SingleSigner::toModel)).bindItems()
    }

    private fun setupViews() {
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_more) {
                showMoreOptions()
            }
            false
        }
        binding.walletName.setOnClickListener { onEditClicked() }
        binding.btnDone.setOnClickListener {
            navigator.openMainScreen(this)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun showMoreOptions() {
        val options = listOf(
            SheetOption(SheetOptionType.TYPE_EXPORT_AS_QR, R.drawable.ic_qr, R.string.nc_show_as_qr_code),
            SheetOption(SheetOptionType.TYPE_EXPORT_TO_COLD_CARD, R.drawable.ic_export, R.string.nc_wallet_export_coldcard),
            SheetOption(SheetOptionType.TYPE_DELETE_WALLET, R.drawable.ic_delete_red, R.string.nc_wallet_delete_wallet, isDeleted = true),
        )
        val bottomSheet = BottomSheetOption.newInstance(options)
        bottomSheet.show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showSubOptionsExportQr() {
        val options = listOf(
            SheetOption(SheetOptionType.TYPE_EXPORT_KEYSTONE_QR, R.drawable.ic_qr, R.string.nc_export_as_qr_keystone),
            SheetOption(SheetOptionType.TYPE_EXPORT_PASSPORT_QR, R.drawable.ic_qr, R.string.nc_export_as_passport),
        )
        val bottomSheet = BottomSheetOption.newInstance(options)
        bottomSheet.show(supportFragmentManager, "BottomSheetOption")
    }

    private fun onEditClicked() {
        val bottomSheet = WalletUpdateBottomSheet.show(
            fragmentManager = supportFragmentManager,
            walletName = binding.walletName.text.toString()
        )

        bottomSheet.setListener(viewModel::handleEditCompleteEvent)
    }

    private fun showEditWalletSuccess() {
        binding.root.post { NCToastMessage(this).show(R.string.nc_text_change_wallet_success) }
    }

    private fun handleExportColdcard() {
        if (checkReadExternalPermission()) {
            viewModel.handleExportColdcard()
        }
    }

    companion object {
        private const val CONFIRMATION_TEXT = "DELETE"

        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(WalletConfigArgs(walletId = walletId).buildIntent(activityContext))
        }

        fun start(launcher: ActivityResultLauncher<Intent>, activityContext: Context, walletId: String) {
            launcher.launch(WalletConfigArgs(walletId = walletId).buildIntent(activityContext))
        }
    }

}