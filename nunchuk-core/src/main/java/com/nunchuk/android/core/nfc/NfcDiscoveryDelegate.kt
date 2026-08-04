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

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.EXTRA_REQUEST_NFC_CODE
import com.nunchuk.android.utils.PendingIntentUtils
import com.nunchuk.android.utils.parcelable
import timber.log.Timber

/**
 * Owns NFC tag discovery, picking the mechanism per request code. Stays armed while the host
 * activity is resumed, even with no scan pending (request code 0), so tags are not dispatched to
 * other apps.
 *
 * CKTAP cards (TAPSIGNER / SATSCARD) use reader mode: foreground dispatch makes the platform
 * activate the tag, probe it for NDEF and deactivate it before handing it back, which on
 * Motorola/MediaTek stacks leaves the bare ISO-DEP applet deselected and the handle dead by the
 * time [IsoDep.connect] runs. Reader mode also allows raising the presence-check interval, which
 * otherwise interrupts multi-second derive/sign operations.
 *
 * Everything else must stay on foreground dispatch. The Coldcard Mk4 is an ST25DV (ISO 15693 /
 * NFC-V) and reader mode's NDEF detection fails on it -- the tag arrives as `[NfcV, NdefFormatable]`
 * rather than `[NfcV, Ndef]`, so `Ndef.get(tag)` returns null and every Mk4 flow breaks. Portal
 * stays too: it holds a tag across a long session, and reader mode is torn down on every pause.
 */
class NfcDiscoveryDelegate(
    private val activity: Activity,
    private val onTagDiscovered: (requestCode: Int, tag: Tag, records: List<NdefRecord>) -> Unit,
) {
    private val nfcAdapter: NfcAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        NfcAdapter.getDefaultAdapter(activity)
    }

    private val readerCallback = NfcAdapter.ReaderCallback(::handleReaderModeTag)

    /** Reader mode only; the dispatch path carries its code in the PendingIntent. */
    @Volatile
    private var readerModeRequestCode: Int = 0

    private var isReaderModeActive = false

    val isSupported: Boolean
        get() = nfcAdapter != null

    val isEnabled: Boolean
        get() = nfcAdapter?.isEnabled == true

    /**
     * (Re)arms discovery for [requestCode], switching mechanism if needed. Restarts RF discovery, so
     * it must not be called while a tag already handed to [onTagDiscovered] is still in use -- call
     * [updateRequestCode] instead.
     */
    fun arm(requestCode: Int) {
        readerModeRequestCode = requestCode
        val adapter = nfcAdapter ?: return
        if (usesReaderMode(requestCode)) {
            if (!isReaderModeActive) runCatching { adapter.disableForegroundDispatch(activity) }
            isReaderModeActive = true
            Timber.d("arm reader mode: requestCode=$requestCode")
            runCatching { adapter.enableReaderMode(activity, readerCallback, READER_FLAGS, readerExtras()) }
                .onFailure { Timber.e(it, "enableReaderMode failed") }
        } else {
            if (isReaderModeActive) runCatching { adapter.disableReaderMode(activity) }
            isReaderModeActive = false
            Timber.d("arm dispatch: requestCode=$requestCode")
            enableForegroundDispatch(adapter, requestCode)
        }
    }

    /** Swaps the pending request code without restarting RF discovery. */
    fun updateRequestCode(requestCode: Int) {
        readerModeRequestCode = requestCode
        if (isReaderModeActive) return
        // Re-registering foreground dispatch leaves the RF state alone. Throws while the activity is
        // paused, in which case onResume re-arms anyway.
        val adapter = nfcAdapter ?: return
        enableForegroundDispatch(adapter, requestCode)
    }

    fun disarm() {
        readerModeRequestCode = 0
        val adapter = nfcAdapter ?: return
        runCatching { adapter.disableReaderMode(activity) }
        runCatching { adapter.disableForegroundDispatch(activity) }
        isReaderModeActive = false
    }

    fun handleIntent(intent: Intent) {
        if (!isNfcTagIntent(intent)) return
        val tag = intent.parcelable(NfcAdapter.EXTRA_TAG) as? Tag ?: return
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_NFC_CODE, 0)
        Timber.d("dispatch tag: requestCode=$requestCode, tech=[${tag.techNames()}]")
        if (requestCode == 0) return
        onTagDiscovered(requestCode, tag, intent.ndefRecords())
    }

    private fun handleReaderModeTag(tag: Tag) {
        val requestCode = readerModeRequestCode
        Timber.d("reader tag: requestCode=$requestCode, tech=[${tag.techNames()}]")
        if (requestCode == 0) return
        if (IsoDep.get(tag) == null) {
            // The CKTAP flows bail out silently on a null IsoDep, which reads as "nothing happened".
            Timber.e("no IsoDep: requestCode=$requestCode, tech=[${tag.techNames()}]")
        }
        // ReaderCallback is invoked on a binder thread.
        activity.runOnUiThread { onTagDiscovered(requestCode, tag, emptyList()) }
    }

    private fun enableForegroundDispatch(adapter: NfcAdapter, requestCode: Int) {
        runCatching {
            adapter.enableForegroundDispatch(activity, pendingIntentFor(requestCode), null, null)
        }
    }

    private fun pendingIntentFor(requestCode: Int) = PendingIntent.getActivity(
        activity,
        0,
        Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).apply {
            putExtra(EXTRA_REQUEST_NFC_CODE, requestCode)
        },
        PendingIntentUtils.getFlagCompat()
    )

    private fun readerExtras() = Bundle().apply {
        putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY_MS)
    }

    private fun Tag.techNames() = techList.joinToString { it.substringAfterLast('.') }

    private fun Intent.ndefRecords(): List<NdefRecord> {
        val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }
        return messages.orEmpty()
            .filterIsInstance<NdefMessage>()
            .flatMap { it.records.toList() }
    }

    companion object {
        /**
         * Polls every technology the platform polls by default. Narrowing this is a trap: assuming
         * every card was NFC-A is what made the NFC-V Coldcard undetectable. SKIP_NDEF_CHECK is safe
         * because only CKTAP uses reader mode, and it is raw ISO-DEP with no NDEF.
         */
        private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        /** The platform default (125ms) interrupts the multi-second TAPSIGNER derive/sign. */
        private const val PRESENCE_CHECK_DELAY_MS = 1000
    }
}
