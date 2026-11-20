package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Apartment
import com.example.data.model.Building
import com.example.data.model.BuildingMember
import com.example.data.model.CotisationPayment
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.util.ReceiptGenerator
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.EditBuildingSettingsDialog
import com.example.ui.components.ExecutiveGradientCard
import com.example.ui.components.FullScreenBase64Dialog
import com.example.ui.components.StatusPill
import com.example.ui.components.expenseCategoryLabel
import com.example.ui.components.formatMad
import com.example.ui.components.paymentStatusLabel
import com.example.ui.components.unitTypeLabel
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors
import com.example.ui.viewmodel.SyndicController
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanceScreen(
  s: AppStrings,
  building: Building,
  myMember: BuildingMember,
  apartments: List<Apartment>,
  payments: List<CotisationPayment>,
  expenses: List<Expense>,
  controller: SyndicController,
) {
  val context = LocalContext.current
  val isAdmin = myMember.isAdmin()
  val semantic = LocalSyndicColors.current

  var subTab by remember { mutableIntStateOf(0) }
  val cal = remember { Calendar.getInstance() }
  var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
  var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
  val monthKey = remember(selectedYear, selectedMonth) { "%04d-%02d".format(selectedYear, selectedMonth) }

  var statusFilter by remember { mutableStateOf<PaymentStatus?>(null) }
  var selectedApartmentForPayment by remember { mutableStateOf<Apartment?>(null) }
  var selectedMonthForDialog by remember { mutableStateOf<String?>(null) }
  var showAddExpense by remember { mutableStateOf(false) }
  var showEditBuildingSettings by remember { mutableStateOf(false) }
  var previewInvoice by remember { mutableStateOf<Expense?>(null) }

  val totalCollected =
    remember(payments) { payments.filter { it.statusEnum() == PaymentStatus.PAID }.sumOf { it.amountMad } }
  val totalSpent = remember(expenses) { expenses.sumOf { it.amountMad } }
  val netBalance = totalCollected - totalSpent

  val paymentsByAptAndMonth =
    remember(payments) { payments.associateBy { "${it.apartmentId}_${it.monthKey}" } }
  val paymentsForMonth =
    remember(payments, monthKey) {
      payments.filter { it.monthKey == monthKey }.associateBy { it.apartmentId }
    }

  val monthExpected = remember(apartments) { apartments.sumOf { it.monthlyFee } }
  val monthPaidAptsCount =
    remember(apartments, paymentsForMonth) {
      apartments.count { apt -> paymentsForMonth[apt.id]?.statusEnum() == PaymentStatus.PAID }
    }
  val monthCollected =
    remember(apartments, paymentsForMonth) {
      apartments.sumOf { apt ->
        val p = paymentsForMonth[apt.id]
        if (p?.statusEnum() == PaymentStatus.PAID) p.amountMad else 0.0
      }
    }
  val monthRemaining = (monthExpected - monthCollected).coerceAtLeast(0.0)
  val collectionRatio =
    if (monthExpected > 0.0) (monthCollected / monthExpected).toFloat().coerceIn(0f, 1f) else 0f

  val unpaidPhonesForMonth =
    remember(apartments, paymentsForMonth) {
      apartments
        .filter { apt -> paymentsForMonth[apt.id]?.statusEnum() != PaymentStatus.PAID }
        .map { it.residentPhone }
        .filter { it.isNotBlank() }
        .distinct()
    }

  val expenseBreakdown =
    remember(expenses, totalSpent) {
      ExpenseCategory.entries
        .map { cat ->
          val catSum = expenses.filter { it.categoryEnum() == cat }.sumOf { it.amountMad }
          cat to catSum
        }
        .filter { it.second > 0.0 }
        .sortedByDescending { it.second }
    }

  val windowMonths =
    remember(selectedYear, selectedMonth) {
      val anchor =
        Calendar.getInstance().apply {
          set(Calendar.YEAR, selectedYear)
          set(Calendar.MONTH, selectedMonth - 1)
          set(Calendar.DAY_OF_MONTH, 1)
        }
      (5 downTo 0).map { offset ->
        val c = (anchor.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
        "%04d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Executive Midnight Navy Treasury Card scoped strictly to active 3imara
    item {
      ExecutiveGradientCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "${s.treasuryTitle} • ${building.name}",
              style = MaterialTheme.typography.labelLarge,
              color = semantic.heroSubtleContent,
            )
            Text(
              text = formatMad(netBalance, s),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              color = semantic.heroContent,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isAdmin) {
              OutlinedButton(
                onClick = { showEditBuildingSettings = true },
                border = BorderStroke(1.dp, semantic.heroContent.copy(alpha = 0.25f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("finance_edit_building_settings"),
              ) {
                Icon(Icons.Default.Edit, contentDescription = s.editBuildingSettingsAction, tint = semantic.heroContent, modifier = Modifier.size(16.dp))
              }
            }
            Button(
              onClick = {
                ReceiptGenerator.shareMonthlyFinancialReport(
                  context = context,
                  building = building,
                  monthKey = monthKey,
                  paidCount = monthPaidAptsCount,
                  totalUnits = apartments.size,
                  monthCollectedMad = monthCollected,
                  monthExpectedMad = monthExpected,
                  totalCollectedAllMad = totalCollected,
                  totalSpentAllMad = totalSpent,
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = semantic.heroButtonBg, contentColor = semantic.heroButtonContent),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("share_monthly_report_button"),
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(s.shareMonthlyReport, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TreasuryGlassStat(
            label = s.totalCollected,
            value = formatMad(totalCollected, s),
            accent = semantic.paidText,
            modifier = Modifier.weight(1f),
          )
          TreasuryGlassStat(
            label = s.totalSpent,
            value = formatMad(totalSpent, s),
            accent = semantic.overdueText,
            modifier = Modifier.weight(1f),
          )
          TreasuryGlassStat(
            label = s.reserveFund,
            value = formatMad(building.reserveFundBalance, s),
            accent = semantic.pendingText,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 3-tab Sub-navigation: Monthly Cotisations vs 6-Month Tracking vs Expenses
    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = subTab == 0,
          onClick = { subTab = 0 },
          shape = SegmentedButtonDefaults.itemShape(0, 3),
          modifier = Modifier.testTag("subtab_cotisations"),
        ) {
          Text(s.subTabCotisations, maxLines = 1)
        }
        SegmentedButton(
          selected = subTab == 1,
          onClick = { subTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(1, 3),
          modifier = Modifier.testTag("subtab_tracking"),
        ) {
          Text(s.subTabTracking, maxLines = 1)
        }
        SegmentedButton(
          selected = subTab == 2,
          onClick = { subTab = 2 },
          shape = SegmentedButtonDefaults.itemShape(2, 3),
          modifier = Modifier.testTag("subtab_expenses"),
        ) {
          Text(s.subTabExpenses, maxLines = 1)
        }
      }
    }

    if (subTab == 0 || subTab == 1) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, semantic.hairline),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = {
                if (selectedMonth == 1) {
                  selectedMonth = 12
                  selectedYear--
                } else {
                  selectedMonth--
                }
              },
            ) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
              text = monthKey,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            IconButton(
              onClick = {
                if (selectedMonth == 12) {
                  selectedMonth = 1
                  selectedYear++
                } else {
                  selectedMonth++
                }
              },
            ) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
          }
        }
      }
    }

    if (subTab == 0) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, semantic.hairline),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${s.collectionProgressLabel}: $monthPaidAptsCount / ${apartments.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${(collectionRatio * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
            LinearProgressIndicator(
              progress = { collectionRatio },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "${s.expectedLabel}: ${formatMad(monthExpected, s)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${s.remainingLabel}: ${formatMad(monthRemaining, s)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (monthRemaining > 0.0) semantic.overdueText else semantic.paidText,
                fontWeight = FontWeight.SemiBold,
              )
            }
            if (isAdmin && monthPaidAptsCount < apartments.size) {
              OutlinedButton(
                onClick = {
                  ReceiptGenerator.sendBulkUnpaidReminders(
                    context = context,
                    building = building,
                    monthKey = monthKey,
                    unpaidPhones = unpaidPhonesForMonth,
                  )
                },
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("bulk_remind_unpaid_button"),
              ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("${s.bulkRemindUnpaidAction} (${apartments.size - monthPaidAptsCount})")
              }
            }
          }
        }
      }

      item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = statusFilter == null,
            onClick = { statusFilter = null },
            label = { Text("${s.filterAll} (${apartments.size})") },
          )
          PaymentStatus.entries.forEach { st ->
            val cnt =
              apartments.count { apt ->
                (paymentsForMonth[apt.id]?.statusEnum() ?: PaymentStatus.PENDING) == st
              }
            FilterChip(
              selected = statusFilter == st,
              onClick = { statusFilter = st },
              label = { Text("${paymentStatusLabel(st, s)} ($cnt)") },
            )
          }
        }
      }

      val filteredApartments =
        apartments.filter { apt ->
          val st = paymentsForMonth[apt.id]?.statusEnum() ?: PaymentStatus.PENDING
          statusFilter == null || st == statusFilter
        }

      if (filteredApartments.isEmpty()) {
        item {
          Text(s.noPaymentsYet, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }
      } else {
        item {
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
          ) {
            filteredApartments.forEach { apt ->
              val payment = paymentsForMonth[apt.id]
              val st = payment?.statusEnum() ?: PaymentStatus.PENDING
              val (bg, fg) =
                when (st) {
                  PaymentStatus.PAID -> semantic.paidContainer to semantic.paidText
                  PaymentStatus.PENDING -> semantic.pendingContainer to semantic.pendingText
                  PaymentStatus.OVERDUE -> semantic.overdueContainer to semantic.overdueText
                }

              Card(
                border = BorderStroke(1.dp, semantic.hairline),
                modifier =
                  Modifier
                    .weight(1f)
                    .clickable {
                      selectedMonthForDialog = monthKey
                      selectedApartmentForPayment = apt
                    }
                    .testTag("cotisation_card_${apt.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = apt.unitNumber,
                      style = MaterialTheme.typography.titleLarge,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Bold,
                    )
                    StatusPill(
                      text = paymentStatusLabel(st, s),
                      containerColor = bg,
                      contentColor = fg,
                    )
                  }
                  Text(apt.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                  if (apt.residentName.isNotBlank()) {
                    Text(
                      text =
                        listOf(apt.residentName, apt.residentPhone)
                          .filter { it.isNotBlank() }
                          .joinToString(" • "),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = "${unitTypeLabel(apt.unitTypeEnum(), s)} • ${formatMad(payment?.amountMad ?: apt.monthlyFee, s)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (payment != null && st == PaymentStatus.PAID) {
                    TextButton(
                      onClick = {
                        ReceiptGenerator.sharePaymentReceiptViaWhatsApp(context, building, payment)
                      },
                      contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                      Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(s.shareReceiptWhatsapp, style = MaterialTheme.typography.labelSmall)
                    }
                  } else if (isAdmin) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      TextButton(
                        onClick = {
                          controller.recordPayment(
                            apartment = apt,
                            monthKey = monthKey,
                            amountMad = apt.monthlyFee,
                            status = PaymentStatus.PAID,
                            payerName = apt.residentName,
                            method = PaymentMethod.CASH,
                          )
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("quick_pay_${apt.id}"),
                      ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.quickMarkPaid, style = MaterialTheme.typography.labelSmall)
                      }
                      TextButton(
                        onClick = {
                          ReceiptGenerator.sendPaymentReminder(
                            context = context,
                            building = building,
                            apartmentLabel = apt.label,
                            residentName = apt.residentName,
                            residentPhone = apt.residentPhone,
                            monthKey = monthKey,
                            amountMad = apt.monthlyFee,
                          )
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("remind_apt_${apt.id}"),
                      ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.sendReminderAction, style = MaterialTheme.typography.labelSmall)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else if (subTab == 1) {
      item {
        Text(
          text = s.trackingWindowSubtitle,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (apartments.isEmpty()) {
        item {
          Text(s.noPaymentsYet, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }
      } else {
        items(apartments, key = { "track_${it.id}" }) { apt ->
          val paidInWindow =
            windowMonths.count { mKey ->
              paymentsByAptAndMonth["${apt.id}_$mKey"]?.statusEnum() == PaymentStatus.PAID
            }
          val unpaidCount = windowMonths.size - paidInWindow
          val arrearsMad = unpaidCount * apt.monthlyFee

          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${apt.unitNumber} • ${apt.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      if (apt.residentName.isNotBlank()) {
                        listOf(apt.residentName, apt.residentPhone).filter { it.isNotBlank() }.joinToString(" • ")
                      } else {
                        s.unassignedUnit
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  StatusPill(
                    text = if (unpaidCount == 0) s.upToDateBadge else "${s.arrearsLabel}: ${formatMad(arrearsMad, s)}",
                    containerColor = if (unpaidCount == 0) semantic.paidContainer else semantic.overdueContainer,
                    contentColor = if (unpaidCount == 0) semantic.paidText else semantic.overdueText,
                  )
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = "$paidInWindow/${windowMonths.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                windowMonths.forEach { mKey ->
                  val payment = paymentsByAptAndMonth["${apt.id}_$mKey"]
                  val st = payment?.statusEnum() ?: PaymentStatus.PENDING
                  val (cellBg, cellFg, symbol) =
                    when (st) {
                      PaymentStatus.PAID -> Triple(semantic.paidContainer, semantic.paidText, "✓")
                      PaymentStatus.PENDING -> Triple(semantic.pendingContainer, semantic.pendingText, "•")
                      PaymentStatus.OVERDUE -> Triple(semantic.overdueContainer, semantic.overdueText, "!")
                    }
                  val monthNum = mKey.takeLast(2)

                  Surface(
                    color = cellBg,
                    shape = MaterialTheme.shapes.small,
                    modifier =
                      Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                          selectedMonthForDialog = mKey
                          selectedApartmentForPayment = apt
                        }
                        .testTag("track_cell_${apt.id}_$mKey"),
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 8.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                      Text(
                        text = monthNum,
                        style = MaterialTheme.typography.labelSmall,
                        color = cellFg,
                        textAlign = TextAlign.Center,
                      )
                      Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cellFg,
                      )
                    }
                  }
                }
              }

              if (apt.residentPhone.isNotBlank() || (isAdmin && unpaidCount > 0)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  if (apt.residentPhone.isNotBlank()) {
                    OutlinedButton(
                      onClick = { ReceiptGenerator.openPhoneDialer(context, apt.residentPhone) },
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                      Icon(Icons.Default.Call, contentDescription = s.callAction, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(s.callAction, style = MaterialTheme.typography.labelSmall)
                    }
                  }
                  if (isAdmin && unpaidCount > 0) {
                    val oldestUnpaidMonth =
                      windowMonths.firstOrNull { mKey ->
                        paymentsByAptAndMonth["${apt.id}_$mKey"]?.statusEnum() != PaymentStatus.PAID
                      } ?: monthKey
                    OutlinedButton(
                      onClick = {
                        ReceiptGenerator.sendPaymentReminder(
                          context = context,
                          building = building,
                          apartmentLabel = apt.label,
                          residentName = apt.residentName,
                          residentPhone = apt.residentPhone,
                          monthKey = oldestUnpaidMonth,
                          amountMad = arrearsMad,
                        )
                      },
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                      Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(s.sendReminderAction, style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else {
      if (isAdmin) {
        item {
          Button(
            onClick = { showAddExpense = true },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_expense_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(s.addExpenseAction)
          }
        }
      }

      if (expenseBreakdown.isNotEmpty() && totalSpent > 0.0) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(s.expenseBreakdownTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
              expenseBreakdown.forEach { (cat, sum) ->
                val ratio = (sum / totalSpent).toFloat().coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = expenseCategoryLabel(cat, s),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Medium,
                    )
                    Text(
                      text = "${formatMad(sum, s)} (${(ratio * 100).toInt()}%)",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                  )
                }
              }
            }
          }
        }
      }

      if (expenses.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Text(
              text = s.noExpensesYet,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(20.dp).fillMaxWidth(),
            )
          }
        }
      } else {
        items(expenses, key = { it.id }) { exp ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Base64Thumbnail(
                base64 = exp.invoicePhotoBase64,
                contentDescription = s.viewInvoiceAction,
                modifier = Modifier.size(64.dp),
                onClick = { previewInvoice = exp },
              )
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(exp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                  text =
                    listOf(
                      expenseCategoryLabel(exp.categoryEnum(), s),
                      exp.vendorName,
                      exp.expenseDateLabel,
                    ).filter { it.isNotBlank() }.joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = formatMad(exp.amountMad, s),
                  style = MaterialTheme.typography.titleSmall,
                  color = semantic.overdueText,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }

  val aptTarget = selectedApartmentForPayment
  if (aptTarget != null) {
    val dialogMonth = selectedMonthForDialog ?: monthKey
    PaymentActionDialog(
      s = s,
      building = building,
      apartment = aptTarget,
      monthKey = dialogMonth,
      existingPayment = paymentsByAptAndMonth["${aptTarget.id}_$dialogMonth"],
      isAdmin = isAdmin,
      onDismiss = {
        selectedApartmentForPayment = null
        selectedMonthForDialog = null
      },
      onSavePayment = { amt, st, payer, method ->
        controller.recordPayment(aptTarget, dialogMonth, amt, st, payer, method)
        selectedApartmentForPayment = null
        selectedMonthForDialog = null
      },
      onSaveApartmentFee = { newFee, uType, uLabel, rName, rPhone, occType ->
        controller.updateApartmentDetails(
          apartment = aptTarget,
          newFee = newFee,
          unitType = uType,
          label = uLabel,
          residentName = rName,
          residentPhone = rPhone,
          occupancyType = occType,
        )
        selectedApartmentForPayment = null
        selectedMonthForDialog = null
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
      },
    )
  }

  if (showEditBuildingSettings) {
    EditBuildingSettingsDialog(
      s = s,
      building = building,
      onDismiss = { showEditBuildingSettings = false },
      onSubmit = { name, city, neighborhood, defaultFee, reserveFund ->
        controller.updateBuildingSettings(name, city, neighborhood, defaultFee, reserveFund)
        showEditBuildingSettings = false
      },
    )
  }

  val expPreview = previewInvoice
  if (expPreview != null) {
    FullScreenBase64Dialog(
      base64 = expPreview.invoicePhotoBase64,
      title = "${expPreview.title} • ${formatMad(expPreview.amountMad, s)}",
      onDismiss = { previewInvoice = null },
    )
  }
}

@Composable
private fun TreasuryGlassStat(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalSyndicColors.current
  Surface(
    color = semantic.heroChipBg,
    shape = MaterialTheme.shapes.small,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = semantic.heroSubtleContent, maxLines = 1)
      Spacer(Modifier.height(3.dp))
      Text(value, style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.Bold, maxLines = 1)
    }
  }
}
