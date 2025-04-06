package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.wallet.WalletOrder
import com.nunchuk.android.repository.SettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWalletOrderListUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<WalletOrder>>(ioDispatcher) {
    override fun execute(parameters: Unit): Flow<List<WalletOrder>> {
        return settingRepository.getAllWalletOrders()
    }
}
