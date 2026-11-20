package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Apartment
import com.example.data.model.Building
import com.example.data.model.BuildingMember
import com.example.data.model.BuildingOverview
import com.example.data.model.BuildingWork
import com.example.data.model.CotisationPayment
import com.example.data.model.Expense
import com.example.data.model.PaymentStatus
import com.example.data.model.UserMembership
import com.example.data.model.WorkStatus
import com.example.ui.components.EditBuildingSettingsDialog
import com.example.ui.components.ExecutiveGradientCard
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.formatMad
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors
import com.example.ui.viewmodel.SyndicController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  s: AppStrings,
  building: Building,
  myMember: BuildingMember,
  memberships: List<UserMembership>,
  buildingOverviews: Map<String, BuildingOverview>,
  apartments: List<Apartment>,
  payments: List<CotisationPayment>,
  expenses: List<Expense>,
  works: List<BuildingWork>,
  controller: SyndicController,
  onOpenCreateBuilding: () -> Unit,
  onNavigateToTab: (Int) -> Unit,
) {
  val isAdmin = myMember.isAdmin()
  val semantic = LocalSyndicColors.current
  var showEditSettings by remember { mutableStateOf(false) }

  val totalCollected =
    remember(payments) {
      payments.filter { it.statusEnum() == PaymentStatus.PAID }.sumOf { it.amountMad }
    }
  val totalSpent = remember(expenses) { expenses.sumOf { it.amountMad } }
  val netBalance = totalCollected - totalSpent
  val activeWorksCount = remember(works) { works.count { it.statusEnum() != WorkStatus.COMPLETED } }

  val displayMemberships =
    remember(memberships, building) {
      if (memberships.any { it.buildingId == building.id }) {
        memberships
      } else {
        listOf(
          UserMembership(
            id = building.id,
            buildingId = building.id,
            buildingName = building.name,
            city = building.city,
            apartmentLabel = myMember.apartmentLabel,
          )
        ) + memberships
      }
    }

  val portfolioNetSum =
    remember(displayMemberships, buildingOverviews, building.id, netBalance) {
      displayMemberships.sumOf { m ->
        buildingOverviews[m.buildingId]?.netBalanceMad
          ?: if (m.buildingId == building.id) netBalance else 0.0
      }
    }
  val portfolioUnitsSum =
    remember(displayMemberships, buildingOverviews, building.id, apartments.size) {
      displayMemberships.sumOf { m ->
        buildingOverviews[m.buildingId]?.apartmentCount
          ?: if (m.buildingId == building.id) apartments.size else 0
      }
    }
  val portfolioPaidSum =
    remember(displayMemberships, buildingOverviews) {
      displayMemberships.sumOf { m -> buildingOverviews[m.buildingId]?.paidThisMonthCount ?: 0 }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Portfolio Aggregate Executive Banner
    item {
      ExecutiveGradientCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = s.portfolioSummaryTitle,
              style = MaterialTheme.typography.labelLarge,
              color = semantic.heroSubtleContent,
            )
            Text(
              text = formatMad(portfolioNetSum, s),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              color = semantic.heroContent,
            )
          }
          Button(
            onClick = onOpenCreateBuilding,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = semantic.heroButtonBg,
                contentColor = semantic.heroButtonContent,
              ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("home_create_building_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(s.createBuildingTab, fontWeight = FontWeight.Bold)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          HeroKpiChip(
            label = s.buildingsCountLabel,
            value = "${displayMemberships.size}",
            modifier = Modifier.weight(1f),
          )
          HeroKpiChip(
            label = s.unitsShortLabel,
            value = "$portfolioUnitsSum",
            modifier = Modifier.weight(1f),
          )
          HeroKpiChip(
            label = s.paidThisMonthLabel,
            value = "$portfolioPaidSum/$portfolioUnitsSum",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 2. Section Title: Managed 3imarat
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = "${s.allBuildingsHubTitle} (${displayMemberships.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    // 3. Dedicated Isolated Card per 3imara with direct jumps into its separated tabs
    items(displayMemberships, key = { "home_bld_${it.buildingId}" }) { m ->
      val isActive = m.buildingId == building.id
      val overview = buildingOverviews[m.buildingId]

      Card(
        shape = MaterialTheme.shapes.large,
        border =
          if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
          else BorderStroke(1.dp, semantic.hairline),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp),
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable { controller.switchActiveBuilding(m.buildingId) }
            .testTag("home_building_card_${m.buildingId}"),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Surface(
                shape = MaterialTheme.shapes.small,
                color =
                  if (isActive) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Default.Apartment,
                    contentDescription = null,
                    tint =
                      if (isActive) Color.White
                      else MaterialTheme.colorScheme.primary,
                  )
                }
              }
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = m.buildingName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = listOf(m.city, m.apartmentLabel).filter { it.isNotBlank() }.joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (isActive) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusPill(
                  text = s.currentlyViewingBadge,
                  containerColor = semantic.paidContainer,
                  contentColor = semantic.paidText,
                )
                if (isAdmin) {
                  IconButton(
                    onClick = { showEditSettings = true },
                    modifier = Modifier.testTag("home_edit_building_settings"),
                  ) {
                    Icon(
                      Icons.Default.Edit,
                      contentDescription = s.editBuildingSettingsAction,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }
            } else {
              OutlinedButton(
                onClick = { controller.switchActiveBuilding(m.buildingId) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("home_access_building_${m.buildingId}"),
              ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(s.accessBuildingAction, style = MaterialTheme.typography.labelMedium)
              }
            }
          }

          if (overview != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              SlateMiniStat(
                label = s.unitsShortLabel,
                value = "${overview.apartmentCount}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(0.9f),
              )
              SlateMiniStat(
                label = s.paidThisMonthLabel,
                value = "${overview.paidThisMonthCount}/${overview.apartmentCount}",
                color = semantic.paidText,
                modifier = Modifier.weight(1f),
              )
              SlateMiniStat(
                label = s.activeWorksLabel,
                value = "${overview.activeWorksCount}",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(0.85f),
              )
              SlateMiniStat(
                label = s.netBalance,
                value = formatMad(overview.netBalanceMad, s),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1.35f),
              )
            }
          }

          HorizontalDivider(color = semantic.hairline)

          // Isolated Navigation Actions scoped specifically to THIS 3imara
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = {
                controller.switchActiveBuilding(m.buildingId)
                onNavigateToTab(1)
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("home_finance_building_${m.buildingId}"),
            ) {
              Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(s.tabFinance, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = {
                controller.switchActiveBuilding(m.buildingId)
                onNavigateToTab(2)
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("home_works_building_${m.buildingId}"),
            ) {
              Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(s.tabWorks, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = {
                controller.switchActiveBuilding(m.buildingId)
                onNavigateToTab(3)
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("home_residents_building_${m.buildingId}"),
            ) {
              Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(s.tabResidents, style = MaterialTheme.typography.labelMedium)
            }
            if (isAdmin) {
              OutlinedButton(
                onClick = {
                  controller.switchActiveBuilding(m.buildingId)
                  onNavigateToTab(4)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("home_create_for_${m.buildingId}"),
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(s.tabCreate, style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }
    }

    // 4. Active Building Quick Profile Banner
    item {
      Card(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, semantic.hairline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column {
          Box(modifier = Modifier.fillMaxWidth().height(136.dp)) {
            Image(
              painter = painterResource(id = R.drawable.img_building_hero),
              contentDescription = stringResource(R.string.cd_building_hero),
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      listOf(Color.Transparent, semantic.heroStart.copy(alpha = 0.85f))
                    )
                  )
            )
            Column(
              modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            ) {
              Text(
                text = building.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  listOf(building.city, building.neighborhood, "${apartments.size} ${s.unitsShortLabel}")
                    .filter { it.isNotBlank() }
                    .joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = myMember.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${myMember.apartmentLabel} • $activeWorksCount ${s.activeWorksLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            RoleBadge(role = myMember.roleEnum(), s = s)
          }
        }
      }
    }
  }

  if (showEditSettings) {
    EditBuildingSettingsDialog(
      s = s,
      building = building,
      onDismiss = { showEditSettings = false },
      onSubmit = { name, city, neighborhood, defaultFee, reserveFund ->
        controller.updateBuildingSettings(name, city, neighborhood, defaultFee, reserveFund)
        showEditSettings = false
      },
    )
  }
}

@Composable
private fun HeroKpiChip(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalSyndicColors.current
  Surface(
    color = semantic.heroChipBg,
    shape = MaterialTheme.shapes.small,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = semantic.heroSubtleContent,
        maxLines = 1,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = semantic.heroContent,
      )
    }
  }
}

@Composable
private fun SlateMiniStat(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    shape = MaterialTheme.shapes.small,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
    }
  }
}
