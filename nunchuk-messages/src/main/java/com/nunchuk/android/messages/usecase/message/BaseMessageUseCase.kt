package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.model.SessionLostException
import org.matrix.android.sdk.api.session.Session

abstract class BaseMessageUseCase(private val sessionHolder: SessionHolder) {

    protected val session: Session
        get() = sessionHolder.getSafeActiveSession() ?: throw SessionLostException()

}