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

package com.nunchuk.android.core.nfc

import android.app.Dialog
import android.content.Intent
import android.nfc.NdefRecord
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseShareSaveFileActivity
import com.nunchuk.android.core.util.isValidCvc
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NUMBER_TYPE
import kotlinx.coroutines.launch

abstract class BaseNfcActivity<Binding : ViewBinding> : BaseShareSaveFileActivity<Binding>(), NfcActionListener {
    protected val nfcViewModel: NfcViewModel by viewModels()
    private var requestCode: Int = 0
    private var description: String = ""

    private val nfcDiscovery: NfcDiscoveryDelegate by lazy(LazyThreadSafetyMode.NONE) {
        NfcDiscoveryDelegate(this, ::onNfcTagDiscovered)
    }

    private val nfcUnsupportedDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        NCInfoDialog(this).init(
            title = getString(R.string.nc_no_nfc_title),
            message = getString(R.string.no_nfc_message)
        )
    }

    private val requestEnableNfc =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (nfcDiscovery.isEnabled) {
                if (shouldShowInputCvcFirst(requestCode)) {
                    showInputCvcDialog(descMessage = description)
                } else {
                    askToScan()
                }
            }
        }

    private val askScanNfcDialog: NfcScanDialog by lazy(LazyThreadSafetyMode.NONE) {
        NfcScanDialog(this).apply {
            // Re-arming here would drop the tag this dismissal was triggered by.
            setOnDismissListener { nfcDiscovery.updateRequestCode(0) }
        }
    }

    private var mk4HintOverride: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            requestCode = savedInstanceState.getInt(EXTRA_REQUEST_NFC_CODE, 0)
            description = savedInstanceState.getString(EXTRA_REQUEST_NFC_DESCRIPTION, "")
        }
        observer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_REQUEST_NFC_CODE, requestCode)
        outState.putString(EXTRA_REQUEST_NFC_DESCRIPTION, description)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        askScanNfcDialog.dismiss()
        super.onDestroy()
    }

    private fun observer() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                nfcViewModel.event.collect {
                    when (it) {
                        NfcState.WrongCvc -> handleWrongCvc()
                        NfcState.LimitCvcInput -> handleLimitCvcInput()
                    }
                    nfcViewModel.clearEvent()
                }
            }
        }
    }

    private fun handleLimitCvcInput() {
        if (shouldShowInputCvcFirst(requestCode)) {
            showInputCvcDialog(
                errorMessage = getString(R.string.nc_incorrect_cvc_please_try_again),
                descMessage = getString(R.string.nc_cvc_incorrect_3_times)
            )
        } else {
            NCInfoDialog(this).showDialog(message = getString(R.string.nc_cvc_incorrect_3_times))
        }
    }

    private fun handleWrongCvc() {
        if (shouldShowInputCvcFirst(requestCode)) {
            showInputCvcDialog(errorMessage = getString(R.string.nc_incorrect_cvc_please_try_again))
        }
    }

    override fun onResume() {
        super.onResume()
        // Armed with 0 when idle, so tags are not dispatched to other apps.
        nfcDiscovery.arm(if (askScanNfcDialog.isShowing) requestCode else 0)
    }

    override fun onPause() {
        nfcDiscovery.disarm()
        super.onPause()
    }

    override fun startNfcFlow(requestCode: Int, description: String) {
        this.requestCode = requestCode
        this.description = description
        if (!nfcDiscovery.isSupported) {
            nfcUnsupportedDialog.show()
            return
        }
        if (nfcDiscovery.isEnabled) {
            if (shouldShowInputCvcFirst(requestCode)) {
                showInputCvcDialog(descMessage = description)
            } else {
                askToScan()
            }
        } else {
            navigateTurnOnNfc()
        }
    }

    private fun askToScan() {
        // Not in an onShow listener: that would not fire when the dialog is already visible.
        nfcDiscovery.arm(requestCode)
        if (isMk4Request(requestCode)) {
            val hint = mk4HintOverride ?: getMk4Hint(this, requestCode)
            askScanNfcDialog.update(
                message = getString(R.string.nc_hold_device_near_the_coldcard),
                hint = hint
            )
        } else {
            askScanNfcDialog.update(message = getString(R.string.nc_hold_your_device_near_the_nfc))
        }
        askScanNfcDialog.show()
    }

    private fun navigateTurnOnNfc() {
        requestEnableNfc.launch(Intent(this, TurnOnNfcActivity::class.java))
    }

    private fun onNfcTagDiscovered(requestCode: Int, tag: Tag, records: List<NdefRecord>) {
        askScanNfcDialog.dismiss()
        nfcViewModel.updateNfcScanInfo(requestCode, tag, records)
    }

    private fun showInputCvcDialog(errorMessage: String? = null, descMessage: String? = null) {
        NCInputDialog(this)
            .showDialog(
                title = "Enter PIN",
                onConfirmed = { cvc ->
                    if (cvc.isValidCvc()) {
                        nfcViewModel.updateInputCvc(cvc)
                        askToScan()
                    } else {
                        showInputCvcDialog(errorMessage = getString(R.string.nc_required_minimum_6_characters))
                    }
                },
                isMaskedInput = true,
                errorMessage = errorMessage,
                descMessage = descMessage,
                inputType = NUMBER_TYPE
            ).show()
    }

    protected fun isNfcIntent(intent: Intent) = isNfcTagIntent(intent)

    fun setMk4HintOverride(hint: String?) {
        mk4HintOverride = hint
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcDiscovery.handleIntent(intent)
    }

    companion object {
        const val EXTRA_REQUEST_NFC_CODE = "EXTRA_REQUEST_NFC_CODE"
        private const val EXTRA_REQUEST_NFC_DESCRIPTION = "EXTRA_REQUEST_NFC_DESCRIPTION"

        // NFC
        const val REQUEST_NFC_STATUS = 1
        const val REQUEST_NFC_CHANGE_CVC = 2
        const val REQUEST_NFC_ADD_KEY = 3
        const val REQUEST_NFC_SIGN_TRANSACTION = 4
        const val REQUEST_NFC_VIEW_BACKUP_KEY = 5
        const val REQUEST_NFC_TOPUP_XPUBS = 6
        const val REQUEST_NFC_HEALTH_CHECK = 7

        // SATSCARD
        const val REQUEST_AUTO_CARD_STATUS = 8
        const val REQUEST_SATSCARD_SWEEP_SLOT = 9
        const val REQUEST_SATSCARD_SETUP = 10

        // Mk4
        const val REQUEST_MK4_ADD_KEY = 11
        const val REQUEST_EXPORT_WALLET_TO_MK4 = 12
        const val REQUEST_MK4_EXPORT_TRANSACTION = 13
        const val REQUEST_MK4_IMPORT_SIGNATURE = 14
        const val REQUEST_IMPORT_MULTI_WALLET_FROM_MK4 = 15
        const val REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4 = 16
        const val REQUEST_GENERATE_HEAL_CHECK_MSG = 17

        // PORTAL
        const val REQUEST_PORTAL = 18
    }
}