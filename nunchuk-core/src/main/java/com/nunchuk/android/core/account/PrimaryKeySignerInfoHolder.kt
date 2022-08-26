package com.nunchuk.android.core.account

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetPrimaryKeyListUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrimaryKeySignerInfoHolder @Inject constructor(
    private val accountManager: AccountManager,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val getPrimaryKeyListUseCase: GetPrimaryKeyListUseCase
) {

    private var masterSigner: MasterSigner? = null
    private var primaryKey: PrimaryKey? = null
    private val signerMutex = Mutex()
    private val primaryKeyMutex = Mutex()

    fun setSignerInfo(masterSigner: MasterSigner) {
        this.masterSigner = masterSigner
    }

    suspend fun getSignerInfo(): MasterSigner? {
        signerMutex.withLock {
            if (masterSigner != null) return masterSigner
            val signerId = accountManager.getPrimaryKeyInfo()?.xfp ?: return null
            val result =
                getMasterSignerUseCase.execute(signerId)
            if (result is Result.Success) {
                masterSigner = result.data
            }
        }
        return masterSigner
    }

    fun setPrimaryKeyInfo(primaryKey: PrimaryKey) {
        this.primaryKey = primaryKey
    }

    suspend fun getPrimaryKeyInfo(): PrimaryKey? {
        primaryKeyMutex.withLock {
            if (primaryKey != null) return primaryKey
            val result = getPrimaryKeyListUseCase.invoke(Unit)
            if (result.isSuccess) {
                result.getOrNull()?.firstOrNull {
                    it.masterFingerprint == accountManager.getPrimaryKeyInfo()?.xfp
                }?.also {
                    this.primaryKey = it
                }
            }
        }
        return primaryKey
    }

    suspend fun isNeedPassphraseSent(): Boolean {
        val masterSigner = getSignerInfo()
        return masterSigner?.let {
            it.software && it.device.needPassPhraseSent
        } == true
    }

    fun clear() {
        masterSigner = null
        primaryKey = null
    }
}