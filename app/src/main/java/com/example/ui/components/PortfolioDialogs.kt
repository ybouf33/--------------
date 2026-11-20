package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Apartment
import com.example.data.model.OccupancyType
import com.example.data.model.UnitType
import com.example.data.model.UserMembership
import com.example.ui.i18n.AppStrings

@Composable
fun BuildingPortfolioDialog(
  s: AppStrings,
  activeBuildingId: String,
  memberships: List<UserMembership>,
  onSelectBuilding: (String) -> Unit,
  onCreateNewBuilding: () -> Unit,
  onJoinWithCode: (() -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("${s.myBuildingsTitle} (${memberships.size})")
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        memberships.forEach { m ->
          val isActive = m.buildingId == activeBuildingId
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor =
                  if (isActive) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              ),
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable {
                  onSelectBuilding(m.buildingId)
                  onDismiss()
                }
                .testTag("portfolio_building_${m.buildingId}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = m.buildingName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color =
                    if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = listOf(m.city, m.apartmentLabel).filter { it.isNotBlank() }.joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (isActive) {
                Icon(
                  Icons.Default.CheckCircle,
                  contentDescription = s.activeBuildingBadge,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Button(
          onClick = {
            onDismiss()
            onCreateNewBuilding()
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_building_portfolio_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(s.createNewBuildingAction)
        }

        if (onJoinWithCode != null) {
          OutlinedButton(
            onClick = {
              onDismiss()
              onJoinWithCode()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Icon(Icons.Default.Key, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(s.joinBuildingTab)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateBuildingDialog(
  s: AppStrings,
  onDismiss: () -> Unit,
  onSubmit: (
    name: String,
    city: String,
    neighborhood: String,
    floors: Int,
    apartments: Int,
    defaultFee: Double,
    reserveFund: Double,
    syndicAptLabel: String,
  ) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var city by remember { mutableStateOf(if (s.langCode == "AR") "الدار البيضاء" else "Casablanca") }
  var neighborhood by remember { mutableStateOf("") }
  var floors by remember { mutableStateOf("4") }
  var apartments by remember { mutableStateOf("8") }
  var defaultFee by remember { mutableStateOf("250") }
  var reserveFund by remember { mutableStateOf("5000") }
  var syndicApt by remember { mutableStateOf("طابق 1 - شقة 1") }

  val moroccanCities =
    if (s.langCode == "AR") {
      listOf("الدار البيضاء", "الرباط", "مراكش", "طنجة", "أكادير", "فاس")
    } else {
      listOf("Casablanca", "Rabat", "Marrakech", "Tanger", "Agadir", "Fès")
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(s.createNewBuildingAction) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(s.buildingNameLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("dialog_building_name"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          moroccanCities.forEach { c ->
            FilterChip(selected = city == c, onClick = { city = c }, label = { Text(c) })
          }
        }
        OutlinedTextField(
          value = city,
          onValueChange = { city = it },
          label = { Text(s.cityLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = neighborhood,
          onValueChange = { neighborhood = it },
          label = { Text(s.neighborhoodLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = floors,
            onValueChange = { floors = it.filter { ch -> ch.isDigit() } },
            label = { Text(s.floorsLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = apartments,
            onValueChange = { apartments = it.filter { ch -> ch.isDigit() } },
            label = { Text(s.apartmentsLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = defaultFee,
            onValueChange = { defaultFee = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text(s.defaultFeeLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = reserveFund,
            onValueChange = { reserveFund = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text(s.reserveFundInputLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = syndicApt,
          onValueChange = { syndicApt = it },
          label = { Text(s.syndicAptLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmit(
            name,
            city,
            neighborhood,
            floors.toIntOrNull() ?: 4,
            apartments.toIntOrNull() ?: 8,
            defaultFee.toDoubleOrNull() ?: 250.0,
            reserveFund.toDoubleOrNull() ?: 0.0,
            syndicApt,
          )
        },
        enabled = name.trim().length >= 2 && city.trim().length >= 2,
        modifier = Modifier.testTag("dialog_create_building_confirm"),
      ) {
        Text(s.createBuildingAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignResidentUnitDialog(
  s: AppStrings,
  existingApartment: Apartment?,
  defaultFee: Double,
  suggestedUnitNumber: String,
  onDismiss: () -> Unit,
  onSubmitExisting: (
    apartment: Apartment,
    newFee: Double,
    unitType: UnitType,
    label: String,
    residentName: String,
    residentPhone: String,
    occupancyType: OccupancyType,
  ) -> Unit,
  onSubmitNew: (
    unitNumber: String,
    floor: Int,
    label: String,
    unitType: UnitType,
    monthlyFee: Double,
    residentName: String,
    residentPhone: String,
    occupancyType: OccupancyType,
  ) -> Unit,
) {
  var unitNumber by remember { mutableStateOf(existingApartment?.unitNumber ?: suggestedUnitNumber) }
  var floorText by remember { mutableStateOf((existingApartment?.floor ?: 1).toString()) }
  var unitLabel by remember {
    mutableStateOf(existingApartment?.label ?: "طابق 1 - شقة $suggestedUnitNumber")
  }
  var unitType by remember { mutableStateOf(existingApartment?.unitTypeEnum() ?: UnitType.APARTMENT) }
  var feeText by remember {
    mutableStateOf((existingApartment?.monthlyFee ?: defaultFee).toInt().toString())
  }
  var residentName by remember { mutableStateOf(existingApartment?.residentName ?: "") }
  var residentPhone by remember { mutableStateOf(existingApartment?.residentPhone ?: "") }
  var occupancy by remember {
    mutableStateOf(existingApartment?.occupancyEnum() ?: OccupancyType.OWNER)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(if (existingApartment != null) s.assignResidentTitle else s.addUnitAction)
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (existingApartment == null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = unitNumber,
              onValueChange = { unitNumber = it },
              label = { Text(s.unitNumberLabel) },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = floorText,
              onValueChange = { floorText = it.filter { ch -> ch.isDigit() } },
              label = { Text(s.floorNumberLabel) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }

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
          value = feeText,
          onValueChange = { feeText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(s.defaultFeeLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        OutlinedTextField(
          value = residentName,
          onValueChange = { residentName = it },
          label = { Text(s.residentNameLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_resident_name"),
        )

        OutlinedTextField(
          value = residentPhone,
          onValueChange = { residentPhone = it },
          label = { Text(s.residentPhoneLabel) },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_resident_phone"),
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = occupancy == OccupancyType.OWNER,
            onClick = { occupancy = OccupancyType.OWNER },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
          ) {
            Text(s.occupancyOwner)
          }
          SegmentedButton(
            selected = occupancy == OccupancyType.TENANT,
            onClick = { occupancy = OccupancyType.TENANT },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
          ) {
            Text(s.occupancyTenant)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val fee = feeText.toDoubleOrNull() ?: defaultFee
          if (existingApartment != null) {
            onSubmitExisting(
              existingApartment,
              fee,
              unitType,
              unitLabel,
              residentName,
              residentPhone,
              occupancy,
            )
          } else {
            onSubmitNew(
              unitNumber,
              floorText.toIntOrNull() ?: 1,
              unitLabel,
              unitType,
              fee,
              residentName,
              residentPhone,
              occupancy,
            )
          }
        },
        enabled = unitLabel.trim().isNotBlank(),
        modifier = Modifier.testTag("confirm_assign_resident_button"),
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
fun EditBuildingSettingsDialog(
  s: AppStrings,
  building: com.example.data.model.Building,
  onDismiss: () -> Unit,
  onSubmit: (
    name: String,
    city: String,
    neighborhood: String,
    defaultMonthlyFee: Double,
    reserveFundBalance: Double,
  ) -> Unit,
) {
  var name by remember(building.id) { mutableStateOf(building.name) }
  var city by remember(building.id) { mutableStateOf(building.city) }
  var neighborhood by remember(building.id) { mutableStateOf(building.neighborhood) }
  var defaultFee by remember(building.id) { mutableStateOf(building.defaultMonthlyFee.toInt().toString()) }
  var reserveFund by remember(building.id) { mutableStateOf(building.reserveFundBalance.toInt().toString()) }

  val moroccanCities =
    if (s.langCode == "AR") {
      listOf("الدار البيضاء", "الرباط", "مراكش", "طنجة", "أكادير", "فاس")
    } else {
      listOf("Casablanca", "Rabat", "Marrakech", "Tanger", "Agadir", "Fès")
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${s.editBuildingSettingsAction} • ${building.name}") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(s.buildingNameLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("edit_building_name_input"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          moroccanCities.forEach { c ->
            FilterChip(selected = city == c, onClick = { city = c }, label = { Text(c) })
          }
        }
        OutlinedTextField(
          value = city,
          onValueChange = { city = it },
          label = { Text(s.cityLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = neighborhood,
          onValueChange = { neighborhood = it },
          label = { Text(s.neighborhoodLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = defaultFee,
          onValueChange = { defaultFee = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(s.defaultFeeLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = reserveFund,
          onValueChange = { reserveFund = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(s.reserveFundInputLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("edit_building_reserve_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmit(
            name,
            city,
            neighborhood,
            defaultFee.toDoubleOrNull() ?: building.defaultMonthlyFee,
            reserveFund.toDoubleOrNull() ?: building.reserveFundBalance,
          )
        },
        enabled = name.trim().length >= 2 && city.trim().length >= 2,
        modifier = Modifier.testTag("confirm_edit_building_settings"),
      ) {
        Text(s.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

