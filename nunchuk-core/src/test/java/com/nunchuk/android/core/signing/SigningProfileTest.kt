package com.nunchuk.android.core.signing

import com.nunchuk.android.core.signing.SigningTransport.FILE
import com.nunchuk.android.core.signing.SigningTransport.NFC
import com.nunchuk.android.core.signing.SigningTransport.QR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the claim's signing sheets as they were before the profiles existed: which routes each
 * device offers, in which order, and what each route carries.
 */
class SigningProfileTest {

    @Test
    fun `message signing sheet routes, in order`() {
        assertRoutes(SigningDevice.COLDCARD, SigningOperation.MESSAGE, listOf(FILE, QR, NFC), listOf(FILE, QR, NFC))
        assertRoutes(SigningDevice.KRUX, SigningOperation.MESSAGE, listOf(FILE, QR), listOf(FILE, QR))
        assertRoutes(SigningDevice.PASSPORT, SigningOperation.MESSAGE, listOf(FILE), listOf(FILE))
        listOf(SigningDevice.JADE, SigningDevice.KEYSTONE, SigningDevice.SEEDSIGNER).forEach {
            assertRoutes(it, SigningOperation.MESSAGE, listOf(QR), listOf(QR))
        }
    }

    @Test
    fun `PSBT signing sheet routes, in order`() {
        assertRoutes(SigningDevice.COLDCARD, SigningOperation.PSBT, listOf(FILE, QR, NFC), listOf(FILE, QR, NFC))
        listOf(SigningDevice.PASSPORT, SigningDevice.KRUX).forEach {
            assertRoutes(it, SigningOperation.PSBT, listOf(FILE, QR), listOf(FILE, QR))
        }
        listOf(SigningDevice.JADE, SigningDevice.KEYSTONE, SigningDevice.SEEDSIGNER).forEach {
            assertRoutes(it, SigningOperation.PSBT, listOf(QR), listOf(QR))
        }
    }

    @Test
    fun `export completed screen routes and copy, per device`() {
        assertAfterFileExport(SigningDevice.COLDCARD, listOf(FILE), FileSigningInstructions.COLDCARD)
        assertAfterFileExport(SigningDevice.PASSPORT, listOf(FILE), FileSigningInstructions.PASSPORT)
        assertAfterFileExport(SigningDevice.KRUX, listOf(QR, FILE), FileSigningInstructions.KRUX)
    }

    @Test
    fun `saving the claiming PSBT ends the export`() {
        exportImportDevices(SigningOperation.PSBT).forEach { device ->
            val method = exportImport(device, SigningOperation.PSBT)
            assertNull("$device", method.fileRoute?.afterExport)
            assertEquals("$device", emptyList<SigningTransport>(), method.importRoutes(RouteContext.AFTER_FILE_EXPORT))
        }
    }

    @Test
    fun `message request formats, file names and QR framing`() {
        assertFile(SigningDevice.COLDCARD, PayloadCodecKind.COLDCARD_JSON, "coldcard_message.txt")
        assertQr(SigningDevice.COLDCARD, SigningOperation.MESSAGE, PayloadCodecKind.COLDCARD_JSON, QrFraming.BBQR_JSON)
        assertEquals(PayloadCodecKind.COLDCARD_JSON, exportImport(SigningDevice.COLDCARD, SigningOperation.MESSAGE).nfcRoute?.codec)

        assertFile(SigningDevice.PASSPORT, PayloadCodecKind.PASSPORT_TEXT, "passport_message.txt")

        // Krux: its own text format as a file, the Specter request as a QR.
        assertFile(SigningDevice.KRUX, PayloadCodecKind.KRUX_TEXT, "krux_message.txt")
        assertQr(SigningDevice.KRUX, SigningOperation.MESSAGE, PayloadCodecKind.SPECTER_MESSAGE, QrFraming.PLAIN)

        listOf(SigningDevice.JADE, SigningDevice.KEYSTONE, SigningDevice.SEEDSIGNER).forEach {
            assertQr(it, SigningOperation.MESSAGE, PayloadCodecKind.SPECTER_MESSAGE, QrFraming.PLAIN)
        }
    }

    @Test
    fun `PSBT goes out raw, over BBQR for a Coldcard and UR for everyone else`() {
        assertQr(SigningDevice.COLDCARD, SigningOperation.PSBT, PayloadCodecKind.RAW_PSBT, QrFraming.BBQR)
        listOf(
            SigningDevice.JADE, SigningDevice.KEYSTONE, SigningDevice.SEEDSIGNER,
            SigningDevice.PASSPORT, SigningDevice.KRUX,
        ).forEach { assertQr(it, SigningOperation.PSBT, PayloadCodecKind.RAW_PSBT, QrFraming.UR) }
        exportImportDevices(SigningOperation.PSBT).forEach { device ->
            exportImport(device, SigningOperation.PSBT).fileRoute?.let { route ->
                assertEquals("$device", PayloadCodecKind.RAW_PSBT, route.codec)
                assertEquals("$device", "transaction.psbt", route.fileName)
            }
        }
    }

    @Test
    fun `QR export names the device only where the plain-message screen shows it`() {
        assertEquals("Jade", SigningDevice.JADE.profile().qrDeviceName)
        assertEquals("Keystone", SigningDevice.KEYSTONE.profile().qrDeviceName)
        assertEquals("SeedSigner", SigningDevice.SEEDSIGNER.profile().qrDeviceName)
        assertEquals("Krux", SigningDevice.KRUX.profile().qrDeviceName)
        assertEquals("", SigningDevice.COLDCARD.profile().qrDeviceName)
    }

    @Test
    fun `methods other than export and import`() {
        listOf(SigningOperation.MESSAGE, SigningOperation.PSBT).forEach { operation ->
            assertEquals(SigningMethod.Software, SigningDevice.SOFTWARE.profile().method(operation))
            assertEquals(SigningMethod.TapSigner, SigningDevice.TAPSIGNER.profile().method(operation))
            assertEquals(SigningMethod.InApp(InAppSigningDevice.LEDGER), SigningDevice.LEDGER.profile().method(operation))
            assertEquals(SigningMethod.InApp(InAppSigningDevice.BITBOX), SigningDevice.BITBOX.profile().method(operation))
            assertEquals(SigningMethod.TrezorSuite, SigningDevice.TREZOR.profile().method(operation))
        }
    }

    @Test
    fun `devices the claim cannot sign with are unsupported for both operations`() {
        listOf(
            SigningDevice.FOREIGN_SOFTWARE, SigningDevice.PORTAL, SigningDevice.GENERIC_AIRGAP,
            SigningDevice.OTHER_HARDWARE, SigningDevice.SERVER, SigningDevice.PLATFORM, SigningDevice.UNKNOWN,
        ).forEach { device ->
            assertEquals("$device", SigningMethod.Unsupported, device.profile().message)
            assertEquals("$device", SigningMethod.Unsupported, device.profile().psbt)
        }
    }

    @Test
    fun `every import route has the export route it answers`() {
        SigningDevice.entries.forEach { device ->
            SigningOperation.entries.forEach { operation ->
                (device.profile().method(operation) as? SigningMethod.ExportImport)?.let { method ->
                    if (QR in method.imports) assertNotNull("$device $operation", method.qrRoute)
                    if (NFC in method.imports) assertNotNull("$device $operation", method.nfcRoute)
                    if (FILE in method.imports) assertNotNull("$device $operation", method.fileRoute)
                }
            }
        }
    }

    @Test
    fun `a message file route always leads to the export completed screen`() {
        exportImportDevices(SigningOperation.MESSAGE).forEach { device ->
            val fileRoute = exportImport(device, SigningOperation.MESSAGE).fileRoute ?: return@forEach
            assertNotNull("$device", fileRoute.afterExport)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no route at all is rejected`() {
        SigningMethod.ExportImport(export = emptyList(), imports = emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `two routes over one transport are rejected`() {
        SigningMethod.ExportImport(
            export = listOf(
                ExportRoute.Qr(PayloadCodecKind.SPECTER_MESSAGE, QrFraming.PLAIN),
                ExportRoute.Qr(PayloadCodecKind.COLDCARD_JSON, QrFraming.BBQR_JSON),
            ),
            imports = listOf(QR),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an import with no export route to follow is rejected`() {
        SigningMethod.ExportImport(
            export = listOf(ExportRoute.Qr(PayloadCodecKind.SPECTER_MESSAGE, QrFraming.PLAIN)),
            imports = listOf(QR, FILE),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an export completed route missing from the signing sheet is rejected`() {
        SigningMethod.ExportImport(
            export = listOf(
                ExportRoute.File(
                    codec = PayloadCodecKind.PASSPORT_TEXT,
                    fileName = "passport_message.txt",
                    afterExport = AfterFileExport(listOf(QR), FileSigningInstructions.PASSPORT),
                ),
            ),
            imports = listOf(FILE),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an empty export completed list is rejected`() {
        AfterFileExport(emptyList(), FileSigningInstructions.COLDCARD)
    }

    private fun exportImport(device: SigningDevice, operation: SigningOperation) =
        device.profile().method(operation) as SigningMethod.ExportImport

    private fun exportImportDevices(operation: SigningOperation) =
        SigningDevice.entries.filter { it.profile().method(operation) is SigningMethod.ExportImport }

    private fun assertRoutes(
        device: SigningDevice,
        operation: SigningOperation,
        export: List<SigningTransport>,
        imports: List<SigningTransport>,
    ) {
        val method = exportImport(device, operation)
        assertEquals("$device $operation export", export, method.export.map { it.transport })
        assertEquals("$device $operation import", imports, method.importRoutes(RouteContext.SIGNING_SHEET))
    }

    private fun assertAfterFileExport(
        device: SigningDevice,
        imports: List<SigningTransport>,
        instructions: FileSigningInstructions,
    ) {
        val method = exportImport(device, SigningOperation.MESSAGE)
        assertEquals("$device", imports, method.importRoutes(RouteContext.AFTER_FILE_EXPORT))
        assertEquals("$device", instructions, method.fileRoute?.afterExport?.instructions)
    }

    private fun assertFile(device: SigningDevice, codec: PayloadCodecKind, fileName: String) {
        val route = exportImport(device, SigningOperation.MESSAGE).fileRoute
        assertNotNull("$device", route)
        assertEquals("$device", codec, route?.codec)
        assertEquals("$device", fileName, route?.fileName)
    }

    private fun assertQr(device: SigningDevice, operation: SigningOperation, codec: PayloadCodecKind, framing: QrFraming) {
        val route = exportImport(device, operation).qrRoute
        assertTrue("$device $operation has a QR route", route != null)
        assertEquals("$device $operation", codec, route?.codec)
        assertEquals("$device $operation", framing, route?.framing)
    }
}
