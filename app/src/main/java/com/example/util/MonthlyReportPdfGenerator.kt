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
import com.example.data.repository.computeRunningBalances
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object MonthlyReportPdfGenerator {

    /**
     * Generates a multi-page PDF statement for an individual party.
     * Entries are sorted oldest -> newest (first entry at top).
     * Every row has its own running balance.
     * Bold closing balance card is placed on the final page.
     */
    fun generatePartyStatementPdf(
        context: Context,
        profile: BusinessProfile,
        party: Party,
        entries: List<TransactionEntry>,
        periodLabel: String = "Full Khata Statement"
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val currency = profile.currencySymbol.ifBlank { "₹" }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val shortDate = SimpleDateFormat("dd MMM yy", Locale.getDefault())

            // Sort oldest -> newest
            val sortedEntries = entries.sortedWith(compareBy({ it.timestamp }, { it.id }))
            val runningBalances = computeRunningBalances(sortedEntries)

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val primaryBlue = Color.parseColor("#1565C0")
            val textDark = Color.parseColor("#0F172A")
            val textSecondary = Color.parseColor("#475569")
            val dividerColor = Color.parseColor("#E2E8F0")

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 20f
                isFakeBoldText = true
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E3F2FD")
                textSize = 10f
            }
            val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryBlue
                style = Paint.Style.FILL
            }

            fun drawTopBanner(c: Canvas, isFirstPage: Boolean) {
                if (isFirstPage) {
                    c.drawRect(0f, 0f, 595f, 95f, bannerPaint)
                    c.drawText(profile.businessName.ifBlank { "Kirio Digital Khata" }, 36f, 38f, titlePaint)
                    c.drawText(
                        "${profile.address.ifBlank { "Kirio Smart Ledger" }} • Phone: ${profile.phone.ifBlank { "N/A" }}",
                        36f,
                        58f,
                        subPaint
                    )
                    c.drawText(
                        "Statement: $periodLabel  |  Generated: ${dateFormat.format(Date())}",
                        36f,
                        78f,
                        subPaint
                    )
                } else {
                    c.drawRect(0f, 0f, 595f, 45f, bannerPaint)
                    val compactTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 12f
                        isFakeBoldText = true
                    }
                    c.drawText("${profile.businessName} — Statement for ${party.name} (Page $pageNumber)", 36f, 28f, compactTitle)
                }
            }

            fun drawTableHeader(c: Canvas, atY: Float) {
                val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E2E8F0")
                    style = Paint.Style.FILL
                }
                c.drawRect(36f, atY, 559f, atY + 22f, tableHeaderPaint)

                val thTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1E293B")
                    textSize = 9f
                    isFakeBoldText = true
                }
                c.drawText("DATE", 44f, atY + 15f, thTextPaint)
                c.drawText("PARTICULARS / REMARKS", 120f, atY + 15f, thTextPaint)
                c.drawText("YOU GAVE (+)", 335f, atY + 15f, thTextPaint)
                c.drawText("YOU GOT (-)", 420f, atY + 15f, thTextPaint)
                c.drawText("BALANCE", 500f, atY + 15f, thTextPaint)
            }

            fun drawFooter(c: Canvas, pg: Int) {
                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#94A3B8")
                    textSize = 8f
                }
                c.drawText("Generated by Kirio Smart Khata • Safe & 100% Verified Ledger", 36f, 815f, footerPaint)
                c.drawText("Page $pg", 520f, 815f, footerPaint)
            }

            // Draw first page header
            drawTopBanner(canvas, isFirstPage = true)

            // Party Info & Summary Card on Page 1
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(36f, 110f, 559f, 175f, 8f, 8f, cardPaint)

            val partyNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textDark
                textSize = 15f
                isFakeBoldText = true
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 9.5f
            }
            canvas.drawText("PARTY: ${party.name.uppercase()}", 50f, 134f, partyNamePaint)
            canvas.drawText("Phone: ${party.phone.ifBlank { "N/A" }}  |  Type: ${party.type.name}  |  Total Entries: ${sortedEntries.size}", 50f, 155f, metaPaint)

            var y = 195f
            drawTableHeader(canvas, y)
            y += 34f

            val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 9.5f
            }
            val gaveTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C62828")
                textSize = 9.5f
                isFakeBoldText = true
            }
            val gotTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2E7D32")
                textSize = 9.5f
                isFakeBoldText = true
            }
            val balTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textDark
                textSize = 9.5f
                isFakeBoldText = true
            }
            val dividerPaint = Paint().apply {
                color = dividerColor
                strokeWidth = 0.8f
            }

            for (entry in sortedEntries) {
                // Check if page height exceeded
                if (y > 750f) {
                    drawFooter(canvas, pageNumber)
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawTopBanner(canvas, isFirstPage = false)
                    y = 60f
                    drawTableHeader(canvas, y)
                    y += 32f
                }

                val dateStr = shortDate.format(Date(entry.timestamp))
                val title = buildString {
                    if (entry.billNumber.isNotBlank()) append("#${entry.billNumber} ")
                    if (entry.description.isNotBlank()) append(entry.description)
                    else if (entry.type == TransactionType.YOU_GAVE) append("Goods / Credit")
                    else append("Payment Received")
                }.take(30)

                val amtStr = "$currency${formatAmount(entry.amount)}"
                val rowBal = runningBalances[entry.id] ?: 0.0
                val balStr = "$currency${formatAmount(abs(rowBal))}"

                canvas.drawText(dateStr, 44f, y, rowTextPaint)
                canvas.drawText(title, 120f, y, rowTextPaint)

                if (entry.type == TransactionType.YOU_GAVE) {
                    canvas.drawText(amtStr, 335f, y, gaveTextPaint)
                    canvas.drawText("-", 420f, y, rowTextPaint)
                } else {
                    canvas.drawText("-", 335f, y, rowTextPaint)
                    canvas.drawText(amtStr, 420f, y, gotTextPaint)
                }

                canvas.drawText(balStr, 500f, y, balTextPaint)
                canvas.drawLine(36f, y + 6f, 559f, y + 6f, dividerPaint)
                y += 24f
            }

            // Check if closing balance fits on current page
            if (y > 710f) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawTopBanner(canvas, isFirstPage = false)
                y = 70f
            } else {
                y += 12f
            }

            // Bold CLOSING BALANCE card at the bottom
            val closingBalance = if (sortedEntries.isNotEmpty()) {
                runningBalances[sortedEntries.last().id] ?: party.balance
            } else {
                party.balance
            }

            val closingBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when {
                    closingBalance > 0.0 -> Color.parseColor("#E8F5E9") // Green
                    closingBalance < 0.0 -> Color.parseColor("#FFEBEE") // Red
                    else -> Color.parseColor("#F1F5F9")
                }
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(36f, y, 559f, y + 55f, 8f, 8f, closingBoxPaint)

            val closingLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when {
                    closingBalance > 0.0 -> Color.parseColor("#2E7D32")
                    closingBalance < 0.0 -> Color.parseColor("#C62828")
                    else -> Color.parseColor("#475569")
                }
                textSize = 11f
                isFakeBoldText = true
            }

            val closingValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = closingLabelPaint.color
                textSize = 18f
                isFakeBoldText = true
            }

            val closingText = when {
                closingBalance > 0.0 -> "CLOSING BALANCE (YOU WILL GET / લેવાના / लेने हैं)"
                closingBalance < 0.0 -> "CLOSING BALANCE (YOU WILL GIVE / આપવાના / देने हैं)"
                else -> "CLOSING BALANCE (SETTLED / ચૂકતે / बराबर)"
            }

            canvas.drawText(closingText, 52f, y + 24f, closingLabelPaint)
            canvas.drawText("$currency${formatAmount(abs(closingBalance))}", 52f, y + 46f, closingValuePaint)

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)

            val reportsDir = File(context.cacheDir, "statements").apply { mkdirs() }
            val cleanPartyName = party.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val outFile = File(reportsDir, "Statement_${cleanPartyName}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            outFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a multi-page PDF summary for the monthly ledger and cashbook report.
     */
    fun generateMonthlyReportPdf(
        context: Context,
        profile: BusinessProfile,
        parties: List<Party>,
        transactions: List<TransactionEntry>,
        periodLabel: String = "Current Month Statement"
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val currency = profile.currencySymbol.ifBlank { "₹" }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val shortDate = SimpleDateFormat("dd MMM", Locale.getDefault())

            val sortedTxs = transactions.sortedWith(compareBy({ it.timestamp }, { it.id }))

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1565C0")
                style = Paint.Style.FILL
            }

            fun drawBanner(c: Canvas, isFirst: Boolean) {
                if (isFirst) {
                    c.drawRect(0f, 0f, 595f, 105f, headerPaint)
                    val whiteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 22f
                        isFakeBoldText = true
                    }
                    val whiteSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#E3F2FD")
                        textSize = 10.5f
                    }
                    c.drawText(profile.businessName.ifBlank { "Kirio Digital Ledger" }, 36f, 40f, whiteTitlePaint)
                    c.drawText(
                        "${profile.address.ifBlank { "Digital Khata Statement" }} • Phone: ${profile.phone.ifBlank { "N/A" }}",
                        36f,
                        62f,
                        whiteSubPaint
                    )
                    c.drawText(
                        "Statement: $periodLabel  |  Generated: ${dateFormat.format(Date())}",
                        36f,
                        84f,
                        whiteSubPaint
                    )
                } else {
                    c.drawRect(0f, 0f, 595f, 40f, headerPaint)
                    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 12f
                        isFakeBoldText = true
                    }
                    c.drawText("${profile.businessName} — General Report (Page $pageNumber)", 36f, 26f, titlePaint)
                }
            }

            drawBanner(canvas, isFirst = true)

            // Summary Box (Page 1 only)
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(36f, 120f, 559f, 195f, 8f, 8f, boxPaint)

            val totalGet = parties.filter { it.balance > 0 }.sumOf { it.balance }
            val totalGive = parties.filter { it.balance < 0 }.sumOf { abs(it.balance) }
            val cashIn = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
            val cashOut = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 9.5f
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 14f
                isFakeBoldText = true
            }

            canvas.drawText("YOU WILL GET", 56f, 145f, labelPaint)
            canvas.drawText("$currency${formatAmount(totalGet)}", 56f, 170f, valuePaint.apply { color = Color.parseColor("#2E7D32") })

            canvas.drawText("YOU WILL GIVE", 215f, 145f, labelPaint)
            canvas.drawText("$currency${formatAmount(totalGive)}", 215f, 170f, valuePaint.apply { color = Color.parseColor("#D32F2F") })

            canvas.drawText("NET CASHBOOK", 385f, 145f, labelPaint)
            canvas.drawText("$currency${formatAmount(cashIn - cashOut)}", 385f, 170f, valuePaint.apply { color = Color.parseColor("#1565C0") })

            fun drawReportTableHeader(c: Canvas, atY: Float) {
                val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E2E8F0")
                    style = Paint.Style.FILL
                }
                c.drawRect(36f, atY, 559f, atY + 22f, tableHeaderPaint)

                val thTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1E293B")
                    textSize = 9.5f
                    isFakeBoldText = true
                }
                c.drawText("DATE", 44f, atY + 15f, thTextPaint)
                c.drawText("PARTY / PARTICULARS", 115f, atY + 15f, thTextPaint)
                c.drawText("TYPE", 335f, atY + 15f, thTextPaint)
                c.drawText("MODE", 415f, atY + 15f, thTextPaint)
                c.drawText("AMOUNT", 485f, atY + 15f, thTextPaint)
            }

            var y = 220f
            drawReportTableHeader(canvas, y)
            y += 34f

            val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 9.5f
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 0.8f
            }

            fun drawReportFooter(c: Canvas, pg: Int) {
                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#94A3B8")
                    textSize = 8.5f
                }
                c.drawText("Generated by Kirio Smart Digital Khata • 100% Safe & Accurate", 36f, 815f, footerPaint)
                c.drawText("Page $pg", 520f, 815f, footerPaint)
            }

            for (tx in sortedTxs) {
                if (y > 760f) {
                    drawReportFooter(canvas, pageNumber)
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawBanner(canvas, isFirst = false)
                    y = 55f
                    drawReportTableHeader(canvas, y)
                    y += 32f
                }

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

            drawReportFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val outFile = File(reportsDir, "Kirio_Report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            outFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a plain-text/WhatsApp statement formatted oldest -> newest with running balances
     * and closing balance at the bottom.
     */
    fun generateTextStatement(
        profile: BusinessProfile,
        party: Party,
        entries: List<TransactionEntry>,
        dateRangeLabel: String = "Full Khata Statement"
    ): String {
        val currency = profile.currencySymbol.ifBlank { "₹" }
        val shortDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val sortedEntries = entries.sortedWith(compareBy({ it.timestamp }, { it.id }))
        val runningBalances = computeRunningBalances(sortedEntries)

        val closingBalance = if (sortedEntries.isNotEmpty()) {
            runningBalances[sortedEntries.last().id] ?: party.balance
        } else {
            party.balance
        }

        val closingLabel = when {
            closingBalance > 0.0 -> "YOU WILL GET / લેવાના / लेने हैं"
            closingBalance < 0.0 -> "YOU WILL GIVE / આપવાના / देने हैं"
            else -> "SETTLED / बराबर"
        }

        return buildString {
            append("*${profile.businessName.ifBlank { "Kirio Digital Khata" }}*\n")
            if (profile.phone.isNotBlank()) append("Phone: ${profile.phone}\n")
            append("--------------------------------\n")
            append("*Statement for ${party.name}*\n")
            append("Period: $dateRangeLabel\n")
            append("--------------------------------\n")
            if (sortedEntries.isEmpty()) {
                append("No entries found for this period.\n")
            } else {
                sortedEntries.forEach { entry ->
                    val date = shortDate.format(Date(entry.timestamp))
                    val desc = if (entry.billNumber.isNotBlank()) "Bill #${entry.billNumber}" else entry.description.ifBlank { entry.type.name }
                    val typeSign = if (entry.type == TransactionType.YOU_GAVE) "(+) GAVE" else "(-) GOT"
                    val amt = "$currency${formatAmount(entry.amount)}"
                    val bal = "$currency${formatAmount(abs(runningBalances[entry.id] ?: 0.0))}"
                    append("$date: $desc\n")
                    append("  $typeSign: $amt | Bal: $bal\n")
                }
            }
            append("--------------------------------\n")
            append("*CLOSING BALANCE: $currency${formatAmount(abs(closingBalance))}*\n")
            append("($closingLabel)\n")
            if (profile.upiId.isNotBlank() && closingBalance > 0.0) {
                append("UPI ID for Payment: *${profile.upiId}*\n")
            }
            append("--------------------------------\n")
            append("Generated by Kirio Smart Khata")
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
        val fmt = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
        fmt.maximumFractionDigits = 0
        return fmt.format(amount)
    }
}
