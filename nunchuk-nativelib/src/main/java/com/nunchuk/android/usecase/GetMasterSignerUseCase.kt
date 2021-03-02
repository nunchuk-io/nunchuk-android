package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner

interface GetMasterSignerUseCase {
    fun execute(mastersigner_id: String): MasterSigner
}