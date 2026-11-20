package com.example.ui.guest

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.Apartment
import com.example.data.model.BroadcastAudience
import com.example.data.model.Building
import com.example.data.model.BuildingContact
import com.example.data.model.BuildingIssue
import com.example.data.model.BuildingMember
import com.example.data.model.BuildingNotification
import com.example.data.model.BuildingOverview
import com.example.data.model.BuildingWork
import com.example.data.model.ContactCategory
import com.example.data.model.CotisationPayment
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.IssueCategory
import com.example.data.model.IssueComment
import com.example.data.model.IssueStatus
import com.example.data.model.IssueUpvote
import com.example.data.model.IssueUrgency
import com.example.data.model.MemberRole
import com.example.data.model.MemberStatus
import com.example.data.model.NotificationType
import com.example.data.model.OccupancyType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Poll
import com.example.data.model.PollVote
import com.example.data.model.UnitType
import com.example.data.model.UserMembership
import com.example.data.model.WorkCategory
import com.example.data.model.WorkStatus
import com.example.data.util.SokanAccountStore
import com.example.ui.i18n.AppLanguage
import com.example.ui.viewmodel.SyndicController
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private data class DemoBuildingWorkspace(
  val building: Building,
  val myMember: BuildingMember,
  val members: List<BuildingMember>,
  val apartments: List<Apartment>,
  val payments: List<CotisationPayment>,
  val expenses: List<Expense>,
  val issues: List<BuildingIssue>,
  val issueUpvotes: Map<String, List<IssueUpvote>>,
  val issueComments: Map<String, List<IssueComment>>,
  val announcements: List<Announcement>,
  val polls: List<Poll>,
  val pollVotes: Map<String, List<PollVote>>,
  val contacts: List<BuildingContact>,
  val works: List<BuildingWork> = emptyList(),
  val notifications: List<BuildingNotification> = emptyList(),
)

/**
 * Local-only multi-building in-memory sandbox controller for Guest & Sokan (Resident) preview.
 * Supports multiple buildings ("3imarat"), resident phone assignment, broadcasts, live notifications,
 * invite-link phone auto-approval, and monthly/6-month cotisation tracking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuestSessionController(
  initialLanguage: AppLanguage,
  private val initialRole: MemberRole = MemberRole.SYNDIC,
  private val initialSokanPhone: String? = null,
  private val initialSokanName: String? = null,
  private val initialBuildingIds: List<String> = emptyList(),
) : SyndicController {
  override val currentUserId: String = GUEST_UID
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _language = MutableStateFlow(initialLanguage)
  val language: StateFlow<AppLanguage> = _language.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val sampleInvoiceBase64 by lazy { createDemoInvoiceBase64() }

  private val monthKeysWindow: List<String> = run {
    val cal = Calendar.getInstance()
    (5 downTo 0).map { offset ->
      val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
      "%04d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }
  }
  private val currentMonthKey: String = monthKeysWindow.last()

  private val _workspaces = MutableStateFlow(createInitialWorkspaces())
  private val _activeBuildingId = MutableStateFlow(resolveInitialBuildingId(_workspaces.value))

  private fun resolveInitialBuildingId(map: Map<String, DemoBuildingWorkspace>): String {
    val normalizedTarget = initialSokanPhone?.let { SokanAccountStore.normalizePhoneKey(it) }.orEmpty()
    if (normalizedTarget.length >= 6) {
      val byPhone =
        map.entries.firstOrNull { (_, ws) ->
          ws.apartments.any { SokanAccountStore.normalizePhoneKey(it.residentPhone) == normalizedTarget }
        }?.key
      if (byPhone != null) return byPhone
    }
    val byId = initialBuildingIds.firstOrNull { map.containsKey(it) }
    return byId ?: DEMO_BUILDING_1_ID
  }

  private val activeWorkspace: StateFlow<DemoBuildingWorkspace> =
    combine(_workspaces, _activeBuildingId) { map, activeId ->
      map[activeId] ?: map.values.first()
    }.stateIn(scope, SharingStarted.Eagerly, _workspaces.value.getValue(_activeBuildingId.value))

  val memberships: StateFlow<List<UserMembership>> =
    _workspaces
      .map { map ->
        map.values.map { ws ->
          UserMembership(
            id = ws.building.id,
            buildingId = ws.building.id,
            buildingName = ws.building.name,
            city = ws.building.city,
            apartmentLabel = ws.myMember.apartmentLabel,
          )
        }
      }
      .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val buildingOverviews: StateFlow<Map<String, BuildingOverview>> =
    _workspaces
      .map { map ->
        map.mapValues { (bId, ws) ->
          val totalCollected =
            ws.payments.filter { it.statusEnum() == PaymentStatus.PAID }.sumOf { it.amountMad }
          val totalSpent = ws.expenses.sumOf { it.amountMad }
          val paidThisMonth =
            ws.payments.count { it.monthKey == currentMonthKey && it.statusEnum() == PaymentStatus.PAID }
          val openIssues = ws.issues.count { it.statusEnum() != IssueStatus.RESOLVED }
          val activeWorks = ws.works.count { it.statusEnum() != WorkStatus.COMPLETED }
          BuildingOverview(
            buildingId = bId,
            apartmentCount = ws.apartments.size,
            paidThisMonthCount = paidThisMonth,
            netBalanceMad = totalCollected - totalSpent,
            announcementsCount = ws.announcements.size,
            openIssuesCount = openIssues,
            activeWorksCount = activeWorks,
          )
        }
      }
      .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val building: StateFlow<Building> =
    activeWorkspace.map { it.building }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.building)

  val myMember: StateFlow<BuildingMember> =
    activeWorkspace.map { it.myMember }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.myMember)

  val members: StateFlow<List<BuildingMember>> =
    activeWorkspace.map { it.members }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.members)

  val apartments: StateFlow<List<Apartment>> =
    activeWorkspace.map { it.apartments }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.apartments)

  val payments: StateFlow<List<CotisationPayment>> =
    activeWorkspace.map { it.payments }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.payments)

  val expenses: StateFlow<List<Expense>> =
    activeWorkspace.map { it.expenses }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.expenses)

  val issues: StateFlow<List<BuildingIssue>> =
    activeWorkspace.map { it.issues }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.issues)

  val announcements: StateFlow<List<Announcement>> =
    activeWorkspace.map { it.announcements }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.announcements)

  val polls: StateFlow<List<Poll>> =
    activeWorkspace.map { it.polls }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.polls)

  val contacts: StateFlow<List<BuildingContact>> =
    activeWorkspace.map { it.contacts }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.contacts)

  val works: StateFlow<List<BuildingWork>> =
    activeWorkspace.map { it.works }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.works)

  override val notificationsState: StateFlow<List<BuildingNotification>> =
    activeWorkspace.map { it.notifications }.stateIn(scope, SharingStarted.Eagerly, activeWorkspace.value.notifications)

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun showNote(message: String) {
    _feedbackMessage.value = message
  }

  fun togglePreviewRole() {
    updateActiveWorkspace { ws ->
      val nextRole =
        if (ws.myMember.roleEnum() == MemberRole.SYNDIC) MemberRole.RESIDENT else MemberRole.SYNDIC
      val updatedMember = ws.myMember.copy(role = nextRole.name)
      ws.copy(
        myMember = updatedMember,
        members = ws.members.map { if (it.uid == GUEST_UID) updatedMember else it },
      )
    }
  }

  override fun switchActiveBuilding(buildingId: String) {
    if (_workspaces.value.containsKey(buildingId)) {
      _activeBuildingId.value = buildingId
    }
  }

  override fun createBuilding(
    name: String,
    city: String,
    neighborhood: String,
    floorCount: Int,
    apartmentCount: Int,
    defaultFee: Double,
    reserveFund: Double,
    syndicAptLabel: String,
  ) {
    val cleanName = name.trim().take(120)
    val cleanCity = city.trim().take(80)
    if (cleanName.length < 2 || cleanCity.length < 2) return
    val bId = "demo_bld_${UUID.randomUUID().toString().take(6)}"
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val code = buildString(6) { repeat(6) { append(chars[Random.nextInt(chars.length)]) } }
    val cleanApts = apartmentCount.coerceIn(1, 30)
    val cleanFloors = floorCount.coerceIn(1, 50)
    val perFloor = ((cleanApts + cleanFloors - 1) / cleanFloors).coerceAtLeast(1)

    val newApts =
      (1..cleanApts).map { i ->
        val floor = ((i - 1) / perFloor) + 1
        Apartment(
          id = "${bId}_apt_$i",
          unitNumber = i.toString(),
          floor = floor,
          label = if (i == 1 && syndicAptLabel.isNotBlank()) syndicAptLabel else "طابق $floor - شقة $i",
          unitType = UnitType.APARTMENT.name,
          monthlyFee = defaultFee,
          residentName = if (i == 1) "كريم العلمي (السانديك)" else "",
          residentPhone = if (i == 1) "0661001122" else "",
          occupancyType = OccupancyType.OWNER.name,
          residentUid = if (i == 1) GUEST_UID else "",
        )
      }

    val syndicMember =
      BuildingMember(
        id = GUEST_UID,
        uid = GUEST_UID,
        displayName = "كريم العلمي (ضيف)",
        apartmentId = newApts.first().id,
        apartmentLabel = syndicAptLabel.ifBlank { "طابق 1 - شقة 1" },
        occupancyType = OccupancyType.OWNER.name,
        role = MemberRole.SYNDIC.name,
        status = MemberStatus.APPROVED.name,
        phone = "0661001122",
      )

    val ws =
      DemoBuildingWorkspace(
        building =
          Building(
            id = bId,
            name = cleanName,
            city = cleanCity,
            neighborhood = neighborhood.trim(),
            floorCount = cleanFloors,
            apartmentCount = cleanApts,
            defaultMonthlyFee = defaultFee,
            reserveFundBalance = reserveFund,
            inviteCode = code,
            syndicUid = GUEST_UID,
          ),
        myMember = syndicMember,
        members = listOf(syndicMember),
        apartments = newApts,
        payments = emptyList(),
        expenses = emptyList(),
        issues = emptyList(),
        issueUpvotes = emptyMap(),
        issueComments = emptyMap(),
        announcements =
          listOf(
            Announcement(
              id = "${bId}_ann_1",
              title = "مرحباً بكم في فضاء $cleanName",
              body = "تم تفعيل فضاء العمارة. يمكن للسانديك الآن تعيين السكان وتتبع الأداء الشهري وبث الرسائل.",
              category = AnnouncementCategory.GENERAL.name,
              targetAudience = BroadcastAudience.ALL.name,
              pinned = true,
              authorUid = GUEST_UID,
              authorName = syndicMember.displayName,
              createdAt = Timestamp.now(),
            )
          ),
        polls = emptyList(),
        pollVotes = emptyMap(),
        contacts =
          listOf(
            BuildingContact("${bId}_c1", "الوقاية المدنية", "طوارئ", ContactCategory.EMERGENCY.name, "15"),
            BuildingContact("${bId}_c2", "الشرطة", "طوارئ", ContactCategory.EMERGENCY.name, "19"),
          ),
      )

    _workspaces.value = _workspaces.value + (bId to ws)
    _activeBuildingId.value = bId
  }

  override fun observePollVotes(buildingId: String, pollId: String): Flow<List<PollVote>> =
    _workspaces.map { it[buildingId]?.pollVotes?.get(pollId).orEmpty() }

  override fun observeIssueUpvotes(buildingId: String, issueId: String): Flow<List<IssueUpvote>> =
    _workspaces.map { it[buildingId]?.issueUpvotes?.get(issueId).orEmpty() }

  override fun observeIssueComments(buildingId: String, issueId: String): Flow<List<IssueComment>> =
    _workspaces.map { it[buildingId]?.issueComments?.get(issueId).orEmpty() }

  override fun toggleLanguage() {
    _language.value = if (_language.value == AppLanguage.AR) AppLanguage.FR else AppLanguage.AR
  }

  override fun markAllNotificationsRead() {
    updateActiveWorkspace { ws ->
      ws.copy(
        notifications =
          ws.notifications.map { notif ->
            if (notif.readByUids.contains(currentUserId)) notif
            else notif.copy(readByUids = notif.readByUids + currentUserId)
          }
      )
    }
  }

  override fun syncOrRecoverResidentPhone(
    oldPhone: String,
    newPhone: String,
    displayName: String,
    passwordPlain: String,
    isRecovery: Boolean,
  ) {
    val cleanPhone = newPhone.trim().ifBlank { oldPhone.trim() }
    val cleanName = displayName.trim()
    val normKey = SokanAccountStore.normalizePhoneKey(cleanPhone)
    _workspaces.value =
      _workspaces.value.mapValues { (_, ws) ->
        val updatedMember =
          ws.myMember.copy(
            displayName = cleanName.ifBlank { ws.myMember.displayName },
            phone = cleanPhone.ifBlank { ws.myMember.phone },
            phoneKey = normKey.ifBlank { ws.myMember.phoneKey },
          )
        ws.copy(
          myMember = updatedMember,
          members = ws.members.map { if (it.uid == currentUserId) updatedMember else it },
        )
      }
    _feedbackMessage.value =
      if (_language.value == AppLanguage.AR) "تم تحديث واسترجاع حساب الساكن بنجاح ✓"
      else "Compte résident mis à jour et récupéré ✓"
  }

  fun findBuildingByInviteCode(code: String): Building? {
    val clean = code.trim().uppercase()
    return _workspaces.value.values.firstOrNull { it.building.inviteCode.equals(clean, ignoreCase = true) }?.building
  }

  fun isPhonePreApprovedForCode(code: String, phone: String): Apartment? {
    val cleanCode = code.trim().uppercase()
    val phoneKey = SokanAccountStore.normalizePhoneKey(phone)
    if (phoneKey.length < 6) return null
    val ws = _workspaces.value.values.firstOrNull { it.building.inviteCode.equals(cleanCode, ignoreCase = true) } ?: return null
    return ws.apartments.firstOrNull { SokanAccountStore.normalizePhoneKey(it.residentPhone) == phoneKey }
  }

  fun joinBuildingByInviteCode(
    code: String,
    residentName: String,
    phone: String,
    aptLabel: String,
    occupancyType: OccupancyType,
    phoneVerifiedViaWhatsApp: Boolean,
  ): Result<MemberStatus> {
    val cleanCode = code.trim().uppercase()
    val entry =
      _workspaces.value.entries.firstOrNull { it.value.building.inviteCode.equals(cleanCode, ignoreCase = true) }
        ?: return Result.failure(IllegalArgumentException("Invite code not found"))
    val bId = entry.key
    val ws = entry.value
    val phoneKey = SokanAccountStore.normalizePhoneKey(phone)
    val matchedApt =
      if (phoneKey.length >= 6) {
        ws.apartments.firstOrNull { SokanAccountStore.normalizePhoneKey(it.residentPhone) == phoneKey }
      } else null
    val finalStatus =
      if (matchedApt != null && phoneVerifiedViaWhatsApp) MemberStatus.APPROVED
      else MemberStatus.PENDING
    val finalAptLabel = aptLabel.trim().ifBlank { matchedApt?.label ?: "شقة جديدة" }
    val cleanName = residentName.trim().ifBlank { matchedApt?.residentName ?: "ساكن" }

    val updatedMember =
      ws.myMember.copy(
        displayName = cleanName,
        apartmentId = matchedApt?.id ?: ws.myMember.apartmentId,
        apartmentLabel = finalAptLabel,
        occupancyType = occupancyType.name,
        role = if (ws.myMember.roleEnum() == MemberRole.SYNDIC && matchedApt == null) MemberRole.RESIDENT.name else MemberRole.RESIDENT.name,
        status = finalStatus.name,
        phone = phone.trim(),
        phoneKey = phoneKey,
      )
    val notif =
      BuildingNotification(
        id = "notif_${UUID.randomUUID().toString().take(8)}",
        title = if (finalStatus == MemberStatus.APPROVED) "تفعيل ساكن برقم الهاتف" else "طلب انضمام عبر الرابط",
        body = "$cleanName ($finalAptLabel) — ${ws.building.name}",
        type = NotificationType.MEMBER_JOIN.name,
        senderUid = "system_invite",
        senderName = cleanName,
        readByUids = emptyList(),
        createdAt = Timestamp.now(),
      )
    _workspaces.value =
      _workspaces.value +
        (bId to ws.copy(
          myMember = updatedMember,
          members = ws.members.filterNot { it.uid == currentUserId } + updatedMember,
          notifications = listOf(notif) + ws.notifications,
        ))
    _activeBuildingId.value = bId
    return Result.success(finalStatus)
  }

  override fun approveOrRejectMember(memberUid: String, status: MemberStatus, role: MemberRole?) {
    updateActiveWorkspace { ws ->
      ws.copy(
        members =
          ws.members.map { m ->
            if (m.uid == memberUid) m.copy(status = status.name, role = role?.name ?: m.role) else m
          }
      )
    }
  }

  override fun updateApartmentDetails(
    apartment: Apartment,
    newFee: Double,
    unitType: UnitType,
    label: String,
    residentName: String,
    residentPhone: String,
    occupancyType: OccupancyType,
  ) {
    updateActiveWorkspace { ws ->
      ws.copy(
        apartments =
          ws.apartments.map { apt ->
            if (apt.id == apartment.id) {
              apt.copy(
                monthlyFee = newFee.coerceIn(0.0, 100_000.0),
                unitType = unitType.name,
                label = label.trim().ifBlank { apt.label },
                residentName = residentName.trim(),
                residentPhone = residentPhone.trim(),
                occupancyType = occupancyType.name,
              )
            } else {
              apt
            }
          }
      )
    }
  }

  override fun addApartmentUnit(
    unitNumber: String,
    floor: Int,
    label: String,
    unitType: UnitType,
    monthlyFee: Double,
    residentName: String,
    residentPhone: String,
    occupancyType: OccupancyType,
  ) {
    updateActiveWorkspace { ws ->
      val cleanUnit = unitNumber.trim().ifBlank { (ws.apartments.size + 1).toString() }
      val newApt =
        Apartment(
          id = "apt_${UUID.randomUUID().toString().take(8)}",
          unitNumber = cleanUnit,
          floor = floor.coerceIn(0, 100),
          label = label.trim().ifBlank { "طابق $floor - شقة $cleanUnit" },
          unitType = unitType.name,
          monthlyFee = monthlyFee.coerceIn(0.0, 100_000.0),
          residentName = residentName.trim(),
          residentPhone = residentPhone.trim(),
          occupancyType = occupancyType.name,
        )
      ws.copy(apartments = ws.apartments + newApt)
    }
  }

  override fun recordPayment(
    apartment: Apartment,
    monthKey: String,
    amountMad: Double,
    status: PaymentStatus,
    payerName: String,
    method: PaymentMethod,
  ) {
    updateActiveWorkspace { ws ->
      val docId = "${apartment.id}_${monthKey.replace("-", "")}"
      val paidLabel =
        if (status == PaymentStatus.PAID) SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()) else ""
      val receiptNum =
        if (status == PaymentStatus.PAID) "REC-${monthKey.replace("-", "")}-${apartment.unitNumber}" else ""

      val updated =
        CotisationPayment(
          id = docId,
          apartmentId = apartment.id,
          apartmentLabel = apartment.label,
          monthKey = monthKey,
          amountMad = amountMad,
          status = status.name,
          payerName = payerName,
          paymentMethod = method.name,
          receiptNumber = receiptNum,
          paidAtLabel = paidLabel,
          recordedByUid = GUEST_UID,
        )
      val newPayments =
        if (ws.payments.any { it.id == docId }) ws.payments.map { if (it.id == docId) updated else it }
        else listOf(updated) + ws.payments
      val notif =
        if (status == PaymentStatus.PAID) {
          BuildingNotification(
            id = "notif_${UUID.randomUUID().toString().take(8)}",
            title = "تأكيد أداء: ${apartment.label}",
            body = "تم تسجيل أداء ${amountMad.toInt()} درهم لشهر $monthKey (${payerName.ifBlank { apartment.residentName }})",
            type = NotificationType.PAYMENT.name,
            senderUid = GUEST_UID,
            senderName = ws.myMember.displayName,
            readByUids = listOf(GUEST_UID),
            createdAt = Timestamp.now(),
          )
        } else null
      ws.copy(
        payments = newPayments,
        notifications = if (notif != null) listOf(notif) + ws.notifications else ws.notifications,
      )
    }
  }

  override fun recordExpense(
    title: String,
    category: ExpenseCategory,
    amountMad: Double,
    vendorName: String,
    invoicePhotoBase64: String,
    notes: String,
  ) {
    updateActiveWorkspace { ws ->
      val newExp =
        Expense(
          id = "exp_${UUID.randomUUID().toString().take(8)}",
          title = title.trim(),
          category = category.name,
          amountMad = amountMad,
          vendorName = vendorName.trim(),
          expenseDateLabel = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
          invoicePhotoBase64 = invoicePhotoBase64,
          notes = notes.trim(),
          recordedByUid = GUEST_UID,
          recordedByName = ws.myMember.displayName,
        )
      ws.copy(expenses = listOf(newExp) + ws.expenses)
    }
  }

  override fun reportIssue(
    title: String,
    description: String,
    category: IssueCategory,
    urgency: IssueUrgency,
    photoBase64: String?,
  ) {
    updateActiveWorkspace { ws ->
      val newIssue =
        BuildingIssue(
          id = "iss_${UUID.randomUUID().toString().take(8)}",
          title = title.trim(),
          description = description.trim(),
          category = category.name,
          urgency = urgency.name,
          status = IssueStatus.UNDER_REVIEW.name,
          photoBase64 = photoBase64.orEmpty(),
          authorUid = GUEST_UID,
          authorName = ws.myMember.displayName,
          apartmentLabel = ws.myMember.apartmentLabel,
          upvoteCount = 1,
        )
      val notif =
        BuildingNotification(
          id = "notif_${UUID.randomUUID().toString().take(8)}",
          title = "تبليغ عطل جديد: ${title.trim()}",
          body = description.trim(),
          type = NotificationType.ISSUE.name,
          senderUid = GUEST_UID,
          senderName = ws.myMember.displayName,
          readByUids = listOf(GUEST_UID),
          createdAt = Timestamp.now(),
        )
      ws.copy(
        issues = listOf(newIssue) + ws.issues,
        issueUpvotes = ws.issueUpvotes + (newIssue.id to listOf(IssueUpvote(GUEST_UID, GUEST_UID))),
        notifications = listOf(notif) + ws.notifications,
      )
    }
  }

  override fun toggleIssueUpvote(issueId: String) {
    updateActiveWorkspace { ws ->
      val current = ws.issueUpvotes[issueId].orEmpty()
      val exists = current.any { it.uid == GUEST_UID }
      val updated =
        if (exists) current.filterNot { it.uid == GUEST_UID }
        else current + IssueUpvote(GUEST_UID, GUEST_UID)
      ws.copy(
        issueUpvotes = ws.issueUpvotes + (issueId to updated),
        issues = ws.issues.map { if (it.id == issueId) it.copy(upvoteCount = updated.size) else it },
      )
    }
  }

  override fun addIssueComment(issueId: String, text: String) {
    updateActiveWorkspace { ws ->
      val current = ws.issueComments[issueId].orEmpty()
      val comment =
        IssueComment(
          id = "cmt_${UUID.randomUUID().toString().take(8)}",
          authorUid = GUEST_UID,
          authorName = ws.myMember.displayName,
          apartmentLabel = ws.myMember.apartmentLabel,
          text = text.trim(),
        )
      ws.copy(issueComments = ws.issueComments + (issueId to (current + comment)))
    }
  }

  override fun updateIssueStatus(issueId: String, status: IssueStatus, repairCostNote: String) {
    updateActiveWorkspace { ws ->
      ws.copy(
        issues =
          ws.issues.map {
            if (it.id == issueId) it.copy(status = status.name, repairCostNote = repairCostNote.trim())
            else it
          }
      )
    }
  }

  override fun createAnnouncement(
    title: String,
    body: String,
    category: AnnouncementCategory,
    targetAudience: BroadcastAudience,
    pinned: Boolean,
  ) {
    updateActiveWorkspace { ws ->
      val ann =
        Announcement(
          id = "ann_${UUID.randomUUID().toString().take(8)}",
          title = title.trim(),
          body = body.trim(),
          category = category.name,
          targetAudience = targetAudience.name,
          pinned = pinned,
          authorUid = GUEST_UID,
          authorName = ws.myMember.displayName,
          createdAt = Timestamp.now(),
        )
      val notif =
        BuildingNotification(
          id = "notif_${UUID.randomUUID().toString().take(8)}",
          title = title.trim(),
          body = body.trim(),
          type = NotificationType.ANNOUNCEMENT.name,
          senderUid = GUEST_UID,
          senderName = ws.myMember.displayName,
          readByUids = listOf(GUEST_UID),
          createdAt = Timestamp.now(),
        )
      ws.copy(
        announcements = (listOf(ann) + ws.announcements).sortedByDescending { it.pinned },
        notifications = listOf(notif) + ws.notifications,
      )
    }
  }

  override fun toggleAnnouncementPin(announcementId: String, pinned: Boolean) {
    updateActiveWorkspace { ws ->
      ws.copy(
        announcements =
          ws.announcements
            .map { if (it.id == announcementId) it.copy(pinned = pinned) else it }
            .sortedByDescending { it.pinned }
      )
    }
  }

  override fun createPoll(question: String, description: String, options: List<String>) {
    updateActiveWorkspace { ws ->
      val poll =
        Poll(
          id = "poll_${UUID.randomUUID().toString().take(8)}",
          question = question.trim(),
          description = description.trim(),
          options = options.map { it.trim() }.filter { it.isNotBlank() },
          closed = false,
          authorUid = GUEST_UID,
          authorName = ws.myMember.displayName,
        )
      ws.copy(polls = listOf(poll) + ws.polls)
    }
  }

  override fun castVote(pollId: String, optionIndex: Int) {
    updateActiveWorkspace { ws ->
      val existing = ws.pollVotes[pollId].orEmpty().filterNot { it.uid == GUEST_UID }
      ws.copy(
        pollVotes = ws.pollVotes + (pollId to (existing + PollVote(GUEST_UID, GUEST_UID, optionIndex)))
      )
    }
  }

  override fun closePoll(pollId: String, closed: Boolean) {
    updateActiveWorkspace { ws ->
      ws.copy(polls = ws.polls.map { if (it.id == pollId) it.copy(closed = closed) else it })
    }
  }

  override fun addContact(
    name: String,
    roleTitle: String,
    category: ContactCategory,
    phone: String,
    notes: String,
  ) {
    updateActiveWorkspace { ws ->
      val contact =
        BuildingContact(
          id = "cnt_${UUID.randomUUID().toString().take(8)}",
          name = name.trim(),
          roleTitle = roleTitle.trim(),
          category = category.name,
          phone = phone.trim(),
          notes = notes.trim(),
        )
      ws.copy(contacts = ws.contacts + contact)
    }
  }

  override fun updateBuildingSettings(
    name: String,
    city: String,
    neighborhood: String,
    defaultMonthlyFee: Double,
    reserveFundBalance: Double,
  ) {
    val cleanName = name.trim().take(120)
    val cleanCity = city.trim().take(80)
    if (cleanName.length < 2 || cleanCity.length < 2) return
    updateActiveWorkspace { ws ->
      ws.copy(
        building =
          ws.building.copy(
            name = cleanName,
            city = cleanCity,
            neighborhood = neighborhood.trim().take(100),
            defaultMonthlyFee = defaultMonthlyFee.coerceIn(0.0, 100_000.0),
            reserveFundBalance = reserveFundBalance.coerceIn(0.0, 100_000_000.0),
          )
      )
    }
  }

  override fun createWork(
    title: String,
    category: WorkCategory,
    contractorName: String,
    contractorPhone: String,
    estimatedCostMad: Double,
    dueDateLabel: String,
    notes: String,
  ) {
    if (title.trim().length < 2) return
    updateActiveWorkspace { ws ->
      val work =
        BuildingWork(
          id = "wrk_${UUID.randomUUID().toString().take(8)}",
          title = title.trim(),
          category = category.name,
          contractorName = contractorName.trim(),
          contractorPhone = contractorPhone.trim(),
          estimatedCostMad = estimatedCostMad.coerceAtLeast(0.0),
          dueDateLabel = dueDateLabel.trim(),
          status = WorkStatus.PLANNED.name,
          notes = notes.trim(),
          authorUid = GUEST_UID,
          createdAt = Timestamp.now(),
        )
      val notif =
        BuildingNotification(
          id = "notif_${UUID.randomUUID().toString().take(8)}",
          title = "أشغال جديدة: ${title.trim()}",
          body = listOf(contractorName, dueDateLabel).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { title.trim() },
          type = NotificationType.WORK.name,
          senderUid = GUEST_UID,
          senderName = ws.myMember.displayName,
          readByUids = listOf(GUEST_UID),
          createdAt = Timestamp.now(),
        )
      ws.copy(
        works = listOf(work) + ws.works,
        notifications = listOf(notif) + ws.notifications,
      )
    }
  }

  override fun updateWorkStatus(workId: String, status: WorkStatus) {
    updateActiveWorkspace { ws ->
      ws.copy(works = ws.works.map { if (it.id == workId) it.copy(status = status.name) else it })
    }
  }

  private fun updateActiveWorkspace(transform: (DemoBuildingWorkspace) -> DemoBuildingWorkspace) {
    val activeId = _activeBuildingId.value
    val map = _workspaces.value
    val current = map[activeId] ?: return
    _workspaces.value = map + (activeId to transform(current))
  }

  private fun createInitialWorkspaces(): Map<String, DemoBuildingWorkspace> {
    val yassamineApts =
      listOf(
        Apartment("apt_shop_1", "M1", 0, "محل تجاري 1 (الطابق الأرضي)", UnitType.SHOP.name, 400.0, "مكتبة النور", "0661112233", OccupancyType.TENANT.name),
        Apartment("apt_1", "1", 1, "طابق 1 - شقة 1", UnitType.APARTMENT.name, 250.0, "كريم العلمي", "0661001122", OccupancyType.OWNER.name, GUEST_UID),
        Apartment("apt_2", "2", 1, "طابق 1 - شقة 2", UnitType.APARTMENT.name, 250.0, "نادية بناني", "0662334455", OccupancyType.OWNER.name),
        Apartment("apt_3", "3", 2, "طابق 2 - شقة 3", UnitType.APARTMENT.name, 250.0, "يوسف التازي", "0663556677", OccupancyType.OWNER.name),
        Apartment("apt_4", "4", 2, "طابق 2 - شقة 4", UnitType.APARTMENT.name, 250.0, "سلمى الفاسي", "0664778899", OccupancyType.TENANT.name),
        Apartment("apt_5", "5", 3, "دوبلكس - الطابق 3", UnitType.DUPLEX.name, 350.0, "عمر الإدريسي", "0665112233", OccupancyType.OWNER.name),
      )

    val yassaminePayments =
      buildList {
        monthKeysWindow.forEachIndexed { mIdx, mk ->
          val isCurrent = mIdx == monthKeysWindow.lastIndex
          yassamineApts.forEachIndexed { aIdx, apt ->
            val st =
              when {
                aIdx == 0 || aIdx == 1 || aIdx == 2 -> PaymentStatus.PAID
                aIdx == 5 -> if (isCurrent) PaymentStatus.PAID else PaymentStatus.PAID
                aIdx == 3 -> if (isCurrent) PaymentStatus.PENDING else if (mIdx >= 4) PaymentStatus.OVERDUE else PaymentStatus.PAID
                else -> if (mIdx >= 3) PaymentStatus.OVERDUE else PaymentStatus.PAID
              }
            val method =
              when (aIdx % 3) {
                0 -> PaymentMethod.TRANSFER
                1 -> PaymentMethod.CASH
                else -> PaymentMethod.CHECK
              }
            add(
              CotisationPayment(
                id = "${apt.id}_${mk.replace("-", "")}",
                apartmentId = apt.id,
                apartmentLabel = apt.label,
                monthKey = mk,
                amountMad = apt.monthlyFee,
                status = st.name,
                payerName = apt.residentName,
                paymentMethod = method.name,
                receiptNumber = if (st == PaymentStatus.PAID) "REC-${mk.replace("-", "")}-${apt.unitNumber}" else "",
                paidAtLabel = if (st == PaymentStatus.PAID) "05/${mk.takeLast(2)}/${mk.take(4)}" else "",
                recordedByUid = GUEST_UID,
              )
            )
          }
        }
      }

    val yassamineSyndic =
      BuildingMember(
        id = GUEST_UID,
        uid = GUEST_UID,
        displayName = "كريم العلمي (ضيف)",
        apartmentId = "apt_1",
        apartmentLabel = "طابق 1 - شقة 1",
        occupancyType = OccupancyType.OWNER.name,
        role = MemberRole.SYNDIC.name,
        status = MemberStatus.APPROVED.name,
        phone = "0661001122",
      )

    val ws1 =
      DemoBuildingWorkspace(
        building =
          Building(
            id = DEMO_BUILDING_1_ID,
            name = "إقامة الياسمين",
            city = "الدار البيضاء",
            neighborhood = "المعاريف",
            floorCount = 4,
            apartmentCount = 6,
            defaultMonthlyFee = 250.0,
            reserveFundBalance = 18500.0,
            inviteCode = "YSM482",
            syndicUid = GUEST_UID,
          ),
        myMember = yassamineSyndic,
        members =
          listOf(
            yassamineSyndic,
            BuildingMember("demo_deputy", "demo_deputy", "نادية بناني", "apt_2", "طابق 1 - شقة 2", OccupancyType.OWNER.name, MemberRole.DEPUTY.name, MemberStatus.APPROVED.name, "0662334455"),
            BuildingMember("demo_res_1", "demo_res_1", "يوسف التازي", "apt_3", "طابق 2 - شقة 3", OccupancyType.OWNER.name, MemberRole.RESIDENT.name, MemberStatus.APPROVED.name, "0663556677"),
            BuildingMember("demo_res_2", "demo_res_2", "سلمى الفاسي", "apt_4", "طابق 2 - شقة 4", OccupancyType.TENANT.name, MemberRole.RESIDENT.name, MemberStatus.APPROVED.name, "0664778899"),
            BuildingMember("demo_pending_1", "demo_pending_1", "عمر الإدريسي", "apt_5", "طابق 3 - شقة 5", OccupancyType.OWNER.name, MemberRole.RESIDENT.name, MemberStatus.PENDING.name, "0665112233"),
          ),
        apartments = yassamineApts,
        payments = yassaminePayments,
        expenses =
          listOf(
            Expense("exp_1", "أجور الحراسة والنظافة الشهرية", ExpenseCategory.SECURITY.name, 1450.0, "الحاج محمد (العساس)", "05/${currentMonthKey.takeLast(2)}/${currentMonthKey.take(4)}", sampleInvoiceBase64, "أداء مستحقات الحراسة اليومية والنظافة", GUEST_UID, "كريم العلمي"),
            Expense("exp_2", "فاتورة الماء والكهرباء المشتركة", ExpenseCategory.UTILITIES.name, 420.0, "LYDEC", "10/${currentMonthKey.takeLast(2)}/${currentMonthKey.take(4)}", sampleInvoiceBase64, "إنارة الدرج والمصعد + ماء النظافة", GUEST_UID, "كريم العلمي"),
          ),
        issues =
          listOf(
            BuildingIssue("iss_1", "تسرب ماء بالمرآب الطابق -1", "يوجد تسرب خفيف بالقرب من أنبوب الصرف الرئيسي بالمرآب ويحتاج لمعاينة السباك.", IssueCategory.PLUMBING.name, IssueUrgency.URGENT.name, IssueStatus.IN_PROGRESS.name, "", "demo_res_1", "يوسف التازي", "طابق 2 - شقة 3", "السباك بصدد تغيير الصمام — التكلفة 350 درهم", 3),
            BuildingIssue("iss_2", "إنارة المدخل الرئيسي لا تعمل ليلاً", "مصباح المدخل الرئيسي يتوقف تلقائياً، المرجو فحص المؤقت الكهربائي.", IssueCategory.ELECTRICAL.name, IssueUrgency.NORMAL.name, IssueStatus.UNDER_REVIEW.name, "", "demo_res_2", "سلمى الفاسي", "طابق 2 - شقة 4", "", 1),
          ),
        issueUpvotes =
          mapOf(
            "iss_1" to listOf(IssueUpvote("demo_res_1", "demo_res_1"), IssueUpvote("demo_deputy", "demo_deputy"), IssueUpvote(GUEST_UID, GUEST_UID)),
            "iss_2" to listOf(IssueUpvote("demo_res_2", "demo_res_2")),
          ),
        issueComments =
          mapOf(
            "iss_1" to listOf(IssueComment("cmt_1", "demo_deputy", "نادية بناني", "طابق 1 - شقة 2", "تم التواصل مع الرصاص رشيد وسيحضر غداً على الساعة 10 صباحاً."))
          ),
        announcements =
          listOf(
            Announcement("ann_1", "الجمع العام السنوي للملاك المشتركين", "ينعقد الجمع العام يوم السبت على الساعة 17:00 ببهو الإقامة لمناقشة الميزانية السنوية وصندوق الاحتياط وفق قانون 18-00.", AnnouncementCategory.GENERAL_ASSEMBLY.name, BroadcastAudience.ALL.name, true, GUEST_UID, "كريم العلمي", Timestamp.now()),
            Announcement("ann_2", "تذكير بأداء واجب السانديك الشهري", "المرجو من السكان المتأخرين تسوية واجب الشهر قبل يوم 10 عبر التحويل البنكي أو لدى السانديك.", AnnouncementCategory.GENERAL.name, BroadcastAudience.UNPAID.name, false, GUEST_UID, "كريم العلمي", Timestamp.now()),
          ),
        polls =
          listOf(
            Poll("poll_1", "تجديد طلاء واجهة العمارة من صندوق الاحتياط", "التكلفة الإجمالية المقترحة: 12,000 درهم موزعة على الأشغال والصباغة المقاومة للرطوبة.", listOf("موافق على التنفيذ الآن", "تأجيل للربع القادم", "مناقشة عروض أخرى في الجمع العام"), false, GUEST_UID, "كريم العلمي")
          ),
        pollVotes =
          mapOf(
            "poll_1" to listOf(PollVote("demo_deputy", "demo_deputy", 0), PollVote("demo_res_1", "demo_res_1", 0), PollVote("demo_res_2", "demo_res_2", 2))
          ),
        contacts =
          listOf(
            BuildingContact("cnt_1", "الحاج محمد", "العساس والحراسة", ContactCategory.CONCIERGE.name, "0661998877"),
            BuildingContact("cnt_2", "رشيد بنعلي", "الرصاص والسباكة", ContactCategory.PLUMBER.name, "0662445566"),
            BuildingContact("cnt_3", "أطلس للمصاعد", "صيانة المصعد 24/7", ContactCategory.ELEVATOR.name, "0522334455"),
            BuildingContact("cnt_4", "الوقاية المدنية", "طوارئ", ContactCategory.EMERGENCY.name, "15"),
            BuildingContact("cnt_5", "الشرطة", "طوارئ", ContactCategory.EMERGENCY.name, "19"),
          ),
        works =
          listOf(
            BuildingWork("wrk_1", "صيانة المصعد وفحص الكابلات الدورية", WorkCategory.ELEVATOR.name, "أطلس للمصاعد", "0522334455", 950.0, "2026-11-28", WorkStatus.IN_PROGRESS.name, "زيارة التقني الشهرية وتزييت السكك", GUEST_UID, Timestamp.now()),
            BuildingWork("wrk_2", "صباغة مدخل العمارة والبهو", WorkCategory.RENOVATION.name, "المعلم سعيد", "0663221144", 3200.0, "2026-12-05", WorkStatus.PLANNED.name, "صباغة مائية مقاومة للرطوبة", GUEST_UID, Timestamp.now()),
          ),
        notifications =
          listOf(
            BuildingNotification("notif_y_1", "الجمع العام السنوي للملاك المشتركين", "ينعقد الجمع العام يوم السبت على الساعة 17:00 ببهو إقامة الياسمين.", NotificationType.ANNOUNCEMENT.name, "", "demo_deputy", "نادية بناني", emptyList(), Timestamp.now()),
            BuildingNotification("notif_y_2", "صيانة المصعد جارية اليوم", "تقني شركة أطلس للمصاعد متواجد لفحص الكابلات الدورية.", NotificationType.WORK.name, "", "demo_deputy", "نادية بناني", emptyList(), Timestamp.now()),
          ),
      )

    // Second Moroccan Building managed by the same Syndic!
    val nakhilApts =
      listOf(
        Apartment("nkl_apt_1", "1", 1, "طابق 1 - شقة 1", UnitType.APARTMENT.name, 300.0, "أمين برادة", "0667112233", OccupancyType.OWNER.name),
        Apartment("nkl_apt_2", "2", 1, "طابق 1 - شقة 2", UnitType.APARTMENT.name, 300.0, "حسناء العلوي", "0668223344", OccupancyType.OWNER.name),
        Apartment("nkl_apt_3", "3", 2, "طابق 2 - شقة 3", UnitType.APARTMENT.name, 300.0, "طارق السقاط", "0669334455", OccupancyType.TENANT.name),
        Apartment("nkl_apt_4", "M1", 0, "محل تجاري 1", UnitType.SHOP.name, 500.0, "مقهى النخيل", "0660445566", OccupancyType.TENANT.name),
      )
    val nakhilPayments =
      buildList {
        monthKeysWindow.forEachIndexed { mIdx, mk ->
          val isCurrent = mIdx == monthKeysWindow.lastIndex
          nakhilApts.forEachIndexed { aIdx, apt ->
            val st =
              when {
                aIdx == 0 || aIdx == 3 -> PaymentStatus.PAID
                aIdx == 1 -> if (isCurrent) PaymentStatus.PENDING else PaymentStatus.PAID
                else -> if (mIdx >= 4) PaymentStatus.OVERDUE else PaymentStatus.PAID
              }
            add(
              CotisationPayment(
                id = "${apt.id}_${mk.replace("-", "")}",
                apartmentId = apt.id,
                apartmentLabel = apt.label,
                monthKey = mk,
                amountMad = apt.monthlyFee,
                status = st.name,
                payerName = apt.residentName,
                paymentMethod = PaymentMethod.TRANSFER.name,
                receiptNumber = if (st == PaymentStatus.PAID) "REC-NKL-${mk.replace("-", "")}-${apt.unitNumber}" else "",
                paidAtLabel = if (st == PaymentStatus.PAID) "04/${mk.takeLast(2)}/${mk.take(4)}" else "",
                recordedByUid = GUEST_UID,
              )
            )
          }
        }
      }
    val nakhilSyndic =
      BuildingMember(
        id = GUEST_UID,
        uid = GUEST_UID,
        displayName = "كريم العلمي (ضيف)",
        apartmentId = "nkl_apt_1",
        apartmentLabel = "طابق 1 - شقة 1",
        occupancyType = OccupancyType.OWNER.name,
        role = MemberRole.SYNDIC.name,
        status = MemberStatus.APPROVED.name,
        phone = "0661001122",
      )
    val ws2 =
      DemoBuildingWorkspace(
        building =
          Building(
            id = DEMO_BUILDING_2_ID,
            name = "عمارة النخيل",
            city = "الرباط",
            neighborhood = "أكدال",
            floorCount = 3,
            apartmentCount = 4,
            defaultMonthlyFee = 300.0,
            reserveFundBalance = 9200.0,
            inviteCode = "NKL315",
            syndicUid = GUEST_UID,
          ),
        myMember = nakhilSyndic,
        members = listOf(nakhilSyndic),
        apartments = nakhilApts,
        payments = nakhilPayments,
        expenses =
          listOf(
            Expense("nkl_exp_1", "تنظيف الدرج والمدخل", ExpenseCategory.CLEANING.name, 550.0, "شركة الصفاء", "07/${currentMonthKey.takeLast(2)}/${currentMonthKey.take(4)}", sampleInvoiceBase64, "تنظيف أسبوعي لعمارة النخيل", GUEST_UID, "كريم العلمي"),
          ),
        issues =
          listOf(
            BuildingIssue("nkl_iss_1", "قفل البوابة الرئيسية يحتاج تعديل", "الباب الأوتوماتيكي لا يغلق بإحكام عند الدخول.", IssueCategory.SECURITY.name, IssueUrgency.URGENT.name, IssueStatus.UNDER_REVIEW.name, "", GUEST_UID, "كريم العلمي", "طابق 1 - شقة 1", "", 2),
          ),
        issueUpvotes = mapOf("nkl_iss_1" to listOf(IssueUpvote(GUEST_UID, GUEST_UID))),
        issueComments = emptyMap(),
        announcements =
          listOf(
            Announcement("nkl_ann_1", "أشغال تنظيف خزان الماء - عمارة النخيل", "سيتم تنظيف الخزان المشترك يوم الجمعة من 09:00 إلى 12:00.", AnnouncementCategory.WATER.name, BroadcastAudience.ALL.name, true, GUEST_UID, "كريم العلمي", Timestamp.now()),
          ),
        polls = emptyList(),
        pollVotes = emptyMap(),
        contacts =
          listOf(
            BuildingContact("nkl_c1", "سي مصطفى", "حارس عمارة النخيل", ContactCategory.CONCIERGE.name, "0666778899"),
            BuildingContact("nkl_c2", "الوقاية المدنية", "طوارئ", ContactCategory.EMERGENCY.name, "15"),
          ),
        works =
          listOf(
            BuildingWork("nkl_wrk_1", "تنظيف وتعقيم خزان الماء الرئيسي", WorkCategory.CLEANING.name, "شركة الصفاء", "0661889900", 800.0, "2026-11-25", WorkStatus.PLANNED.name, "تعقيم الخزان بالكلور الغذائي", GUEST_UID, Timestamp.now()),
          ),
        notifications =
          listOf(
            BuildingNotification("notif_n_1", "أشغال تنظيف خزان الماء - عمارة النخيل", "سيتم تنظيف الخزان المشترك يوم الجمعة من 09:00 إلى 12:00.", NotificationType.WORK.name, "", "nkl_admin", "سانديك النخيل", emptyList(), Timestamp.now()),
          ),
      )

    val rawMap = linkedMapOf(
      DEMO_BUILDING_1_ID to ws1,
      DEMO_BUILDING_2_ID to ws2,
    )
    return if (initialRole == MemberRole.RESIDENT) {
      val targetKey = initialSokanPhone?.let { SokanAccountStore.normalizePhoneKey(it) }.orEmpty()
      rawMap.mapValues { (_, ws) ->
        val matchedApt =
          if (targetKey.length >= 6) {
            ws.apartments.firstOrNull { SokanAccountStore.normalizePhoneKey(it.residentPhone) == targetKey }
          } else null
        val sokanName = initialSokanName?.takeIf { it.isNotBlank() } ?: matchedApt?.residentName ?: "ساكن"
        val sokanMember =
          ws.myMember.copy(
            displayName = sokanName,
            apartmentId = matchedApt?.id ?: ws.apartments.firstOrNull()?.id.orEmpty(),
            apartmentLabel = matchedApt?.label ?: ws.myMember.apartmentLabel,
            occupancyType = matchedApt?.occupancyType ?: OccupancyType.OWNER.name,
            role = MemberRole.RESIDENT.name,
            status = MemberStatus.APPROVED.name,
            phone = initialSokanPhone ?: matchedApt?.residentPhone ?: ws.myMember.phone,
            phoneKey = targetKey,
          )
        ws.copy(
          myMember = sokanMember,
          members = ws.members.map { if (it.uid == GUEST_UID) sokanMember else it },
        )
      }
    } else {
      rawMap
    }
  }

  private fun createDemoInvoiceBase64(): String {
    val bmp = Bitmap.createBitmap(420, 260, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.parseColor("#FAF7F2"))

    val headerPaint = Paint().apply { color = Color.parseColor("#0C635A") }
    canvas.drawRect(0f, 0f, 420f, 54f, headerPaint)

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 20f
        isFakeBoldText = true
      }
    canvas.drawText("FACTURE / فاتورة نموذجية", 24f, 35f, titlePaint)

    val bodyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222222")
        textSize = 16f
      }
    canvas.drawText("Copropriete Loi 18-00 Maroc", 24f, 95f, bodyPaint)
    canvas.drawText("Statut: Paye / مؤداة", 24f, 130f, bodyPaint)
    canvas.drawText("Piece justificative (Apercu Invite)", 24f, 165f, bodyPaint)

    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
    bmp.recycle()
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
  }

  companion object {
    const val GUEST_UID = "guest_local_preview"
    const val DEMO_BUILDING_1_ID = "demo_building_yassamine"
    const val DEMO_BUILDING_2_ID = "demo_building_nakhil"
  }
}
