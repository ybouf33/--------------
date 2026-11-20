package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AnnouncementCategory
import com.example.data.model.BroadcastAudience
import com.example.data.model.MemberStatus
import com.example.data.model.OccupancyType
import com.example.data.model.ThemeMode
import com.example.data.model.WorkCategory
import com.example.data.model.WorkStatus
import com.example.data.util.SokanAccountStore
import com.example.ui.guest.GuestSessionController
import com.example.ui.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("سانديك العمارة", appName)
  }

  @Test
  fun `theme mode defaults to light and persists dark selection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(ThemeMode.LIGHT, SokanAccountStore.getThemeMode(context))
    SokanAccountStore.setThemeMode(context, ThemeMode.DARK)
    assertEquals(ThemeMode.DARK, SokanAccountStore.getThemeMode(context))
    SokanAccountStore.setThemeMode(context, ThemeMode.LIGHT)
    assertEquals(ThemeMode.LIGHT, SokanAccountStore.getThemeMode(context))
  }

  @Test
  fun `moroccan phone normalization whatsapp otp and account recovery migrate data`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("212663556677", SokanAccountStore.normalizePhoneKey("0663556677"))
    assertEquals("212663556677", SokanAccountStore.normalizePhoneKey("+212 663-556677"))
    assertEquals("212663556677", SokanAccountStore.normalizePhoneKey("00212663556677"))

    val otpCode = SokanAccountStore.issueWhatsAppAuthCode(context, "0663556677")
    assertTrue(SokanAccountStore.verifyWhatsAppAuthCode(context, "+212663556677", otpCode))
    assertFalse(SokanAccountStore.verifyWhatsAppAuthCode(context, "0663556677", "000000"))

    SokanAccountStore.ensureSeededDemoAccounts(context)
    assertTrue(SokanAccountStore.verifyPassword(context, "0663556677", "1234"))

    val (recoveryCode, tempPass) = SokanAccountStore.issueRecoveryCredentials(context, "0663556677")
    assertTrue(SokanAccountStore.verifyRecoveryCredentials(context, "+212663556677", tempPass))

    val recovered =
      SokanAccountStore.recoverAndMigrateAccount(
        context = context,
        oldPhone = "0663556677",
        newPhone = "0670112233",
        enteredSecret = recoveryCode,
        newPassword = "newpass99",
      )
    assertTrue(recovered.isSuccess)
    val migratedProfile = recovered.getOrThrow()
    assertTrue(migratedProfile.buildingIds.contains("demo_building_yassamine"))
    assertEquals("0670112233", migratedProfile.displayPhone)
    assertTrue(SokanAccountStore.verifyPassword(context, "0670112233", "newpass99"))
  }

  @Test
  fun `invite link auto approves pre approved resident phone and updates live notifications`() {
    val controller = GuestSessionController(AppLanguage.AR)
    val mainLooper = Shadows.shadowOf(Looper.getMainLooper())
    mainLooper.idle()

    assertNotNull(controller.findBuildingByInviteCode("ysm482"))
    assertNotNull(controller.isPhonePreApprovedForCode("YSM482", "+212663556677"))
    assertNull(controller.isPhonePreApprovedForCode("YSM482", "0699887766"))

    val initialNotifCount = controller.notificationsState.value.size
    val initialUnread = controller.notificationsState.value.count { !it.isReadBy(controller.currentUserId) }
    assertTrue(initialUnread > 0)

    controller.createAnnouncement(
      title = "تنبيه قطع الماء للصيانة",
      body = "سيتم قطع الماء يوم الغد من 10:00 إلى 12:00.",
      category = AnnouncementCategory.WATER,
      targetAudience = BroadcastAudience.ALL,
      pinned = false,
    )
    mainLooper.idle()
    assertEquals(initialNotifCount + 1, controller.notificationsState.value.size)
    assertEquals("تنبيه قطع الماء للصيانة", controller.notificationsState.value.first().title)

    val unknownJoin =
      controller.joinBuildingByInviteCode(
        code = "YSM482",
        residentName = "ساكن جديد",
        phone = "0699887766",
        aptLabel = "طابق 4 - شقة 8",
        occupancyType = OccupancyType.TENANT,
        phoneVerifiedViaWhatsApp = true,
      )
    mainLooper.idle()
    assertEquals(MemberStatus.PENDING, unknownJoin.getOrThrow())

    val approvedJoin =
      controller.joinBuildingByInviteCode(
        code = "YSM482",
        residentName = "يوسف التازي",
        phone = "0663556677",
        aptLabel = "",
        occupancyType = OccupancyType.OWNER,
        phoneVerifiedViaWhatsApp = true,
      )
    mainLooper.idle()
    assertEquals(MemberStatus.APPROVED, approvedJoin.getOrThrow())
    assertTrue(controller.notificationsState.value.count { !it.isReadBy(controller.currentUserId) } > 0)

    controller.markAllNotificationsRead()
    mainLooper.idle()
    assertEquals(0, controller.notificationsState.value.count { !it.isReadBy(controller.currentUserId) })
  }

  @Test
  fun `multi 3imara workspace isolates finances works and settings`() {
    val controller = GuestSessionController(AppLanguage.AR)
    val mainLooper = Shadows.shadowOf(Looper.getMainLooper())
    mainLooper.idle()

    val initialBuildingId = controller.building.value.id
    val initialWorksCount = controller.works.value.size
    assertTrue(initialWorksCount >= 1)

    // Add a Building Work to active 3imara
    controller.createWork(
      title = "صباغة واجهة العمارة",
      category = WorkCategory.RENOVATION,
      contractorName = "شركة الأطلس",
      contractorPhone = "0612345678",
      estimatedCostMad = 4500.0,
      dueDateLabel = "2025-07",
      notes = "طبقتان",
    )
    mainLooper.idle()
    assertEquals(initialWorksCount + 1, controller.works.value.size)

    // Update active 3imara reserve & settings
    controller.updateBuildingSettings(
      name = "إقامة الياسمين المحدثة",
      city = "الدار البيضاء",
      neighborhood = "المعاريف",
      defaultMonthlyFee = 320.0,
      reserveFundBalance = 18000.0,
    )
    mainLooper.idle()
    assertEquals("إقامة الياسمين المحدثة", controller.building.value.name)
    assertEquals(18000.0, controller.building.value.reserveFundBalance, 0.01)

    // Create a second isolated 3imara
    controller.createBuilding(
      name = "إقامة الأمل",
      city = "الرباط",
      neighborhood = "أكدال",
      floorCount = 3,
      apartmentCount = 6,
      defaultFee = 400.0,
      reserveFund = 9000.0,
      syndicAptLabel = "طابق 1 - شقة 1",
    )
    mainLooper.idle()
    assertEquals("إقامة الأمل", controller.building.value.name)
    assertEquals(0, controller.works.value.size)
    assertEquals(6, controller.apartments.value.size)

    // Switch back to initial building and verify its works and settings stayed isolated
    controller.switchActiveBuilding(initialBuildingId)
    mainLooper.idle()
    assertEquals("إقامة الياسمين المحدثة", controller.building.value.name)
    assertEquals(initialWorksCount + 1, controller.works.value.size)
    val createdWork = controller.works.value.first()
    assertEquals(WorkStatus.PLANNED, createdWork.statusEnum())
    controller.updateWorkStatus(createdWork.id, WorkStatus.IN_PROGRESS)
    mainLooper.idle()
    assertEquals(WorkStatus.IN_PROGRESS, controller.works.value.first { it.id == createdWork.id }.statusEnum())
  }
}
