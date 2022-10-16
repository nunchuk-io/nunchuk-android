package com.nunchuk.android.core.domain

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.GetPrimaryKeyListUseCase
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class ReloadPrimaryKeyInfoUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val accountManager: AccountManager,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder,
    private val getPrimaryKeyListUseCase: GetPrimaryKeyListUseCase
) : UseCase<ReloadPrimaryKeyInfoUseCase.Param, Unit>(dispatcher) {

    override suspend fun execute(parameters: Param) {
        when (parameters.loadingInfo) {
            InfoType.MASTER_SIGNER -> loadMasterSigner()
            InfoType.PRIMARY_KEY -> loadPrimaryKey()
            InfoType.ALL -> {
                supervisorScope {
                    val masterSignerDeferred = async { loadMasterSigner() }
                    val primaryKeyDeferred = async { loadPrimaryKey() }
                    masterSignerDeferred.await()
                    primaryKeyDeferred.await()
                }
            }
        }
    }

    private fun loadMasterSigner() {
        val xrp = accountManager.getPrimaryKeyInfo()?.xfp ?: return
        val signer = nunchukNativeSdk.getMasterSigner(xrp)
        primaryKeySignerInfoHolder.setSignerInfo(signer)
    }

    private suspend fun loadPrimaryKey() {
        val result = getPrimaryKeyListUseCase.invoke(Unit)
        if (result.isSuccess) {
            result.getOrNull()?.firstOrNull {
                it.masterFingerprint == accountManager.getPrimaryKeyInfo()?.xfp
            }?.also {
                primaryKeySignerInfoHolder.setPrimaryKeyInfo(it)
            }
        }
    }

    class Param(val loadingInfo: InfoType)

    enum class InfoType {
        MASTER_SIGNER, PRIMARY_KEY, ALL
    }
}