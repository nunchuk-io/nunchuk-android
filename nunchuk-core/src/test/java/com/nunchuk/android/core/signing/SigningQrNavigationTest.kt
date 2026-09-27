package com.nunchuk.android.core.signing

import com.nunchuk.android.share.model.SignFlowType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the flows the shared export/import QR screens were opened with before the profiles. */
class SigningQrNavigationTest {

    @Test
    fun `each framing opens the flow the claim used`() {
        assertEquals(
            SigningQrNavigation(SignFlowType.ClaimAirgapMessage),
            ExportRoute.Qr(PayloadCodecKind.SPECTER_MESSAGE, QrFraming.PLAIN).navigation(),
        )
        assertEquals(
            SigningQrNavigation(SignFlowType.ClaimDummy),
            ExportRoute.Qr(PayloadCodecKind.COLDCARD_JSON, QrFraming.BBQR_JSON).navigation(),
        )
        assertEquals(
            SigningQrNavigation(SignFlowType.NormalDummy, isBBQR = false),
            ExportRoute.Qr(PayloadCodecKind.RAW_PSBT, QrFraming.UR).navigation(),
        )
        assertEquals(
            SigningQrNavigation(SignFlowType.NormalDummy, isBBQR = true),
            ExportRoute.Qr(PayloadCodecKind.RAW_PSBT, QrFraming.BBQR).navigation(),
        )
    }

    @Test
    fun `every configured QR route resolves`() {
        SigningDevice.entries.forEach { device ->
            SigningOperation.entries.forEach { operation ->
                (device.profile().method(operation) as? SigningMethod.ExportImport)?.qrRoute?.navigation()
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a PSBT framing with a message payload is rejected`() {
        ExportRoute.Qr(PayloadCodecKind.SPECTER_MESSAGE, QrFraming.UR).navigation()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a message framing with a PSBT payload is rejected`() {
        ExportRoute.Qr(PayloadCodecKind.RAW_PSBT, QrFraming.PLAIN).navigation()
    }
}
