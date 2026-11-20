package com.example.ui.viewmodel

import com.example.data.model.AnnouncementCategory
import com.example.data.model.Apartment
import com.example.data.model.BroadcastAudience
import com.example.data.model.BuildingNotification
import com.example.data.model.ContactCategory
import com.example.data.model.ExpenseCategory
import com.example.data.model.IssueCategory
import com.example.data.model.IssueComment
import com.example.data.model.IssueStatus
import com.example.data.model.IssueUpvote
import com.example.data.model.IssueUrgency
import com.example.data.model.MemberRole
import com.example.data.model.MemberStatus
import com.example.data.model.OccupancyType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PollVote
import com.example.data.model.UnitType
import com.example.data.model.WorkCategory
import com.example.data.model.WorkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyndicController {
  val currentUserId: String
  val notificationsState: StateFlow<List<BuildingNotification>>

  fun observePollVotes(buildingId: String, pollId: String): Flow<List<PollVote>>

  fun observeIssueUpvotes(buildingId: String, issueId: String): Flow<List<IssueUpvote>>

  fun observeIssueComments(buildingId: String, issueId: String): Flow<List<IssueComment>>

  fun toggleLanguage()

  fun markAllNotificationsRead()

  fun syncOrRecoverResidentPhone(
    oldPhone: String,
    newPhone: String,
    displayName: String,
    passwordPlain: String,
    isRecovery: Boolean,
  )

  fun switchActiveBuilding(buildingId: String)

  fun createBuilding(
    name: String,
    city: String,
    neighborhood: String,
    floorCount: Int,
    apartmentCount: Int,
    defaultFee: Double,
    reserveFund: Double,
    syndicAptLabel: String,
  )

  fun approveOrRejectMember(memberUid: String, status: MemberStatus, role: MemberRole? = null)

  fun updateApartmentDetails(
    apartment: Apartment,
    newFee: Double,
    unitType: UnitType,
    label: String,
    residentName: String,
    residentPhone: String,
    occupancyType: OccupancyType,
  )

  fun addApartmentUnit(
    unitNumber: String,
    floor: Int,
    label: String,
    unitType: UnitType,
    monthlyFee: Double,
    residentName: String,
    residentPhone: String,
    occupancyType: OccupancyType,
  )

  fun recordPayment(
    apartment: Apartment,
    monthKey: String,
    amountMad: Double,
    status: PaymentStatus,
    payerName: String,
    method: PaymentMethod,
  )

  fun recordExpense(
    title: String,
    category: ExpenseCategory,
    amountMad: Double,
    vendorName: String,
    invoicePhotoBase64: String,
    notes: String,
  )

  fun reportIssue(
    title: String,
    description: String,
    category: IssueCategory,
    urgency: IssueUrgency,
    photoBase64: String?,
  )

  fun toggleIssueUpvote(issueId: String)

  fun addIssueComment(issueId: String, text: String)

  fun updateIssueStatus(issueId: String, status: IssueStatus, repairCostNote: String)

  fun createAnnouncement(
    title: String,
    body: String,
    category: AnnouncementCategory,
    targetAudience: BroadcastAudience,
    pinned: Boolean,
  )

  fun toggleAnnouncementPin(announcementId: String, pinned: Boolean)

  fun createPoll(question: String, description: String, options: List<String>)

  fun castVote(pollId: String, optionIndex: Int)

  fun closePoll(pollId: String, closed: Boolean)

  fun addContact(
    name: String,
    roleTitle: String,
    category: ContactCategory,
    phone: String,
    notes: String,
  )

  fun updateBuildingSettings(
    name: String,
    city: String,
    neighborhood: String,
    defaultMonthlyFee: Double,
    reserveFundBalance: Double,
  )

  fun createWork(
    title: String,
    category: WorkCategory,
    contractorName: String,
    contractorPhone: String,
    estimatedCostMad: Double,
    dueDateLabel: String,
    notes: String,
  )

  fun updateWorkStatus(workId: String, status: WorkStatus)
}
