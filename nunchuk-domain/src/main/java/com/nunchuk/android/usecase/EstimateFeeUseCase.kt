package com.nunchuk.android.usecase

import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

const val CONF_TARGET_PRIORITY = 2
const val CONF_TARGET_STANDARD = 6
const val CONF_TARGET_ECONOMICAL = 144

interface EstimateFeeUseCase {
    fun execute(): Flow<EstimateFeeRates>
}

internal class EstimateFeeUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : EstimateFeeUseCase {

    override fun execute() = flow {
        emit(
            EstimateFeeRates(
                priorityRate = nativeSdk.estimateFee(CONF_TARGET_PRIORITY),
                standardRate = nativeSdk.estimateFee(CONF_TARGET_STANDARD),
                economicRate = nativeSdk.estimateFee(CONF_TARGET_ECONOMICAL)
            )
        )
    }

}