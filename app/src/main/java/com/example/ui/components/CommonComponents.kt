package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AnnouncementCategory
import com.example.data.model.BroadcastAudience
import com.example.data.model.ExpenseCategory
import com.example.data.model.IssueCategory
import com.example.data.model.IssueStatus
import com.example.data.model.IssueUrgency
import com.example.data.model.MemberRole
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.UnitType
import com.example.data.model.UserMembership
import com.example.data.model.WorkCategory
import com.example.data.model.WorkStatus
import com.example.data.util.ImageUtils
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors
import java.text.NumberFormat
import java.util.Locale

fun formatMad(amount: Double, s: AppStrings): String {
  val nf = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
  return "${nf.format(amount)} ${s.madCurrency}"
}

@Composable
fun ExecutiveGradientCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val semantic = LocalSyndicColors.current
  Surface(
    shape = MaterialTheme.shapes.large,
    color = Color.Transparent,
    border = BorderStroke(1.dp, semantic.heroBorder),
    shadowElevation = if (semantic.isLight) 2.dp else 4.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier
          .background(
            Brush.linearGradient(
              colors = listOf(semantic.heroStart, semantic.heroEnd)
            )
          )
          .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      content = content,
    )
  }
}

@Composable
fun ThemeToggleChip(
  isDarkTheme: Boolean,
  s: AppStrings,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  IconButton(
    onClick = onToggle,
    modifier = modifier.testTag("theme_toggle_button"),
  ) {
    Icon(
      imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
      contentDescription = s.switchThemeAction,
      tint = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
fun BuildingContextStrip(
  activeBuildingId: String,
  memberships: List<UserMembership>,
  s: AppStrings,
  onSelectBuilding: (String) -> Unit,
  onCreateNewBuilding: () -> Unit,
) {
  val semantic = LocalSyndicColors.current
  Surface(
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        memberships.forEach { m ->
          val isSelected = m.buildingId == activeBuildingId
          Surface(
            shape = RoundedCornerShape(50),
            color =
              if (isSelected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border =
              if (isSelected) null
              else BorderStroke(1.dp, semantic.hairline),
            modifier =
              Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onSelectBuilding(m.buildingId) }
                .testTag("strip_building_${m.buildingId}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Apartment,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint =
                  if (isSelected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.primary,
              )
              Text(
                text = m.buildingName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color =
                  if (isSelected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
          modifier =
            Modifier
              .clip(RoundedCornerShape(50))
              .clickable(onClick = onCreateNewBuilding)
              .testTag("strip_add_building"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = s.createNewBuildingAction,
              modifier = Modifier.size(15.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = s.createBuildingTab,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(semantic.hairline)
      )
    }
  }
}

@Composable
fun StatusPill(
  text: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(50),
    modifier = modifier,
  ) {
    Text(
      text = text,
      color = contentColor,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun RoleBadge(role: MemberRole, s: AppStrings) {
  val (label, bg, fg) =
    when (role) {
      MemberRole.SYNDIC ->
        Triple(
          s.roleSyndic,
          MaterialTheme.colorScheme.primaryContainer,
          MaterialTheme.colorScheme.onPrimaryContainer,
        )
      MemberRole.DEPUTY ->
        Triple(
          s.roleDeputy,
          MaterialTheme.colorScheme.secondaryContainer,
          MaterialTheme.colorScheme.onSecondaryContainer,
        )
      MemberRole.RESIDENT ->
        Triple(
          s.roleResident,
          MaterialTheme.colorScheme.tertiaryContainer,
          MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
  StatusPill(text = label, containerColor = bg, contentColor = fg)
}

@Composable
fun LanguageToggleChip(
  s: AppStrings,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedButton(
    onClick = onToggle,
    modifier = modifier.testTag("language_toggle_button"),
    shape = RoundedCornerShape(50),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
  ) {
    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(4.dp))
    Text(s.switchLanguageLabel, style = MaterialTheme.typography.labelMedium)
  }
}

@Composable
fun Base64Thumbnail(
  base64: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val bitmap = remember(base64) { ImageUtils.decodeBase64ToImageBitmap(base64) }
  if (bitmap != null) {
    Image(
      bitmap = bitmap,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier =
        modifier
          .clip(MaterialTheme.shapes.small)
          .clickable(onClick = onClick),
    )
  }
}

@Composable
fun FullScreenBase64Dialog(
  base64: String,
  title: String,
  onDismiss: () -> Unit,
) {
  val bitmap = remember(base64) { ImageUtils.decodeBase64ToImageBitmap(base64) }
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = Color.Black.copy(alpha = 0.92f),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }
        Box(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          if (bitmap != null) {
            Image(
              bitmap = bitmap,
              contentDescription = title,
              modifier = Modifier.fillMaxWidth(),
              contentScale = ContentScale.Fit,
            )
          }
        }
      }
    }
  }
}

@Composable
fun LoadingBlock(label: String) {
  Box(
    modifier = Modifier.fillMaxWidth().padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      Text(label, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
fun ErrorBanner(message: String, retryLabel: String? = null, onRetry: (() -> Unit)? = null) {
  Surface(
    color = MaterialTheme.colorScheme.errorContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().padding(16.dp),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        Icons.Default.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.weight(1f),
      )
      if (retryLabel != null && onRetry != null) {
        Button(onClick = onRetry) { Text(retryLabel) }
      }
    }
  }
}

fun expenseCategoryLabel(cat: ExpenseCategory, s: AppStrings): String =
  when (cat) {
    ExpenseCategory.CLEANING -> s.expCleaning
    ExpenseCategory.SECURITY -> s.expSecurity
    ExpenseCategory.UTILITIES -> s.expUtilities
    ExpenseCategory.ELEVATOR -> s.expElevator
    ExpenseCategory.REPAIRS -> s.expRepairs
    ExpenseCategory.OTHER -> s.expOther
  }

fun issueCategoryLabel(cat: IssueCategory, s: AppStrings): String =
  when (cat) {
    IssueCategory.PLUMBING -> s.issPlumbing
    IssueCategory.ELECTRICAL -> s.issElectrical
    IssueCategory.ELEVATOR -> s.issElevator
    IssueCategory.CLEANING -> s.issCleaning
    IssueCategory.SECURITY -> s.issSecurity
    IssueCategory.STRUCTURAL -> s.issStructural
  }

fun issueUrgencyLabel(u: IssueUrgency, s: AppStrings): String =
  when (u) {
    IssueUrgency.NORMAL -> s.urgencyNormal
    IssueUrgency.URGENT -> s.urgencyUrgent
    IssueUrgency.EMERGENCY -> s.urgencyEmergency
  }

fun issueStatusLabel(st: IssueStatus, s: AppStrings): String =
  when (st) {
    IssueStatus.UNDER_REVIEW -> s.statusUnderReview
    IssueStatus.IN_PROGRESS -> s.statusInProgress
    IssueStatus.RESOLVED -> s.statusResolved
  }

fun workCategoryLabel(cat: WorkCategory, s: AppStrings): String =
  when (cat) {
    WorkCategory.MAINTENANCE -> s.workCatMaintenance
    WorkCategory.CLEANING -> s.workCatCleaning
    WorkCategory.ELEVATOR -> s.workCatElevator
    WorkCategory.PLUMBING -> s.workCatPlumbing
    WorkCategory.SECURITY -> s.workCatSecurity
    WorkCategory.RENOVATION -> s.workCatRenovation
  }

fun workStatusLabel(st: WorkStatus, s: AppStrings): String =
  when (st) {
    WorkStatus.PLANNED -> s.workPlanned
    WorkStatus.IN_PROGRESS -> s.workInProgress
    WorkStatus.COMPLETED -> s.workCompleted
  }

fun announcementCategoryLabel(cat: AnnouncementCategory, s: AppStrings): String =
  when (cat) {
    AnnouncementCategory.WATER -> s.annWater
    AnnouncementCategory.ELECTRICITY -> s.annElectricity
    AnnouncementCategory.ELEVATOR -> s.annElevator
    AnnouncementCategory.CLEANING -> s.annCleaning
    AnnouncementCategory.GENERAL_ASSEMBLY -> s.annGeneralAssembly
    AnnouncementCategory.GENERAL -> s.annGeneral
  }

fun unitTypeLabel(type: UnitType, s: AppStrings): String =
  when (type) {
    UnitType.APARTMENT -> s.unitTypeApartment
    UnitType.SHOP -> s.unitTypeShop
    UnitType.DUPLEX -> s.unitTypeDuplex
  }

fun paymentStatusLabel(st: PaymentStatus, s: AppStrings): String =
  when (st) {
    PaymentStatus.PAID -> s.statusPaid
    PaymentStatus.PENDING -> s.statusWait
    PaymentStatus.OVERDUE -> s.statusOverdue
  }

fun paymentMethodLabel(m: PaymentMethod, s: AppStrings): String =
  when (m) {
    PaymentMethod.CASH -> s.paymentMethodCash
    PaymentMethod.TRANSFER -> s.paymentMethodTransfer
    PaymentMethod.CHECK -> s.paymentMethodCheck
  }

fun audienceLabel(audience: BroadcastAudience, s: AppStrings): String =
  when (audience) {
    BroadcastAudience.ALL -> s.audienceAll
    BroadcastAudience.UNPAID -> s.audienceUnpaid
    BroadcastAudience.OWNERS -> s.audienceOwners
  }
