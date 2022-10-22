package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckPassphrasePrimaryKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val signerSoftwareRepository: SignerSoftwareRepository
) : UseCase<CheckPassphrasePrimaryKeyUseCase.Param, CheckPassphrasePrimaryKeyUseCase.Result?>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): Result? {
        val address =
            nunchukNativeSdk.getPrimaryKeyAddress(parameters.mnemonic, parameters.passphrase)
        if (address.isNullOrBlank()) return null
        val response = signerSoftwareRepository.pKeyUserInfo(
            address = address
        )
        return Result(username = response.username.orEmpty(), address = address)
    }

    class Param(val mnemonic: String, val passphrase: String)

    class Result(val username: String, val address: String)
}