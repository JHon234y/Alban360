package com.example.util

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val colombianLocale = Locale("es", "CO")

fun formatCop(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(colombianLocale).apply {
        maximumFractionDigits = 0
    }
    return formatter.format(amount)
}

fun formatNumber(value: Double, maxDecimals: Int = 1): String {
    val formatter = NumberFormat.getNumberInstance(colombianLocale).apply {
        maximumFractionDigits = maxDecimals
        minimumFractionDigits = 0
    }
    return formatter.format(value)
}

fun todayIsoDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

fun nowColombiaTimestampLabel(): String {
    val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", colombianLocale).apply {
        timeZone = TimeZone.getTimeZone("America/Bogota")
    }
    return "${sdf.format(Date())} (Colombia)"
}

fun generateRecordCode(prefix: String): String {
    val suffix = (System.currentTimeMillis() % 1_000_000L).toString(36).uppercase(Locale.US)
    return "${prefix.uppercase(Locale.US)}-$suffix"
}

fun generateQrBitmap(content: String, sizePx: Int = 384): ImageBitmap? = try {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val darkColor = AndroidColor.parseColor("#173B30")
    val lightColor = AndroidColor.WHITE
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        val offset = y * sizePx
        for (x in 0 until sizePx) {
            pixels[offset + x] = if (matrix[x, y]) darkColor else lightColor
        }
    }
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    bitmap.asImageBitmap()
} catch (_: Exception) {
    null
}

fun profileShareUrl(recordCodeOrId: String): String {
    return "https://albancafe360.narino.co/#perfil/$recordCodeOrId"
}

fun shareText(context: Context, title: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
