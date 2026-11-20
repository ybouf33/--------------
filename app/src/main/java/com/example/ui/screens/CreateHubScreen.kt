package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Apartment
import com.example.data.model.Building
import com.example.data.model.BuildingMember
import com.example.ui.components.AddWorkDialog
import com.example.ui.components.AssignResidentUnitDialog
import com.example.ui.components.CreatePollDialog
import com.example.ui.components.ExecutiveGradientCard
import com.example.ui.components.PostAnnouncementDialog
import com.example.ui.components.ReportIssueDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors
import com.example.ui.viewmodel.SyndicController

@Composable
fun CreateHubScreen(
  s: AppStrings,
  building: Building,
  myMember: BuildingMember,
  apartments: List<Apartment>,
  controller: SyndicController,
  onOpenCreateBuilding: () -> Unit,
  onJoinWithCode: (() -> Unit)?,
  onNavigateToTab: (Int) -> Unit,
) {
  val isAdmin = myMember.isAdmin()
  val semantic = LocalSyndicColors.current

  var showAddUnit by remember { mutableStateOf(false) }
  var showAddWork by remember { mutableStateOf(false) }
  var showAddExpense by remember { mutableStateOf(false) }
  var showBroadcast by remember { mutableStateOf(false) }
  var showCreatePoll by remember { mutableStateOf(false) }
  var showReportIssue by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      ExecutiveGradientCard {
        Text(
          text = s.createHubTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          color = semantic.heroContent,
        )
        Text(
          text = s.createHubSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = semantic.heroSubtleContent,
        )
        Surface(
          color = semantic.heroChipBg,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Default.Apartment, contentDescription = null, tint = semantic.goldAccent, modifier = Modifier.size(18.dp))
            Text(
              text = "${s.selectTargetBuilding} ${building.name} (${building.city})",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = semantic.heroContent,
            )
          }
        }
      }
    }

    // 1. Create a New 3imara (Portfolio level)
    item {
      CreateActionCard(
        title = s.quickCreateBuildingCard,
        subtitle = s.createNewBuildingAction,
        icon = Icons.Default.Apartment,
        accent = MaterialTheme.colorScheme.primary,
        tag = "create_hub_new_building",
        onClick = onOpenCreateBuilding,
      )
    }

    if (isAdmin) {
      // 2. Add Apartment/Shop & Assign Resident Phone for Active 3imara
      item {
        CreateActionCard(
          title = s.quickCreateUnitCard,
          subtitle = building.name,
          icon = Icons.Default.PersonAdd,
          accent = MaterialTheme.colorScheme.secondary,
          tag = "create_hub_new_unit",
          onClick = { showAddUnit = true },
        )
      }

      // 3. Create Building Work Order (A3mal) for Active 3imara
      item {
        CreateActionCard(
          title = s.quickCreateWorkCard,
          subtitle = building.name,
          icon = Icons.Default.Construction,
          accent = MaterialTheme.colorScheme.tertiary,
          tag = "create_hub_new_work",
          onClick = { showAddWork = true },
        )
      }

      // 4. Record Expense with Mandatory Invoice for Active 3imara
      item {
        CreateActionCard(
          title = s.quickCreateExpenseCard,
          subtitle = building.name,
          icon = Icons.Default.ReceiptLong,
          accent = semantic.overdueText,
          tag = "create_hub_new_expense",
          onClick = { showAddExpense = true },
        )
      }

      // 5. Broadcast Message to Active 3imara Residents
      item {
        CreateActionCard(
          title = s.quickCreateBroadcastCard,
          subtitle = building.name,
          icon = Icons.Default.Campaign,
          accent = MaterialTheme.colorScheme.primary,
          tag = "create_hub_new_broadcast",
          onClick = { showBroadcast = true },
        )
      }

      // 6. Create Poll for General Assembly in Active 3imara
      item {
        CreateActionCard(
          title = s.quickCreatePollCard,
          subtitle = building.name,
          icon = Icons.Default.HowToVote,
          accent = MaterialTheme.colorScheme.secondary,
          tag = "create_hub_new_poll",
          onClick = { showCreatePoll = true },
        )
      }
    }

    // 7. Report Issue in Active 3imara (All members)
    item {
      CreateActionCard(
        title = s.quickCreateIssueCard,
        subtitle = building.name,
        icon = Icons.Default.ReportProblem,
        accent = semantic.pendingText,
        tag = "create_hub_new_issue",
        onClick = { showReportIssue = true },
      )
    }

    // 8. Join Another Building via Invite Code (Authenticated mode)
    if (onJoinWithCode != null) {
      item {
        CreateActionCard(
          title = s.joinBuildingTab,
          subtitle = s.inviteCodeInputLabel,
          icon = Icons.Default.Key,
          accent = MaterialTheme.colorScheme.primary,
          tag = "create_hub_join_code",
          onClick = onJoinWithCode,
        )
      }
    }
  }

  if (showAddUnit) {
    AssignResidentUnitDialog(
      s = s,
      existingApartment = null,
      defaultFee = building.defaultMonthlyFee,
      suggestedUnitNumber = (apartments.size + 1).toString(),
      onDismiss = { showAddUnit = false },
      onSubmitExisting = { _, _, _, _, _, _, _ -> showAddUnit = false },
      onSubmitNew = { unitNum, floor, label, unitType, fee, residentName, residentPhone, occupancyType ->
        controller.addApartmentUnit(
          unitNumber = unitNum,
          floor = floor,
          label = label,
          unitType = unitType,
          monthlyFee = fee,
          residentName = residentName,
          residentPhone = residentPhone,
          occupancyType = occupancyType,
        )
        showAddUnit = false
        onNavigateToTab(3)
      },
    )
  }

  if (showAddWork) {
    AddWorkDialog(
      s = s,
      buildingName = building.name,
      onDismiss = { showAddWork = false },
      onSubmit = { title, cat, contractorName, contractorPhone, cost, dueDate, notes ->
        controller.createWork(title, cat, contractorName, contractorPhone, cost, dueDate, notes)
        showAddWork = false
        onNavigateToTab(2)
      },
    )
  }

  if (showAddExpense) {
    AddExpenseDialog(
      s = s,
      onDismiss = { showAddExpense = false },
      onSubmit = { title, cat, amt, vendor, b64, notes ->
        controller.recordExpense(title, cat, amt, vendor, b64, notes)
        showAddExpense = false
        onNavigateToTab(1)
      },
    )
  }

  if (showBroadcast) {
    PostAnnouncementDialog(
      s = s,
      buildingName = building.name,
      onDismiss = { showBroadcast = false },
      onSubmit = { title, body, cat, audience, pinned ->
        controller.createAnnouncement(title, body, cat, audience, pinned)
        showBroadcast = false
        onNavigateToTab(3)
      },
    )
  }

  if (showCreatePoll) {
    CreatePollDialog(
      s = s,
      buildingName = building.name,
      onDismiss = { showCreatePoll = false },
      onSubmit = { q, desc, opts ->
        controller.createPoll(q, desc, opts)
        showCreatePoll = false
        onNavigateToTab(3)
      },
    )
  }

  if (showReportIssue) {
    ReportIssueDialog(
      s = s,
      onDismiss = { showReportIssue = false },
      onSubmit = { title, desc, cat, urgency, photo ->
        controller.reportIssue(title, desc, cat, urgency, photo)
        showReportIssue = false
        onNavigateToTab(2)
      },
    )
  }
}

@Composable
private fun CreateActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  tag: String,
  onClick: () -> Unit,
) {
  val semantic = LocalSyndicColors.current
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, semantic.hairline),
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .testTag(tag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        modifier = Modifier.size(46.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.width(4.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
