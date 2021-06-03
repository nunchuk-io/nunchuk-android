package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetBip39WordListUseCase {
    suspend fun execute(): Result<List<String>>
}

internal class GetBip39WordListUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetBip39WordListUseCase {

    override suspend fun execute() = exe { nativeSdk.getBip39WordList() }

}
