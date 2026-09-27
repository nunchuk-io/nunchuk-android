package com.nunchuk.android.core.signing

import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.signer.GenerateColdCardHealthCheckMessageStringUseCase
import com.nunchuk.android.usecase.signer.GenerateKruxMessageSigningUseCase
import com.nunchuk.android.usecase.signer.GenerateMessageSigningQrUseCase
import com.nunchuk.android.usecase.signer.GeneratePassportMessageSigningUseCase
import javax.inject.Inject

/** Builds payloads only; framing, file names and UI remain in the route/host. */
class SigningPayloadCodec @Inject constructor(
    private val specter: GenerateMessageSigningQrUseCase,
    private val coldcard: GenerateColdCardHealthCheckMessageStringUseCase,
    private val passport: GeneratePassportMessageSigningUseCase,
    private val krux: GenerateKruxMessageSigningUseCase,
) {
    suspend fun build(kind: PayloadCodecKind, path: String, payload: String): Result<String> = when (kind) {
        PayloadCodecKind.SPECTER_MESSAGE -> specter(GenerateMessageSigningQrUseCase.Param(path, payload))
        PayloadCodecKind.COLDCARD_JSON -> coldcard(
            GenerateColdCardHealthCheckMessageStringUseCase.Param(path, payload, AddressType.LEGACY)
        )
        PayloadCodecKind.PASSPORT_TEXT -> passport(GeneratePassportMessageSigningUseCase.Param(path, payload))
        PayloadCodecKind.KRUX_TEXT -> krux(GenerateKruxMessageSigningUseCase.Param(path, payload))
        PayloadCodecKind.RAW_PSBT -> Result.success(payload)
    }
}
