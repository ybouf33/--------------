package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

inline fun <reified T : Enum<T>> safeEnum(raw: String?, default: T): T =
  enumValues<T>().firstOrNull { it.name == raw } ?: default

enum class ThemeMode {
  LIGHT,
  DARK,
  SYSTEM,
}

enum class MemberRole {
  SYNDIC,
  DEPUTY,
  RESIDENT,
}

enum class MemberStatus {
  PENDING,
  APPROVED,
  REJECTED,
}

enum class OccupancyType {
  OWNER,
  TENANT,
}

enum class UnitType {
  APARTMENT,
  SHOP,
  DUPLEX,
}

enum class PaymentStatus {
  PAID,
  PENDING,
  OVERDUE,
}

enum class PaymentMethod {
  CASH,
  TRANSFER,
  CHECK,
}

enum class ExpenseCategory {
  CLEANING,
  SECURITY,
  UTILITIES,
  ELEVATOR,
  REPAIRS,
  OTHER,
}

enum class IssueCategory {
  PLUMBING,
  ELECTRICAL,
  ELEVATOR,
  CLEANING,
  SECURITY,
  STRUCTURAL,
}

enum class IssueUrgency {
  NORMAL,
  URGENT,
  EMERGENCY,
}

enum class IssueStatus {
  UNDER_REVIEW,
  IN_PROGRESS,
  RESOLVED,
}

enum class AnnouncementCategory {
  WATER,
  ELECTRICITY,
  ELEVATOR,
  CLEANING,
  GENERAL_ASSEMBLY,
  GENERAL,
}

enum class ContactCategory {
  CONCIERGE,
  PLUMBER,
  ELECTRICIAN,
  ELEVATOR,
  EMERGENCY,
  SYNDIC,
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val phone: String = "",
  val preferredLang: String = "AR",
  val activeBuildingId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class UserMembership(
  @DocumentId val id: String = "",
  val buildingId: String = "",
  val buildingName: String = "",
  val city: String = "",
  val apartmentLabel: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class InviteInfo(
  @DocumentId val code: String = "",
  val buildingId: String = "",
  val buildingName: String = "",
  val city: String = "",
  val syndicUid: String = "",
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Building(
  @DocumentId val id: String = "",
  val name: String = "",
  val city: String = "",
  val neighborhood: String = "",
  val floorCount: Int = 1,
  val apartmentCount: Int = 1,
  val defaultMonthlyFee: Double = 200.0,
  val reserveFundBalance: Double = 0.0,
  val inviteCode: String = "",
  val syndicUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class BuildingMember(
  @DocumentId val id: String = "",
  val uid: String = "",
  val displayName: String = "",
  val apartmentId: String = "",
  val apartmentLabel: String = "",
  val occupancyType: String = OccupancyType.OWNER.name,
  val role: String = MemberRole.RESIDENT.name,
  val status: String = MemberStatus.PENDING.name,
  val phone: String = "",
  val phoneKey: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun roleEnum(): MemberRole = safeEnum(role, MemberRole.RESIDENT)
  fun statusEnum(): MemberStatus = safeEnum(status, MemberStatus.PENDING)
  fun occupancyEnum(): OccupancyType = safeEnum(occupancyType, OccupancyType.OWNER)
  fun isAdmin(): Boolean =
    statusEnum() == MemberStatus.APPROVED &&
      (roleEnum() == MemberRole.SYNDIC || roleEnum() == MemberRole.DEPUTY)
}

@Keep
@IgnoreExtraProperties
data class Apartment(
  @DocumentId val id: String = "",
  val unitNumber: String = "",
  val floor: Int = 1,
  val label: String = "",
  val unitType: String = UnitType.APARTMENT.name,
  val monthlyFee: Double = 200.0,
  val residentName: String = "",
  val residentPhone: String = "",
  val occupancyType: String = OccupancyType.OWNER.name,
  val residentUid: String = "",
  val phoneKey: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun unitTypeEnum(): UnitType = safeEnum(unitType, UnitType.APARTMENT)
  fun occupancyEnum(): OccupancyType = safeEnum(occupancyType, OccupancyType.OWNER)
}

@Keep
@IgnoreExtraProperties
data class CotisationPayment(
  @DocumentId val id: String = "",
  val apartmentId: String = "",
  val apartmentLabel: String = "",
  val monthKey: String = "",
  val amountMad: Double = 0.0,
  val status: String = PaymentStatus.PENDING.name,
  val payerName: String = "",
  val paymentMethod: String = PaymentMethod.CASH.name,
  val receiptNumber: String = "",
  val paidAtLabel: String = "",
  val recordedByUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun statusEnum(): PaymentStatus = safeEnum(status, PaymentStatus.PENDING)
  fun methodEnum(): PaymentMethod = safeEnum(paymentMethod, PaymentMethod.CASH)
}

@Keep
@IgnoreExtraProperties
data class Expense(
  @DocumentId val id: String = "",
  val title: String = "",
  val category: String = ExpenseCategory.OTHER.name,
  val amountMad: Double = 0.0,
  val vendorName: String = "",
  val expenseDateLabel: String = "",
  val invoicePhotoBase64: String = "",
  val notes: String = "",
  val recordedByUid: String = "",
  val recordedByName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun categoryEnum(): ExpenseCategory = safeEnum(category, ExpenseCategory.OTHER)
}

@Keep
@IgnoreExtraProperties
data class BuildingIssue(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val category: String = IssueCategory.PLUMBING.name,
  val urgency: String = IssueUrgency.NORMAL.name,
  val status: String = IssueStatus.UNDER_REVIEW.name,
  val photoBase64: String = "",
  val authorUid: String = "",
  val authorName: String = "",
  val apartmentLabel: String = "",
  val repairCostNote: String = "",
  val upvoteCount: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun categoryEnum(): IssueCategory = safeEnum(category, IssueCategory.PLUMBING)
  fun urgencyEnum(): IssueUrgency = safeEnum(urgency, IssueUrgency.NORMAL)
  fun statusEnum(): IssueStatus = safeEnum(status, IssueStatus.UNDER_REVIEW)
}

@Keep
@IgnoreExtraProperties
data class IssueUpvote(
  @DocumentId val id: String = "",
  val uid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class IssueComment(
  @DocumentId val id: String = "",
  val authorUid: String = "",
  val authorName: String = "",
  val apartmentLabel: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class BroadcastAudience {
  ALL,
  UNPAID,
  OWNERS,
}

@Keep
@IgnoreExtraProperties
data class Announcement(
  @DocumentId val id: String = "",
  val title: String = "",
  val body: String = "",
  val category: String = AnnouncementCategory.GENERAL.name,
  val targetAudience: String = BroadcastAudience.ALL.name,
  val pinned: Boolean = false,
  val authorUid: String = "",
  val authorName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun categoryEnum(): AnnouncementCategory = safeEnum(category, AnnouncementCategory.GENERAL)
  fun audienceEnum(): BroadcastAudience = safeEnum(targetAudience, BroadcastAudience.ALL)
}

@Keep
@IgnoreExtraProperties
data class Poll(
  @DocumentId val id: String = "",
  val question: String = "",
  val description: String = "",
  val options: List<String> = emptyList(),
  val closed: Boolean = false,
  val authorUid: String = "",
  val authorName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PollVote(
  @DocumentId val id: String = "",
  val uid: String = "",
  val optionIndex: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class BuildingContact(
  @DocumentId val id: String = "",
  val name: String = "",
  val roleTitle: String = "",
  val category: String = ContactCategory.CONCIERGE.name,
  val phone: String = "",
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun categoryEnum(): ContactCategory = safeEnum(category, ContactCategory.CONCIERGE)
}

enum class WorkCategory {
  MAINTENANCE,
  CLEANING,
  ELEVATOR,
  PLUMBING,
  SECURITY,
  RENOVATION,
}

enum class WorkStatus {
  PLANNED,
  IN_PROGRESS,
  COMPLETED,
}

@Keep
@IgnoreExtraProperties
data class BuildingWork(
  @DocumentId val id: String = "",
  val title: String = "",
  val category: String = WorkCategory.MAINTENANCE.name,
  val contractorName: String = "",
  val contractorPhone: String = "",
  val estimatedCostMad: Double = 0.0,
  val dueDateLabel: String = "",
  val status: String = WorkStatus.PLANNED.name,
  val notes: String = "",
  val authorUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun categoryEnum(): WorkCategory = safeEnum(category, WorkCategory.MAINTENANCE)
  fun statusEnum(): WorkStatus = safeEnum(status, WorkStatus.PLANNED)
}

data class BuildingOverview(
  val buildingId: String,
  val apartmentCount: Int,
  val paidThisMonthCount: Int,
  val netBalanceMad: Double,
  val announcementsCount: Int,
  val openIssuesCount: Int,
  val activeWorksCount: Int = 0,
)

enum class NotificationType {
  ANNOUNCEMENT,
  WORK,
  ISSUE,
  PAYMENT,
  MEMBER_JOIN,
  GENERAL,
}

@Keep
@IgnoreExtraProperties
data class BuildingNotification(
  @DocumentId val id: String = "",
  val title: String = "",
  val body: String = "",
  val type: String = NotificationType.GENERAL.name,
  val targetPhoneKey: String = "",
  val senderUid: String = "",
  val senderName: String = "",
  val readByUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun typeEnum(): NotificationType = safeEnum(type, NotificationType.GENERAL)
  fun isReadBy(uid: String): Boolean = readByUids.contains(uid)
}

@Keep
@IgnoreExtraProperties
data class PreApprovedPhone(
  @DocumentId val id: String = "",
  val phoneKey: String = "",
  val apartmentId: String = "",
  val apartmentLabel: String = "",
  val residentName: String = "",
  val occupancyType: String = OccupancyType.OWNER.name,
  val addedByUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ResidentAccount(
  @DocumentId val id: String = "",
  val phoneKey: String = "",
  val displayPhone: String = "",
  val displayName: String = "",
  val passwordHash: String = "",
  val recoveryCodeHash: String = "",
  val ownerUid: String = "",
  val buildingIds: List<String> = emptyList(),
  val apartmentLabels: List<String> = emptyList(),
  @ServerTimestamp val recoveryVerifiedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)


