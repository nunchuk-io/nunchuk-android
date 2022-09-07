package com.nunchuk.android.wallet.components.base

import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.checkReadExternalPermission
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.upload.SharedWalletConfigurationViewModel
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.flow.filter

abstract class BaseWalletConfigActivity<Binding : ViewBinding> : BaseNfcActivity<Binding>(), BottomSheetOptionListener {
    protected val sharedViewModel by viewModels<SharedWalletConfigurationViewModel>()

    override fun onOptionClicked(option: SheetOption) {
        when(option.type) {
            SheetOptionType.EXPORT_COLDCARD_VIA_NFC -> handleColdcardExportToNfc()
            SheetOptionType.EXPORT_COLDCARD_VIA_FILE -> handleColdcardExportToFile()
            SheetOptionType.TYPE_EXPORT_KEYSTONE_QR -> sharedViewModel.handleExportWalletQR()
            SheetOptionType.TYPE_EXPORT_PASSPORT_QR -> sharedViewModel.handleExportPassport()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observer()
    }

    private fun observer() {
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_EXPORT_WALLET_TO_MK4 }) {
            sharedViewModel.handleColdcardExportNfc(Ndef.get(it.tag) ?: return@flowObserver)
        }
        sharedViewModel.event.observe(this, ::handleSharedEvent)
    }

    @CallSuper
    protected open fun handleSharedEvent(event: UploadConfigurationEvent) {
        when (event) {
            is UploadConfigurationEvent.ShowError -> showError(event)
            is UploadConfigurationEvent.OpenDynamicQRScreen -> openDynamicQRScreen(event)
            is UploadConfigurationEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
        }
    }

    protected fun showSubOptionsExportQr() {
        val options = listOf(
            SheetOption(SheetOptionType.TYPE_EXPORT_KEYSTONE_QR, R.drawable.ic_qr, R.string.nc_export_as_qr_keystone),
            SheetOption(SheetOptionType.TYPE_EXPORT_PASSPORT_QR, R.drawable.ic_qr, R.string.nc_export_as_passport),
        )
        val bottomSheet = BottomSheetOption.newInstance(options)
        bottomSheet.show(supportFragmentManager, "BottomSheetOption")
    }

    protected fun showExportColdcardOptions() {
        BottomSheetOption.newInstance(listOf(
            SheetOption(SheetOptionType.EXPORT_COLDCARD_VIA_NFC, resId = R.drawable.ic_qr, stringId = R.string.nc_export_via_nfc),
            SheetOption(SheetOptionType.EXPORT_COLDCARD_VIA_FILE, resId = R.drawable.ic_qr, stringId = R.string.nc_export_via_file)
        )).show(supportFragmentManager, "BottomSheetOption")
    }

    protected fun openDynamicQRScreen(event: UploadConfigurationEvent.OpenDynamicQRScreen) {
        navigator.openDynamicQRScreen(this, event.values)
    }

    protected fun showError(event: UploadConfigurationEvent.ShowError) {
        NCToastMessage(this).showError(event.message)
    }

    private fun handleColdcardExportToNfc() {
        startNfcFlow(REQUEST_EXPORT_WALLET_TO_MK4)
    }

    private fun handleColdcardExportToFile() {
        if (checkReadExternalPermission()) {
            sharedViewModel.handleColdcardExportToFile()
        }
    }
}