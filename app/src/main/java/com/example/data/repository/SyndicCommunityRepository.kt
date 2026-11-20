package com.example.data.repository

import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.BroadcastAudience
import com.example.data.model.BuildingContact
import com.example.data.model.BuildingIssue
import com.example.data.model.BuildingNotification
import com.example.data.model.BuildingWork
import com.example.data.model.ContactCategory
import com.example.data.model.IssueCategory
import com.example.data.model.IssueComment
import com.example.data.model.IssueStatus
import com.example.data.model.IssueUpvote
import com.example.data.model.IssueUrgency
import com.example.data.model.NotificationType
import com.example.data.model.Poll
import com.example.data.model.PollVote
import com.example.data.model.WorkCategory
import com.example.data.model.WorkStatus
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SyndicCommunityRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun observeIssues(buildingId: String, userId: String): Flow<List<BuildingIssue>> =
    flow {
      val path = "buildings/$buildingId/issues"
      db.collection("buildings")
        .document(buildingId)
        .collection("issues")
        .snapshots()
        .map { snap ->
          snap.toObjects(BuildingIssue::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeIssueUpvotes(buildingId: String, issueId: String, userId: String): Flow<List<IssueUpvote>> =
    flow {
      val path = "buildings/$buildingId/issues/$issueId/upvotes"
      db.collection("buildings")
        .document(buildingId)
        .collection("issues")
        .document(issueId)
        .collection("upvotes")
        .snapshots()
        .map { snap ->
          snap.toObjects(IssueUpvote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeIssueComments(buildingId: String, issueId: String, userId: String): Flow<List<IssueComment>> =
    flow {
      val path = "buildings/$buildingId/issues/$issueId/comments"
      db.collection("buildings")
        .document(buildingId)
        .collection("issues")
        .document(issueId)
        .collection("comments")
        .snapshots()
        .map { snap ->
          snap.toObjects(IssueComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeAnnouncements(buildingId: String, userId: String): Flow<List<Announcement>> =
    flow {
      val path = "buildings/$buildingId/announcements"
      db.collection("buildings")
        .document(buildingId)
        .collection("announcements")
        .snapshots()
        .map { snap ->
          snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(
              compareByDescending<Announcement> { it.pinned }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observePolls(buildingId: String, userId: String): Flow<List<Poll>> =
    flow {
      val path = "buildings/$buildingId/polls"
      db.collection("buildings")
        .document(buildingId)
        .collection("polls")
        .snapshots()
        .map { snap ->
          snap.toObjects(Poll::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observePollVotes(buildingId: String, pollId: String, userId: String): Flow<List<PollVote>> =
    flow {
      val path = "buildings/$buildingId/polls/$pollId/votes"
      db.collection("buildings")
        .document(buildingId)
        .collection("polls")
        .document(pollId)
        .collection("votes")
        .snapshots()
        .map { snap ->
          snap.toObjects(PollVote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeContacts(buildingId: String, userId: String): Flow<List<BuildingContact>> =
    flow {
      val path = "buildings/$buildingId/contacts"
      db.collection("buildings")
        .document(buildingId)
        .collection("contacts")
        .snapshots()
        .map { snap ->
          snap.toObjects(BuildingContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeWorks(buildingId: String, userId: String): Flow<List<BuildingWork>> =
    flow {
      val path = "buildings/$buildingId/works"
      db.collection("buildings")
        .document(buildingId)
        .collection("works")
        .snapshots()
        .map { snap ->
          snap.toObjects(BuildingWork::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeNotifications(buildingId: String, userId: String): Flow<List<BuildingNotification>> =
    flow {
      val path = "buildings/$buildingId/notifications"
      db.collection("buildings")
        .document(buildingId)
        .collection("notifications")
        .snapshots()
        .map { snap ->
          snap.toObjects(BuildingNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  suspend fun emitNotification(
    buildingId: String,
    title: String,
    body: String,
    type: NotificationType,
    senderName: String,
  ) {
    val uid = auth.currentUser?.uid ?: return
    val cleanTitle = title.trim().ifBlank { "تنبيه" }.take(120)
    val cleanBody = body.trim().ifBlank { cleanTitle }.take(500)
    runCatching {
      db.collection("buildings")
        .document(buildingId)
        .collection("notifications")
        .document()
        .set(
          mapOf(
            "title" to cleanTitle,
            "body" to cleanBody,
            "type" to type.name,
            "senderUid" to uid,
            "senderName" to senderName.trim().ifBlank { "السانديك" }.take(100),
            "readByUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    }
  }

  suspend fun markNotificationsRead(buildingId: String, notificationIds: List<String>): Result<Unit> {
    if (notificationIds.isEmpty()) return Result.success(Unit)
    var path = "buildings/$buildingId/notifications"
    return try {
      val uid = requireUserId()
      val batch = db.batch()
      notificationIds.take(40).forEach { notifId ->
        val ref = db.collection("buildings").document(buildingId).collection("notifications").document(notifId)
        path = ref.path
        batch.update(
          ref,
          mapOf(
            "readByUids" to FieldValue.arrayUnion(uid),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun createIssue(
    buildingId: String,
    title: String,
    description: String,
    category: IssueCategory,
    urgency: IssueUrgency,
    photoBase64: String?,
    authorName: String,
    apartmentLabel: String,
  ): Result<Unit> {
    var path = "buildings/$buildingId/issues"
    return try {
      val uid = requireUserId()
      val cleanTitle = title.trim().take(120)
      val cleanDesc = description.trim().take(1000)
      require(cleanTitle.length >= 2) { "Title too short" }
      require(cleanDesc.length >= 2) { "Description too short" }

      val docRef = db.collection("buildings").document(buildingId).collection("issues").document()
      path = docRef.path
      val payload =
        mutableMapOf<String, Any>(
          "title" to cleanTitle,
          "description" to cleanDesc,
          "category" to category.name,
          "urgency" to urgency.name,
          "status" to IssueStatus.UNDER_REVIEW.name,
          "authorUid" to uid,
          "authorName" to authorName.trim().ifBlank { "ساكن" }.take(100),
          "upvoteCount" to 0,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (!photoBase64.isNullOrBlank() && photoBase64.length <= 150_000) {
        payload["photoBase64"] = photoBase64
      }
      if (apartmentLabel.isNotBlank()) {
        payload["apartmentLabel"] = apartmentLabel.trim().take(80)
      }
      docRef.set(payload).await()
      emitNotification(
        buildingId = buildingId,
        title = "تبليغ عطل جديد: $cleanTitle",
        body = cleanDesc,
        type = NotificationType.ISSUE,
        senderName = authorName.trim().ifBlank { "ساكن" },
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun toggleIssueUpvote(buildingId: String, issueId: String): Result<Unit> {
    val issueRef = db.collection("buildings").document(buildingId).collection("issues").document(issueId)
    val path = issueRef.path
    return try {
      val uid = requireUserId()
      val upvoteRef = issueRef.collection("upvotes").document(uid)
      db.runTransaction { tx ->
        val issueSnap = tx.get(issueRef)
        val upvoteSnap = tx.get(upvoteRef)
        val currentCount = (issueSnap.getLong("upvoteCount") ?: 0L).toInt()
        if (upvoteSnap.exists()) {
          tx.delete(upvoteRef)
          tx.update(
            issueRef,
            mapOf(
              "upvoteCount" to (currentCount - 1).coerceAtLeast(0),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          tx.set(
            upvoteRef,
            mapOf(
              "uid" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
            ),
          )
          tx.update(
            issueRef,
            mapOf(
              "upvoteCount" to currentCount + 1,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun addIssueComment(
    buildingId: String,
    issueId: String,
    authorName: String,
    apartmentLabel: String,
    text: String,
  ): Result<Unit> {
    var path = "buildings/$buildingId/issues/$issueId/comments"
    return try {
      val uid = requireUserId()
      val cleanText = text.trim().take(500)
      require(cleanText.isNotEmpty()) { "Comment cannot be empty" }
      val docRef =
        db.collection("buildings")
          .document(buildingId)
          .collection("issues")
          .document(issueId)
          .collection("comments")
          .document()
      path = docRef.path
      val payload =
        mutableMapOf<String, Any>(
          "authorUid" to uid,
          "authorName" to authorName.trim().ifBlank { "ساكن" }.take(100),
          "text" to cleanText,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      if (apartmentLabel.isNotBlank()) {
        payload["apartmentLabel"] = apartmentLabel.trim().take(80)
      }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateIssueStatus(
    buildingId: String,
    issueId: String,
    status: IssueStatus,
    repairCostNote: String,
  ): Result<Unit> {
    val path = "buildings/$buildingId/issues/$issueId"
    return try {
      requireUserId()
      val updates =
        mutableMapOf<String, Any>(
          "status" to status.name,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (repairCostNote.isNotBlank()) {
        updates["repairCostNote"] = repairCostNote.trim().take(200)
      }
      db.collection("buildings")
        .document(buildingId)
        .collection("issues")
        .document(issueId)
        .update(updates)
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun createAnnouncement(
    buildingId: String,
    title: String,
    body: String,
    category: AnnouncementCategory,
    targetAudience: BroadcastAudience,
    pinned: Boolean,
    authorName: String,
  ): Result<Unit> {
    var path = "buildings/$buildingId/announcements"
    return try {
      val uid = requireUserId()
      val cleanTitle = title.trim().take(120)
      val cleanBody = body.trim().take(1500)
      require(cleanTitle.length >= 2 && cleanBody.length >= 2) { "Announcement fields required" }
      val docRef =
        db.collection("buildings").document(buildingId).collection("announcements").document()
      path = docRef.path
      docRef.set(
        mapOf(
          "title" to cleanTitle,
          "body" to cleanBody,
          "category" to category.name,
          "targetAudience" to targetAudience.name,
          "pinned" to pinned,
          "authorUid" to uid,
          "authorName" to authorName.trim().ifBlank { "السانديك" }.take(100),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      emitNotification(
        buildingId = buildingId,
        title = cleanTitle,
        body = cleanBody,
        type = NotificationType.ANNOUNCEMENT,
        senderName = authorName,
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun toggleAnnouncementPin(
    buildingId: String,
    announcementId: String,
    pinned: Boolean,
  ): Result<Unit> {
    val path = "buildings/$buildingId/announcements/$announcementId"
    return try {
      requireUserId()
      db.collection("buildings")
        .document(buildingId)
        .collection("announcements")
        .document(announcementId)
        .update(
          mapOf(
            "pinned" to pinned,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun createPoll(
    buildingId: String,
    question: String,
    description: String,
    options: List<String>,
    authorName: String,
  ): Result<Unit> {
    var path = "buildings/$buildingId/polls"
    return try {
      val uid = requireUserId()
      val cleanQuestion = question.trim().take(200)
      val cleanOptions = options.map { it.trim().take(100) }.filter { it.isNotBlank() }.take(6)
      require(cleanQuestion.length >= 3) { "Poll question too short" }
      require(cleanOptions.size >= 2) { "At least 2 options required" }
      val docRef = db.collection("buildings").document(buildingId).collection("polls").document()
      path = docRef.path
      val payload =
        mutableMapOf<String, Any>(
          "question" to cleanQuestion,
          "options" to cleanOptions,
          "closed" to false,
          "authorUid" to uid,
          "authorName" to authorName.trim().ifBlank { "السانديك" }.take(100),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (description.isNotBlank()) {
        payload["description"] = description.trim().take(500)
      }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun castVote(buildingId: String, pollId: String, optionIndex: Int): Result<Unit> {
    var path = "buildings/$buildingId/polls/$pollId/votes"
    return try {
      val uid = requireUserId()
      val voteRef =
        db.collection("buildings")
          .document(buildingId)
          .collection("polls")
          .document(pollId)
          .collection("votes")
          .document(uid)
      path = voteRef.path
      val existing = voteRef.get().await()
      if (existing.exists()) {
        voteRef.update(mapOf("optionIndex" to optionIndex)).await()
      } else {
        voteRef.set(
          mapOf(
            "uid" to uid,
            "optionIndex" to optionIndex,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun closePoll(buildingId: String, pollId: String, closed: Boolean): Result<Unit> {
    val path = "buildings/$buildingId/polls/$pollId"
    return try {
      requireUserId()
      db.collection("buildings")
        .document(buildingId)
        .collection("polls")
        .document(pollId)
        .update(
          mapOf(
            "closed" to closed,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun addContact(
    buildingId: String,
    name: String,
    roleTitle: String,
    category: ContactCategory,
    phone: String,
    notes: String,
  ): Result<Unit> {
    var path = "buildings/$buildingId/contacts"
    return try {
      requireUserId()
      val cleanName = name.trim().take(100)
      val cleanRole = roleTitle.trim().take(80)
      val cleanPhone = phone.trim().take(30)
      require(cleanName.length >= 2 && cleanRole.length >= 2 && cleanPhone.length >= 2) {
        "Contact details incomplete"
      }
      val docRef = db.collection("buildings").document(buildingId).collection("contacts").document()
      path = docRef.path
      val payload =
        mutableMapOf<String, Any>(
          "name" to cleanName,
          "roleTitle" to cleanRole,
          "category" to category.name,
          "phone" to cleanPhone,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (notes.isNotBlank()) payload["notes"] = notes.trim().take(200)
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun createWork(
    buildingId: String,
    title: String,
    category: WorkCategory,
    contractorName: String,
    contractorPhone: String,
    estimatedCostMad: Double,
    dueDateLabel: String,
    notes: String,
  ): Result<Unit> {
    var path = "buildings/$buildingId/works"
    return try {
      val uid = requireUserId()
      val cleanTitle = title.trim().take(140)
      require(cleanTitle.length >= 2) { "Work title too short" }
      val docRef = db.collection("buildings").document(buildingId).collection("works").document()
      path = docRef.path
      val payload =
        mutableMapOf<String, Any>(
          "title" to cleanTitle,
          "category" to category.name,
          "estimatedCostMad" to estimatedCostMad.coerceIn(0.0, 10_000_000.0),
          "status" to WorkStatus.PLANNED.name,
          "authorUid" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (contractorName.isNotBlank()) payload["contractorName"] = contractorName.trim().take(100)
      if (contractorPhone.isNotBlank()) payload["contractorPhone"] = contractorPhone.trim().take(30)
      if (dueDateLabel.isNotBlank()) payload["dueDateLabel"] = dueDateLabel.trim().take(40)
      if (notes.isNotBlank()) payload["notes"] = notes.trim().take(500)
      docRef.set(payload).await()
      emitNotification(
        buildingId = buildingId,
        title = "أشغال جديدة: $cleanTitle",
        body = listOf(contractorName, dueDateLabel).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { cleanTitle },
        type = NotificationType.WORK,
        senderName = "السانديك",
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateWorkStatus(
    buildingId: String,
    workId: String,
    status: WorkStatus,
  ): Result<Unit> {
    val path = "buildings/$buildingId/works/$workId"
    return try {
      requireUserId()
      db.collection("buildings")
        .document(buildingId)
        .collection("works")
        .document(workId)
        .update(
          mapOf(
            "status" to status.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  private fun logStreamErrorIfActive(
    error: Throwable,
    expectedUid: String,
    operation: OperationType,
    path: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val toLog = firestoreError ?: (error as? Exception)
    if (toLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(toLog, operation, path)
    }
  }
}
