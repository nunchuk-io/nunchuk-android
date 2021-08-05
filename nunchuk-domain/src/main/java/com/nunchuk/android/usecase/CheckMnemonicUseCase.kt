package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CheckMnemonicUseCase {
    fun execute(mnemonic: String): Flow<Boolean>
}

internal class CheckMnemonicUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CheckMnemonicUseCase {

    override fun execute(mnemonic: String) = flow { emit(nativeSdk.checkMnemonic(mnemonic)) }

}