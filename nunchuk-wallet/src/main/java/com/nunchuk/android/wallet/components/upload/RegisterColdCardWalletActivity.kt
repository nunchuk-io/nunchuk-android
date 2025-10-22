package com.nunchuk.android.wallet.components.upload

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.ExportWalletQRCodeType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.ExportColdcardSuccess
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterColdCardWalletActivity : BaseComposeActivity(), BottomSheetOptionListener {

    private val viewModel by viewModels<SharedWalletConfigurationViewModel>()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                viewModel.doneScanQr()
                openWalletCreatedSuccess()
            }
        }

    private val launcherSharing =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            openWalletCreatedSuccess()
        }

    private val sharingController: IntentSharingController by lazy {
        IntentSharingController.from(this, launcherSharing)
    }

    private val walletId by lazy { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }
    private val groupId by lazy { intent.getStringExtra(EXTRA_GROUP_ID) }
    private val replacedWalletId by lazy { intent.getStringExtra(EXTRA_REPLACED_WALLET_ID).orEmpty() }
    private val quickWalletParam by lazy { intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isExportViaFile = intent.getBooleanExtra(EXTRA_IS_EXPORT_VIA_FILE, false)

        if (walletId.isEmpty()) {
            finish()
            return
        }

        viewModel.init(walletId)
        observer()

        setContentView(
            ComposeView(this).apply {
                setContent {
                    RegisterColdCardWalletScreen(
                        isExportViaFile = isExportViaFile,
                        onDownloadClick = { showSaveShareOption() },
                        onContinueClick = {
                            if (isExportViaFile) {
                            } else {
                                openDynamicQRScreen(walletId, ExportWalletQRCodeType.BBQR)
                            }
                        }
                    )
                }
            },
        )
    }

    private fun openDynamicQRScreen(walletId: String, qrCodeType: Int) {
        navigator.openDynamicQRScreen(this, launcher, walletId, qrCodeType)
    }

    private fun observer() {
        flowObserver(viewModel.event, collector = ::handleEvent)
    }

    private fun handleEvent(event: UploadConfigurationEvent) {
        when (event) {
            is ExportColdcardSuccess -> {
                shareConfigurationFile(event.filePath)
            }

            is UploadConfigurationEvent.ShowError -> {
                NCToastMessage(this).showError(event.message)
            }

            is UploadConfigurationEvent.SaveLocalFile -> {
                showSaveFileState(event.isSuccess)
                if (event.isSuccess) {
                    openWalletCreatedSuccess()
                }
            }

            else -> {}
        }
    }

    private fun showSaveShareOption() {
        BottomSheetOption.newInstance(
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_SAVE_FILE,
                    stringId = R.string.nc_save_file
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_SHARE_FILE,
                    stringId = R.string.nc_share_file
                )
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_SAVE_FILE -> saveFileToLocal()
            SheetOptionType.TYPE_SHARE_FILE -> shareFile()
            else -> {}
        }
    }

    private fun shareFile() {
        viewModel.handleColdcardExportToFile(false)
    }

    private fun saveFileToLocal() {
        viewModel.handleColdcardExportToFile(true)
    }

    private fun shareConfigurationFile(filePath: String?) {
        if (filePath.isNullOrEmpty().not()) {
            sharingController.shareFile(filePath.orEmpty())
        }
    }

    private fun showSaveFileState(isSuccess: Boolean) {
        hideLoading()
        if (isSuccess) {
            NCToastMessage(this).showMessage(getString(R.string.nc_save_file_success))
        } else {
            NCToastMessage(this).showError(getString(R.string.nc_save_file_failed))
        }
    }

    private fun openWalletCreatedSuccess() {
        navigator.openMembershipActivity(
            activityContext = this,
            groupStep = com.nunchuk.android.model.MembershipStage.CREATE_WALLET_SUCCESS,
            walletId = walletId,
            groupId = groupId,
            replacedWalletId = replacedWalletId,
            quickWalletParam = quickWalletParam
        )
        ActivityManager.popUntilRoot()
    }

    companion object {
        const val EXTRA_IS_EXPORT_VIA_FILE = "is_export_via_file"
        const val EXTRA_WALLET_ID = "wallet_id"
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_REPLACED_WALLET_ID = "replaced_wallet_id"
        const val EXTRA_QUICK_WALLET_PARAM = "quick_wallet_param"

        fun createIntent(
            context: Context,
            walletId: String,
            isExportViaFile: Boolean,
            groupId: String? = null,
            replacedWalletId: String? = null,
            quickWalletParam: QuickWalletParam? = null
        ): Intent {
            return Intent(context, RegisterColdCardWalletActivity::class.java).apply {
                putExtra(EXTRA_WALLET_ID, walletId)
                putExtra(EXTRA_IS_EXPORT_VIA_FILE, isExportViaFile)
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_REPLACED_WALLET_ID, replacedWalletId)
                putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
            }
        }
    }
}

@Composable
internal fun RegisterColdCardWalletScreen(
    remainTime: Int = 0,
    isExportViaFile: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_add_coldcard_view_nfc_intro,
                    title =
                        stringResource(
                            id = R.string.nc_estimate_remain_time,
                            remainTime
                        )
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onContinueClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = "Register wallet on COLDCARD",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = if (isExportViaFile) {
                        "Download the wallet configuration file below and copy it to a microSD card.\n\nInsert the card into the COLDCARD, then go to Settings → Miniscript → Import file."
                    } else {
                        "On COLDCARD, go to Settings → Miniscript → Scan QR code.\n\nTap Continue to display the QR code."
                    },
                    style = NunchukTheme.typography.body
                )

                if (isExportViaFile) {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .background(
                                color = colorResource(R.color.nc_bg_mid_gray),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onDownloadClick() }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                text = "Download file",
                                style = NunchukTheme.typography.body,
                                textAlign = TextAlign.Center
                            )

                            NcIcon(
                                painter = painterResource(id = R.drawable.ic_download),
                                contentDescription = "",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RegisterColdCardWalletScreenPreview() {
    RegisterColdCardWalletScreen()
}