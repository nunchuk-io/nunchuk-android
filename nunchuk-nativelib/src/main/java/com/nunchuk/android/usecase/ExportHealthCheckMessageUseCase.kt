package com.nunchuk.android.usecase

interface ExportHealthCheckMessageUseCase {
    fun execute(message: String, filePath: String): Boolean
}