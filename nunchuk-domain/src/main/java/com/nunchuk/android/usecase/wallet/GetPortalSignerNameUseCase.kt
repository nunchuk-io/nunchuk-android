package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetPortalSignerNameUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<Unit, String>(ioDispatcher) {
    override suspend fun execute(parameters: Unit): String {
        val remoteSigners = nativeSdk.getRemoteSigners()
        val portalSignerNames = remoteSigners
            .filter { it.type == SignerType.PORTAL_NFC }
            .map { it.name }.toSet()
        return generateSequence(1) { it + 1 }
            .map { "Portal" + if (it == 1) "" else " #$it" }
            .first { it !in portalSignerNames }
    }
}