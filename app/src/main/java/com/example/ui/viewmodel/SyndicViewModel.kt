package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.Apartment
import com.example.data.model.BroadcastAudience
import com.example.data.model.Building
import com.example.data.model.BuildingContact
import com.example.data.model.BuildingIssue
import com.example.data.model.BuildingMember
import com.example.data.model.BuildingNotification
import com.example.data.model.ContactCategory
import com.example.data.model.CotisationPayment
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.InviteInfo
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
import com.example.data.model.PreApprovedPhone
import com.example.data.model.UiState
import com.example.data.model.UnitType
import com.example.data.model.UserMembership
import com.example.data.model.UserProfile
import com.example.data.model.BuildingWork
import com.example.data.model.WorkCategory
import com.example.data.model.WorkStatus
import com.example.data.repository.SyndicBuildingRepository
import com.example.data.repository.SyndicCommunityRepository
import com.example.data.repository.SyndicFinanceRepository
import com.example.ui.i18n.AppLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SyndicViewModel(
  private val buildingRepo: SyndicBuildingRepository,
  private val financeRepo: SyndicFinanceRepository,
  private val communityRepo: SyndicCommunityRepository,
  override val currentUserId: String,
  private val initialDisplayName: String,
  private val initialEmail: String,
) : ViewModel(), SyndicController {

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _lookedUpInvite = MutableStateFlow<InviteInfo?>(null)
  val lookedUpInvite: StateFlow<InviteInfo?> = _lookedUpInvite.asStateFlow()

  private val _preApprovedMatch = MutableStateFlow<PreApprovedPhone?>(null)
  val preApprovedMatch: StateFlow<PreApprovedPhone?> = _preApprovedMatch.asStateFlow()

  private val _forceOnboarding = MutableStateFlow(false)
  val forceOnboarding: StateFlow<Boolean> = _forceOnboarding.asStateFlow()

  private val _localLanguageOverride = MutableStateFlow(AppLanguage.AR)
  private val _localActiveBuildingId = MutableStateFlow<String?>(null)

  init {
    viewModelScope.launch {
      buildingRepo.ensureUserProfile(initialDisplayName, initialEmail)
    }
  }

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    buildingRepo
      .observeUserProfile(currentUserId)
      .toUiState("Failed to load profile")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val language: StateFlow<AppLanguage> =
    combine(userProfileState, _localLanguageOverride) { profileState, localLang ->
        when (profileState) {
          is UiState.Success -> {
            val saved = profileState.data?.preferredLang
            if (saved != null) AppLanguage.fromCode(saved) else localLang
          }
          else -> localLang
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppLanguage.AR)

  val membershipsState: StateFlow<UiState<List<UserMembership>>> =
    buildingRepo
      .observeUserMemberships(currentUserId)
      .toUiState("Failed to load memberships")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val activeBuildingId: StateFlow<String?> =
    combine(userProfileState, membershipsState, _localActiveBuildingId) { profileState, memberships, localId ->
        val profileBuilding = (profileState as? UiState.Success)?.data?.activeBuildingId.orEmpty()
        val list = (memberships as? UiState.Success)?.data.orEmpty()
        when {
          !localId.isNullOrBlank() && list.any { it.buildingId == localId } ->
            localId
          profileBuilding.isNotBlank() && list.any { it.buildingId == profileBuilding } ->
            profileBuilding
          list.isNotEmpty() -> list.first().buildingId
          else -> null
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  val myMemberState: StateFlow<UiState<BuildingMember?>> =
    activeBuildingId
      .flatMapLatest { bId ->
        if (bId.isNullOrBlank()) flowOf(UiState.Success(null))
        else buildingRepo.observeMyMemberRecord(bId, currentUserId).toUiState("Failed to load member")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  private val approvedBuildingId: Flow<String?> =
    combine(activeBuildingId, myMemberState) { bId, memberState ->
      val member = (memberState as? UiState.Success)?.data
      if (!bId.isNullOrBlank() && member?.statusEnum() == MemberStatus.APPROVED) bId else null
    }

  val buildingState: StateFlow<UiState<Building?>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(null))
        else buildingRepo.observeBuilding(bId, currentUserId).toUiState("Failed to load building")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val membersState: StateFlow<UiState<List<BuildingMember>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else buildingRepo.observeBuildingMembers(bId, currentUserId).toUiState("Failed to load members")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val apartmentsState: StateFlow<UiState<List<Apartment>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else financeRepo.observeApartments(bId, currentUserId).toUiState("Failed to load apartments")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val paymentsState: StateFlow<UiState<List<CotisationPayment>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else financeRepo.observePayments(bId, currentUserId).toUiState("Failed to load payments")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val expensesState: StateFlow<UiState<List<Expense>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else financeRepo.observeExpenses(bId, currentUserId).toUiState("Failed to load expenses")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val issuesState: StateFlow<UiState<List<BuildingIssue>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else communityRepo.observeIssues(bId, currentUserId).toUiState("Failed to load issues")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val announcementsState: StateFlow<UiState<List<Announcement>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else communityRepo.observeAnnouncements(bId, currentUserId).toUiState("Failed to load announcements")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val pollsState: StateFlow<UiState<List<Poll>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else communityRepo.observePolls(bId, currentUserId).toUiState("Failed to load polls")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val contactsState: StateFlow<UiState<List<BuildingContact>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else communityRepo.observeContacts(bId, currentUserId).toUiState("Failed to load contacts")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val worksState: StateFlow<UiState<List<BuildingWork>>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(UiState.Success(emptyList()))
        else communityRepo.observeWorks(bId, currentUserId).toUiState("Failed to load works")
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  override val notificationsState: StateFlow<List<BuildingNotification>> =
    approvedBuildingId
      .flatMapLatest { bId ->
        if (bId == null) flowOf(emptyList())
        else {
          communityRepo.observeNotifications(bId, currentUserId).catch { err ->
            unwrapFirestoreOrThrow(err)
            emit(emptyList())
          }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  override fun observePollVotes(buildingId: String, pollId: String): Flow<List<PollVote>> =
    communityRepo.observePollVotes(buildingId, pollId, currentUserId).catch { err ->
      unwrapFirestoreOrThrow(err)
      emit(emptyList())
    }

  override fun observeIssueUpvotes(buildingId: String, issueId: String): Flow<List<IssueUpvote>> =
    communityRepo.observeIssueUpvotes(buildingId, issueId, currentUserId).catch { err ->
      unwrapFirestoreOrThrow(err)
      emit(emptyList())
    }

  override fun observeIssueComments(buildingId: String, issueId: String): Flow<List<IssueComment>> =
    communityRepo.observeIssueComments(buildingId, issueId, currentUserId).catch { err ->
      unwrapFirestoreOrThrow(err)
      emit(emptyList())
    }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun setShowOnboarding(show: Boolean) {
    _forceOnboarding.value = show
  }

  override fun toggleLanguage() {
    val next = if (language.value == AppLanguage.AR) AppLanguage.FR else AppLanguage.AR
    _localLanguageOverride.value = next
    viewModelScope.launch {
      buildingRepo.updatePreferredLanguage(next.code)
    }
  }

  override fun switchActiveBuilding(buildingId: String) {
    _localActiveBuildingId.value = buildingId
    _forceOnboarding.value = false
    viewModelScope.launch {
      buildingRepo.setActiveBuilding(buildingId)
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
    runAction {
      val displayName =
        (userProfileState.value as? UiState.Success)?.data?.displayName?.ifBlank { initialDisplayName }
          ?: initialDisplayName
      buildingRepo.createBuilding(
        name = name,
        city = city,
        neighborhood = neighborhood,
        floorCount = floorCount,
        apartmentCount = apartmentCount,
        defaultMonthlyFee = defaultFee,
        reserveFundBalance = reserveFund,
        syndicDisplayName = displayName,
        syndicApartmentLabel = syndicAptLabel,
      ).onSuccess { newBuildingId ->
        _localActiveBuildingId.value = newBuildingId
        _forceOnboarding.value = false
      }
    }
  }

  fun lookupInvite(code: String) {
    runAction {
      _preApprovedMatch.value = null
      buildingRepo.lookupInviteCode(code).onSuccess { info ->
        _lookedUpInvite.value = info
        if (info == null) {
          _feedbackMessage.value =
            if (language.value == AppLanguage.AR) "رمز الدعوة غير صحيح أو غير مفعل"
            else "Code d'invitation introuvable ou inactif"
        }
      }
    }
  }

  fun lookupPreApprovedPhone(buildingId: String, phone: String) {
    viewModelScope.launch {
      buildingRepo.checkPreApprovedPhone(buildingId, phone).onSuccess { match ->
        _preApprovedMatch.value = match
      }
    }
  }

  fun clearPreApprovedMatch() {
    _preApprovedMatch.value = null
  }

  fun submitJoinRequest(
    invite: InviteInfo,
    apartmentLabel: String,
    occupancyType: OccupancyType,
    phone: String,
    phoneVerifiedViaWhatsApp: Boolean = false,
  ) {
    runAction {
      val displayName =
        (userProfileState.value as? UiState.Success)?.data?.displayName?.ifBlank { initialDisplayName }
          ?: initialDisplayName
      buildingRepo.submitJoinRequest(
        invite = invite,
        displayName = displayName,
        apartmentLabel = apartmentLabel,
        occupancyType = occupancyType,
        phone = phone,
        phoneVerifiedViaWhatsApp = phoneVerifiedViaWhatsApp,
      ).onSuccess { memberStatus ->
        _lookedUpInvite.value = null
        _preApprovedMatch.value = null
        _forceOnboarding.value = false
        _feedbackMessage.value =
          if (memberStatus == MemberStatus.APPROVED) {
            if (language.value == AppLanguage.AR) "تم تفعيل انضمامك تلقائياً عبر رقم الهاتف المعتمد ✓"
            else "Adhésion approuvée automatiquement par téléphone vérifié ✓"
          } else {
            if (language.value == AppLanguage.AR) "تم إرسال طلب الانضمام للسانديك"
            else "Demande d'adhésion envoyée au syndic"
          }
      }
    }
  }

  override fun markAllNotificationsRead() {
    val bId = activeBuildingId.value ?: return
    val unreadIds = notificationsState.value.filter { !it.isReadBy(currentUserId) }.map { it.id }
    if (unreadIds.isEmpty()) return
    viewModelScope.launch {
      communityRepo.markNotificationsRead(bId, unreadIds)
    }
  }

  override fun syncOrRecoverResidentPhone(
    oldPhone: String,
    newPhone: String,
    displayName: String,
    passwordPlain: String,
    isRecovery: Boolean,
  ) {
    val memberships = (membershipsState.value as? UiState.Success)?.data.orEmpty()
    val bIds = memberships.map { it.buildingId }.filter { it.isNotBlank() }
    val aptLabels = memberships.map { it.apartmentLabel }.filter { it.isNotBlank() }
    runAction {
      buildingRepo.syncOrRecoverResidentAccountCloud(
        oldPhone = oldPhone,
        newPhone = newPhone,
        displayName = displayName,
        passwordPlain = passwordPlain,
        buildingIds = bIds,
        apartmentLabels = aptLabels,
        isRecovery = isRecovery,
      ).onSuccess { acc ->
        _feedbackMessage.value =
          if (language.value == AppLanguage.AR) {
            "تم مزامنة واسترجاع بيانات الساكن (${acc.buildingIds.size} عمارات) ✓"
          } else {
            "Compte résident synchronisé / récupéré (${acc.buildingIds.size} immeubles) ✓"
          }
      }
    }
  }

  override fun approveOrRejectMember(memberUid: String, status: MemberStatus, role: MemberRole?) {
    val bId = activeBuildingId.value ?: return
    runAction {
      buildingRepo.updateMemberStatusAndRole(bId, memberUid, status, role)
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
    val bId = activeBuildingId.value ?: return
    runAction {
      buildingRepo.updateApartmentDetails(
        bId,
        apartment,
        newFee,
        unitType,
        label,
        residentName,
        residentPhone,
        occupancyType,
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
    val bId = activeBuildingId.value ?: return
    runAction {
      buildingRepo.addApartmentUnit(
        buildingId = bId,
        unitNumber = unitNumber,
        floor = floor,
        label = label,
        unitType = unitType,
        monthlyFee = monthlyFee,
        residentName = residentName,
        residentPhone = residentPhone,
        occupancyType = occupancyType,
      )
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
    val bId = activeBuildingId.value ?: return
    val memberName =
      (myMemberState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
    runAction {
      financeRepo.recordPayment(bId, apartment, monthKey, amountMad, status, payerName, method).also { res ->
        if (res.isSuccess && status == PaymentStatus.PAID) {
          communityRepo.emitNotification(
            buildingId = bId,
            title = "تأكيد أداء: ${apartment.label}",
            body = "تم تسجيل أداء ${amountMad.toInt()} درهم لشهر $monthKey (${payerName.ifBlank { apartment.residentName }})",
            type = NotificationType.PAYMENT,
            senderName = memberName,
          )
        }
      }
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
    val bId = activeBuildingId.value ?: return
    val memberName =
      (myMemberState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
    runAction {
      financeRepo.recordExpense(
        buildingId = bId,
        title = title,
        category = category,
        amountMad = amountMad,
        vendorName = vendorName,
        invoicePhotoBase64 = invoicePhotoBase64,
        notes = notes,
        recordedByName = memberName,
      )
    }
  }

  override fun reportIssue(
    title: String,
    description: String,
    category: IssueCategory,
    urgency: IssueUrgency,
    photoBase64: String?,
  ) {
    val bId = activeBuildingId.value ?: return
    val member = (myMemberState.value as? UiState.Success)?.data
    runAction {
      communityRepo.createIssue(
        buildingId = bId,
        title = title,
        description = description,
        category = category,
        urgency = urgency,
        photoBase64 = photoBase64,
        authorName = member?.displayName ?: initialDisplayName,
        apartmentLabel = member?.apartmentLabel ?: "",
      )
    }
  }

  override fun toggleIssueUpvote(issueId: String) {
    val bId = activeBuildingId.value ?: return
    viewModelScope.launch {
      communityRepo.toggleIssueUpvote(bId, issueId).onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Error updating upvote"
      }
    }
  }

  override fun addIssueComment(issueId: String, text: String) {
    val bId = activeBuildingId.value ?: return
    val member = (myMemberState.value as? UiState.Success)?.data
    runAction {
      communityRepo.addIssueComment(
        buildingId = bId,
        issueId = issueId,
        authorName = member?.displayName ?: initialDisplayName,
        apartmentLabel = member?.apartmentLabel ?: "",
        text = text,
      )
    }
  }

  override fun updateIssueStatus(issueId: String, status: IssueStatus, repairCostNote: String) {
    val bId = activeBuildingId.value ?: return
    runAction {
      communityRepo.updateIssueStatus(bId, issueId, status, repairCostNote)
    }
  }

  override fun createAnnouncement(
    title: String,
    body: String,
    category: AnnouncementCategory,
    targetAudience: BroadcastAudience,
    pinned: Boolean,
  ) {
    val bId = activeBuildingId.value ?: return
    val memberName =
      (myMemberState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
    runAction {
      communityRepo.createAnnouncement(bId, title, body, category, targetAudience, pinned, memberName)
    }
  }

  override fun toggleAnnouncementPin(announcementId: String, pinned: Boolean) {
    val bId = activeBuildingId.value ?: return
    viewModelScope.launch {
      communityRepo.toggleAnnouncementPin(bId, announcementId, pinned)
    }
  }

  override fun createPoll(question: String, description: String, options: List<String>) {
    val bId = activeBuildingId.value ?: return
    val memberName =
      (myMemberState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
    runAction {
      communityRepo.createPoll(bId, question, description, options, memberName)
    }
  }

  override fun castVote(pollId: String, optionIndex: Int) {
    val bId = activeBuildingId.value ?: return
    viewModelScope.launch {
      communityRepo.castVote(bId, pollId, optionIndex).onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Vote failed"
      }
    }
  }

  override fun closePoll(pollId: String, closed: Boolean) {
    val bId = activeBuildingId.value ?: return
    viewModelScope.launch {
      communityRepo.closePoll(bId, pollId, closed)
    }
  }

  override fun addContact(
    name: String,
    roleTitle: String,
    category: ContactCategory,
    phone: String,
    notes: String,
  ) {
    val bId = activeBuildingId.value ?: return
    runAction {
      communityRepo.addContact(bId, name, roleTitle, category, phone, notes)
    }
  }

  override fun updateBuildingSettings(
    name: String,
    city: String,
    neighborhood: String,
    defaultMonthlyFee: Double,
    reserveFundBalance: Double,
  ) {
    val bId = activeBuildingId.value ?: return
    runAction {
      buildingRepo.updateBuildingSettings(
        buildingId = bId,
        name = name,
        city = city,
        neighborhood = neighborhood,
        defaultMonthlyFee = defaultMonthlyFee,
        reserveFundBalance = reserveFundBalance,
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
    val bId = activeBuildingId.value ?: return
    runAction {
      communityRepo.createWork(
        buildingId = bId,
        title = title,
        category = category,
        contractorName = contractorName,
        contractorPhone = contractorPhone,
        estimatedCostMad = estimatedCostMad,
        dueDateLabel = dueDateLabel,
        notes = notes,
      )
    }
  }

  override fun updateWorkStatus(workId: String, status: WorkStatus) {
    val bId = activeBuildingId.value ?: return
    runAction {
      communityRepo.updateWorkStatus(bId, workId, status)
    }
  }

  private fun runAction(block: suspend () -> Result<*>) {
    viewModelScope.launch {
      _isBusy.value = true
      val result = block()
      _isBusy.value = false
      result.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "حدث خطأ غير متوقع / Erreur"
      }
    }
  }

  private fun <T> Flow<T>.toUiState(fallbackError: String): Flow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }.catch { error ->
      val root = unwrapFirestoreOrThrow(error)
      Log.w(TAG, fallbackError, root)
      emit(UiState.Error(root.message ?: fallbackError))
    }

  private fun unwrapFirestoreOrThrow(error: Throwable): Throwable {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    return firestoreError ?: error
  }

  private companion object {
    const val TAG = "SyndicVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
