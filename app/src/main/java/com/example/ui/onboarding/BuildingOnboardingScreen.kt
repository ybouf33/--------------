package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.BuildingMember
import com.example.data.model.InviteInfo
import com.example.data.model.MemberStatus
import com.example.data.model.OccupancyType
import com.example.data.model.PreApprovedPhone
import com.example.data.model.UserMembership
import com.example.data.util.ReceiptGenerator
import com.example.data.util.SokanAccountStore
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.ThemeToggleChip
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildingOnboardingScreen(
  s: AppStrings,
  initialInviteCode: String?,
  myMember: BuildingMember?,
  memberships: List<UserMembership>,
  lookedUpInvite: InviteInfo?,
  preApprovedMatch: PreApprovedPhone? = null,
  isBusy: Boolean,
  isDarkTheme: Boolean = false,
  feedbackMessage: String?,
  onToggleLanguage: () -> Unit,
  onToggleTheme: () -> Unit = {},
  onCreateBuilding: (
    name: String,
    city: String,
    neighborhood: String,
    floors: Int,
    apartments: Int,
    defaultFee: Double,
    reserveFund: Double,
    syndicAptLabel: String,
  ) -> Unit,
  onLookupInvite: (String) -> Unit,
  onCheckPreApprovedPhone: (buildingId: String, phone: String) -> Unit = { _, _ -> },
  onSubmitJoinRequest: (
    invite: InviteInfo,
    apartmentLabel: String,
    occupancyType: OccupancyType,
    phone: String,
    phoneVerifiedViaWhatsApp: Boolean,
  ) -> Unit,
  onSelectExistingBuilding: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(if (!initialInviteCode.isNullOrBlank()) 1 else 0) }
  var showCreationForms by remember(myMember?.status) {
    mutableStateOf(myMember == null)
  }

  LaunchedEffect(initialInviteCode) {
    if (!initialInviteCode.isNullOrBlank()) {
      selectedTab = 1
      showCreationForms = true
      onLookupInvite(initialInviteCode)
    }
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = s.appName,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          ThemeToggleChip(isDarkTheme = isDarkTheme, s = s, onToggle = onToggleTheme)
          LanguageToggleChip(s = s, onToggle = onToggleLanguage)
          IconButton(onClick = onSignOut) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = s.signOutAction)
          }
        }
      }

      if (isBusy) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      if (!feedbackMessage.isNullOrBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = feedbackMessage,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      if (myMember != null && !showCreationForms) {
        val status = myMember.statusEnum()
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.HourglassTop,
              contentDescription = null,
              tint =
                if (status == MemberStatus.REJECTED) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text =
                if (status == MemberStatus.REJECTED) s.rejectedTitle
                else s.pendingApprovalTitle,
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = myMember.apartmentLabel,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = s.pendingApprovalSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { showCreationForms = true }) {
              Text("${s.createBuildingTab} / ${s.joinBuildingTab}")
            }
          }
        }
      } else {
        Text(
          text = s.onboardingWelcome,
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text = s.onboardingSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (memberships.isNotEmpty()) {
          Text(s.switchBuildingAction, style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            memberships.forEach { m ->
              FilterChip(
                selected = false,
                onClick = { onSelectExistingBuilding(m.buildingId) },
                label = { Text("${m.buildingName} (${m.city})") },
                leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
              )
            }
          }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("tab_create_building"),
          ) {
            Text(s.createBuildingTab)
          }
          SegmentedButton(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("tab_join_building"),
          ) {
            Text(s.joinBuildingTab)
          }
        }

        if (selectedTab == 0) {
          CreateBuildingForm(s = s, isBusy = isBusy, onSubmit = onCreateBuilding)
        } else {
          JoinBuildingForm(
            s = s,
            initialCode = initialInviteCode.orEmpty(),
            lookedUpInvite = lookedUpInvite,
            preApprovedMatch = preApprovedMatch,
            isBusy = isBusy,
            onLookupInvite = onLookupInvite,
            onCheckPreApprovedPhone = onCheckPreApprovedPhone,
            onSubmitJoinRequest = onSubmitJoinRequest,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateBuildingForm(
  s: AppStrings,
  isBusy: Boolean,
  onSubmit: (String, String, String, Int, Int, Double, Double, String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var city by remember { mutableStateOf("الدار البيضاء") }
  var neighborhood by remember { mutableStateOf("") }
  var floors by remember { mutableStateOf("5") }
  var apartments by remember { mutableStateOf("10") }
  var defaultFee by remember { mutableStateOf("250") }
  var reserveFund by remember { mutableStateOf("5000") }
  var syndicApt by remember { mutableStateOf("طابق 1 - شقة 1") }

  val moroccanCities =
    if (s.langCode == "AR") {
      listOf("الدار البيضاء", "الرباط", "مراكش", "طنجة", "أكادير", "فاس")
    } else {
      listOf("Casablanca", "Rabat", "Marrakech", "Tanger", "Agadir", "Fès")
    }

  Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(s.buildingNameLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("input_building_name"),
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
      Button(
        onClick = {
          onSubmit(
            name,
            city,
            neighborhood,
            floors.toIntOrNull() ?: 4,
            apartments.toIntOrNull() ?: 8,
            defaultFee.toDoubleOrNull() ?: 200.0,
            reserveFund.toDoubleOrNull() ?: 0.0,
            syndicApt,
          )
        },
        enabled = !isBusy && name.trim().length >= 2 && city.trim().length >= 2,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_building_submit"),
      ) {
        Icon(Icons.Default.Apartment, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(s.createBuildingAction)
      }
    }
  }
}

@Composable
private fun JoinBuildingForm(
  s: AppStrings,
  initialCode: String,
  lookedUpInvite: InviteInfo?,
  preApprovedMatch: PreApprovedPhone?,
  isBusy: Boolean,
  onLookupInvite: (String) -> Unit,
  onCheckPreApprovedPhone: (String, String) -> Unit,
  onSubmitJoinRequest: (InviteInfo, String, OccupancyType, String, Boolean) -> Unit,
) {
  val context = LocalContext.current
  val semantic = LocalSyndicColors.current
  var code by remember(initialCode) { mutableStateOf(initialCode.uppercase()) }
  var apartmentLabel by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var occupancy by remember { mutableStateOf(OccupancyType.OWNER) }
  var otpCode by remember { mutableStateOf("") }
  var generatedOtpPreview by remember { mutableStateOf<String?>(null) }
  var isPhoneVerified by remember { mutableStateOf(false) }

  LaunchedEffect(preApprovedMatch) {
    if (preApprovedMatch != null && apartmentLabel.isBlank() && preApprovedMatch.apartmentLabel.isNotBlank()) {
      apartmentLabel = preApprovedMatch.apartmentLabel
    }
  }

  Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = code,
          onValueChange = { code = it.uppercase().take(6) },
          label = { Text(s.inviteCodeInputLabel) },
          leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("input_invite_code"),
        )
        Button(
          onClick = { onLookupInvite(code) },
          enabled = !isBusy && code.trim().length == 6,
          modifier = Modifier.height(52.dp).testTag("lookup_invite_button"),
        ) {
          Icon(Icons.Default.Search, contentDescription = s.lookupBuildingAction)
        }
      }

      if (lookedUpInvite != null) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = lookedUpInvite.buildingName,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = lookedUpInvite.city,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }

        OutlinedTextField(
          value = phone,
          onValueChange = {
            phone = it
            isPhoneVerified = false
            if (SokanAccountStore.normalizePhoneKey(it).length >= 9) {
              onCheckPreApprovedPhone(lookedUpInvite.buildingId, it)
            }
          },
          label = { Text(s.contactPhoneLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_join_phone"),
        )

        if (preApprovedMatch != null) {
          Surface(
            color = semantic.paidContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = semantic.paidText, modifier = Modifier.size(18.dp))
              Text(
                text = "${s.phoneMatchedAutoApproveMsg} (${preApprovedMatch.apartmentLabel})",
                color = semantic.paidText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        // WhatsApp OTP Phone Verification Row for Instant Approval
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = otpCode,
            onValueChange = {
              val clean = it.filter { ch -> ch.isDigit() }.take(6)
              otpCode = clean
              if (clean.length == 6 && SokanAccountStore.verifyWhatsAppAuthCode(context, phone, clean)) {
                isPhoneVerified = true
              }
            },
            label = { Text(s.sokanOtpLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_join_whatsapp_otp"),
          )
          FilledTonalButton(
            onClick = {
              if (SokanAccountStore.normalizePhoneKey(phone).length >= 6) {
                val issued = SokanAccountStore.issueWhatsAppAuthCode(context, phone)
                generatedOtpPreview = issued
                runCatching {
                  ReceiptGenerator.sendWhatsAppAuthCode(context, phone, issued, lookedUpInvite.buildingName)
                }
              }
            },
            enabled = SokanAccountStore.normalizePhoneKey(phone).length >= 6,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier.testTag("join_send_whatsapp_otp"),
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(s.sendWhatsappOtpAction, style = MaterialTheme.typography.labelSmall)
          }
        }

        if (generatedOtpPreview != null) {
          Text(
            text = "${s.otpSentHint} $generatedOtpPreview",
            style = MaterialTheme.typography.labelMedium,
            color = semantic.paidText,
            fontWeight = FontWeight.Bold,
          )
        }
        if (isPhoneVerified) {
          Surface(
            color = semantic.paidContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = s.phoneVerifiedBadge,
              color = semantic.paidText,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        OutlinedTextField(
          value = apartmentLabel,
          onValueChange = { apartmentLabel = it },
          label = { Text(s.selectApartmentLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_join_apartment"),
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

        Button(
          onClick = {
            if (isPhoneVerified && SokanAccountStore.normalizePhoneKey(phone).length >= 6) {
              SokanAccountStore.registerOrUpdateAccount(
                context = context,
                rawPhone = phone,
                displayName = preApprovedMatch?.residentName.orEmpty().ifBlank { "ساكن" },
                plainPassword = "",
                buildingId = lookedUpInvite.buildingId,
                buildingName = lookedUpInvite.buildingName,
                apartmentLabel = apartmentLabel,
              )
            }
            onSubmitJoinRequest(lookedUpInvite, apartmentLabel, occupancy, phone, isPhoneVerified)
          },
          enabled = !isBusy && (apartmentLabel.trim().isNotBlank() || preApprovedMatch != null),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_join_request"),
        ) {
          Text(s.sendJoinRequest)
        }
      }
    }
  }
}
