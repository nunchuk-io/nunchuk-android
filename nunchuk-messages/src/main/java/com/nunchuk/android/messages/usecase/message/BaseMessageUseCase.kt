package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.model.SessionLostException
import com.nunchuk.android.usecase.BaseUseCase
import org.matrix.android.sdk.api.session.Session

abstract class BaseMessageUseCase : BaseUseCase() {

    protected val session: Session
        get() = SessionHolder.currentSession ?: throw SessionLostException()
}