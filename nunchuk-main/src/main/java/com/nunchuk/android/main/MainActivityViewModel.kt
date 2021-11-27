package com.nunchuk.android.main

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.AddBlockChainConnectionListenerUseCase
import com.nunchuk.android.core.domain.GetPriceConvertBTCUseCase
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCase
import com.nunchuk.android.core.util.BLOCKCHAIN_STATUS
import com.nunchuk.android.core.util.BTC_USD_EXCHANGE_RATE
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsState
import com.nunchuk.android.model.ConnectionStatusExecutor
import com.nunchuk.android.model.ConnectionStatusHelper
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class MainActivityViewModel @Inject constructor(
    private val getPriceConvertBTCUseCase: GetPriceConvertBTCUseCase,
    private val scheduleGetPriceConvertBTCUseCase: ScheduleGetPriceConvertBTCUseCase,
    private val addBlockChainConnectionListenerUseCase: AddBlockChainConnectionListenerUseCase
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    override val initialState = WalletsState()

    init {
        registerBlockChainConnectionStatusExecutor()
        addBlockChainConnectionListener()
    }

    private fun getBTCConvertPrice() {
        viewModelScope.launch {
            getPriceConvertBTCUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException {}
                .flowOn(Dispatchers.Main)
                .collect { btcResponse ->
                    btcResponse?.usd?.let { BTC_USD_EXCHANGE_RATE = it }
                }
        }
    }

    fun scheduleGetBTCConvertPrice() {
        viewModelScope.launch {
            scheduleGetPriceConvertBTCUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException {}
                .flowOn(Dispatchers.Main)
                .collect {
                    getBTCConvertPrice()
                }
        }
    }

    fun registerBlockChainConnectionStatusExecutor() {
        ConnectionStatusHelper.executor = object : ConnectionStatusExecutor {
            override fun execute(connectionStatus: ConnectionStatus, percent: Int) {
                BLOCKCHAIN_STATUS = connectionStatus
                event(
                    WalletsEvent.GetConnectionStatusSuccessEvent(connectionStatus)
                )
            }
        }
    }

    fun addBlockChainConnectionListener() {
        viewModelScope.launch {
            addBlockChainConnectionListenerUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect {}
        }
    }

}