package com.nunchuk.android.core.util

import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardIdManager @Inject constructor(private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase) {

    private val tapSignerCardIds = hashMapOf<String, String>()
    private val mutex = Mutex()

    suspend fun getCardId(signerId: String): String {
        mutex.withLock {
            if (tapSignerCardIds.contains(signerId)) return tapSignerCardIds[signerId]!!
            val cardId = getTapSignerStatusByIdUseCase(signerId).getOrNull()?.ident.orEmpty()
            if (cardId.isNotEmpty()) tapSignerCardIds[signerId] = cardId
            return cardId
        }
    }
}