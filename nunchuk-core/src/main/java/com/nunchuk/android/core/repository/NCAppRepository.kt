package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.NCAppApi
import com.nunchuk.android.core.data.model.AppUpdateResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface NCAppRepository {
    fun checkAppUpdate(): Flow<AppUpdateResponse>
}

internal class NCAppRepositoryImpl @Inject constructor(
    private val ncAppApi: NCAppApi
) : NCAppRepository {

    override fun checkAppUpdate() = flow {
        emit(ncAppApi.checkAppUpdate().data)
    }
}