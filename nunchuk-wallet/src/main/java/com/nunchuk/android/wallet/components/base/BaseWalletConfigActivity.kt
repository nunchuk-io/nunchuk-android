/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.wallet.components.base

import android.app.Activity
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.ExportWalletQRCodeType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.upload.SharedWalletConfigurationViewModel
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.flow.filter

abstract class BaseWalletConfigActivity<Binding : ViewBinding> : BaseNfcActivity<Binding>(),
    BottomSheetOptionListener {
    protected val sharedViewModel by viewModels<SharedWalletConfigurationViewModel>()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                sharedViewModel.doneScanQr()
            }
        }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.EXPORT_COLDCARD_VIA_NFC -> handleColdcardExportToNfc()
            SheetOptionType.EXPORT_COLDCARD_VIA_FILE -> handleColdcardExportToFile()
            SheetOptionType.TYPE_QR_BC_UR2_LEGACY -> openDynamicQRScreen(sharedViewModel.walletId, ExportWalletQRCodeType.BC_UR2_LEGACY)
            SheetOptionType.TYPE_QR_BC_UR2 -> openDynamicQRScreen(sharedViewModel.walletId, ExportWalletQRCodeType.BC_UR2)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observer()
    }

    private fun observer() {
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_EXPORT_WALLET_TO_MK4 }) {
            sharedViewModel.handleColdcardExportNfc(Ndef.get(it.tag) ?: return@flowObserver)
            nfcViewModel.clearScanInfo()
        }
        flowObserver(sharedViewModel.event, collector = ::handleSharedEvent)
    }

    @CallSuper
    protected open fun handleSharedEvent(event: UploadConfigurationEvent) {
        when (event) {
            is UploadConfigurationEvent.ShowError -> showError(event)
            is UploadConfigurationEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading, true)
            else -> {}
        }
    }

    fun showExportColdcardOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.EXPORT_COLDCARD_VIA_NFC,
                    resId = R.drawable.ic_nfc_indicator_small,
                    stringId = R.string.nc_export_via_nfc
                ),
                SheetOption(
                    type = SheetOptionType.EXPORT_COLDCARD_VIA_FILE,
                    resId = R.drawable.ic_export,
                    stringId = R.string.nc_export_via_file_advance
                )
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    fun showExportQRTypeOption() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_select_qr_type),
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_QR_BC_UR2_LEGACY,
                    stringId = R.string.nc_bc_ur2_legacy,
                    subStringId = R.string.nc_bc_ur2_legacy_desc
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_QR_BC_UR2,
                    stringId = R.string.nc_bc_ur2
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    fun openDynamicQRScreen(walletId: String, qrCodeType: Int = ExportWalletQRCodeType.BC_UR2_LEGACY) {
        navigator.openDynamicQRScreen(this, launcher, walletId, qrCodeType)
    }

    protected fun showError(event: UploadConfigurationEvent.ShowError) {
        NCToastMessage(this).showError(event.message)
    }

    private fun handleColdcardExportToNfc() {
        startNfcFlow(REQUEST_EXPORT_WALLET_TO_MK4)
    }

    private fun handleColdcardExportToFile() {
        sharedViewModel.handleColdcardExportToFile()
    }
}