package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.model.Building
import com.example.data.model.CotisationPayment
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReceiptGenerator {

  fun shareInviteViaWhatsApp(context: Context, building: Building) {
    val deepLink = "https://syndicmaroc.app/join/${building.inviteCode}"
    val message =
      buildString {
        appendLine("🏢 دعوة للانضمام إلى فضاء السانديك - ${building.name} (${building.city})")
        appendLine("رمز الانضمام: *${building.inviteCode}*")
        appendLine("الرابط المباشر: $deepLink")
        appendLine("---")
        appendLine("Invitation Syndic - ${building.name} (${building.city})")
        appendLine("Code d'accès : *${building.inviteCode}*")
        append(deepLink)
      }
    shareTextPreferWhatsApp(context, message)
  }

  fun sharePaymentReceiptViaWhatsApp(
    context: Context,
    building: Building,
    payment: CotisationPayment,
  ) {
    val bitmap = renderBilingualReceiptBitmap(building, payment)
    val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val safeName = "recu_${payment.apartmentId}_${payment.monthKey}.png"
    val file = File(receiptsDir, safeName)
    FileOutputStream(file).use { out ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bitmap.recycle()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val caption =
      "وصل أداء واجب السانديك | Reçu de cotisation\n" +
        "${building.name} - ${payment.apartmentLabel}\n" +
        "الشهر / Mois: ${payment.monthKey}\n" +
        "المبلغ / Montant: ${String.format(Locale.US, "%.0f", payment.amountMad)} MAD"

    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    launchPreferWhatsApp(context, intent, "مشاركة وصل الأداء / Partager le reçu")
  }

  fun openPhoneDialer(context: Context, phone: String) {
    val intent =
      Intent(Intent.ACTION_DIAL).apply {
        data = android.net.Uri.parse("tel:${phone.trim()}")
      }
    context.startActivity(intent)
  }

  fun formatMoroccanPhoneForWhatsApp(rawPhone: String): String? {
    val digits = rawPhone.filter { it.isDigit() }
    if (digits.length < 6) return null
    return when {
      digits.startsWith("00") && digits.length > 4 -> digits.drop(2)
      digits.startsWith("212") -> digits
      digits.startsWith("0") && digits.length >= 9 -> "212" + digits.drop(1)
      else -> digits
    }
  }

  fun openWhatsAppChat(context: Context, rawPhone: String, message: String) {
    val intl = formatMoroccanPhoneForWhatsApp(rawPhone)
    if (intl == null) {
      shareTextPreferWhatsApp(context, message)
      return
    }
    val url = "https://wa.me/$intl?text=${android.net.Uri.encode(message)}"
    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
    try {
      context.startActivity(intent)
    } catch (_: Exception) {
      shareTextPreferWhatsApp(context, message)
    }
  }

  fun openSmsBroadcast(context: Context, phones: List<String>, message: String) {
    val cleanPhones = phones.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (cleanPhones.isEmpty()) {
      shareTextPreferWhatsApp(context, message)
      return
    }
    val sep = if (android.os.Build.MANUFACTURER.equals("samsung", ignoreCase = true)) "," else ";"
    val recipients = cleanPhones.joinToString(sep)
    val intent =
      Intent(Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("smsto:$recipients")
        putExtra("sms_body", message)
      }
    try {
      context.startActivity(intent)
    } catch (_: Exception) {
      shareTextPreferWhatsApp(context, message)
    }
  }

  fun sendPaymentReminder(
    context: Context,
    building: Building,
    apartmentLabel: String,
    residentName: String,
    residentPhone: String,
    monthKey: String,
    amountMad: Double,
  ) {
    val amountStr = String.format(Locale.US, "%.0f", amountMad)
    val greeting = if (residentName.isNotBlank()) residentName else "جارنا الكريم"
    val message =
      buildString {
        appendLine("السلام عليكم $greeting،")
        appendLine("تذكير ودي بأداء واجب السانديك لشهر *$monthKey*:")
        appendLine("🏢 ${building.name} (${building.city})")
        appendLine("🏠 $apartmentLabel")
        appendLine("💰 المبلغ: *$amountStr درهم (MAD)*")
        appendLine("---")
        appendLine("Bonjour $greeting, rappel de cotisation syndic ($monthKey) pour ${building.name} - $apartmentLabel : *$amountStr MAD*. Merci.")
      }
    if (residentPhone.isNotBlank()) {
      openWhatsAppChat(context, residentPhone, message)
    } else {
      shareTextPreferWhatsApp(context, message)
    }
  }

  fun shareMonthlyFinancialReport(
    context: Context,
    building: Building,
    monthKey: String,
    paidCount: Int,
    totalUnits: Int,
    monthCollectedMad: Double,
    monthExpectedMad: Double,
    totalCollectedAllMad: Double,
    totalSpentAllMad: Double,
  ) {
    val net = totalCollectedAllMad - totalSpentAllMad
    val message =
      buildString {
        appendLine("📊 *التقرير المالي - ${building.name} (${building.city})*")
        appendLine("📅 الشهر / Période : *$monthKey*")
        appendLine("✅ الشقق المؤداة : *$paidCount / $totalUnits*")
        appendLine("💵 محصل الشهر : *${String.format(Locale.US, "%.0f", monthCollectedMad)} / ${String.format(Locale.US, "%.0f", monthExpectedMad)} MAD*")
        appendLine("🏦 مجموع المحصل : ${String.format(Locale.US, "%.0f", totalCollectedAllMad)} MAD")
        appendLine("🧾 مجموع المصاريف : ${String.format(Locale.US, "%.0f", totalSpentAllMad)} MAD")
        appendLine("💼 الرصيد الصافي : *${String.format(Locale.US, "%.0f", net)} MAD*")
        append("🛡️ صندوق الاحتياط : ${String.format(Locale.US, "%.0f", building.reserveFundBalance)} MAD")
      }
    shareTextPreferWhatsApp(context, message)
  }

  fun sendBulkUnpaidReminders(
    context: Context,
    building: Building,
    monthKey: String,
    unpaidPhones: List<String>,
  ) {
    val message =
      buildString {
        appendLine("📢 *تذكير بأداء واجب السانديك - ${building.name} (${building.city})*")
        appendLine("📅 شهر: *$monthKey*")
        appendLine("المرجو من السكان غير المؤدين تسوية واجب السانديك الشهري في أقرب وقت.")
        append("Rappel de cotisation syndic ($monthKey) pour ${building.name}. Merci.")
      }
    if (unpaidPhones.isNotEmpty()) {
      openSmsBroadcast(context, unpaidPhones, message)
    } else {
      shareTextPreferWhatsApp(context, message)
    }
  }

  fun shareTextMessage(context: Context, message: String) {
    shareTextPreferWhatsApp(context, message)
  }

  fun sendWhatsAppAuthCode(
    context: Context,
    phone: String,
    code: String,
    buildingName: String? = null,
  ) {
    val message =
      buildString {
        appendLine("🔐 *سانديك العمارة — رمز الدخول والتحقق للساكن*")
        if (!buildingName.isNullOrBlank()) {
          appendLine("🏢 العمارة / Résidence : *$buildingName*")
        }
        appendLine("📱 رقم الهاتف: *$phone*")
        appendLine("✅ كود التحقق (OTP): *$code*")
        appendLine("---")
        append("Code de vérification WhatsApp Syndic Al-Imara : *$code*")
      }
    if (phone.isNotBlank()) {
      openWhatsAppChat(context, phone, message)
    } else {
      shareTextPreferWhatsApp(context, message)
    }
  }

  fun sendWhatsAppRecoveryCredentials(
    context: Context,
    targetPhone: String,
    oldPhone: String,
    recoveryCode: String,
    tempPassword: String,
    residentName: String,
  ) {
    val message =
      buildString {
        appendLine("♻️ *استرجاع حساب وبيانات الساكن — سانديك العمارة*")
        if (residentName.isNotBlank()) {
          appendLine("👤 الساكن: $residentName")
        }
        appendLine("📞 الهاتف المسجل: $oldPhone")
        appendLine("🔑 كود الاسترجاع: *$recoveryCode*")
        appendLine("🛡️ كلمة المرور المؤقتة: *$tempPassword*")
        appendLine("يمكنك استخدام هذا الكود لاسترجاع بيانات عماراتك أو ربط رقم هاتفك الجديد.")
        appendLine("---")
        append("Récupération de compte Syndic ($oldPhone) — Code: *$recoveryCode* | Mot de passe temp: *$tempPassword*")
      }
    val dialPhone = targetPhone.ifBlank { oldPhone }
    if (dialPhone.isNotBlank()) {
      openWhatsAppChat(context, dialPhone, message)
    } else {
      shareTextPreferWhatsApp(context, message)
    }
  }

  fun sendPhoneApprovedNoticeViaWhatsApp(
    context: Context,
    building: Building,
    residentName: String,
    residentPhone: String,
    apartmentLabel: String,
  ) {
    val deepLink = "https://syndicmaroc.app/join/${building.inviteCode}"
    val message =
      buildString {
        appendLine("✅ *تم تفعيل رقم هاتفك للانضمام التلقائي — ${building.name}*")
        if (residentName.isNotBlank()) appendLine("مرحباً $residentName،")
        appendLine("🏠 الشقة: $apartmentLabel")
        appendLine("🔑 رمز العمارة: *${building.inviteCode}*")
        appendLine("🔗 رابط الانضمام المباشر: $deepLink")
        append("ادخل برقم هاتفك المسجل (${residentPhone}) وسيتم قبول انضمامك تلقائياً.")
      }
    if (residentPhone.isNotBlank()) {
      openWhatsAppChat(context, residentPhone, message)
    } else {
      shareTextPreferWhatsApp(context, message)
    }
  }

  private fun shareTextPreferWhatsApp(context: Context, message: String) {
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
      }
    launchPreferWhatsApp(context, intent, "مشاركة / Partager")
  }

  private fun launchPreferWhatsApp(context: Context, baseIntent: Intent, chooserTitle: String) {
    val pm = context.packageManager
    val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
    val targetPkg =
      whatsappPackages.firstOrNull { pkg ->
        try {
          pm.getPackageInfo(pkg, 0)
          true
        } catch (_: Exception) {
          false
        }
      }
    if (targetPkg != null) {
      baseIntent.setPackage(targetPkg)
      try {
        context.startActivity(baseIntent)
        return
      } catch (_: Exception) {
        baseIntent.setPackage(null)
      }
    }
    context.startActivity(Intent.createChooser(baseIntent, chooserTitle))
  }

  private fun renderBilingualReceiptBitmap(
    building: Building,
    payment: CotisationPayment,
  ): Bitmap {
    val width = 920
    val height = 1120
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Warm limestone background
    canvas.drawColor(Color.parseColor("#FAF8F4"))

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0C635A") }
    canvas.drawRect(0f, 0f, width.toFloat(), 230f, headerPaint)

    val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0A954") }
    canvas.drawRect(0f, 230f, width.toFloat(), 242f, goldPaint)

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val subHeaderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D0F0EA")
        textSize = 26f
        textAlign = Paint.Align.CENTER
      }

    canvas.drawText("وصل الأداء | REÇU DE COTISATION", width / 2f, 92f, titlePaint)
    canvas.drawText("${building.name} • ${building.city}", width / 2f, 148f, subHeaderPaint)
    canvas.drawText("Loi 18-00 Copropriété • قانون الملكية المشتركة 18-00", width / 2f, 195f, subHeaderPaint)

    // Receipt card
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DAD3C7")
        style = Paint.Style.STROKE
        strokeWidth = 3f
      }
    val cardRect = RectF(48f, 280f, width - 48f, height - 120f)
    canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)
    canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

    val labelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#54615E")
        textSize = 26f
        textAlign = Paint.Align.LEFT
      }
    val valuePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1F1E")
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
      }
    val dividerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EFEAE0")
        strokeWidth = 2f
      }

    val receiptNo = payment.receiptNumber.ifBlank { "REC-${payment.monthKey}-${payment.apartmentId}" }
    val methodLabel =
      when (payment.paymentMethod) {
        "TRANSFER" -> "تحويل بنكي / Virement"
        "CHECK" -> "شيك / Chèque"
        else -> "نقداً / Espèces"
      }
    val rows =
      listOf(
        "N° Reçu / رقم الوصل" to receiptNo,
        "Lot / الشقة" to payment.apartmentLabel,
        "Résident / المؤدي" to payment.payerName.ifBlank { "-" },
        "Période / الشهر" to payment.monthKey,
        "Mode / طريقة الأداء" to methodLabel,
        "Date / التاريخ" to payment.paidAtLabel.ifBlank { payment.monthKey },
      )

    var y = 360f
    for ((label, value) in rows) {
      canvas.drawText(label, 90f, y, labelPaint)
      canvas.drawText(value, width - 90f, y, valuePaint)
      y += 28f
      canvas.drawLine(90f, y, width - 90f, y, dividerPaint)
      y += 58f
    }

    // Highlighted amount badge
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2F5EB") }
    val badgeRect = RectF(90f, y, width - 90f, y + 125f)
    canvas.drawRoundRect(badgeRect, 20f, 20f, badgePaint)

    val amountLabelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B8755")
        textSize = 26f
        textAlign = Paint.Align.CENTER
      }
    val amountValuePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0C635A")
        textSize = 48f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText("المبلغ المؤدى / MONTANT PAYÉ", width / 2f, y + 46f, amountLabelPaint)
    canvas.drawText(
      "${String.format(Locale.US, "%.0f", payment.amountMad)} MAD درهم",
      width / 2f,
      y + 102f,
      amountValuePaint,
    )

    val footerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7775")
        textSize = 22f
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(
      "سانديك العمارة • Document officiel généré numériquement",
      width / 2f,
      height - 52f,
      footerPaint,
    )

    return bitmap
  }
}
