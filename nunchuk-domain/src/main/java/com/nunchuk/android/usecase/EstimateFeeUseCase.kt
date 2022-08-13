package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

const val CONF_TARGET_PRIORITY = 2
const val CONF_TARGET_STANDARD = 6
const val CONF_TARGET_ECONOMICAL = 144

class EstimateFeeUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: TransactionRepository,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<Unit, EstimateFeeRates>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): EstimateFeeRates {
        return try {
            repository.getFees()
        } catch (e: Exception) {
            Timber.e(e)
            EstimateFeeRates(
                priorityRate = nativeSdk.estimateFee(CONF_TARGET_PRIORITY).value.toInt(),
                standardRate = nativeSdk.estimateFee(CONF_TARGET_STANDARD).value.toInt(),
                economicRate = nativeSdk.estimateFee(CONF_TARGET_ECONOMICAL).value.toInt()
            )
        }
    }
}