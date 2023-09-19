/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nunchuk.android.domain.di.MainDispatcher
import com.nunchuk.android.repository.NetworkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NetworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val applicationScope: CoroutineScope,
) : NetworkRepository {
    private val connectivityManager = getSystemService(context, ConnectivityManager::class.java)

    private val monitorConnectivity = callbackFlow {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(isConnected())
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Timber.d("onAvailable")
                trySend(isConnected())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Timber.d("onCapabilitiesChanged")
                trySend(isConnected())
            }
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                connectivityManager?.registerNetworkCallback(networkRequest, callback)
                connectivityManager?.registerDefaultNetworkCallback(callback)
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }.flowOn(mainDispatcher)
        .debounce(150L)
        .catch { Timber.e(it, "MonitorConnectivity Exception") }
        .stateIn(
            applicationScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = isConnected()
        )

    @Suppress("DEPRECATION")
    override fun isConnected(): Boolean {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return cm?.activeNetworkInfo?.isConnected == true
    }

    override fun networkStatusFlow(): Flow<Boolean> = monitorConnectivity
}