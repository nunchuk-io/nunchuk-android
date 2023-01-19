package com.nunchuk.android.messages.usecase.media

import android.app.Application
import android.net.Uri
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.messages.components.detail.media.getSelectedMediaFiles
import com.nunchuk.android.messages.components.detail.media.toContentAttachmentData
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

class SendMediaUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val application: Application,
) : UseCase<SendMediaUseCase.Data, Unit>(dispatcher) {

    override suspend fun execute(parameters: Data) {
        parameters.room.sendService().sendMedias(
            parameters.content.getSelectedMediaFiles(application).map { content -> content.toContentAttachmentData() },
            false,
            emptySet(),
        )
    }

    data class Data(
        val room: Room,
        val content: List<Uri>,
    )
}