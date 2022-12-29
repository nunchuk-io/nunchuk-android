package com.nunchuk.android.core.manager

import com.nunchuk.android.core.data.api.UserWalletsApi
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.network.util.TEST_NET_USER_WALLET_API
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class UserWalletApiManager @Inject constructor(
    private val userWalletsApi: UserWalletsApi,
    @Named(TEST_NET_USER_WALLET_API)
    private val testNetUserWalletsApi: UserWalletsApi,
    applicationScope: CoroutineScope,
    ncDataStore: NcDataStore,
) {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    val walletApi: UserWalletsApi
        get() = if (chain.value == Chain.MAIN) userWalletsApi else testNetUserWalletsApi
}