package com.example.data.repository

import com.example.data.model.Apartment
import com.example.data.model.Building
import com.example.data.model.BuildingMember
import com.example.data.model.ContactCategory
import com.example.data.model.InviteInfo
import com.example.data.model.MemberRole
import com.example.data.model.MemberStatus
import com.example.data.model.OccupancyType
import com.example.data.model.PreApprovedPhone
import com.example.data.model.ResidentAccount
import com.example.data.model.UnitType
import com.example.data.model.UserMembership
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.SokanAccountStore
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
import kotlin.random.Random

class SyndicBuildingRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  suspend fun ensureUserProfile(
    displayName: String,
    email: String,
  ): Result<Unit> {
    var path = "users"
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      path = docRef.path
      val snap = docRef.get().await()
      val safeName = displayName.trim().ifBlank { "مستخدم" }.take(100)
      val safeEmail = email.trim().take(150)
      if (!snap.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to safeName,
            "email" to safeEmail,
            "preferredLang" to "AR",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    flow {
      val path = "users/$userId"
      db.collection("users")
        .document(userId)
        .snapshots()
        .map { snap ->
          snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.GET, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeUserMemberships(userId: String): Flow<List<UserMembership>> =
    flow {
      val path = "users/$userId/memberships"
      db.collection("users")
        .document(userId)
        .collection("memberships")
        .snapshots()
        .map { snap ->
          snap.toObjects(UserMembership::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeBuilding(buildingId: String, userId: String): Flow<Building?> =
    flow {
      val path = "buildings/$buildingId"
      db.collection("buildings")
        .document(buildingId)
        .snapshots()
        .map { snap ->
          snap.toObject(Building::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.GET, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeMyMemberRecord(buildingId: String, userId: String): Flow<BuildingMember?> =
    flow {
      val path = "buildings/$buildingId/members/$userId"
      db.collection("buildings")
        .document(buildingId)
        .collection("members")
        .document(userId)
        .snapshots()
        .map { snap ->
          snap.toObject(BuildingMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.GET, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeBuildingMembers(buildingId: String, userId: String): Flow<List<BuildingMember>> =
    flow {
      val path = "buildings/$buildingId/members"
      db.collection("buildings")
        .document(buildingId)
        .collection("members")
        .snapshots()
        .map { snap ->
          snap.toObjects(BuildingMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  suspend fun updatePreferredLanguage(langCode: String): Result<Unit> {
    var path = "users"
    return try {
      val uid = requireUserId()
      val cleanLang = if (langCode == "FR") "FR" else "AR"
      val docRef = db.collection("users").document(uid)
      path = docRef.path
      docRef.update(
        mapOf(
          "preferredLang" to cleanLang,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun setActiveBuilding(buildingId: String): Result<Unit> {
    var path = "users"
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      path = docRef.path
      docRef.update(
        mapOf(
          "activeBuildingId" to buildingId,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun createBuilding(
    name: String,
    city: String,
    neighborhood: String,
    floorCount: Int,
    apartmentCount: Int,
    defaultMonthlyFee: Double,
    reserveFundBalance: Double,
    syndicDisplayName: String,
    syndicApartmentLabel: String,
  ): Result<String> {
    var path = "buildings"
    return try {
      val uid = requireUserId()
      val cleanName = name.trim().take(120)
      val cleanCity = city.trim().take(80)
      val cleanNeighborhood = neighborhood.trim().take(100)
      val cleanFloors = floorCount.coerceIn(1, 100)
      val cleanApts = apartmentCount.coerceIn(1, 500)
      val cleanFee = defaultMonthlyFee.coerceIn(0.0, 100_000.0)
      val cleanReserve = reserveFundBalance.coerceIn(0.0, 100_000_000.0)
      val cleanDisplay = syndicDisplayName.trim().ifBlank { "مدير السانديك" }.take(100)
      val cleanAptLabel = syndicApartmentLabel.trim().ifBlank { "طابق 1 - شقة 1" }.take(80)

      require(cleanName.length >= 2) { "Building name too short" }
      require(cleanCity.length >= 2) { "City name too short" }

      val inviteCode = generateInviteCode()
      val buildingRef = db.collection("buildings").document()
      val buildingId = buildingRef.id
      path = buildingRef.path

      // Step 1: Create building document
      val buildingPayload =
        mutableMapOf<String, Any>(
          "name" to cleanName,
          "city" to cleanCity,
          "floorCount" to cleanFloors,
          "apartmentCount" to cleanApts,
          "defaultMonthlyFee" to cleanFee,
          "reserveFundBalance" to cleanReserve,
          "inviteCode" to inviteCode,
          "syndicUid" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (cleanNeighborhood.isNotBlank()) {
        buildingPayload["neighborhood"] = cleanNeighborhood
      }
      buildingRef.set(buildingPayload).await()

      // Step 2: Create authoritative SYNDIC member document
      val memberRef = buildingRef.collection("members").document(uid)
      path = memberRef.path
      memberRef.set(
        mapOf(
          "uid" to uid,
          "displayName" to cleanDisplay,
          "apartmentId" to "apt_1",
          "apartmentLabel" to cleanAptLabel,
          "occupancyType" to OccupancyType.OWNER.name,
          "role" to MemberRole.SYNDIC.name,
          "status" to MemberStatus.APPROVED.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // Step 3: Batch invite code, user membership, initial apartments, and Moroccan emergency contacts
      val batch = db.batch()
      batch.set(
        db.collection("invites").document(inviteCode),
        mapOf(
          "buildingId" to buildingId,
          "buildingName" to cleanName,
          "city" to cleanCity,
          "syndicUid" to uid,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        db.collection("users").document(uid).collection("memberships").document(buildingId),
        mapOf(
          "buildingId" to buildingId,
          "buildingName" to cleanName,
          "city" to cleanCity,
          "apartmentLabel" to cleanAptLabel,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        db.collection("users").document(uid),
        mapOf(
          "activeBuildingId" to buildingId,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      val unitsToSeed = cleanApts.coerceAtMost(30)
      val perFloor = ((unitsToSeed + cleanFloors - 1) / cleanFloors).coerceAtLeast(1)
      for (i in 1..unitsToSeed) {
        val floor = ((i - 1) / perFloor) + 1
        val aptRef = buildingRef.collection("apartments").document("apt_$i")
        val label = "طابق $floor - شقة $i"
        val aptMap =
          mutableMapOf<String, Any>(
            "unitNumber" to i.toString(),
            "floor" to floor.coerceIn(0, 100),
            "label" to label,
            "unitType" to UnitType.APARTMENT.name,
            "monthlyFee" to cleanFee,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (i == 1) {
          aptMap["residentName"] = cleanDisplay
          aptMap["residentUid"] = uid
        }
        batch.set(aptRef, aptMap)
      }

      val defaultContacts =
        listOf(
          Triple("الوقاية المدنية / Protection Civile", "15", ContactCategory.EMERGENCY),
          Triple("الأمن الوطني / Police", "19", ContactCategory.EMERGENCY),
          Triple("الإسعاف / SAMU", "150", ContactCategory.EMERGENCY),
        )
      defaultContacts.forEachIndexed { idx, (cName, phone, cat) ->
        batch.set(
          buildingRef.collection("contacts").document("emergency_${idx + 1}"),
          mapOf(
            "name" to cName,
            "roleTitle" to "رقم طوارئ وطني",
            "category" to cat.name,
            "phone" to phone,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      batch.commit().await()
      Result.success(buildingId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun lookupInviteCode(code: String): Result<InviteInfo?> {
    val cleanCode = code.trim().uppercase()
    val path = "invites/$cleanCode"
    return try {
      requireUserId()
      if (!cleanCode.matches(Regex("^[A-Z0-9]{6}$"))) {
        return Result.success(null)
      }
      val snap = db.collection("invites").document(cleanCode).get().await()
      val info = snap.toObject(InviteInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      Result.success(if (info != null && info.active) info else null)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun checkPreApprovedPhone(buildingId: String, phone: String): Result<PreApprovedPhone?> {
    val phoneKey = SokanAccountStore.normalizePhoneKey(phone)
    val path = "buildings/$buildingId/preApprovedPhones/$phoneKey"
    return try {
      requireUserId()
      if (phoneKey.length < 6) return Result.success(null)
      val snap =
        db.collection("buildings")
          .document(buildingId)
          .collection("preApprovedPhones")
          .document(phoneKey)
          .get()
          .await()
      val pre =
        snap.toObject(PreApprovedPhone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      Result.success(pre)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun submitJoinRequest(
    invite: InviteInfo,
    displayName: String,
    apartmentLabel: String,
    occupancyType: OccupancyType,
    phone: String,
    phoneVerifiedViaWhatsApp: Boolean = false,
  ): Result<MemberStatus> {
    var path = "buildings/${invite.buildingId}/members"
    return try {
      val uid = requireUserId()
      val cleanName = displayName.trim().ifBlank { "ساكن" }.take(100)
      val cleanApt = apartmentLabel.trim().take(80)
      val cleanPhone = phone.trim().take(30)
      val phoneKey = SokanAccountStore.normalizePhoneKey(cleanPhone)

      var matchedPreApproved: PreApprovedPhone? = null
      if (phoneVerifiedViaWhatsApp && phoneKey.length >= 6) {
        val preSnap =
          runCatching {
            db.collection("buildings")
              .document(invite.buildingId)
              .collection("preApprovedPhones")
              .document(phoneKey)
              .get()
              .await()
          }.getOrNull()
        matchedPreApproved =
          preSnap?.toObject(PreApprovedPhone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }

      val finalAptLabel =
        cleanApt.ifBlank { matchedPreApproved?.apartmentLabel.orEmpty() }
      require(finalAptLabel.isNotBlank()) { "Apartment label required" }

      val finalStatus =
        if (matchedPreApproved != null && phoneVerifiedViaWhatsApp) MemberStatus.APPROVED
        else MemberStatus.PENDING

      val memberRef = db.collection("buildings").document(invite.buildingId).collection("members").document(uid)
      path = memberRef.path
      val memberPayload =
        mutableMapOf<String, Any>(
          "uid" to uid,
          "displayName" to cleanName,
          "apartmentLabel" to finalAptLabel,
          "occupancyType" to (matchedPreApproved?.occupancyType ?: occupancyType.name),
          "role" to MemberRole.RESIDENT.name,
          "status" to finalStatus.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (matchedPreApproved != null && matchedPreApproved.apartmentId.isNotBlank()) {
        memberPayload["apartmentId"] = matchedPreApproved.apartmentId
      }
      if (cleanPhone.isNotBlank()) {
        memberPayload["phone"] = cleanPhone
      }
      if (phoneKey.length >= 6) {
        memberPayload["phoneKey"] = phoneKey
      }
      memberRef.set(memberPayload).await()

      val batch = db.batch()
      batch.set(
        db.collection("users").document(uid).collection("memberships").document(invite.buildingId),
        mapOf(
          "buildingId" to invite.buildingId,
          "buildingName" to invite.buildingName,
          "city" to invite.city,
          "apartmentLabel" to finalAptLabel,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        db.collection("users").document(uid),
        mapOf(
          "activeBuildingId" to invite.buildingId,
          "phone" to cleanPhone,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()

      if (finalStatus == MemberStatus.APPROVED) {
        runCatching {
          db.collection("buildings")
            .document(invite.buildingId)
            .collection("notifications")
            .document()
            .set(
              mapOf(
                "title" to "انضمام وتفعيل ساكن جديد",
                "body" to "تم التحقق من رقم هاتف $cleanName ($finalAptLabel) عبر واتساب وانضم للعمارة تلقائياً.",
                "type" to "MEMBER_JOIN",
                "senderUid" to uid,
                "senderName" to cleanName,
                "readByUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        }
      }
      Result.success(finalStatus)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateMemberStatusAndRole(
    buildingId: String,
    memberUid: String,
    status: MemberStatus,
    role: MemberRole? = null,
  ): Result<Unit> {
    val path = "buildings/$buildingId/members/$memberUid"
    return try {
      requireUserId()
      val updates =
        mutableMapOf<String, Any>(
          "status" to status.name,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (role != null) {
        updates["role"] = role.name
      }
      db.collection("buildings")
        .document(buildingId)
        .collection("members")
        .document(memberUid)
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

  suspend fun updateApartmentDetails(
    buildingId: String,
    apartment: Apartment,
    newFee: Double,
    unitType: UnitType,
    newLabel: String,
    residentName: String,
    residentPhone: String,
    occupancyType: OccupancyType,
  ): Result<Unit> {
    val path = "buildings/$buildingId/apartments/${apartment.id}"
    return try {
      val uid = requireUserId()
      val cleanLabel = newLabel.trim().ifBlank { apartment.label }.take(80)
      val cleanResident = residentName.trim().take(100)
      val cleanPhone = residentPhone.trim().take(30)
      val updates =
        mutableMapOf<String, Any>(
          "monthlyFee" to newFee.coerceIn(0.0, 100_000.0),
          "unitType" to unitType.name,
          "label" to cleanLabel,
          "residentName" to cleanResident,
          "residentPhone" to cleanPhone,
          "occupancyType" to occupancyType.name,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      db.collection("buildings")
        .document(buildingId)
        .collection("apartments")
        .document(apartment.id)
        .update(updates)
        .await()

      val oldPhoneKey = SokanAccountStore.normalizePhoneKey(apartment.residentPhone)
      val newPhoneKey = SokanAccountStore.normalizePhoneKey(cleanPhone)
      if (oldPhoneKey.length >= 6 && oldPhoneKey != newPhoneKey) {
        runCatching {
          db.collection("buildings").document(buildingId).collection("preApprovedPhones").document(oldPhoneKey).delete().await()
        }
      }
      if (newPhoneKey.length >= 6) {
        upsertPreApprovedPhone(
          buildingId = buildingId,
          phoneKey = newPhoneKey,
          apartmentId = apartment.id,
          apartmentLabel = cleanLabel,
          residentName = cleanResident,
          occupancyType = occupancyType,
          addedByUid = uid,
        )
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun addApartmentUnit(
    buildingId: String,
    unitNumber: String,
    floor: Int,
    label: String,
    unitType: UnitType,
    monthlyFee: Double,
    residentName: String,
    residentPhone: String,
    occupancyType: OccupancyType,
  ): Result<Unit> {
    var path = "buildings/$buildingId/apartments"
    return try {
      val uid = requireUserId()
      val cleanUnit = unitNumber.trim().ifBlank { "1" }.take(20)
      val cleanFloor = floor.coerceIn(0, 100)
      val cleanLabel = label.trim().ifBlank { "طابق $cleanFloor - شقة $cleanUnit" }.take(80)
      val cleanResident = residentName.trim().take(100)
      val cleanPhone = residentPhone.trim().take(30)
      val docRef = db.collection("buildings").document(buildingId).collection("apartments").document()
      path = docRef.path
      val payload =
        mutableMapOf<String, Any>(
          "unitNumber" to cleanUnit,
          "floor" to cleanFloor,
          "label" to cleanLabel,
          "unitType" to unitType.name,
          "monthlyFee" to monthlyFee.coerceIn(0.0, 100_000.0),
          "occupancyType" to occupancyType.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (cleanResident.isNotBlank()) payload["residentName"] = cleanResident
      if (cleanPhone.isNotBlank()) payload["residentPhone"] = cleanPhone
      docRef.set(payload).await()

      val newPhoneKey = SokanAccountStore.normalizePhoneKey(cleanPhone)
      if (newPhoneKey.length >= 6) {
        upsertPreApprovedPhone(
          buildingId = buildingId,
          phoneKey = newPhoneKey,
          apartmentId = docRef.id,
          apartmentLabel = cleanLabel,
          residentName = cleanResident,
          occupancyType = occupancyType,
          addedByUid = uid,
        )
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  private suspend fun upsertPreApprovedPhone(
    buildingId: String,
    phoneKey: String,
    apartmentId: String,
    apartmentLabel: String,
    residentName: String,
    occupancyType: OccupancyType,
    addedByUid: String,
  ) {
    val ref = db.collection("buildings").document(buildingId).collection("preApprovedPhones").document(phoneKey)
    val existing = runCatching { ref.get().await() }.getOrNull()
    val payload =
      mutableMapOf<String, Any>(
        "phoneKey" to phoneKey,
        "apartmentId" to apartmentId,
        "apartmentLabel" to apartmentLabel,
        "residentName" to residentName,
        "occupancyType" to occupancyType.name,
        "addedByUid" to addedByUid,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    if (existing != null && existing.exists()) {
      ref.update(payload).await()
    } else {
      payload["createdAt"] = FieldValue.serverTimestamp()
      ref.set(payload).await()
    }
  }

  suspend fun syncOrRecoverResidentAccountCloud(
    oldPhone: String,
    newPhone: String,
    displayName: String,
    passwordPlain: String,
    buildingIds: List<String>,
    apartmentLabels: List<String>,
    isRecovery: Boolean,
  ): Result<ResidentAccount> {
    val oldKey = SokanAccountStore.normalizePhoneKey(oldPhone)
    val newKey = SokanAccountStore.normalizePhoneKey(newPhone.ifBlank { oldPhone })
    val path = "residentAccounts/$newKey"
    return try {
      val uid = requireUserId()
      require(newKey.length >= 6) { "Phone invalid" }
      val oldSnap =
        if (oldKey.length >= 6) db.collection("residentAccounts").document(oldKey).get().await() else null
      val existingAcc =
        oldSnap?.toObject(ResidentAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

      val mergedBuildingIds = (existingAcc?.buildingIds.orEmpty() + buildingIds).filter { it.isNotBlank() }.distinct()
      val mergedAptLabels = (existingAcc?.apartmentLabels.orEmpty() + apartmentLabels).filter { it.isNotBlank() }.distinct()
      val passHash =
        if (passwordPlain.isNotBlank()) SokanAccountStore.sha256Hex(passwordPlain.trim())
        else existingAcc?.passwordHash?.takeIf { it.length == 64 } ?: SokanAccountStore.sha256Hex("1234")
      val cleanDisplay = displayName.trim().ifBlank { existingAcc?.displayName ?: "ساكن" }.take(100)
      val displayPhone = newPhone.trim().ifBlank { oldPhone.trim() }.take(30)

      val docRef = db.collection("residentAccounts").document(newKey)
      val targetExists = docRef.get().await().exists()
      val payload =
        mutableMapOf<String, Any>(
          "phoneKey" to newKey,
          "displayPhone" to displayPhone,
          "displayName" to cleanDisplay,
          "passwordHash" to passHash,
          "ownerUid" to uid,
          "buildingIds" to mergedBuildingIds,
          "apartmentLabels" to mergedAptLabels,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (isRecovery) {
        payload["recoveryVerifiedAt"] = FieldValue.serverTimestamp()
      }
      if (targetExists) {
        docRef.update(payload).await()
      } else {
        payload["createdAt"] = FieldValue.serverTimestamp()
        docRef.set(payload).await()
      }

      // Restore recovered building memberships to current authenticated UID's memberships index
      mergedBuildingIds.forEachIndexed { idx, bId ->
        val bSnap = runCatching { db.collection("buildings").document(bId).get().await() }.getOrNull()
        val bName = bSnap?.getString("name") ?: "عمارة"
        val bCity = bSnap?.getString("city") ?: "المغرب"
        val aptLbl = mergedAptLabels.getOrElse(idx) { mergedAptLabels.firstOrNull().orEmpty() }
        runCatching {
          db.collection("users").document(uid).collection("memberships").document(bId).set(
            mapOf(
              "buildingId" to bId,
              "buildingName" to bName,
              "city" to bCity,
              "apartmentLabel" to aptLbl,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      }

      val finalSnap = docRef.get().await()
      val saved =
        finalSnap.toObject(ResidentAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: ResidentAccount(
            phoneKey = newKey,
            displayPhone = displayPhone,
            displayName = cleanDisplay,
            passwordHash = passHash,
            ownerUid = uid,
            buildingIds = mergedBuildingIds,
            apartmentLabels = mergedAptLabels,
          )
      Result.success(saved)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun updateBuildingSettings(
    buildingId: String,
    name: String,
    city: String,
    neighborhood: String,
    defaultMonthlyFee: Double,
    reserveFundBalance: Double,
  ): Result<Unit> {
    var path = "buildings/$buildingId"
    return try {
      val uid = requireUserId()
      val cleanName = name.trim().take(120)
      val cleanCity = city.trim().take(80)
      require(cleanName.length >= 2 && cleanCity.length >= 2) { "Building info invalid" }
      val updates =
        mutableMapOf<String, Any>(
          "name" to cleanName,
          "city" to cleanCity,
          "neighborhood" to neighborhood.trim().take(100),
          "defaultMonthlyFee" to defaultMonthlyFee.coerceIn(0.0, 100_000.0),
          "reserveFundBalance" to reserveFundBalance.coerceIn(0.0, 100_000_000.0),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      db.collection("buildings").document(buildingId).update(updates).await()
      val membershipRef =
        db.collection("users").document(uid).collection("memberships").document(buildingId)
      path = membershipRef.path
      membershipRef.update(
        mapOf(
          "buildingName" to cleanName,
          "city" to cleanCity,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  private fun generateInviteCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return buildString(6) {
      repeat(6) { append(chars[Random.nextInt(chars.length)]) }
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
