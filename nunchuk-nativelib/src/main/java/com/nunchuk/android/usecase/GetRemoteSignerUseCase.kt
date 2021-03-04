package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.LibNunchukFacade
import io.reactivex.Single
import javax.inject.Inject

interface GetRemoteSignerUseCase {
    fun execute(): Single<SingleSigner>
}

internal class GetRemoteSignerUseCaseImpl @Inject constructor(
        private val nunchukFacade: LibNunchukFacade
) : GetRemoteSignerUseCase {

    override fun execute() = Single.fromCallable(nunchukFacade::getRemoteSigner)

}