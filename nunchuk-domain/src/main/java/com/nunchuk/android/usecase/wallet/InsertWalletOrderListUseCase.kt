package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.wallet.WalletOrder
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class InsertWalletOrderListUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<InsertWalletOrderListUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        settingRepository.insertWalletOrders(parameters.orders)
    }

    data class Params(val orders: List<WalletOrder>)
}
