package com.nunchuk.android.signer

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import com.nunchuk.android.signer.model.NfcScanInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor(

) : ViewModel() {
    private val _nfcScanInfo = MutableStateFlow<NfcScanInfo?>(null)
    val nfcScanInfo = _nfcScanInfo.filterIsInstance<NfcScanInfo>()

    fun updateNfcScanInfo(requestCode: Int, tag: Tag) {
        _nfcScanInfo.value = NfcScanInfo(requestCode, tag)
    }

    fun clearScanInfo() {
        _nfcScanInfo.value = null
    }
}