package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Building
import com.example.data.model.BuildingIssue
import com.example.data.model.BuildingMember
import com.example.data.model.BuildingWork
import com.example.data.model.IssueCategory
import com.example.data.model.IssueStatus
import com.example.data.model.IssueUrgency
import com.example.data.model.WorkStatus
import com.example.data.util.ReceiptGenerator
import com.example.ui.components.AddWorkDialog
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.FullScreenBase64Dialog
import com.example.ui.components.ReportIssueDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.formatMad
import com.example.ui.components.issueCategoryLabel
import com.example.ui.components.issueStatusLabel
import com.example.ui.components.issueUrgencyLabel
import com.example.ui.components.workCategoryLabel
import com.example.ui.components.workStatusLabel
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors
import com.example.ui.viewmodel.SyndicController

@Composable
fun IssuesScreen(
  s: AppStrings,
  building: Building,
  myMember: BuildingMember,
  issues: List<BuildingIssue>,
  works: List<BuildingWork>,
  controller: SyndicController,
) {
  val isAdmin = myMember.isAdmin()
  val semantic = LocalSyndicColors.current
  var subTab by remember { mutableIntStateOf(0) }
  var selectedCategory by remember { mutableStateOf<IssueCategory?>(null) }
  var showReportDialog by remember { mutableStateOf(false) }
  var showAddWorkDialog by remember { mutableStateOf(false) }
  var activeIssueForDetail by remember { mutableStateOf<BuildingIssue?>(null) }
  var fullScreenPhoto by remember { mutableStateOf<String?>(null) }

  val filteredIssues =
    remember(issues, selectedCategory) {
      if (selectedCategory == null) issues
      else issues.filter { it.categoryEnum() == selectedCategory }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Separated A3mal Sub-tabs: Maintenance Works vs Resident Issues
    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = subTab == 0,
          onClick = { subTab = 0 },
          shape = SegmentedButtonDefaults.itemShape(0, 2),
          modifier = Modifier.testTag("works_subtab_projects"),
        ) {
          Text("${s.subTabWorksList} (${works.size})", maxLines = 1)
        }
        SegmentedButton(
          selected = subTab == 1,
          onClick = { subTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(1, 2),
          modifier = Modifier.testTag("works_subtab_issues"),
        ) {
          Text("${s.subTabResidentIssues} (${issues.size})", maxLines = 1)
        }
      }
    }

    if (subTab == 0) {
      if (isAdmin) {
        item {
          Button(
            onClick = { showAddWorkDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_work_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(s.addWorkAction)
          }
        }
      }

      if (works.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Default.Construction,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
              )
              Text(
                text = s.noWorksYet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(works, key = { "work_${it.id}" }) { work ->
          WorkOrderCard(
            work = work,
            buildingName = building.name,
            s = s,
            isAdmin = isAdmin,
            onAdvanceStatus = { next -> controller.updateWorkStatus(work.id, next) },
          )
        }
      }
    } else {
      item {
        Button(
          onClick = { showReportDialog = true },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("report_issue_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(s.reportIssueAction)
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedCategory == null,
            onClick = { selectedCategory = null },
            label = { Text(s.allCategories) },
          )
          IssueCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(issueCategoryLabel(cat, s)) },
            )
          }
        }
      }

      if (filteredIssues.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_empty_issues),
                contentDescription = stringResource(R.string.cd_empty_issues),
                modifier = Modifier.size(140.dp).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
              )
              Text(s.noIssuesTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                s.noIssuesSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(filteredIssues, key = { it.id }) { issue ->
          val upvotes by
            remember(building.id, issue.id) {
              controller.observeIssueUpvotes(building.id, issue.id)
            }.collectAsStateWithLifecycle(initialValue = emptyList())
          val hasUpvoted = upvotes.any { it.uid == controller.currentUserId }

          IssueCard(
            issue = issue,
            s = s,
            hasUpvoted = hasUpvoted,
            upvoteCount = maxOf(issue.upvoteCount, upvotes.size),
            onToggleUpvote = { controller.toggleIssueUpvote(issue.id) },
            onOpenDetail = { activeIssueForDetail = issue },
            onPreviewPhoto = { fullScreenPhoto = issue.photoBase64 },
          )
        }
      }
    }
  }

  if (showAddWorkDialog) {
    AddWorkDialog(
      s = s,
      buildingName = building.name,
      onDismiss = { showAddWorkDialog = false },
      onSubmit = { title, cat, contractorName, contractorPhone, cost, dueDate, notes ->
        controller.createWork(title, cat, contractorName, contractorPhone, cost, dueDate, notes)
        showAddWorkDialog = false
      },
    )
  }

  if (showReportDialog) {
    ReportIssueDialog(
      s = s,
      onDismiss = { showReportDialog = false },
      onSubmit = { title, desc, cat, urgency, photo ->
        controller.reportIssue(title, desc, cat, urgency, photo)
        showReportDialog = false
      },
    )
  }

  val detailIssue = activeIssueForDetail?.let { selected ->
    issues.firstOrNull { it.id == selected.id } ?: selected
  }
  if (detailIssue != null) {
    IssueDetailDialog(
      s = s,
      buildingId = building.id,
      issue = detailIssue,
      isAdmin = isAdmin,
      controller = controller,
      onDismiss = { activeIssueForDetail = null },
    )
  }

  val photo = fullScreenPhoto
  if (!photo.isNullOrBlank()) {
    FullScreenBase64Dialog(
      base64 = photo,
      title = s.tabWorks,
      onDismiss = { fullScreenPhoto = null },
    )
  }
}

@Composable
private fun WorkOrderCard(
  work: BuildingWork,
  buildingName: String,
  s: AppStrings,
  isAdmin: Boolean,
  onAdvanceStatus: (WorkStatus) -> Unit,
) {
  val context = LocalContext.current
  val semantic = LocalSyndicColors.current
  val st = work.statusEnum()
  val (stBg, stFg) =
    when (st) {
      WorkStatus.PLANNED -> semantic.pendingContainer to semantic.pendingText
      WorkStatus.IN_PROGRESS ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
      WorkStatus.COMPLETED -> semantic.paidContainer to semantic.paidText
    }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, semantic.hairline),
    modifier = Modifier.fillMaxWidth().testTag("work_card_${work.id}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusPill(
          text = workCategoryLabel(work.categoryEnum(), s),
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        StatusPill(
          text = workStatusLabel(st, s),
          containerColor = stBg,
          contentColor = stFg,
        )
      }

      Text(work.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
        text =
          listOf(work.contractorName, work.dueDateLabel)
            .filter { it.isNotBlank() }
            .joinToString(" • "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (work.estimatedCostMad > 0.0) {
        Text(
          text = "${s.estimatedCostLabel}: ${formatMad(work.estimatedCostMad, s)}",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
      }

      if (work.notes.isNotBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = work.notes,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (work.contractorPhone.isNotBlank()) {
          OutlinedButton(
            onClick = { ReceiptGenerator.openPhoneDialer(context, work.contractorPhone) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          ) {
            Icon(Icons.Default.Call, contentDescription = s.callAction, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(s.callAction, style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = {
              ReceiptGenerator.openWhatsAppChat(
                context = context,
                rawPhone = work.contractorPhone,
                message = "السلام عليكم ${work.contractorName}، بخصوص أشغال ${work.title} ($buildingName)",
              )
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          ) {
            Icon(Icons.Default.Share, contentDescription = s.whatsappAction, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(s.whatsappAction, style = MaterialTheme.typography.labelSmall)
          }
        }
        Spacer(Modifier.weight(1f))
        if (isAdmin && st != WorkStatus.COMPLETED) {
          val nextStatus =
            if (st == WorkStatus.PLANNED) WorkStatus.IN_PROGRESS else WorkStatus.COMPLETED
          Button(
            onClick = { onAdvanceStatus(nextStatus) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.testTag("advance_work_${work.id}"),
          ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(workStatusLabel(nextStatus, s), style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
  }
}

@Composable
private fun IssueCard(
  issue: BuildingIssue,
  s: AppStrings,
  hasUpvoted: Boolean,
  upvoteCount: Int,
  onToggleUpvote: () -> Unit,
  onOpenDetail: () -> Unit,
  onPreviewPhoto: () -> Unit,
) {
  val semantic = LocalSyndicColors.current
  val (stBg, stFg) =
    when (issue.statusEnum()) {
      IssueStatus.UNDER_REVIEW -> semantic.pendingContainer to semantic.pendingText
      IssueStatus.IN_PROGRESS ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
      IssueStatus.RESOLVED -> semantic.paidContainer to semantic.paidText
    }
  val (urgBg, urgFg) =
    when (issue.urgencyEnum()) {
      IssueUrgency.NORMAL ->
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
      IssueUrgency.URGENT -> semantic.pendingContainer to semantic.pendingText
      IssueUrgency.EMERGENCY -> semantic.overdueContainer to semantic.overdueText
    }

  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail),
    border = BorderStroke(1.dp, semantic.hairline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          StatusPill(
            text = issueCategoryLabel(issue.categoryEnum(), s),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          StatusPill(
            text = issueUrgencyLabel(issue.urgencyEnum(), s),
            containerColor = urgBg,
            contentColor = urgFg,
          )
        }
        StatusPill(
          text = issueStatusLabel(issue.statusEnum(), s),
          containerColor = stBg,
          contentColor = stFg,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(issue.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(issue.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
          Text(
            text = listOf(issue.authorName, issue.apartmentLabel).filter { it.isNotBlank() }.joinToString(" • "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (issue.photoBase64.isNotBlank()) {
          Base64Thumbnail(
            base64 = issue.photoBase64,
            contentDescription = issue.title,
            modifier = Modifier.size(72.dp),
            onClick = onPreviewPhoto,
          )
        }
      }

      if (issue.repairCostNote.isNotBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = issue.repairCostNote,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(onClick = onToggleUpvote) {
          Icon(
            imageVector = if (hasUpvoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            contentDescription = s.upvoteSameProblem,
            modifier = Modifier.size(16.dp),
            tint = if (hasUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.width(6.dp))
          Text("${s.upvoteSameProblem} ($upvoteCount)", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = onOpenDetail) {
          Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(s.commentsLabel)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueDetailDialog(
  s: AppStrings,
  buildingId: String,
  issue: BuildingIssue,
  isAdmin: Boolean,
  controller: SyndicController,
  onDismiss: () -> Unit,
) {
  val comments by
    remember(buildingId, issue.id) { controller.observeIssueComments(buildingId, issue.id) }
      .collectAsStateWithLifecycle(initialValue = emptyList())
  var commentText by remember { mutableStateOf("") }
  var selectedStatus by remember(issue.status) { mutableStateOf(issue.statusEnum()) }
  var repairNote by remember(issue.repairCostNote) { mutableStateOf(issue.repairCostNote) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(issue.title) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(issue.description, style = MaterialTheme.typography.bodyMedium)

        if (isAdmin) {
          HorizontalDivider()
          Text(s.updateStatusTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IssueStatus.entries.forEach { st ->
              FilterChip(
                selected = selectedStatus == st,
                onClick = { selectedStatus = st },
                label = { Text(issueStatusLabel(st, s)) },
              )
            }
          }
          OutlinedTextField(
            value = repairNote,
            onValueChange = { repairNote = it },
            label = { Text(s.repairCostNoteLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          Button(
            onClick = { controller.updateIssueStatus(issue.id, selectedStatus, repairNote) },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(s.saveAction)
          }
        }

        HorizontalDivider()
        Text(s.commentsLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        comments.forEach { c ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = listOf(c.authorName, c.apartmentLabel).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              Text(c.text, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            placeholder = { Text(s.addCommentHint) },
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              if (commentText.isNotBlank()) {
                controller.addIssueComment(issue.id, commentText)
                commentText = ""
              }
            },
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = s.saveAction)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}
