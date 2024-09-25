package com.nunchuk.android.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getFormatDate
import com.nunchuk.android.core.util.hasChangeIndex
import com.nunchuk.android.core.wallet.InvoiceInfo
import com.nunchuk.android.model.Transaction
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.time.delay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ExportInvoices(private val context: Context) {

    private val _progressFlow = MutableStateFlow(0 to 0)
    val progressFlow: StateFlow<Pair<Int, Int>> get() = _progressFlow

    private val margin36 = 36f
    private val margin52 = 52f
    private val margin12 = 12f
    private val margin48 = 48f
    private val margin30 = 30f

    private var yPosition = margin52  // Start y position

    private val latoBold by lazy { ResourcesCompat.getFont(context, R.font.lato_bold) }
    private val latoRegular by lazy { ResourcesCompat.getFont(context, R.font.lato_regular) }
    private val montserratMedium by lazy {
        ResourcesCompat.getFont(
            context,
            R.font.montserrat_medium
        )
    }
    private val colorPrimary by lazy { Color.parseColor("#031F2B") }

    private val paintText = Paint().apply {
        color = colorPrimary
        textSize = 24f
        isAntiAlias = true
        typeface = montserratMedium
    }

    private val paintGrayBox = Paint().apply {
        color = Color.parseColor("#EAEAEA")  // Light gray background
        style = Paint.Style.FILL
    }

    suspend fun generatePDF(invoicesInfos: List<InvoiceInfo>, filePath: String = "", job: Job) {
        _progressFlow.emit(0 to 0)
        val pdfDocument = PdfDocument()
        invoicesInfos.forEachIndexed { index, invoiceInfo ->
            job.ensureActive() // Check for cancellation
            addInvoiceToPDF(pdfDocument, invoiceInfo, index + 1)
            _progressFlow.emit(index + 1 to invoicesInfos.size)
            kotlinx.coroutines.delay(200) // Add delay of 1 second
        }

        try {
            val outputStream = FileOutputStream(File(filePath))
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun addInvoiceToPDF(
        pdfDocument: PdfDocument,
        invoiceInfo: InvoiceInfo,
        pageNumber: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()

        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintBackground = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paintBackground)

        canvas.drawText(
            if (invoiceInfo.isReceive) context.getString(R.string.nc_amount_receive) else context.getString(
                R.string.nc_amount_sent
            ), margin52, margin52, Paint().apply {
                color = colorPrimary
                textSize = 18f
                isAntiAlias = true
                typeface = latoRegular
            })
        yPosition = 80f
        canvas.drawText(invoiceInfo.amountSent, margin52, yPosition, paintText.apply {
            color = colorPrimary
            textSize = 24f
            isAntiAlias = true
            typeface = montserratMedium
        })
        yPosition += 24f
        canvas.drawText(invoiceInfo.confirmTime, margin52, yPosition, paintText.apply {
            textSize = 16f
            typeface = latoRegular
        })

        // Draw transaction ID box
        yPosition = 130f  // Add space between sections
        canvas.drawRect(
            margin36,
            yPosition,
            canvas.width - margin36,
            yPosition + margin48,
            paintGrayBox
        )
        canvas.drawText(
            context.getString(R.string.nc_transaction_id),
            margin36 + margin12,
            yPosition + margin30,
            paintText.apply {
                textSize = 16f
                typeface = latoBold
            })

        yPosition += 80f  // Next box for transaction ID context
        val transactionId = invoiceInfo.transactionId
        var maxWidth = canvas.width - margin36 * 2 - margin12 * 2  // Adjust to fit within the box
        var lines = splitTextIntoLines(transactionId, paintText.apply {
            textSize = 18f
            typeface = latoRegular
            color = colorPrimary
        }, maxWidth)
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, margin36 + margin12, yPosition, paintText)
            if (index != lines.size - 1) yPosition += 20f  // Line height
        }

        // Draw "Send to address" box
        yPosition += 20f  // Space before next section
        canvas.drawRect(
            margin36,
            yPosition,
            canvas.width - margin36,
            yPosition + margin48,
            paintGrayBox
        )
        canvas.drawText(
            if (invoiceInfo.isReceive) context.getString(R.string.nc_transaction_receive_at) else context.getString(
                R.string.nc_transaction_send_to_address
            ),
            margin36 + margin12,
            yPosition + margin30,
            paintText.apply {
                textSize = 16f
                typeface = latoBold
            })

        // Draw the address and amount
        yPosition += 60f
        invoiceInfo.txOutputs.forEachIndexed { index, txOutput ->
            yPosition += if (index != 0) 40f else 20f
            val textWidthSendToAdd = paintText.apply {
                textSize = 18f
                typeface = latoBold
                color = colorPrimary
            }.measureText(txOutput.second.getBTCAmount())
            val xPositionSendToAdd = canvas.width - margin36 - textWidthSendToAdd - margin12
            canvas.drawText(
                txOutput.second.getBTCAmount(),
                xPositionSendToAdd,
                yPosition,
                paintText
            )

            maxWidth =
                canvas.width - margin36 * 2 - margin12 * 3 - textWidthSendToAdd  // Adjust to fit within the box
            lines = splitTextIntoLines(txOutput.first, paintText.apply {
                textSize = 18f
                typeface = latoBold
                color = colorPrimary
            }, maxWidth)
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, margin36 + margin12, yPosition, paintText)
                if (index != lines.size - 1) yPosition += 20f  // Line height
            }
        }

        //draw a line to separate the sections
        yPosition += 20f

        canvas.drawLine(margin36, yPosition, canvas.width - margin36, yPosition, Paint().apply {
            color = Color.parseColor("#DEDEDE")
        })

        // Draw transaction fee and total amount
        if (invoiceInfo.estimatedFee.isNotEmpty()) {
            yPosition += 30f
            canvas.drawText(
                context.getString(R.string.nc_transaction_fee),
                margin36 + margin12,
                yPosition,
                paintText.apply {
                    textSize = 18f
                    typeface = latoRegular
                    color = colorPrimary
                })
            val textWidthTxFee = paintText.measureText(invoiceInfo.estimatedFee)
            val xPositionTxFee = canvas.width - margin36 - textWidthTxFee - margin12

            // Draw the text
            canvas.drawText(invoiceInfo.estimatedFee, xPositionTxFee, yPosition, paintText.apply {
                textSize = 18f
                typeface = latoBold
                color = colorPrimary
            })
        }

        if (invoiceInfo.isReceive.not()) {
            yPosition += 30f
            canvas.drawText(
                context.getString(R.string.nc_transaction_total_amount),
                margin36 + margin12,
                yPosition,
                paintText.apply {
                    textSize = 18f
                    typeface = latoRegular
                    color = colorPrimary
                })
            val textWidthTotalAmount = paintText.measureText(invoiceInfo.amountSent)
            val xPositionTotalAmount = canvas.width - margin36 - textWidthTotalAmount - margin12
            canvas.drawText(
                invoiceInfo.amountSent,
                xPositionTotalAmount,
                yPosition,
                paintText.apply {
                    textSize = 18f
                    typeface = latoBold
                    color = colorPrimary
                })
        }

        if (invoiceInfo.changeAddress.isNotEmpty() && invoiceInfo.changeAddressAmount.isNotEmpty()) {
            // Draw change address
            yPosition += 20f
            canvas.drawRect(
                margin36,
                yPosition,
                canvas.width - margin36,
                yPosition + margin48,
                paintGrayBox
            )
            canvas.drawText(
                context.getString(R.string.nc_transaction_change_address),
                margin36 + margin12,
                yPosition + margin30,
                paintText.apply {
                    textSize = 16f
                    typeface = latoBold
                })

            yPosition += 80f
            val textWidthChangeAdd = paintText.apply {
                textSize = 18f
                typeface = latoBold
                color = colorPrimary
            }.measureText(invoiceInfo.changeAddressAmount)
            val xPositionChangeAdd = canvas.width - margin36 - textWidthChangeAdd - margin12
            canvas.drawText(
                invoiceInfo.changeAddressAmount,
                xPositionChangeAdd,
                yPosition,
                paintText
            )

            val changeAddId = invoiceInfo.changeAddress
            maxWidth =
                canvas.width - margin36 * 2 - margin12 * 3 - textWidthChangeAdd  // Adjust to fit within the box
            lines = splitTextIntoLines(changeAddId, paintText.apply {
                textSize = 18f
                typeface = latoBold
                color = colorPrimary
            }, maxWidth)
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, margin36 + margin12, yPosition, paintText)
                if (index != lines.size - 1) yPosition += 20f  // Line height
            }
        }

        // Draw transaction note
        yPosition += 20f
        canvas.drawRect(
            margin36,
            yPosition,
            canvas.width - margin36,
            yPosition + margin48,
            paintGrayBox
        )
        canvas.drawText(
            "Transaction note",
            margin36 + margin12,
            yPosition + margin30,
            paintText.apply {
                textSize = 16f
                typeface = latoBold
            })

        yPosition += 80f
        val longText = invoiceInfo.note
        for (line in splitTextIntoLinesWords(
            longText,
            paintText.apply {
                textSize = 18f
                typeface = latoRegular
                color = colorPrimary
            },
            canvas.width - margin36 * 2 - margin12
        )) {
            canvas.drawText(line, margin36 + margin12, yPosition, paintText)
            yPosition += 20f  // Adjust line height as needed
        }
        pdfDocument.finishPage(page)
    }

    private fun splitTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (char in text) {
            val testLine = currentLine + char
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = char.toString()
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    // Utility function to split text into lines that fit within the specified width
    private fun splitTextIntoLinesWords(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}

fun Transaction.toInvoiceInfo(context: Context, isInheritanceClaimingFlow: Boolean): InvoiceInfo {
    val transaction = this
    val coins = if (transaction.isReceive)
        transaction.receiveOutputs else
        transaction.outputs.filterIndexed { index, _ -> index != transaction.changeIndex }
    val txOutput =
        if (transaction.hasChangeIndex()) transaction.outputs[transaction.changeIndex] else null
    return InvoiceInfo(
        amountSent = transaction.totalAmount.getBTCAmount(),
        confirmTime = if (isInheritanceClaimingFlow.not()) transaction.getFormatDate() else "",
        transactionId = transaction.txId,
        txOutputs = coins,
        estimatedFee = if (!transaction.isReceive) transaction.fee.getBTCAmount() else "",
        changeAddress = if (transaction.hasChangeIndex()) txOutput?.first.orEmpty() else "",
        changeAddressAmount = if (transaction.hasChangeIndex()) txOutput?.second?.getBTCAmount()
            .orEmpty() else "",
        note = transaction.memo.ifEmpty { context.getString(R.string.nc_none) },
        isReceive = transaction.isReceive,
    )
}