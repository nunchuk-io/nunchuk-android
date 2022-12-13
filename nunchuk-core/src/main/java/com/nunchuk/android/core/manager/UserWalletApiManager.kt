package com.nunchuk.android.core.manager

import com.google.gson.Gson
import com.nunchuk.android.core.data.api.UserWalletsApi
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.network.util.TEST_NET_USER_WALLET_API
import com.nunchuk.android.type.Chain
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class UserWalletApiManager @Inject constructor(
    private val userWalletsApi: UserWalletsApi,
    @Named(TEST_NET_USER_WALLET_API)
    private val testNetUserWalletsApi: UserWalletsApi,
    private val gson: Gson,
    private val ncSharePreferences: NCSharePreferences,
) {
    private val chain by lazy {
        gson.fromJson(
            ncSharePreferences.appSettings,
            AppSettings::class.java
        )?.chain ?: Chain.MAIN
    }

    val walletApi: UserWalletsApi
        get() = if (chain == Chain.MAIN) userWalletsApi else testNetUserWalletsApi
}