package com.nunchuk.android.usecase

interface RescanBlockchainUseCase {
    fun execute(startHeight: Int, stopHeight: Int = -1)
}