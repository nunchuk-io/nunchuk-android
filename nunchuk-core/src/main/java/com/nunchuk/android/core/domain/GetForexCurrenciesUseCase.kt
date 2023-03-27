package com.nunchuk.android.core.domain

import com.nunchuk.android.core.repository.BtcPriceRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.util.SortedMap
import javax.inject.Inject

class GetForexCurrenciesUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val priceRepository: BtcPriceRepository,
) : UseCase<Unit, LinkedHashMap<String, String>>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): LinkedHashMap<String, String> {
        val mapCurrency = priceRepository.getForexCurrencies()
        val list = ArrayList(mapCurrency.entries)
        list.sortWith(compareBy { it.value })
        val sortedMap = LinkedHashMap<String, String>()
        for (entry in list) {
            sortedMap[entry.key] = entry.value
        }
        return sortedMap
    }
}