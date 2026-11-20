package com.example.data.repository

import com.example.data.model.Apartment
import com.example.data.model.CotisationPayment
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyndicFinanceRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun observeApartments(buildingId: String, userId: String): Flow<List<Apartment>> =
    flow {
      val path = "buildings/$buildingId/apartments"
      db.collection("buildings")
        .document(buildingId)
        .collection("apartments")
        .snapshots()
        .map { snap ->
          snap.toObjects(Apartment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.unitNumber.toIntOrNull() ?: Int.MAX_VALUE }
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observePayments(buildingId: String, userId: String): Flow<List<CotisationPayment>> =
    flow {
      val path = "buildings/$buildingId/payments"
      db.collection("buildings")
        .document(buildingId)
        .collection("payments")
        .snapshots()
        .map { snap ->
          snap.toObjects(CotisationPayment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  fun observeExpenses(buildingId: String, userId: String): Flow<List<Expense>> =
    flow {
      val path = "buildings/$buildingId/expenses"
      db.collection("buildings")
        .document(buildingId)
        .collection("expenses")
        .snapshots()
        .map { snap ->
          snap.toObjects(Expense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logStreamErrorIfActive(error, userId, OperationType.LIST, path)
          throw error
        }
        .collect { emit(it) }
    }

  suspend fun recordPayment(
    buildingId: String,
    apartment: Apartment,
    monthKey: String,
    amountMad: Double,
    status: PaymentStatus,
    payerName: String,
    paymentMethod: PaymentMethod,
  ): Result<Unit> {
    val docId = "${apartment.id}_${monthKey.replace("-", "")}"
    val path = "buildings/$buildingId/payments/$docId"
    return try {
      val uid = requireUserId()
      require(monthKey.matches(Regex("^20[2-9][0-9]-(0[1-9]|1[0-2])$"))) { "Invalid monthKey" }
      val docRef =
        db.collection("buildings")
          .document(buildingId)
          .collection("payments")
          .document(docId)
      val existing = docRef.get().await()
      val todayLabel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      val receiptNo = "REC-${monthKey.replace("-", "")}-${apartment.unitNumber}"

      if (existing.exists()) {
        val updates =
          mutableMapOf<String, Any>(
            "amountMad" to amountMad.coerceIn(0.0, 100_000.0),
            "status" to status.name,
            "paymentMethod" to paymentMethod.name,
            "receiptNumber" to receiptNo,
            "paidAtLabel" to todayLabel,
            "recordedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (payerName.isNotBlank()) {
          updates["payerName"] = payerName.trim().take(100)
        }
        docRef.update(updates).await()
      } else {
        val payload =
          mutableMapOf<String, Any>(
            "apartmentId" to apartment.id,
            "apartmentLabel" to apartment.label.take(80),
            "monthKey" to monthKey,
            "amountMad" to amountMad.coerceIn(0.0, 100_000.0),
            "status" to status.name,
            "paymentMethod" to paymentMethod.name,
            "receiptNumber" to receiptNo,
            "paidAtLabel" to todayLabel,
            "recordedByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (payerName.isNotBlank()) {
          payload["payerName"] = payerName.trim().take(100)
        }
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

  suspend fun recordExpense(
    buildingId: String,
    title: String,
    category: ExpenseCategory,
    amountMad: Double,
    vendorName: String,
    invoicePhotoBase64: String,
    notes: String,
    recordedByName: String,
  ): Result<Unit> {
    var path = "buildings/$buildingId/expenses"
    return try {
      val uid = requireUserId()
      val cleanTitle = title.trim().take(120)
      require(cleanTitle.length >= 2) { "Expense title too short" }
      require(amountMad > 0.0 && amountMad <= 10_000_000.0) { "Invalid expense amount" }
      require(invoicePhotoBase64.isNotBlank() && invoicePhotoBase64.length <= 150_000) {
        "Mandatory invoice photo required"
      }

      val docRef = db.collection("buildings").document(buildingId).collection("expenses").document()
      path = docRef.path
      val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      val payload =
        mutableMapOf<String, Any>(
          "title" to cleanTitle,
          "category" to category.name,
          "amountMad" to amountMad,
          "expenseDateLabel" to dateLabel,
          "invoicePhotoBase64" to invoicePhotoBase64,
          "recordedByUid" to uid,
          "recordedByName" to recordedByName.trim().ifBlank { "السانديك" }.take(100),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (vendorName.isNotBlank()) payload["vendorName"] = vendorName.trim().take(100)
      if (notes.isNotBlank()) payload["notes"] = notes.trim().take(500)

      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
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
