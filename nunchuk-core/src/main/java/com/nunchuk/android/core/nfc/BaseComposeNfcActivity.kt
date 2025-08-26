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
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
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
import com.nunchuk.android.utils.PendingIntentUtils
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NUMBER_TYPE
import kotlinx.coroutines.launch

abstract class BaseComposeNfcActivity : BaseComposeShareSaveFileActivity(), NfcActionListener {
    protected val nfcViewModel: NfcViewModel by viewModels()
    private var requestCode: Int = 0

    private val nfcAdapter: NfcAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        NfcAdapter.getDefaultAdapter(this)
    }

    private val nfcUnsupportedDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        NCInfoDialog(this).init(
            title = getString(R.string.nc_no_nfc_title),
            message = getString(R.string.no_nfc_message)
        )
    }

    private val requestEnableNfc =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (nfcAdapter?.isEnabled == true) {
                askToScan()
            }
        }

    private val askScanNfcDialog: NfcScanDialog by lazy(LazyThreadSafetyMode.NONE) {
        NfcScanDialog(this).apply {
            setOnShowListener {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    nfcAdapter?.enableForegroundDispatch(
                        this@BaseComposeNfcActivity,
                        getNfcPendingIntent(requestCode),
                        null,
                        null
                    )
                }
            }

            setOnDismissListener {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    nfcAdapter?.enableForegroundDispatch(
                        this@BaseComposeNfcActivity,
                        getNfcPendingIntent(0),
                        null,
                        null
                    )
                }
            }
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
        val requestCode = if (askScanNfcDialog.isShowing) requestCode else 0
        nfcAdapter?.enableForegroundDispatch(this, getNfcPendingIntent(requestCode), null, null)
    }

    override fun onPause() {
        nfcAdapter?.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun startNfcFlow(requestCode: Int, description: String) {
        this.requestCode = requestCode
        nfcAdapter?.let {
            if (it.isEnabled) {
                if (shouldShowInputCvcFirst(requestCode)) {
                    showInputCvcDialog(descMessage = description)
                } else {
                    askToScan()
                }
            } else {
                navigateTurnOnNfc()
            }
        } ?: run {
            nfcUnsupportedDialog.show()
        }
    }

    private fun askToScan() {
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

    private fun getNfcPendingIntent(requestCode: Int) = PendingIntent.getActivity(
        this,
        0,
        Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).apply {
            putExtra(EXTRA_REQUEST_NFC_CODE, requestCode)
        },
        PendingIntentUtils.getFlagCompat()
    )

    private fun processNfcIntent(intent: Intent) {
        if (isNfcIntent(intent)) {
            askScanNfcDialog.dismiss()
            nfcViewModel.updateNfcScanInfo(intent)
        }
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

    protected fun isNfcIntent(intent: Intent) =
        NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action || NfcAdapter.ACTION_TAG_DISCOVERED == intent.action

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processNfcIntent(intent ?: return)
    }
}