package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Apartment
import com.example.data.model.Building
import com.example.data.model.CotisationPayment
import com.example.data.model.ExpenseCategory
import com.example.data.model.OccupancyType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.UnitType
import com.example.data.util.ImageUtils
import com.example.data.util.ReceiptGenerator
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.expenseCategoryLabel
import com.example.ui.components.paymentMethodLabel
import com.example.ui.components.paymentStatusLabel
import com.example.ui.components.unitTypeLabel
import com.example.ui.i18n.AppStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentActionDialog(
  s: AppStrings,
  building: Building,
  apartment: Apartment,
  monthKey: String,
  existingPayment: CotisationPayment?,
  isAdmin: Boolean,
  onDismiss: () -> Unit,
  onSavePayment: (Double, PaymentStatus, String, PaymentMethod) -> Unit,
  onSaveApartmentFee: (Double, UnitType, String, String, String, OccupancyType) -> Unit,
) {
  val context = LocalContext.current
  var status by remember { mutableStateOf(existingPayment?.statusEnum() ?: PaymentStatus.PAID) }
  var method by remember { mutableStateOf(existingPayment?.methodEnum() ?: PaymentMethod.CASH) }
  var amountText by remember {
    mutableStateOf((existingPayment?.amountMad ?: apartment.monthlyFee).toInt().toString())
  }
  var payerName by remember {
    mutableStateOf(existingPayment?.payerName?.ifBlank { apartment.residentName } ?: apartment.residentName)
  }

  var editingUnitFee by remember { mutableStateOf(false) }
  var unitFeeText by remember { mutableStateOf(apartment.monthlyFee.toInt().toString()) }
  var unitType by remember { mutableStateOf(apartment.unitTypeEnum()) }
  var unitLabel by remember { mutableStateOf(apartment.label) }
  var residentPhone by remember { mutableStateOf(apartment.residentPhone) }
  var occupancyType by remember { mutableStateOf(apartment.occupancyEnum()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${apartment.label} • $monthKey") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (existingPayment != null && existingPayment.statusEnum() == PaymentStatus.PAID) {
          OutlinedButton(
            onClick = {
              ReceiptGenerator.sharePaymentReceiptViaWhatsApp(context, building, existingPayment)
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.shareReceiptWhatsapp)
          }
          HorizontalDivider()
        } else if (isAdmin) {
          OutlinedButton(
            onClick = {
              ReceiptGenerator.sendPaymentReminder(
                context = context,
                building = building,
                apartmentLabel = apartment.label,
                residentName = apartment.residentName,
                residentPhone = apartment.residentPhone,
                monthKey = monthKey,
                amountMad = existingPayment?.amountMad ?: apartment.monthlyFee,
              )
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.sendReminderAction)
          }
          HorizontalDivider()
        }

        if (isAdmin) {
          if (!editingUnitFee) {
            Text(s.recordPaymentTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              PaymentStatus.entries.forEach { st ->
                FilterChip(
                  selected = status == st,
                  onClick = { status = st },
                  label = { Text(paymentStatusLabel(st, s)) },
                )
              }
            }
            OutlinedTextField(
              value = amountText,
              onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
              label = { Text("${s.expenseAmountLabel}") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = payerName,
              onValueChange = { payerName = it },
              label = { Text(s.payerNameLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              PaymentMethod.entries.forEach { m ->
                FilterChip(
                  selected = method == m,
                  onClick = { method = m },
                  label = { Text(paymentMethodLabel(m, s)) },
                )
              }
            }
            TextButton(onClick = { editingUnitFee = true }) {
              Text(s.editApartmentFee)
            }
          } else {
            Text(s.editApartmentFee, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
              value = unitLabel,
              onValueChange = { unitLabel = it },
              label = { Text(s.selectApartmentLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              UnitType.entries.forEach { t ->
                FilterChip(
                  selected = unitType == t,
                  onClick = { unitType = t },
                  label = { Text(unitTypeLabel(t, s)) },
                )
              }
            }
            OutlinedTextField(
              value = unitFeeText,
              onValueChange = { unitFeeText = it.filter { ch -> ch.isDigit() || ch == '.' } },
              label = { Text(s.defaultFeeLabel) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = payerName,
              onValueChange = { payerName = it },
              label = { Text(s.residentNameLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = residentPhone,
              onValueChange = { residentPhone = it },
              label = { Text(s.residentPhoneLabel) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilterChip(
                selected = occupancyType == OccupancyType.OWNER,
                onClick = { occupancyType = OccupancyType.OWNER },
                label = { Text(s.occupancyOwner) },
              )
              FilterChip(
                selected = occupancyType == OccupancyType.TENANT,
                onClick = { occupancyType = OccupancyType.TENANT },
                label = { Text(s.occupancyTenant) },
              )
            }
          }
        } else {
          Text(
            text = "${s.statusWait}: ${paymentStatusLabel(existingPayment?.statusEnum() ?: PaymentStatus.PENDING, s)}",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    },
    confirmButton = {
      if (isAdmin) {
        Button(
          onClick = {
            if (!editingUnitFee) {
              onSavePayment(
                amountText.toDoubleOrNull() ?: apartment.monthlyFee,
                status,
                payerName,
                method,
              )
            } else {
              onSaveApartmentFee(
                unitFeeText.toDoubleOrNull() ?: apartment.monthlyFee,
                unitType,
                unitLabel,
                payerName,
                residentPhone,
                occupancyType,
              )
            }
          },
        ) {
          Text(s.savePaymentAction)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
  s: AppStrings,
  onDismiss: () -> Unit,
  onSubmit: (String, ExpenseCategory, Double, String, String, String) -> Unit,
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(ExpenseCategory.CLEANING) }
  var amountText by remember { mutableStateOf("") }
  var vendorName by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var invoiceBase64 by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        invoiceBase64 = ImageUtils.uriToBoundedBase64(context, uri)
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(s.addExpenseAction) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(s.expenseTitleLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ExpenseCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(expenseCategoryLabel(cat, s)) },
            )
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(s.expenseAmountLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = vendorName,
          onValueChange = { vendorName = it },
          label = { Text(s.expenseVendorLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(s.expenseNotesLabel) },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Default.PhotoCamera, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (invoiceBase64 != null) s.invoiceAttached else s.attachInvoiceMandatory)
        }
        val b64 = invoiceBase64
        if (b64 != null) {
          Base64Thumbnail(
            base64 = b64,
            contentDescription = s.viewInvoiceAction,
            modifier = Modifier.fillMaxWidth().height(110.dp),
            onClick = {},
          )
        }
      }
    },
    confirmButton = {
      val amount = amountText.toDoubleOrNull() ?: 0.0
      val img = invoiceBase64
      Button(
        onClick = {
          if (img != null) {
            onSubmit(title, category, amount, vendorName, img, notes)
          }
        },
        enabled = title.trim().length >= 2 && amount > 0.0 && !img.isNullOrBlank(),
      ) {
        Text(s.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}
