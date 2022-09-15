package com.nunchuk.android.signer.tapsigner.decryption

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ImportTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.utils.CrashlyticsReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class NfcDecryptionKeyViewModel @Inject constructor(
    private val importTapSignerUseCase: ImportTapSignerUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val application: Application
) : ViewModel() {
    private val _event = MutableStateFlow<NfcDecryptionKeyEvent?>(null)
    val event = _event.filterIsInstance<NfcDecryptionKeyEvent>()

    fun decryptBackUpKey(backUpFileUri: Uri, decryptionKey: String) {
        _event.value = NfcDecryptionKeyEvent.Loading
        viewModelScope.launch {
            withContext(ioDispatcher) {
                getFileFromUri(backUpFileUri, application.cacheDir)
            }?.let { file ->
                val result = importTapSignerUseCase(ImportTapSignerUseCase.Data(file.absolutePath, decryptionKey))
                runCatching { file.delete() }
                if (result.isSuccess) {
                    _event.value = NfcDecryptionKeyEvent.ImportTapSignerSuccess(result.getOrThrow())
                } else {
                    _event.value = NfcDecryptionKeyEvent.ImportTapSignerFailed(result.exceptionOrNull())
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun getFileFromUri(uri: Uri, directory: File) = try {
        val file = File.createTempFile("NCsuffix", ".prefixNC", directory)
        file.outputStream().use {
            application.contentResolver.openInputStream(uri)?.copyTo(it)
        }
        file
    } catch (t: Throwable) {
        CrashlyticsReporter.recordException(t)
        null
    }
}

sealed class NfcDecryptionKeyEvent {
    object Loading : NfcDecryptionKeyEvent()
    class ImportTapSignerSuccess(val masterSigner: MasterSigner): NfcDecryptionKeyEvent()
    class ImportTapSignerFailed(val e: Throwable?): NfcDecryptionKeyEvent()
}