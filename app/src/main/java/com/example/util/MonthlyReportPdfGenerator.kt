package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object MonthlyReportPdfGenerator {

    fun generateMonthlyReportPdf(
        context: Context,
        profile: BusinessProfile,
        parties: List<Party>,
        transactions: List<TransactionEntry>,
        periodLabel: String = "Current Month Statement"
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in pt
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val currency = profile.currencySymbol.ifBlank { "₹" }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val shortDate = SimpleDateFormat("dd MMM", Locale.getDefault())

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1565C0")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 110f, headerPaint)

            val whiteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 22f
                isFakeBoldText = true
            }
            val whiteSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E3F2FD")
                textSize = 11f
            }

            canvas.drawText(profile.businessName.ifBlank { "Kirio Digital Ledger" }, 36f, 44f, whiteTitlePaint)
            canvas.drawText(
                "${profile.address.ifBlank { "Digital Khata Statement" }} • Phone: ${profile.phone.ifBlank { "N/A" }}",
                36f,
                66f,
                whiteSubPaint
            )
            canvas.drawText(
                "Statement: $periodLabel  |  Generated: ${dateFormat.format(Date())}",
                36f,
                88f,
                whiteSubPaint
            )

            // Summary Box
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(36f, 130f, 559f, 210f, 10f, 10f, boxPaint)

            val totalGet = parties.filter { it.balance > 0 }.sumOf { it.balance }
            val totalGive = parties.filter { it.balance < 0 }.sumOf { abs(it.balance) }
            val cashIn = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
            val cashOut = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 10f
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 15f
                isFakeBoldText = true
            }

            canvas.drawText("YOU WILL GET", 56f, 155f, labelPaint)
            canvas.drawText("$currency${formatAmount(totalGet)}", 56f, 180f, valuePaint.apply { color = Color.parseColor("#2E7D32") })

            canvas.drawText("YOU WILL GIVE", 215f, 155f, labelPaint)
            canvas.drawText("$currency${formatAmount(totalGive)}", 215f, 180f, valuePaint.apply { color = Color.parseColor("#D32F2F") })

            canvas.drawText("NET CASHBOOK", 385f, 155f, labelPaint)
            canvas.drawText("$currency${formatAmount(cashIn - cashOut)}", 385f, 180f, valuePaint.apply { color = Color.parseColor("#1565C0") })

            // Table Header
            var y = 245f
            val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 14f
                isFakeBoldText = true
            }
            canvas.drawText("Recent Ledger & Cashbook Entries (${transactions.size} total)", 36f, y, sectionTitlePaint)
            y += 18f

            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.FILL
            }
            canvas.drawRect(36f, y, 559f, y + 24f, tableHeaderPaint)

            val thTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 10f
                isFakeBoldText = true
            }
            canvas.drawText("DATE", 44f, y + 16f, thTextPaint)
            canvas.drawText("PARTY / PARTICULARS", 115f, y + 16f, thTextPaint)
            canvas.drawText("TYPE", 335f, y + 16f, thTextPaint)
            canvas.drawText("MODE", 415f, y + 16f, thTextPaint)
            canvas.drawText("AMOUNT", 485f, y + 16f, thTextPaint)
            y += 38f

            val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 10f
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }

            transactions.take(22).forEach { tx ->
                if (y > 780f) return@forEach
                val dateStr = shortDate.format(Date(tx.timestamp))
                val title = tx.partyNameSnapshot.ifBlank { tx.description.ifBlank { "Entry" } }.take(28)
                val typeStr = tx.type.name.replace("_", " ")
                val amtStr = "$currency${formatAmount(tx.amount)}"

                canvas.drawText(dateStr, 44f, y, rowTextPaint)
                canvas.drawText(title, 115f, y, rowTextPaint)
                canvas.drawText(typeStr, 335f, y, rowTextPaint)
                canvas.drawText(tx.paymentMode.name, 415f, y, rowTextPaint)
                canvas.drawText(amtStr, 485f, y, rowTextPaint)
                canvas.drawLine(36f, y + 6f, 559f, y + 6f, dividerPaint)
                y += 22f
            }

            // Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 9f
            }
            canvas.drawText(
                "Generated by Kirio Smart Digital Khata • 100% Safe & Accurate Bookkeeping",
                36f,
                815f,
                footerPaint
            )

            pdfDocument.finishPage(page)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val outFile = File(reportsDir, "Kirio_Statement_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            outFile
        } catch (e: Exception) {
            null
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File, title: String = "Share Kirio Statement PDF") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Please find attached the official Khata Statement generated via Kirio.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    private fun formatAmount(amount: Double): String {
        val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
        fmt.maximumFractionDigits = 0
        return fmt.format(amount)
    }
}
