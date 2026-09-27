package com.nunchuk.android.core.signing

enum class SigningTransport { FILE, QR, NFC }
enum class SigningOperation { MESSAGE, PSBT }
enum class PayloadCodecKind { SPECTER_MESSAGE, COLDCARD_JSON, PASSPORT_TEXT, KRUX_TEXT, RAW_PSBT }
enum class QrFraming { PLAIN, BBQR_JSON, UR, BBQR }
enum class InAppSigningDevice { LEDGER, BITBOX }
enum class FileSigningInstructions { COLDCARD, PASSPORT, KRUX }
enum class RouteContext { SIGNING_SHEET, AFTER_FILE_EXPORT }

/**
 * The "Export completed" screen shown once a message request file is saved or shared: the device's
 * on-device steps and the ways its signature comes back, in the order that screen lists them.
 */
data class AfterFileExport(
    val imports: List<SigningTransport>,
    val instructions: FileSigningInstructions,
) {
    init {
        require(imports.isNotEmpty() && imports.distinct().size == imports.size)
    }
}

sealed interface ExportRoute {
    val transport: SigningTransport
    val codec: PayloadCodecKind

    data class File(
        override val codec: PayloadCodecKind,
        val fileName: String,
        /** Null when saving or sharing the file ends the export (the claiming PSBT). */
        val afterExport: AfterFileExport? = null,
    ) : ExportRoute {
        override val transport = SigningTransport.FILE
    }

    data class Qr(override val codec: PayloadCodecKind, val framing: QrFraming) : ExportRoute {
        override val transport = SigningTransport.QR
    }

    data class Nfc(override val codec: PayloadCodecKind) : ExportRoute {
        override val transport = SigningTransport.NFC
    }
}

sealed interface SigningMethod {
    data object Software : SigningMethod
    data object TapSigner : SigningMethod
    data class InApp(val device: InAppSigningDevice) : SigningMethod
    data object TrezorSuite : SigningMethod
    data object Unsupported : SigningMethod

    /** [export] and [imports] are in the order the signing sheet lists them. */
    data class ExportImport(
        val export: List<ExportRoute>,
        val imports: List<SigningTransport>,
    ) : SigningMethod {
        init {
            require(export.isNotEmpty() && imports.isNotEmpty())
            require(export.map { it.transport }.distinct().size == export.size)
            require(imports.distinct().size == imports.size)
            // Every import comes back the way the request went out, so it has a route to follow.
            require(imports.all { route -> export.any { it.transport == route } })
            require(fileRoute?.afterExport?.imports.orEmpty().all { it in imports })
        }

        val fileRoute: ExportRoute.File? get() = export.firstNotNullOfOrNull { it as? ExportRoute.File }
        val qrRoute: ExportRoute.Qr? get() = export.firstNotNullOfOrNull { it as? ExportRoute.Qr }
        val nfcRoute: ExportRoute.Nfc? get() = export.firstNotNullOfOrNull { it as? ExportRoute.Nfc }

        fun importRoutes(context: RouteContext): List<SigningTransport> = when (context) {
            RouteContext.SIGNING_SHEET -> imports
            RouteContext.AFTER_FILE_EXPORT -> fileRoute?.afterExport?.imports.orEmpty()
        }
    }
}

data class SignerProfile(
    val message: SigningMethod,
    val psbt: SigningMethod,
    /** Names the device on the plain-message QR export screen; blank elsewhere. */
    val qrDeviceName: String = "",
) {
    fun method(operation: SigningOperation): SigningMethod = when (operation) {
        SigningOperation.MESSAGE -> message
        SigningOperation.PSBT -> psbt
    }
}

/** Signing sheet imports follow the export order. */
private fun routes(vararg export: ExportRoute) =
    SigningMethod.ExportImport(export.toList(), export.map { it.transport })

private fun psbtRoutes(file: Boolean, coldcard: Boolean = false) = routes(
    *buildList {
        if (file) add(ExportRoute.File(PayloadCodecKind.RAW_PSBT, PSBT_FILE_NAME))
        add(ExportRoute.Qr(PayloadCodecKind.RAW_PSBT, if (coldcard) QrFraming.BBQR else QrFraming.UR))
        if (coldcard) add(ExportRoute.Nfc(PayloadCodecKind.RAW_PSBT))
    }.toTypedArray()
)

/** Jade, Keystone and SeedSigner: one plain-text QR out, the signature QR back; the PSBT over UR. */
private fun qrOnlyAirgap(deviceName: String) = SignerProfile(
    message = routes(ExportRoute.Qr(PayloadCodecKind.SPECTER_MESSAGE, QrFraming.PLAIN)),
    psbt = psbtRoutes(file = false),
    qrDeviceName = deviceName,
)

private const val PSBT_FILE_NAME = "transaction.psbt"

/** No fallback to Coldcard: adding a device requires an explicit operation profile. */
fun SigningDevice.profile(): SignerProfile = when (this) {
    SigningDevice.SOFTWARE -> SignerProfile(SigningMethod.Software, SigningMethod.Software)
    SigningDevice.TAPSIGNER -> SignerProfile(SigningMethod.TapSigner, SigningMethod.TapSigner)
    SigningDevice.COLDCARD -> SignerProfile(
        message = routes(
            ExportRoute.File(
                codec = PayloadCodecKind.COLDCARD_JSON,
                fileName = "coldcard_message.txt",
                afterExport = AfterFileExport(listOf(SigningTransport.FILE), FileSigningInstructions.COLDCARD),
            ),
            ExportRoute.Qr(PayloadCodecKind.COLDCARD_JSON, QrFraming.BBQR_JSON),
            ExportRoute.Nfc(PayloadCodecKind.COLDCARD_JSON),
        ),
        psbt = psbtRoutes(file = true, coldcard = true),
    )
    SigningDevice.JADE -> qrOnlyAirgap("Jade")
    SigningDevice.KEYSTONE -> qrOnlyAirgap("Keystone")
    SigningDevice.SEEDSIGNER -> qrOnlyAirgap("SeedSigner")
    SigningDevice.PASSPORT -> SignerProfile(
        message = routes(
            ExportRoute.File(
                codec = PayloadCodecKind.PASSPORT_TEXT,
                fileName = "passport_message.txt",
                afterExport = AfterFileExport(listOf(SigningTransport.FILE), FileSigningInstructions.PASSPORT),
            ),
        ),
        psbt = psbtRoutes(file = true),
    )
    SigningDevice.KRUX -> SignerProfile(
        message = routes(
            ExportRoute.File(
                codec = PayloadCodecKind.KRUX_TEXT,
                fileName = "krux_message.txt",
                // Krux can also show the signature as a QR ("Sign to QR code"), listed first there.
                afterExport = AfterFileExport(
                    listOf(SigningTransport.QR, SigningTransport.FILE),
                    FileSigningInstructions.KRUX,
                ),
            ),
            ExportRoute.Qr(PayloadCodecKind.SPECTER_MESSAGE, QrFraming.PLAIN),
        ),
        psbt = psbtRoutes(file = true),
        qrDeviceName = "Krux",
    )
    SigningDevice.LEDGER -> SignerProfile(
        SigningMethod.InApp(InAppSigningDevice.LEDGER), SigningMethod.InApp(InAppSigningDevice.LEDGER),
    )
    SigningDevice.BITBOX -> SignerProfile(
        SigningMethod.InApp(InAppSigningDevice.BITBOX), SigningMethod.InApp(InAppSigningDevice.BITBOX),
    )
    SigningDevice.TREZOR -> SignerProfile(SigningMethod.TrezorSuite, SigningMethod.TrezorSuite)
    SigningDevice.FOREIGN_SOFTWARE, SigningDevice.PORTAL, SigningDevice.GENERIC_AIRGAP,
    SigningDevice.OTHER_HARDWARE, SigningDevice.SERVER, SigningDevice.PLATFORM, SigningDevice.UNKNOWN ->
        SignerProfile(SigningMethod.Unsupported, SigningMethod.Unsupported)
}
