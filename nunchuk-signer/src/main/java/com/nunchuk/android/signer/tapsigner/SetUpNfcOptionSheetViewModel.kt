package com.nunchuk.android.signer.tapsigner

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.persistence.NcDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetUpNfcOptionSheetViewModel @Inject constructor(
    private val dataStore: NcDataStore,
    private val applicationScope: CoroutineScope
) : ViewModel() {
    suspend fun showPortal() = dataStore.getShowPortalAndSet()

    fun markShowPortal() {
        applicationScope.launch {
            dataStore.setShowPortal(false)
        }
    }
}