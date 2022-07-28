package com.nunchuk.android.usecase

import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

const val CONF_TARGET_PRIORITY = 2
const val CONF_TARGET_STANDARD = 6
const val CONF_TARGET_ECONOMICAL = 144

interface EstimateFeeUseCase {
    fun execute(): Flow<EstimateFeeRates>
}

internal class EstimateFeeUseCaseImpl @Inject constructor(
    private val repository: TransactionRepository,
    private val nativeSdk: NunchukNativeSdk
) : EstimateFeeUseCase {

    override fun execute() = flow {
        emit(
            repository.getFees()
        )
    }.catch {
        emit(
            EstimateFeeRates(
                priorityRate = nativeSdk.estimateFee(CONF_TARGET_PRIORITY).value.toInt(),
                standardRate = nativeSdk.estimateFee(CONF_TARGET_STANDARD).value.toInt(),
                economicRate = nativeSdk.estimateFee(CONF_TARGET_ECONOMICAL).value.toInt()
            )
        )
    }

}