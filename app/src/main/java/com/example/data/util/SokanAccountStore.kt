package com.example.data.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ThemeMode
import java.security.MessageDigest
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

data class LocalSokanProfile(
  val phoneKey: String,
  val displayPhone: String,
  val displayName: String,
  val passwordHash: String,
  val buildingIds: List<String>,
  val buildingNames: List<String>,
  val apartmentLabel: String,
  val isVerifiedViaWhatsApp: Boolean = true,
)

/**
 * Local persistent engine & cryptographic helper for Moroccan phone normalization,
 * SHA-256 credential hashing, 6-digit WhatsApp OTP generation, and Sokan account/data recovery.
 */
object SokanAccountStore {
  private const val PREFS_NAME = "syndic_sokan_prefs"
  private const val KEY_ACCOUNTS_JSON = "sokan_accounts_map_v1"
  private const val KEY_THEME_MODE = "app_theme_mode"
  private const val KEY_OTP_MAP = "pending_otps_v1"
  private const val KEY_RECOVERY_MAP = "pending_recoveries_v1"
  private const val KEY_ACTIVE_SOKAN_PHONE = "active_sokan_phone"

  fun normalizePhoneKey(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    if (digits.length < 6) return digits
    return when {
      digits.startsWith("00") && digits.length > 4 -> digits.drop(2)
      digits.startsWith("212") -> digits
      digits.startsWith("0") && digits.length >= 9 -> "212" + digits.drop(1)
      digits.length == 9 && (digits.startsWith("5") || digits.startsWith("6") || digits.startsWith("7")) ->
        "212$digits"
      else -> digits
    }
  }

  fun sha256Hex(raw: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun generateSixDigitCode(): String = (100000 + Random.nextInt(900000)).toString()

  fun generateTempPassword(): String = "SYN-${1000 + Random.nextInt(9000)}"

  private fun prefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun getThemeMode(context: Context): ThemeMode {
    val raw = prefs(context).getString(KEY_THEME_MODE, ThemeMode.LIGHT.name)
    return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.LIGHT
  }

  fun setThemeMode(context: Context, mode: ThemeMode) {
    prefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
  }

  fun getActiveSokanPhone(context: Context): String? =
    prefs(context).getString(KEY_ACTIVE_SOKAN_PHONE, null)?.takeIf { it.isNotBlank() }

  fun setActiveSokanPhone(context: Context, phoneKey: String?) {
    if (phoneKey.isNullOrBlank()) {
      prefs(context).edit().remove(KEY_ACTIVE_SOKAN_PHONE).apply()
    } else {
      prefs(context).edit().putString(KEY_ACTIVE_SOKAN_PHONE, phoneKey).apply()
    }
  }

  fun ensureSeededDemoAccounts(context: Context) {
    val accounts = loadAllAccountsMap(context)
    val demoPhoneKey = normalizePhoneKey("0663556677")
    if (!accounts.containsKey(demoPhoneKey)) {
      saveAccount(
        context = context,
        profile =
          LocalSokanProfile(
            phoneKey = demoPhoneKey,
            displayPhone = "0663556677",
            displayName = "يوسف التازي",
            passwordHash = sha256Hex("1234"),
            buildingIds = listOf("demo_building_yassamine"),
            buildingNames = listOf("إقامة الياسمين"),
            apartmentLabel = "طابق 2 - شقة 3",
            isVerifiedViaWhatsApp = true,
          ),
      )
    }
  }

  fun getAccountByPhone(context: Context, rawPhone: String): LocalSokanProfile? {
    ensureSeededDemoAccounts(context)
    val key = normalizePhoneKey(rawPhone)
    if (key.length < 6) return null
    return loadAllAccountsMap(context)[key]
  }

  fun saveAccount(context: Context, profile: LocalSokanProfile) {
    val map = loadAllAccountsMap(context).toMutableMap()
    map[profile.phoneKey] = profile
    persistAllAccountsMap(context, map)
  }

  fun registerOrUpdateAccount(
    context: Context,
    rawPhone: String,
    displayName: String,
    plainPassword: String,
    buildingId: String? = null,
    buildingName: String? = null,
    apartmentLabel: String? = null,
  ): LocalSokanProfile {
    ensureSeededDemoAccounts(context)
    val phoneKey = normalizePhoneKey(rawPhone)
    val existing = loadAllAccountsMap(context)[phoneKey]
    val newBuildings =
      buildList {
        if (existing != null) addAll(existing.buildingIds)
        if (!buildingId.isNullOrBlank() && !contains(buildingId)) add(buildingId)
      }
    val newBuildingNames =
      buildList {
        if (existing != null) addAll(existing.buildingNames)
        if (!buildingName.isNullOrBlank() && !contains(buildingName)) add(buildingName)
      }
    val passHash =
      if (plainPassword.isNotBlank()) sha256Hex(plainPassword.trim())
      else existing?.passwordHash ?: sha256Hex("1234")

    val updated =
      LocalSokanProfile(
        phoneKey = phoneKey,
        displayPhone = rawPhone.trim().ifBlank { existing?.displayPhone ?: phoneKey },
        displayName = displayName.trim().ifBlank { existing?.displayName ?: "ساكن" },
        passwordHash = passHash,
        buildingIds = newBuildings,
        buildingNames = newBuildingNames,
        apartmentLabel = apartmentLabel?.takeIf { it.isNotBlank() } ?: existing?.apartmentLabel.orEmpty(),
        isVerifiedViaWhatsApp = true,
      )
    saveAccount(context, updated)
    return updated
  }

  fun issueWhatsAppAuthCode(context: Context, rawPhone: String): String {
    val key = normalizePhoneKey(rawPhone)
    val code = generateSixDigitCode()
    val rawJson = prefs(context).getString(KEY_OTP_MAP, "{}") ?: "{}"
    val obj = runCatching { JSONObject(rawJson) }.getOrDefault(JSONObject())
    obj.put(key, sha256Hex(code))
    prefs(context).edit().putString(KEY_OTP_MAP, obj.toString()).apply()
    return code
  }

  fun verifyWhatsAppAuthCode(context: Context, rawPhone: String, enteredCode: String): Boolean {
    val key = normalizePhoneKey(rawPhone)
    val cleanCode = enteredCode.trim()
    if (key.length < 6 || cleanCode.length < 4) return false
    val rawJson = prefs(context).getString(KEY_OTP_MAP, "{}") ?: "{}"
    val obj = runCatching { JSONObject(rawJson) }.getOrDefault(JSONObject())
    val expectedHash = obj.optString(key, "")
    return expectedHash.isNotBlank() && expectedHash == sha256Hex(cleanCode)
  }

  fun verifyPassword(context: Context, rawPhone: String, plainPassword: String): Boolean {
    val profile = getAccountByPhone(context, rawPhone) ?: return false
    return profile.passwordHash == sha256Hex(plainPassword.trim())
  }

  fun issueRecoveryCredentials(context: Context, rawPhone: String): Pair<String, String> {
    val key = normalizePhoneKey(rawPhone)
    val code = generateSixDigitCode()
    val tempPass = generateTempPassword()
    val rawJson = prefs(context).getString(KEY_RECOVERY_MAP, "{}") ?: "{}"
    val obj = runCatching { JSONObject(rawJson) }.getOrDefault(JSONObject())
    obj.put(
      key,
      JSONObject().apply {
        put("codeHash", sha256Hex(code))
        put("tempHash", sha256Hex(tempPass))
      },
    )
    prefs(context).edit().putString(KEY_RECOVERY_MAP, obj.toString()).apply()
    return code to tempPass
  }

  fun verifyRecoveryCredentials(context: Context, oldPhone: String, enteredSecret: String): Boolean {
    val key = normalizePhoneKey(oldPhone)
    val enteredHash = sha256Hex(enteredSecret.trim())
    val rawJson = prefs(context).getString(KEY_RECOVERY_MAP, "{}") ?: "{}"
    val obj = runCatching { JSONObject(rawJson) }.getOrDefault(JSONObject())
    val recObj = obj.optJSONObject(key) ?: return false
    return recObj.optString("codeHash") == enteredHash || recObj.optString("tempHash") == enteredHash
  }

  fun recoverAndMigrateAccount(
    context: Context,
    oldPhone: String,
    newPhone: String,
    enteredSecret: String,
    newPassword: String,
    newDisplayName: String? = null,
  ): Result<LocalSokanProfile> {
    ensureSeededDemoAccounts(context)
    val oldKey = normalizePhoneKey(oldPhone)
    if (oldKey.length < 6) return Result.failure(IllegalArgumentException("Invalid old phone"))

    val oldAccount = loadAllAccountsMap(context)[oldKey]
    val validRecovery =
      verifyRecoveryCredentials(context, oldPhone, enteredSecret) ||
        (oldAccount != null && oldAccount.passwordHash == sha256Hex(enteredSecret.trim()))
    if (!validRecovery) {
      return Result.failure(IllegalArgumentException("Invalid recovery code or password"))
    }

    val cleanNewPhone = newPhone.trim().ifBlank { oldPhone.trim() }
    val newKey = normalizePhoneKey(cleanNewPhone)
    if (newKey.length < 6) return Result.failure(IllegalArgumentException("Invalid new phone"))

    val cleanPass = newPassword.trim()
    if (cleanPass.length < 4) return Result.failure(IllegalArgumentException("Password too short"))

    val targetBuildings = oldAccount?.buildingIds ?: listOf("demo_building_yassamine")
    val targetNames = oldAccount?.buildingNames ?: listOf("إقامة الياسمين")
    val targetApt = oldAccount?.apartmentLabel ?: "شقة مسترجعة"
    val finalName =
      newDisplayName?.trim()?.takeIf { it.isNotBlank() }
        ?: oldAccount?.displayName?.takeIf { it.isNotBlank() }
        ?: "ساكن"

    val migrated =
      LocalSokanProfile(
        phoneKey = newKey,
        displayPhone = cleanNewPhone,
        displayName = finalName,
        passwordHash = sha256Hex(cleanPass),
        buildingIds = targetBuildings,
        buildingNames = targetNames,
        apartmentLabel = targetApt,
        isVerifiedViaWhatsApp = true,
      )
    val map = loadAllAccountsMap(context).toMutableMap()
    if (oldKey != newKey) {
      map.remove(oldKey)
    }
    map[newKey] = migrated
    persistAllAccountsMap(context, map)
    setActiveSokanPhone(context, newKey)
    return Result.success(migrated)
  }

  private fun loadAllAccountsMap(context: Context): Map<String, LocalSokanProfile> {
    val raw = prefs(context).getString(KEY_ACCOUNTS_JSON, "{}") ?: "{}"
    val root = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    val out = linkedMapOf<String, LocalSokanProfile>()
    val keys = root.keys()
    while (keys.hasNext()) {
      val k = keys.next()
      val obj = root.optJSONObject(k) ?: continue
      val bIdsArr = obj.optJSONArray("buildingIds") ?: JSONArray()
      val bNamesArr = obj.optJSONArray("buildingNames") ?: JSONArray()
      val bIds = (0 until bIdsArr.length()).mapNotNull { bIdsArr.optString(it).takeIf { s -> s.isNotBlank() } }
      val bNames = (0 until bNamesArr.length()).mapNotNull { bNamesArr.optString(it).takeIf { s -> s.isNotBlank() } }
      out[k] =
        LocalSokanProfile(
          phoneKey = k,
          displayPhone = obj.optString("displayPhone", k),
          displayName = obj.optString("displayName", "ساكن"),
          passwordHash = obj.optString("passwordHash", ""),
          buildingIds = bIds,
          buildingNames = bNames,
          apartmentLabel = obj.optString("apartmentLabel", ""),
          isVerifiedViaWhatsApp = obj.optBoolean("isVerifiedViaWhatsApp", true),
        )
    }
    return out
  }

  private fun persistAllAccountsMap(context: Context, map: Map<String, LocalSokanProfile>) {
    val root = JSONObject()
    map.forEach { (k, profile) ->
      root.put(
        k,
        JSONObject().apply {
          put("displayPhone", profile.displayPhone)
          put("displayName", profile.displayName)
          put("passwordHash", profile.passwordHash)
          put("buildingIds", JSONArray(profile.buildingIds))
          put("buildingNames", JSONArray(profile.buildingNames))
          put("apartmentLabel", profile.apartmentLabel)
          put("isVerifiedViaWhatsApp", profile.isVerifiedViaWhatsApp)
        },
      )
    }
    prefs(context).edit().putString(KEY_ACCOUNTS_JSON, root.toString()).apply()
  }
}
