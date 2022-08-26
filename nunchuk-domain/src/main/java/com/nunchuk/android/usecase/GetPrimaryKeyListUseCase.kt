package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.Chain
import com.nunchuk.android.util.FileHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class GetPrimaryKeyListUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val filerHelper: FileHelper
) : UseCase<Unit, List<PrimaryKey>>(dispatcher) {

    override suspend fun execute(parameters: Unit): List<PrimaryKey> = supervisorScope {
        val primaryKeys = mutableListOf<PrimaryKey>()
        val storagePath = filerHelper.getOrCreateNunchukRootDir()
        val mainResultDeferred = async { nunchukNativeSdk.getPrimaryKeys(chain = Chain.MAIN.ordinal, storagePath = storagePath).map { it.copy(chain = Chain.MAIN) } }
        val testnetResultDeferred = async { nunchukNativeSdk.getPrimaryKeys(chain = Chain.TESTNET.ordinal, storagePath = storagePath).map { it.copy(chain = Chain.TESTNET) } }
        val signetResultDeferred = async { nunchukNativeSdk.getPrimaryKeys(chain = Chain.SIGNET.ordinal, storagePath = storagePath).map { it.copy(chain = Chain.SIGNET) } }
        val mainResult = kotlin.runCatching { mainResultDeferred.await() }
        val testnetResult = kotlin.runCatching { testnetResultDeferred.await() }
        val signetResult = kotlin.runCatching { signetResultDeferred.await()}
        primaryKeys.addAll(mainResult.getOrDefault(emptyList()))
        primaryKeys.addAll(testnetResult.getOrDefault(emptyList()))
        primaryKeys.addAll(signetResult.getOrDefault(emptyList()))
        return@supervisorScope primaryKeys
    }
}