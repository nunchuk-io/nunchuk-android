package com.nunchuk.android.usecase

interface ImportHealthCheckSignatureUseCase {
    fun execute(filePath: String): String
}