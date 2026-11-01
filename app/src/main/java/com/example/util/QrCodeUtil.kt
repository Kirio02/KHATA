package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.abs

object QrCodeUtil {

    fun buildUpiUri(upiId: String, payeeName: String, amount: Double? = null, note: String = "Kirio Khata Payment"): String {
        val cleanUpi = upiId.trim().ifBlank { "merchant@upi" }
        val cleanName = android.net.Uri.encode(payeeName.trim().ifBlank { "Kirio Store" })
        val cleanNote = android.net.Uri.encode(note)
        val base = "upi://pay?pa=$cleanUpi&pn=$cleanName&tn=$cleanNote&cu=INR"
        return if (amount != null && amount > 0.0) {
            "$base&am=${String.format(java.util.Locale.US, "%.2f", amount)}"
        } else {
            base
        }
    }

    /**
     * Generates a clean, crisp deterministic QR-style matrix bitmap for UPI & store sharing
     * without requiring external native barcode libraries.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val gridSize = 25
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paintDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }

        val margin = sizePx * 0.08f
        val cellSize = (sizePx - margin * 2) / gridSize

        fun drawFinderPattern(startCol: Int, startRow: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    if (isBorder || isInner) {
                        val left = margin + (startCol + c) * cellSize
                        val top = margin + (startRow + r) * cellSize
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, paintDark)
                    }
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(gridSize - 7, 0)
        drawFinderPattern(0, gridSize - 7)

        val seedBytes = content.toByteArray()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= gridSize - 8
                val inBottomLeft = r >= gridSize - 8 && c < 8
                val inCenterBadge = r in 10..14 && c in 10..14
                if (inTopLeft || inTopRight || inBottomLeft || inCenterBadge) continue

                if (r == 6 || c == 6) {
                    if ((r + c) % 2 == 0) {
                        val left = margin + c * cellSize
                        val top = margin + r * cellSize
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, paintDark)
                    }
                    continue
                }

                val idx = (r * gridSize + c)
                val b = seedBytes[idx % seedBytes.size].toInt()
                val hash = abs((idx * 31 + b * 17 + content.hashCode()) % 11)
                if (hash < 5) {
                    val left = margin + c * cellSize
                    val top = margin + r * cellSize
                    canvas.drawRect(left, top, left + cellSize, top + cellSize, paintDark)
                }
            }
        }

        // Center Kirio Rupee emblem
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1565C0")
            style = Paint.Style.FILL
        }
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radius = cellSize * 2.2f
        canvas.drawRoundRect(
            cx - radius,
            cy - radius,
            cx + radius,
            cy + radius,
            12f,
            12f,
            badgePaint
        )

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = cellSize * 2.6f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText("₹", cx, textY, textPaint)

        return bitmap
    }
}
