package com.nunchuk.android.signer.mk4.inheritance.backup.encrypted

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.domain.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ColdCardImportEncryptedBackUpViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val application: Application,
): ViewModel(){

    private val _event = MutableSharedFlow<ColdCardImportEncryptedBackUpEvent>()
    val event = _event.asSharedFlow()

    fun getBackUpFilePath(uri: Uri) {
        viewModelScope.launch {
            val filePath = withContext(ioDispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)?.absolutePath
            } ?: return@launch
            _event.emit(ColdCardImportEncryptedBackUpEvent.Success(filePath))
        }
    }
}

sealed class ColdCardImportEncryptedBackUpEvent {
    data class Success(val filePath: String) : ColdCardImportEncryptedBackUpEvent()
}