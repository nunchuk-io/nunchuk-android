package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner

interface UpdateMasterSignerUseCase {
    fun execute(mastersigner: MasterSigner): Boolean
}