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
import android.os.PersistableBundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseComposeShareSaveFileActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.EXTRA_REQUEST_NFC_CODE
import com.nunchuk.android.core.util.isValidCvc
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NUMBER_TYPE
import kotlinx.coroutines.launch

abstract class BaseComposeNfcActivity : BaseComposeShareSaveFileActivity(), NfcActionListener {
    protected val nfcViewModel: NfcViewModel by viewModels()
    private var requestCode: Int = 0

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
                askToScan()
            }
        }

    private val askScanNfcDialog: NfcScanDialog by lazy(LazyThreadSafetyMode.NONE) {
        NfcScanDialog(this).apply {
            // Re-arming here would drop the tag this dismissal was triggered by.
            setOnDismissListener { nfcDiscovery.updateRequestCode(0) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            requestCode = savedInstanceState.getInt(EXTRA_REQUEST_NFC_CODE, 0)
        }
        observer()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putInt(EXTRA_REQUEST_NFC_CODE, requestCode)
        super.onSaveInstanceState(outState, outPersistentState)
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
            askScanNfcDialog.update(
                message = getString(R.string.nc_hold_device_near_the_coldcard),
                hint = getMk4Hint(this, requestCode)
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
                title = getString(R.string.nc_enter_pin),
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcDiscovery.handleIntent(intent)
    }
}