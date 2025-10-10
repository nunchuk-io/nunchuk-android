package com.nunchuk.android.core.signer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnChainAddSignerParam(
    val flags: Int = 0,
    val keyIndex: Int = -1,
    val currentSigner: SignerModel? = null,
) : Parcelable {
    
    companion object {
        const val FLAG_ADD_INHERITANCE_SIGNER = 0x01 // Binary: 0001
        const val FLAG_VERIFY_BACKUP_SEED_PHRASE = 0x02 // Binary: 0010
        const val FLAG_ADD_SIGNER = 0x04 // Binary: 0100
    }

    fun isAddInheritanceSigner(): Boolean {
        return flags == FLAG_ADD_INHERITANCE_SIGNER
    }

    fun isVerifyBackupSeedPhrase(): Boolean {
        return flags == FLAG_VERIFY_BACKUP_SEED_PHRASE
    }

    fun isAddSigner(): Boolean {
        return flags == FLAG_ADD_SIGNER
    }
}