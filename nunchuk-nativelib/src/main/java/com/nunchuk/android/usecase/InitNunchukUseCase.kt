package com.nunchuk.android.usecase

import android.util.Log
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.nativelib.LibNunchukFacade
import io.reactivex.Completable
import javax.inject.Inject

interface InitNunchukUseCase {
    fun execute(appSettings: AppSettings): Completable
}

internal class InitNunchukUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : InitNunchukUseCase {

    override fun execute(appSettings: AppSettings) = Completable.fromCallable {
        Log.i(TAG, "initNunchuk($appSettings)")
        nunchukFacade.initNunchuk(appSettings)
    }

    companion object {
        const val TAG = "InitNunchukUseCase"
    }
}