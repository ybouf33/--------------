package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Announcement
import com.example.data.model.Apartment
import com.example.data.model.Building
import com.example.data.model.BuildingContact
import com.example.data.model.BuildingMember
import com.example.data.model.ContactCategory
import com.example.data.model.CotisationPayment
import com.example.data.model.MemberRole
import com.example.data.model.MemberStatus
import com.example.data.model.OccupancyType
import com.example.data.model.Poll
import com.example.data.model.PollVote
import com.example.data.model.UserMembership
import com.example.data.util.ReceiptGenerator
import com.example.data.util.SokanAccountStore
import com.example.ui.components.AssignResidentUnitDialog
import com.example.ui.components.CreatePollDialog
import com.example.ui.components.DiffuseAnnouncementDialog
import com.example.ui.components.EditBuildingSettingsDialog
import com.example.ui.components.ExecutiveGradientCard
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.PostAnnouncementDialog
import com.example.ui.components.RoleBadge
import com.example.ui.components.SokanAuthAndRecoveryDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.ThemeToggleChip
import com.example.ui.components.announcementCategoryLabel
import com.example.ui.components.audienceLabel
import com.example.ui.components.formatMad
import com.example.ui.components.unitTypeLabel
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors
import com.example.ui.viewmodel.SyndicController
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BuildingScreen(
  s: AppStrings,
  building: Building,
  myMember: BuildingMember,
  apartments: List<Apartment>,
  payments: List<CotisationPayment>,
  announcements: List<Announcement>,
  polls: List<Poll>,
  members: List<BuildingMember>,
  contacts: List<BuildingContact>,
  memberships: List<UserMembership>,
  controller: SyndicController,
  onOpenPortfolio: () -> Unit,
  onSignOut: () -> Unit,
  signOutLabel: String = s.signOutAction,
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val semantic = LocalSyndicColors.current
  val isAdmin = myMember.isAdmin()
  val isSyndic = myMember.statusEnum() == MemberStatus.APPROVED && myMember.roleEnum() == MemberRole.SYNDIC

  var subTab by remember { mutableIntStateOf(0) }
  var showPostAnnouncement by remember { mutableStateOf(false) }
  var showCreatePoll by remember { mutableStateOf(false) }
  var announcementToDiffuse by remember { mutableStateOf<Announcement?>(null) }
  var showAddContact by remember { mutableStateOf(false) }
  var showAddUnitDialog by remember { mutableStateOf(false) }
  var showEditBuildingSettings by remember { mutableStateOf(false) }
  var showSokanAccountDialog by remember { mutableStateOf(false) }
  var apartmentToAssign by remember { mutableStateOf<Apartment?>(null) }

  val pendingMembers = remember(members) { members.filter { it.statusEnum() == MemberStatus.PENDING } }
  val approvedMembers = remember(members) { members.filter { it.statusEnum() == MemberStatus.APPROVED } }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 3 Separated Sub-Tabs: Broadcasts & Polls | Units & Phones | Management
    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = subTab == 0,
          onClick = { subTab = 0 },
          shape = SegmentedButtonDefaults.itemShape(0, 3),
          modifier = Modifier.testTag("sokan_subtab_broadcasts"),
        ) {
          Text(s.subTabBroadcasts, maxLines = 1)
        }
        SegmentedButton(
          selected = subTab == 1,
          onClick = { subTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(1, 3),
          modifier = Modifier.testTag("sokan_subtab_units"),
        ) {
          Text(s.subTabUnitsPhones, maxLines = 1)
        }
        SegmentedButton(
          selected = subTab == 2,
          onClick = { subTab = 2 },
          shape = SegmentedButtonDefaults.itemShape(2, 3),
          modifier = Modifier.testTag("sokan_subtab_management"),
        ) {
          Text(s.subTabManagement, maxLines = 1)
        }
      }
    }

    if (subTab == 0) {
      // Sub-tab 0: Broadcasts & General Assembly Polls scoped to Active 3imara
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
            Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(s.announcementsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          }
          if (isAdmin) {
            Button(
              onClick = { showPostAnnouncement = true },
              modifier = Modifier.testTag("post_announcement_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text(s.broadcastMessageAction)
            }
          }
        }
      }

      if (announcements.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Text(
              text = s.noAnnouncements,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp).fillMaxWidth(),
            )
          }
        }
      } else {
        items(announcements, key = { "${building.id}_${it.id}" }) { ann ->
          AnnouncementItemCard(
            ann = ann,
            s = s,
            isAdmin = isAdmin,
            onTogglePin = { controller.toggleAnnouncementPin(ann.id, !ann.pinned) },
            onDiffuse = { announcementToDiffuse = ann },
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Default.HowToVote, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(s.pollsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          }
          if (isAdmin) {
            OutlinedButton(
              onClick = { showCreatePoll = true },
              modifier = Modifier.testTag("create_poll_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text(s.createPollAction)
            }
          }
        }
      }

      if (polls.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Text(
              text = s.noPollsYet,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp).fillMaxWidth(),
            )
          }
        }
      } else {
        items(polls, key = { "${building.id}_poll_${it.id}" }) { poll ->
          val votes by
            remember(building.id, poll.id) { controller.observePollVotes(building.id, poll.id) }
              .collectAsStateWithLifecycle(initialValue = emptyList())
          PollItemCard(
            poll = poll,
            votes = votes,
            currentUserId = controller.currentUserId,
            isAdmin = isAdmin,
            s = s,
            onVote = { idx -> controller.castVote(poll.id, idx) },
            onToggleClose = { controller.closePoll(poll.id, !poll.closed) },
          )
        }
      }
    } else if (subTab == 1) {
      // Sub-tab 1: Apartments / Shops & Resident Phones Directory
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${s.apartmentsDirectoryTitle} (${apartments.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          if (isAdmin) {
            Button(
              onClick = { showAddUnitDialog = true },
              modifier = Modifier.testTag("add_unit_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(s.addUnitAction)
            }
          }
        }
      }

      items(apartments, key = { "apt_${it.id}" }) { apt ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, semantic.hairline),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
              ) {
                Text(
                  text = apt.unitNumber,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
              Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = apt.label,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "${unitTypeLabel(apt.unitTypeEnum(), s)} • ${formatMad(apt.monthlyFee, s)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  StatusPill(
                    text = if (apt.occupancyEnum() == OccupancyType.OWNER) s.occupancyOwner else s.occupancyTenant,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
                Text(
                  text =
                    if (apt.residentName.isNotBlank()) {
                      listOf(apt.residentName, apt.residentPhone).filter { it.isNotBlank() }.joinToString(" • ")
                    } else {
                      s.unassignedUnit
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (apt.residentName.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = if (apt.residentName.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (apt.residentPhone.isNotBlank()) {
                IconButton(
                  onClick = { ReceiptGenerator.openPhoneDialer(context, apt.residentPhone) },
                ) {
                  Icon(Icons.Default.Call, contentDescription = s.callAction, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                  onClick = {
                    if (isAdmin) {
                      ReceiptGenerator.sendPhoneApprovedNoticeViaWhatsApp(
                        context = context,
                        building = building,
                        residentName = apt.residentName,
                        residentPhone = apt.residentPhone,
                        apartmentLabel = apt.label,
                      )
                    } else {
                      ReceiptGenerator.openWhatsAppChat(
                        context = context,
                        rawPhone = apt.residentPhone,
                        message = "السلام عليكم ${apt.residentName} (${building.name} - ${apt.label})",
                      )
                    }
                  },
                ) {
                  Icon(
                    Icons.Default.Share,
                    contentDescription = if (isAdmin) s.notifyPhoneApprovedAction else s.whatsappAction,
                    tint = MaterialTheme.colorScheme.secondary,
                  )
                }
              }
              if (isAdmin) {
                IconButton(
                  onClick = { apartmentToAssign = apt },
                  modifier = Modifier.testTag("assign_apt_${apt.id}"),
                ) {
                  Icon(
                    imageVector = if (apt.residentName.isBlank()) Icons.Default.PersonAdd else Icons.Default.Edit,
                    contentDescription = s.assignResidentTitle,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }
    } else {
      // Sub-tab 2: Invite Code, Approvals Gate, Emergency Contacts, Members RBAC, Building Settings
      item {
        ExecutiveGradientCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = s.inviteCodeTitle,
                style = MaterialTheme.typography.titleMedium,
                color = semantic.heroContent,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${building.name} • ${building.city}",
                style = MaterialTheme.typography.bodySmall,
                color = semantic.heroSubtleContent,
              )
            }
            Surface(
              color = semantic.heroChipBg,
              border = BorderStroke(1.dp, semantic.heroBorder),
              shape = MaterialTheme.shapes.small,
            ) {
              Text(
                text = building.inviteCode,
                style = MaterialTheme.typography.titleLarge,
                color = semantic.heroContent,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { ReceiptGenerator.shareInviteViaWhatsApp(context, building) },
              modifier = Modifier.weight(1f).testTag("share_whatsapp_invite"),
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(s.shareViaWhatsapp)
            }
            OutlinedButton(
              onClick = {
                clipboard.setText(AnnotatedString(building.inviteCode))
                Toast.makeText(context, s.codeCopiedMsg, Toast.LENGTH_SHORT).show()
              },
              border = BorderStroke(1.dp, semantic.heroContent.copy(alpha = 0.28f)),
            ) {
              Icon(
                Icons.Default.ContentCopy,
                contentDescription = s.copyInviteCode,
                modifier = Modifier.size(18.dp),
                tint = semantic.heroContent,
              )
            }
          }
        }
      }

      if (isAdmin && pendingMembers.isNotEmpty()) {
        item {
          Text(
            text = "${s.pendingRequestsTitle} (${pendingMembers.size})",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
          )
        }
        items(pendingMembers, key = { "pending_${it.uid}" }) { member ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, semantic.hairline),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(member.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                  text =
                    "${member.apartmentLabel} • " +
                      if (member.occupancyEnum() == OccupancyType.OWNER) s.occupancyOwner else s.occupancyTenant,
                  style = MaterialTheme.typography.bodySmall,
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                  onClick = {
                    controller.approveOrRejectMember(member.uid, MemberStatus.APPROVED, MemberRole.RESIDENT)
                  },
                  modifier = Modifier.testTag("approve_member_${member.uid}"),
                ) {
                  Icon(Icons.Default.Check, contentDescription = s.approveAction, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                  onClick = { controller.approveOrRejectMember(member.uid, MemberStatus.REJECTED) },
                ) {
                  Icon(Icons.Default.Close, contentDescription = s.rejectAction, tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(s.contactsDirectoryTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          if (isAdmin) {
            OutlinedButton(onClick = { showAddContact = true }) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(s.addContactAction)
            }
          }
        }
      }

      items(contacts, key = { it.id }) { contact ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, semantic.hairline),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                text = "${contact.roleTitle} • ${contact.phone}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Button(onClick = { ReceiptGenerator.openPhoneDialer(context, contact.phone) }) {
              Icon(Icons.Default.Call, contentDescription = s.callAction, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(s.callAction)
            }
          }
        }
      }

      item {
        Text(
          text = "${s.residentsDirectoryTitle} (${approvedMembers.size})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
      }

      items(approvedMembers, key = { "member_${it.uid}" }) { member ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, semantic.hairline),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(member.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                text =
                  "${member.apartmentLabel} • " +
                    if (member.occupancyEnum() == OccupancyType.OWNER) s.occupancyOwner else s.occupancyTenant,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (isSyndic && member.uid != myMember.uid && member.roleEnum() != MemberRole.SYNDIC) {
                val nextRole =
                  if (member.roleEnum() == MemberRole.DEPUTY) MemberRole.RESIDENT else MemberRole.DEPUTY
                val btnText =
                  if (member.roleEnum() == MemberRole.DEPUTY) s.demoteResident else s.promoteDeputy
                TextButton(
                  onClick = {
                    controller.approveOrRejectMember(member.uid, MemberStatus.APPROVED, nextRole)
                  },
                  contentPadding = PaddingValues(0.dp),
                ) {
                  Text(btnText, style = MaterialTheme.typography.labelMedium)
                }
              }
            }
            RoleBadge(role = member.roleEnum(), s = s)
          }
        }
      }

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
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                  Text(building.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text(
                    text = "${memberships.size.coerceAtLeast(1)} ${s.buildingsCountLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeToggleChip(isDarkTheme = isDarkTheme, s = s, onToggle = onToggleTheme)
                LanguageToggleChip(s = s, onToggle = { controller.toggleLanguage() })
              }
            }
            OutlinedButton(
              onClick = { showSokanAccountDialog = true },
              modifier = Modifier.fillMaxWidth().testTag("sokan_account_recovery_settings_button"),
            ) {
              Icon(Icons.Default.PersonAdd, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(s.recoverAccountTitle)
            }
            if (isAdmin) {
              OutlinedButton(
                onClick = { showEditBuildingSettings = true },
                modifier = Modifier.fillMaxWidth().testTag("edit_building_settings_button"),
              ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(s.editBuildingSettingsAction)
              }
            }
            OutlinedButton(
              onClick = onOpenPortfolio,
              modifier = Modifier.fillMaxWidth().testTag("open_portfolio_button"),
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(s.switchBuildingAction)
            }
            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier.fillMaxWidth().testTag("sign_out_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Spacer(Modifier.width(8.dp))
              Text(signOutLabel, color = MaterialTheme.colorScheme.error)
            }
          }
        }
      }
    }
  }

  if (showSokanAccountDialog) {
    SokanAuthAndRecoveryDialog(
      s = s,
      initialTab = 1,
      initialPhone = myMember.phone,
      initialName = myMember.displayName,
      onDismiss = { showSokanAccountDialog = false },
      onLoginSuccess = { profile, wasRecovered, plainPass ->
        SokanAccountStore.registerOrUpdateAccount(
          context = context,
          rawPhone = profile.displayPhone,
          displayName = profile.displayName,
          plainPassword = plainPass,
          buildingId = building.id,
          buildingName = building.name,
          apartmentLabel = myMember.apartmentLabel,
        )
        controller.syncOrRecoverResidentPhone(
          oldPhone = myMember.phone.ifBlank { profile.displayPhone },
          newPhone = profile.displayPhone,
          displayName = profile.displayName,
          passwordPlain = plainPass,
          isRecovery = wasRecovered,
        )
        showSokanAccountDialog = false
      },
    )
  }

  if (showPostAnnouncement) {
    PostAnnouncementDialog(
      s = s,
      buildingName = building.name,
      onDismiss = { showPostAnnouncement = false },
      onSubmit = { title, body, cat, audience, pinned ->
        controller.createAnnouncement(title, body, cat, audience, pinned)
        showPostAnnouncement = false
      },
    )
  }

  val diffuseTarget = announcementToDiffuse
  if (diffuseTarget != null) {
    DiffuseAnnouncementDialog(
      s = s,
      building = building,
      announcement = diffuseTarget,
      apartments = apartments,
      payments = payments,
      onDismiss = { announcementToDiffuse = null },
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
      },
    )
  }

  if (showAddContact) {
    AddContactDialog(
      s = s,
      onDismiss = { showAddContact = false },
      onSubmit = { name, roleTitle, cat, phone ->
        controller.addContact(name, roleTitle, cat, phone, "")
        showAddContact = false
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

  val targetApt = apartmentToAssign
  if (showAddUnitDialog || targetApt != null) {
    AssignResidentUnitDialog(
      s = s,
      existingApartment = targetApt,
      defaultFee = building.defaultMonthlyFee,
      suggestedUnitNumber = (apartments.size + 1).toString(),
      onDismiss = {
        showAddUnitDialog = false
        apartmentToAssign = null
      },
      onSubmitExisting = { apt, newFee, unitType, label, residentName, residentPhone, occupancyType ->
        controller.updateApartmentDetails(
          apartment = apt,
          newFee = newFee,
          unitType = unitType,
          label = label,
          residentName = residentName,
          residentPhone = residentPhone,
          occupancyType = occupancyType,
        )
        apartmentToAssign = null
      },
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
        showAddUnitDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnnouncementItemCard(
  ann: Announcement,
  s: AppStrings,
  isAdmin: Boolean,
  onTogglePin: () -> Unit,
  onDiffuse: () -> Unit,
) {
  val semantic = LocalSyndicColors.current
  val dateStr =
    remember(ann.createdAt) {
      ann.createdAt?.toDate()?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(it) } ?: ""
    }
  Card(
    border = BorderStroke(1.dp, semantic.hairline),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (ann.pinned) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
          else MaterialTheme.colorScheme.surface,
      ),
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
        FlowRow(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          StatusPill(
            text = announcementCategoryLabel(ann.categoryEnum(), s),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          StatusPill(
            text = audienceLabel(ann.audienceEnum(), s),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          )
          if (ann.pinned) {
            StatusPill(
              text = s.pinnedNotice,
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
        if (isAdmin) {
          IconButton(onClick = onTogglePin) {
            Icon(
              imageVector = if (ann.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
              contentDescription = s.pinnedNotice,
              tint = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }
      Text(ann.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(ann.body, style = MaterialTheme.typography.bodyMedium)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = listOf(ann.authorName, dateStr).filter { it.isNotBlank() }.joinToString(" • "),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        if (isAdmin) {
          OutlinedButton(
            onClick = onDiffuse,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("diffuse_announcement_${ann.id}"),
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.diffuseResidentsAction, style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}

@Composable
private fun PollItemCard(
  poll: Poll,
  votes: List<PollVote>,
  currentUserId: String,
  isAdmin: Boolean,
  s: AppStrings,
  onVote: (Int) -> Unit,
  onToggleClose: () -> Unit,
) {
  val semantic = LocalSyndicColors.current
  val totalVotes = votes.size
  val myVoteIndex = votes.firstOrNull { it.uid == currentUserId }?.optionIndex

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
          text = poll.question,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        if (poll.closed) {
          StatusPill(
            text = s.pollClosedBadge,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }

      if (poll.description.isNotBlank()) {
        Text(poll.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      poll.options.forEachIndexed { index, optionText ->
        val count = votes.count { it.optionIndex == index }
        val fraction = if (totalVotes > 0) count.toFloat() / totalVotes.toFloat() else 0f
        val isSelected = myVoteIndex == index

        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.small)
              .clickable(enabled = !poll.closed) { onVote(index) },
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
              ) {
                if (isSelected) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Text(optionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
              }
              Text(
                text = "${(fraction * 100).toInt()}% ($count)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { fraction },
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "$totalVotes ${s.votesCountLabel}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isAdmin && !poll.closed) {
          TextButton(onClick = onToggleClose) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(s.closePollAction)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddContactDialog(
  s: AppStrings,
  onDismiss: () -> Unit,
  onSubmit: (String, String, ContactCategory, String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var roleTitle by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(ContactCategory.CONCIERGE) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(s.addContactAction) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(s.contactNameLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = roleTitle,
          onValueChange = { roleTitle = it },
          label = { Text(s.contactRoleLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text(s.contactPhoneLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ContactCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.name) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(name, roleTitle, category, phone) },
        enabled = name.trim().length >= 2 && roleTitle.trim().length >= 2 && phone.trim().length >= 2,
      ) {
        Text(s.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}
