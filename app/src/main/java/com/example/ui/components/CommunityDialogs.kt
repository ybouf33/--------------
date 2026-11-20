package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.Apartment
import com.example.data.model.BroadcastAudience
import com.example.data.model.Building
import com.example.data.model.CotisationPayment
import com.example.data.model.IssueCategory
import com.example.data.model.IssueUrgency
import com.example.data.model.OccupancyType
import com.example.data.model.PaymentStatus
import com.example.data.model.WorkCategory
import com.example.data.util.ImageUtils
import com.example.data.util.ReceiptGenerator
import com.example.ui.i18n.AppStrings
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostAnnouncementDialog(
  s: AppStrings,
  buildingName: String,
  onDismiss: () -> Unit,
  onSubmit: (String, String, AnnouncementCategory, BroadcastAudience, Boolean) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var body by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(AnnouncementCategory.GENERAL) }
  var audience by remember { mutableStateOf(BroadcastAudience.ALL) }
  var pinned by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${s.broadcastMessageAction} • $buildingName") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(s.issueTitleLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("announcement_title_input"),
        )
        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          label = { Text(s.issueDescLabel) },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("announcement_body_input"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          AnnouncementCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(announcementCategoryLabel(cat, s)) },
            )
          }
        }
        Text(
          text = s.audienceLabel,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          BroadcastAudience.entries.forEach { aud ->
            FilterChip(
              selected = audience == aud,
              onClick = { audience = aud },
              label = { Text(audienceLabel(aud, s)) },
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = pinned, onCheckedChange = { pinned = it })
          Text(s.pinnedNotice, style = MaterialTheme.typography.bodyMedium)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(title, body, category, audience, pinned) },
        enabled = title.trim().length >= 2 && body.trim().length >= 2,
        modifier = Modifier.testTag("confirm_post_announcement"),
      ) {
        Text(s.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiffuseAnnouncementDialog(
  s: AppStrings,
  building: Building,
  announcement: Announcement,
  apartments: List<Apartment>,
  payments: List<CotisationPayment>,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val currentMonthKey =
    remember {
      val cal = Calendar.getInstance()
      "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

  var audienceFilter by remember(announcement.id) { mutableStateOf(announcement.audienceEnum()) }
  val paidApartmentIdsThisMonth =
    remember(payments, currentMonthKey) {
      payments
        .filter { it.monthKey == currentMonthKey && it.statusEnum() == PaymentStatus.PAID }
        .map { it.apartmentId }
        .toSet()
    }

  val targetApartments =
    remember(apartments, audienceFilter, paidApartmentIdsThisMonth) {
      when (audienceFilter) {
        BroadcastAudience.ALL -> apartments
        BroadcastAudience.UNPAID -> apartments.filter { it.id !in paidApartmentIdsThisMonth }
        BroadcastAudience.OWNERS -> apartments.filter { it.occupancyEnum() == OccupancyType.OWNER }
      }
    }
  val apartmentsWithPhones = remember(targetApartments) { targetApartments.filter { it.residentPhone.isNotBlank() } }
  val formattedMessage =
    remember(building, announcement) {
      buildString {
        appendLine("📢 *${building.name} (${building.city})*")
        appendLine("*${announcement.title}*")
        appendLine()
        appendLine(announcement.body)
        appendLine()
        append("— ${announcement.authorName.ifBlank { s.roleSyndic }}")
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${s.diffuseSheetTitle} • ${building.name}") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = s.audienceLabel,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          BroadcastAudience.entries.forEach { aud ->
            FilterChip(
              selected = audienceFilter == aud,
              onClick = { audienceFilter = aud },
              label = { Text(audienceLabel(aud, s)) },
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = formattedMessage,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp),
          )
        }

        Button(
          onClick = { ReceiptGenerator.shareTextMessage(context, formattedMessage) },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("diffuse_whatsapp_group_button"),
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(s.diffuseWhatsappGroup)
        }

        OutlinedButton(
          onClick = {
            ReceiptGenerator.openSmsBroadcast(
              context = context,
              phones = apartmentsWithPhones.map { it.residentPhone },
              message = formattedMessage,
            )
          },
          enabled = apartmentsWithPhones.isNotEmpty(),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("diffuse_sms_group_button"),
        ) {
          Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text("${s.diffuseSmsGroup} (${apartmentsWithPhones.size})")
        }

        HorizontalDivider()

        if (apartmentsWithPhones.isEmpty()) {
          Text(
            text = s.noPhonesForBroadcast,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          apartmentsWithPhones.forEach { apt ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = apt.residentName.ifBlank { apt.label },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${apt.label} • ${apt.residentPhone}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                OutlinedButton(
                  onClick = {
                    ReceiptGenerator.openWhatsAppChat(context, apt.residentPhone, formattedMessage)
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Text(s.whatsappAction, style = MaterialTheme.typography.labelMedium)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

@Composable
fun CreatePollDialog(
  s: AppStrings,
  buildingName: String,
  onDismiss: () -> Unit,
  onSubmit: (String, String, List<String>) -> Unit,
) {
  var question by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  val options = remember { mutableStateListOf("", "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${s.createPollAction} • $buildingName") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = question,
          onValueChange = { question = it },
          label = { Text(s.pollQuestionLabel) },
          modifier = Modifier.fillMaxWidth().testTag("poll_question_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text(s.expenseNotesLabel) },
          modifier = Modifier.fillMaxWidth(),
        )
        options.forEachIndexed { idx, opt ->
          OutlinedTextField(
            value = opt,
            onValueChange = { options[idx] = it },
            label = { Text("${s.pollOptionHint} ${idx + 1}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        if (options.size < 6) {
          TextButton(onClick = { options.add("") }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(s.addPollOption)
          }
        }
      }
    },
    confirmButton = {
      val validOptions = options.map { it.trim() }.filter { it.isNotEmpty() }
      Button(
        onClick = { onSubmit(question, description, validOptions) },
        enabled = question.trim().length >= 3 && validOptions.size >= 2,
      ) {
        Text(s.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddWorkDialog(
  s: AppStrings,
  buildingName: String,
  onDismiss: () -> Unit,
  onSubmit: (
    title: String,
    category: WorkCategory,
    contractorName: String,
    contractorPhone: String,
    estimatedCostMad: Double,
    dueDateLabel: String,
    notes: String,
  ) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(WorkCategory.MAINTENANCE) }
  var contractorName by remember { mutableStateOf("") }
  var contractorPhone by remember { mutableStateOf("") }
  var costText by remember { mutableStateOf("") }
  var dueDate by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${s.addWorkAction} • $buildingName") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(s.workTitleLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("work_title_input"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          WorkCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(workCategoryLabel(cat, s)) },
            )
          }
        }
        OutlinedTextField(
          value = contractorName,
          onValueChange = { contractorName = it },
          label = { Text(s.contractorNameLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("work_contractor_input"),
        )
        OutlinedTextField(
          value = contractorPhone,
          onValueChange = { contractorPhone = it },
          label = { Text(s.contractorPhoneLabel) },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = costText,
            onValueChange = { costText = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text(s.estimatedCostLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("work_cost_input"),
          )
          OutlinedTextField(
            value = dueDate,
            onValueChange = { dueDate = it },
            label = { Text(s.dueDateLabel) },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(s.expenseNotesLabel) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmit(
            title,
            category,
            contractorName,
            contractorPhone,
            costText.toDoubleOrNull() ?: 0.0,
            dueDate,
            notes,
          )
        },
        enabled = title.trim().length >= 2 && contractorName.trim().length >= 2,
        modifier = Modifier.testTag("confirm_add_work_button"),
      ) {
        Text(s.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportIssueDialog(
  s: AppStrings,
  onDismiss: () -> Unit,
  onSubmit: (String, String, IssueCategory, IssueUrgency, String?) -> Unit,
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(IssueCategory.PLUMBING) }
  var urgency by remember { mutableStateOf(IssueUrgency.NORMAL) }
  var photoBase64 by remember { mutableStateOf<String?>(null) }

  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        photoBase64 = ImageUtils.uriToBoundedBase64(context, uri)
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(s.reportIssueAction) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(s.issueTitleLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("issue_title_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text(s.issueDescLabel) },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("issue_desc_input"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IssueCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(issueCategoryLabel(cat, s)) },
            )
          }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IssueUrgency.entries.forEach { u ->
            FilterChip(
              selected = urgency == u,
              onClick = { urgency = u },
              label = { Text(issueUrgencyLabel(u, s)) },
            )
          }
        }
        OutlinedButton(
          onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Default.PhotoCamera, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (photoBase64 != null) s.invoiceAttached else s.attachPhotoOptional)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(title, description, category, urgency, photoBase64) },
        enabled = title.trim().length >= 2 && description.trim().length >= 2,
        modifier = Modifier.testTag("confirm_report_issue"),
      ) {
        Text(s.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}
