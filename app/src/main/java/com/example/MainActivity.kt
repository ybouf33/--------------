package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.Announcement
import com.example.data.model.Apartment
import com.example.data.model.Building
import com.example.data.model.BuildingContact
import com.example.data.model.BuildingIssue
import com.example.data.model.BuildingMember
import com.example.data.model.BuildingOverview
import com.example.data.model.BuildingWork
import com.example.data.model.CotisationPayment
import com.example.data.model.Expense
import com.example.data.model.IssueStatus
import com.example.data.model.MemberRole
import com.example.data.model.MemberStatus
import com.example.data.model.OccupancyType
import com.example.data.model.PaymentStatus
import com.example.data.model.Poll
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.model.UserMembership
import com.example.data.model.WorkStatus
import com.example.data.repository.SyndicBuildingRepository
import com.example.data.repository.SyndicCommunityRepository
import com.example.data.repository.SyndicFinanceRepository
import com.example.data.util.LiveNotificationManager
import com.example.data.util.ReceiptGenerator
import com.example.data.util.SokanAccountStore
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.BuildingContextStrip
import com.example.ui.components.BuildingPortfolioDialog
import com.example.ui.components.CreateBuildingDialog
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.LoadingBlock
import com.example.ui.components.NotificationCenterDialog
import com.example.ui.components.ThemeToggleChip
import com.example.ui.guest.GuestSessionController
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.onboarding.BuildingOnboardingScreen
import com.example.ui.screens.BuildingScreen
import com.example.ui.screens.CreateHubScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IssuesScreen
import com.example.ui.theme.LocalSyndicColors
import com.example.ui.theme.SyndicTheme
import com.example.ui.viewmodel.SyndicController
import com.example.ui.viewmodel.SyndicViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  private val deepLinkInviteCode = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    LiveNotificationManager.ensureChannel(this)
    SokanAccountStore.ensureSeededDemoAccounts(this)
    deepLinkInviteCode.value = extractInviteCodeFromIntent(intent)
    setContent {
      val context = LocalContext.current
      var themeMode by remember { mutableStateOf(SokanAccountStore.getThemeMode(context)) }
      val isDarkTheme =
        when (themeMode) {
          ThemeMode.LIGHT -> false
          ThemeMode.DARK -> true
          ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
      val onToggleTheme: () -> Unit = {
        val next = if (isDarkTheme) ThemeMode.LIGHT else ThemeMode.DARK
        themeMode = next
        SokanAccountStore.setThemeMode(context, next)
      }
      SyndicTheme(darkTheme = isDarkTheme) {
        AppNavigation(
          deepLinkInviteCode = deepLinkInviteCode.value,
          onDeepLinkConsumed = { deepLinkInviteCode.value = null },
          isDarkTheme = isDarkTheme,
          onToggleTheme = onToggleTheme,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    extractInviteCodeFromIntent(intent)?.let { code ->
      deepLinkInviteCode.value = code
    }
  }

  private fun extractInviteCodeFromIntent(intent: Intent?): String? {
    val uri = intent?.data ?: return null
    val queryCode = uri.getQueryParameter("code")
    if (!queryCode.isNullOrBlank()) return queryCode.trim().uppercase()
    val lastSeg = uri.lastPathSegment
    return if (!lastSeg.isNullOrBlank() && lastSeg.length == 6) lastSeg.uppercase() else null
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private data class GuestLaunchConfig(
  val role: MemberRole = MemberRole.SYNDIC,
  val sokanPhone: String? = null,
  val sokanName: String? = null,
  val buildingIds: List<String> = emptyList(),
  val pendingInviteCode: String? = null,
)

@Composable
fun AppNavigation(
  deepLinkInviteCode: String?,
  onDeepLinkConsumed: () -> Unit,
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
  auth: FirebaseAuth = Firebase.auth,
) {
  val context = LocalContext.current
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var authLanguage by remember { mutableStateOf(AppLanguage.AR) }
  var guestConfig by remember { mutableStateOf<GuestLaunchConfig?>(null) }

  val user = currentUser
  LaunchedEffect(user) {
    if (user != null) {
      guestConfig = null
    }
  }

  if (user == null) {
    val activeGuest = guestConfig
    if (activeGuest != null) {
      SyndicGuestApp(
        initialLanguage = authLanguage,
        launchConfig = activeGuest,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        onExitGuestMode = {
          SokanAccountStore.setActiveSokanPhone(context, null)
          guestConfig = null
        },
      )
    } else {
      val dir = if (authLanguage == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
      CompositionLocalProvider(LocalLayoutDirection provides dir) {
        AuthScreen(
          language = authLanguage,
          isDarkTheme = isDarkTheme,
          deepLinkInviteCode = deepLinkInviteCode,
          onToggleLanguage = {
            authLanguage = if (authLanguage == AppLanguage.AR) AppLanguage.FR else AppLanguage.AR
          },
          onToggleTheme = onToggleTheme,
          onAuthSuccess = { guestConfig = null },
          onGuestSignIn = { guestConfig = GuestLaunchConfig(role = MemberRole.SYNDIC) },
          onSokanSignIn = { profile, _ ->
            guestConfig =
              GuestLaunchConfig(
                role = MemberRole.RESIDENT,
                sokanPhone = profile.displayPhone,
                sokanName = profile.displayName,
                buildingIds = profile.buildingIds,
              )
          },
          onJoinViaInviteGuest = { code ->
            guestConfig =
              GuestLaunchConfig(
                role = MemberRole.RESIDENT,
                pendingInviteCode = code,
              )
            onDeepLinkConsumed()
          },
        )
      }
    }
  } else {
    key(user.uid) {
      SyndicAuthenticatedApp(
        currentUserId = user.uid,
        displayName = user.displayName ?: "",
        email = user.email ?: "",
        deepLinkInviteCode = deepLinkInviteCode,
        onDeepLinkConsumed = onDeepLinkConsumed,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyndicAuthenticatedApp(
  currentUserId: String,
  displayName: String,
  email: String,
  deepLinkInviteCode: String?,
  onDeepLinkConsumed: () -> Unit,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: SyndicViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            SyndicViewModel(
              buildingRepo = SyndicBuildingRepository(db),
              financeRepo = SyndicFinanceRepository(db),
              communityRepo = SyndicCommunityRepository(db),
              currentUserId = currentUserId,
              initialDisplayName = displayName,
              initialEmail = email,
            )
          }
        },
    )

  val language by viewModel.language.collectAsStateWithLifecycle()
  val s = stringsFor(language)
  val layoutDir = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

  val membershipsState by viewModel.membershipsState.collectAsStateWithLifecycle()
  val myMemberState by viewModel.myMemberState.collectAsStateWithLifecycle()
  val buildingState by viewModel.buildingState.collectAsStateWithLifecycle()
  val membersState by viewModel.membersState.collectAsStateWithLifecycle()
  val apartmentsState by viewModel.apartmentsState.collectAsStateWithLifecycle()
  val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
  val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
  val issuesState by viewModel.issuesState.collectAsStateWithLifecycle()
  val worksState by viewModel.worksState.collectAsStateWithLifecycle()
  val announcementsState by viewModel.announcementsState.collectAsStateWithLifecycle()
  val pollsState by viewModel.pollsState.collectAsStateWithLifecycle()
  val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
  val lookedUpInvite by viewModel.lookedUpInvite.collectAsStateWithLifecycle()
  val preApprovedMatch by viewModel.preApprovedMatch.collectAsStateWithLifecycle()
  val forceOnboarding by viewModel.forceOnboarding.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(feedbackMessage) {
    val msg = feedbackMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearFeedback()
    }
  }

  LaunchedEffect(deepLinkInviteCode) {
    if (!deepLinkInviteCode.isNullOrBlank()) {
      viewModel.setShowOnboarding(true)
    }
  }

  val onSignOut = {
    signOutUser(credentialManager = credentialManager, scope = scope)
  }

  CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
    val memberships = (membershipsState as? UiState.Success)?.data.orEmpty()
    val myMember = (myMemberState as? UiState.Success)?.data
    val building = (buildingState as? UiState.Success)?.data

    val isLoadingInitial =
      membershipsState is UiState.Loading ||
        (memberships.isNotEmpty() && myMemberState is UiState.Loading) ||
        (myMember?.statusEnum() == MemberStatus.APPROVED && buildingState is UiState.Loading)

    when {
      membershipsState is UiState.Error -> {
        Scaffold { innerPadding ->
          Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorBanner((membershipsState as UiState.Error).message)
          }
        }
      }
      isLoadingInitial -> {
        Scaffold { innerPadding ->
          Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingBlock(s.loadingLabel)
          }
        }
      }
      forceOnboarding ||
        memberships.isEmpty() ||
        myMember == null ||
        myMember.statusEnum() != MemberStatus.APPROVED ||
        building == null -> {
        if (forceOnboarding && building != null && myMember?.statusEnum() == MemberStatus.APPROVED) {
          BackHandler { viewModel.setShowOnboarding(false) }
        }
        BuildingOnboardingScreen(
          s = s,
          initialInviteCode = deepLinkInviteCode,
          myMember = myMember,
          memberships = memberships,
          lookedUpInvite = lookedUpInvite,
          preApprovedMatch = preApprovedMatch,
          isBusy = isBusy,
          isDarkTheme = isDarkTheme,
          feedbackMessage = feedbackMessage,
          onToggleLanguage = { viewModel.toggleLanguage() },
          onToggleTheme = onToggleTheme,
          onCreateBuilding = { name, city, neighborhood, floors, apts, fee, reserve, syndicApt ->
            onDeepLinkConsumed()
            viewModel.createBuilding(name, city, neighborhood, floors, apts, fee, reserve, syndicApt)
          },
          onLookupInvite = { code -> viewModel.lookupInvite(code) },
          onCheckPreApprovedPhone = { bId, phone -> viewModel.lookupPreApprovedPhone(bId, phone) },
          onSubmitJoinRequest = { invite, aptLabel, occupancy, phone, phoneVerifiedViaWhatsApp ->
            onDeepLinkConsumed()
            viewModel.submitJoinRequest(invite, aptLabel, occupancy, phone, phoneVerifiedViaWhatsApp)
          },
          onSelectExistingBuilding = { bId ->
            onDeepLinkConsumed()
            viewModel.switchActiveBuilding(bId)
          },
          onSignOut = onSignOut,
        )
      }
      else -> {
        val members = (membersState as? UiState.Success)?.data.orEmpty()
        val apartments = (apartmentsState as? UiState.Success)?.data.orEmpty()
        val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
        val expenses = (expensesState as? UiState.Success)?.data.orEmpty()
        val issues = (issuesState as? UiState.Success)?.data.orEmpty()
        val works = (worksState as? UiState.Success)?.data.orEmpty()
        val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
        val polls = (pollsState as? UiState.Success)?.data.orEmpty()
        val contacts = (contactsState as? UiState.Success)?.data.orEmpty()

        val cachedOverviews = remember { androidx.compose.runtime.mutableStateMapOf<String, BuildingOverview>() }
        val activeMonthKey =
          remember {
            val cal = Calendar.getInstance()
            "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
          }
        LaunchedEffect(building.id, apartments, payments, expenses, announcements, issues, works) {
          val totalCol = payments.filter { it.statusEnum() == PaymentStatus.PAID }.sumOf { it.amountMad }
          val totalExp = expenses.sumOf { it.amountMad }
          val paidThisMonth =
            payments.count { it.monthKey == activeMonthKey && it.statusEnum() == PaymentStatus.PAID }
          val openIssues = issues.count { it.statusEnum() != IssueStatus.RESOLVED }
          val activeWorks = works.count { it.statusEnum() != WorkStatus.COMPLETED }
          cachedOverviews[building.id] =
            BuildingOverview(
              buildingId = building.id,
              apartmentCount = apartments.size,
              paidThisMonthCount = paidThisMonth,
              netBalanceMad = totalCol - totalExp,
              announcementsCount = announcements.size,
              openIssuesCount = openIssues,
              activeWorksCount = activeWorks,
            )
        }

        SyndicMainScaffold(
          s = s,
          building = building,
          myMember = myMember,
          memberships = memberships,
          buildingOverviews = cachedOverviews.toMap(),
          members = members,
          apartments = apartments,
          payments = payments,
          expenses = expenses,
          issues = issues,
          works = works,
          announcements = announcements,
          polls = polls,
          contacts = contacts,
          controller = viewModel,
          snackbarHostState = snackbarHostState,
          isDarkTheme = isDarkTheme,
          onToggleTheme = onToggleTheme,
          onJoinWithCode = { viewModel.setShowOnboarding(true) },
          onSignOut = onSignOut,
        )
      }
    }
  }
}

@Composable
private fun SyndicGuestApp(
  initialLanguage: AppLanguage,
  launchConfig: GuestLaunchConfig,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onExitGuestMode: () -> Unit,
) {
  BackHandler { onExitGuestMode() }

  val guestController = remember(launchConfig) {
    GuestSessionController(
      initialLanguage = initialLanguage,
      initialRole = launchConfig.role,
      initialSokanPhone = launchConfig.sokanPhone,
      initialSokanName = launchConfig.sokanName,
      initialBuildingIds = launchConfig.buildingIds,
    )
  }
  val language by guestController.language.collectAsStateWithLifecycle()
  val s = stringsFor(language)
  val layoutDir = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

  val memberships by guestController.memberships.collectAsStateWithLifecycle()
  val buildingOverviews by guestController.buildingOverviews.collectAsStateWithLifecycle()
  val building by guestController.building.collectAsStateWithLifecycle()
  val myMember by guestController.myMember.collectAsStateWithLifecycle()
  val members by guestController.members.collectAsStateWithLifecycle()
  val apartments by guestController.apartments.collectAsStateWithLifecycle()
  val payments by guestController.payments.collectAsStateWithLifecycle()
  val expenses by guestController.expenses.collectAsStateWithLifecycle()
  val issues by guestController.issues.collectAsStateWithLifecycle()
  val works by guestController.works.collectAsStateWithLifecycle()
  val announcements by guestController.announcements.collectAsStateWithLifecycle()
  val polls by guestController.polls.collectAsStateWithLifecycle()
  val contacts by guestController.contacts.collectAsStateWithLifecycle()
  val feedbackMessage by guestController.feedbackMessage.collectAsStateWithLifecycle()
  var showGuestJoinDialog by remember(launchConfig.pendingInviteCode) {
    mutableStateOf(!launchConfig.pendingInviteCode.isNullOrBlank())
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(feedbackMessage) {
    val msg = feedbackMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      guestController.clearFeedback()
    }
  }

  CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
    SyndicMainScaffold(
      s = s,
      building = building,
      myMember = myMember,
      memberships = memberships,
      buildingOverviews = buildingOverviews,
      members = members,
      apartments = apartments,
      payments = payments,
      expenses = expenses,
      issues = issues,
      works = works,
      announcements = announcements,
      polls = polls,
      contacts = contacts,
      controller = guestController,
      snackbarHostState = snackbarHostState,
      isDarkTheme = isDarkTheme,
      onToggleTheme = onToggleTheme,
      onJoinWithCode = { showGuestJoinDialog = true },
      onSignOut = onExitGuestMode,
      signOutLabel = s.guestExitAction,
      topBanner = {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text =
                if (launchConfig.role == MemberRole.RESIDENT && !launchConfig.sokanPhone.isNullOrBlank()) {
                  "${myMember.displayName} • ${launchConfig.sokanPhone} (${myMember.apartmentLabel})"
                } else {
                  s.guestModeBanner
                },
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = { guestController.togglePreviewRole() },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("guest_role_toggle_button"),
              ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                  text =
                    if (myMember.roleEnum() == MemberRole.SYNDIC) s.guestSwitchRoleSyndic
                    else s.guestSwitchRoleResident,
                  style = MaterialTheme.typography.labelMedium,
                )
              }
              Spacer(Modifier.weight(1f))
              FilledTonalButton(
                onClick = onExitGuestMode,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("exit_guest_banner_button"),
              ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(s.signInNowAction, style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      },
    )

    if (showGuestJoinDialog) {
      GuestJoinInviteDialog(
        s = s,
        initialCode = launchConfig.pendingInviteCode ?: "YSM482",
        initialPhone = launchConfig.sokanPhone ?: myMember.phone,
        initialName = launchConfig.sokanName ?: myMember.displayName,
        guestController = guestController,
        onDismiss = { showGuestJoinDialog = false },
      )
    }
  }
}

@Composable
private fun GuestJoinInviteDialog(
  s: AppStrings,
  initialCode: String,
  initialPhone: String,
  initialName: String,
  guestController: GuestSessionController,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val semantic = LocalSyndicColors.current
  var code by remember { mutableStateOf(initialCode.uppercase()) }
  var matchedBuilding by remember { mutableStateOf(guestController.findBuildingByInviteCode(initialCode)) }
  var phone by remember { mutableStateOf(initialPhone) }
  var residentName by remember { mutableStateOf(initialName) }
  var aptLabel by remember { mutableStateOf("") }
  var occupancy by remember { mutableStateOf(OccupancyType.OWNER) }
  var otpCode by remember { mutableStateOf("") }
  var otpPreview by remember { mutableStateOf<String?>(null) }
  var phoneVerified by remember { mutableStateOf(false) }

  val preApprovedApt =
    remember(code, phone, matchedBuilding) {
      if (matchedBuilding != null) guestController.isPhonePreApprovedForCode(code, phone) else null
    }
  LaunchedEffect(preApprovedApt) {
    if (preApprovedApt != null) {
      if (aptLabel.isBlank()) aptLabel = preApprovedApt.label
      if (residentName.isBlank()) residentName = preApprovedApt.residentName
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(s.joinBuildingTab, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = code,
            onValueChange = {
              code = it.uppercase().take(6)
              matchedBuilding = guestController.findBuildingByInviteCode(code)
            },
            label = { Text(s.inviteCodeInputLabel) },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("guest_join_invite_code"),
          )
          Button(onClick = { matchedBuilding = guestController.findBuildingByInviteCode(code) }) {
            Icon(Icons.Default.Search, contentDescription = s.lookupBuildingAction)
          }
        }

        if (matchedBuilding != null) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "${matchedBuilding!!.name} • ${matchedBuilding!!.city}",
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(10.dp),
            )
          }
          OutlinedTextField(
            value = phone,
            onValueChange = {
              phone = it
              phoneVerified = false
            },
            label = { Text(s.sokanPhoneLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("guest_join_phone"),
          )
          if (preApprovedApt != null) {
            Surface(
              color = semantic.paidContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = semantic.paidText, modifier = Modifier.size(16.dp))
                Text(
                  text = "${s.phoneMatchedAutoApproveMsg} (${preApprovedApt.label})",
                  color = semantic.paidText,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = otpCode,
              onValueChange = {
                val clean = it.filter { ch -> ch.isDigit() }.take(6)
                otpCode = clean
                if (clean.length == 6 && SokanAccountStore.verifyWhatsAppAuthCode(context, phone, clean)) {
                  phoneVerified = true
                }
              },
              label = { Text(s.sokanOtpLabel) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
              onClick = {
                val issued = SokanAccountStore.issueWhatsAppAuthCode(context, phone)
                otpPreview = issued
                runCatching {
                  ReceiptGenerator.sendWhatsAppAuthCode(context, phone, issued, matchedBuilding!!.name)
                }
              },
              enabled = SokanAccountStore.normalizePhoneKey(phone).length >= 6,
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(Modifier.width(4.dp))
              Text(s.sendWhatsappOtpAction, style = MaterialTheme.typography.labelSmall)
            }
          }
          if (otpPreview != null) {
            Text(
              text = "${s.otpSentHint} $otpPreview",
              style = MaterialTheme.typography.labelSmall,
              color = semantic.paidText,
              fontWeight = FontWeight.Bold,
            )
          }
          OutlinedTextField(
            value = residentName,
            onValueChange = { residentName = it },
            label = { Text(s.residentNameLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = aptLabel,
            onValueChange = { aptLabel = it },
            label = { Text(s.selectApartmentLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
              selected = occupancy == OccupancyType.OWNER,
              onClick = { occupancy = OccupancyType.OWNER },
              shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) {
              Text(s.occupancyOwner)
            }
            SegmentedButton(
              selected = occupancy == OccupancyType.TENANT,
              onClick = { occupancy = OccupancyType.TENANT },
              shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) {
              Text(s.occupancyTenant)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val b = matchedBuilding ?: return@Button
          guestController.joinBuildingByInviteCode(
            code = code,
            residentName = residentName,
            phone = phone,
            aptLabel = aptLabel,
            occupancyType = occupancy,
            phoneVerifiedViaWhatsApp = phoneVerified,
          ).onSuccess { st ->
            if (phoneVerified) {
              SokanAccountStore.registerOrUpdateAccount(
                context = context,
                rawPhone = phone,
                displayName = residentName,
                plainPassword = "",
                buildingId = b.id,
                buildingName = b.name,
                apartmentLabel = aptLabel,
              )
            }
            guestController.showNote(
              if (st == MemberStatus.APPROVED) s.phoneMatchedAutoApproveMsg
              else s.pendingApprovalTitle
            )
            onDismiss()
          }
        },
        enabled = matchedBuilding != null && (aptLabel.isNotBlank() || preApprovedApt != null),
      ) {
        Text(s.sendJoinRequest)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyndicMainScaffold(
  s: AppStrings,
  building: Building,
  myMember: BuildingMember,
  memberships: List<UserMembership>,
  buildingOverviews: Map<String, BuildingOverview>,
  members: List<BuildingMember>,
  apartments: List<Apartment>,
  payments: List<CotisationPayment>,
  expenses: List<Expense>,
  issues: List<BuildingIssue>,
  works: List<BuildingWork>,
  announcements: List<Announcement>,
  polls: List<Poll>,
  contacts: List<BuildingContact>,
  controller: SyndicController,
  snackbarHostState: SnackbarHostState,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onJoinWithCode: (() -> Unit)?,
  onSignOut: () -> Unit,
  signOutLabel: String = s.signOutAction,
  topBanner: (@Composable () -> Unit)? = null,
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }
  var showPortfolioDialog by remember { mutableStateOf(false) }
  var showCreateBuildingDialog by remember { mutableStateOf(false) }
  var showNotificationsDialog by remember { mutableStateOf(false) }

  val notifications by controller.notificationsState.collectAsStateWithLifecycle()
  val unreadCount = notifications.count { !it.isReadBy(controller.currentUserId) }

  var hasPushPermission by remember {
    mutableStateOf(LiveNotificationManager.hasNotificationPermission(context))
  }
  val pushPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasPushPermission = granted || LiveNotificationManager.hasNotificationPermission(context)
    }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPushPermission) {
      pushPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  val seenNotificationIds = remember(building.id) { mutableSetOf<String>() }
  var notificationsSeeded by remember(building.id) { mutableStateOf(false) }
  LaunchedEffect(building.id, notifications) {
    if (!notificationsSeeded) {
      notifications.forEach { seenNotificationIds.add(it.id) }
      notificationsSeeded = true
    } else {
      notifications
        .filter { !seenNotificationIds.contains(it.id) && !it.isReadBy(controller.currentUserId) && it.senderUid != controller.currentUserId }
        .forEach { notif ->
          seenNotificationIds.add(notif.id)
          LiveNotificationManager.postLiveNotification(
            context = context,
            notificationKey = notif.id,
            title = notif.title,
            body = notif.body,
            buildingName = building.name,
          )
        }
      notifications.forEach { seenNotificationIds.add(it.id) }
    }
  }

  val tabs =
    listOf(
      Triple(s.tabBuildings, Icons.Filled.Apartment, Icons.Outlined.Apartment),
      Triple(s.tabFinance, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
      Triple(s.tabWorks, Icons.Filled.Construction, Icons.Outlined.Construction),
      Triple(s.tabResidents, Icons.Filled.Groups, Icons.Outlined.Groups),
      Triple(s.tabCreate, Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
    )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 600.dp
    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        Column {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { showPortfolioDialog = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("building_switcher_button"),
              ) {
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = building.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                      Icons.Default.KeyboardArrowDown,
                      contentDescription = s.myBuildingsTitle,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                  Text(
                    text = "${building.city} • ${memberships.size.coerceAtLeast(1)} ${s.buildingsCountLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            },
            actions = {
              IconButton(
                onClick = { showNotificationsDialog = true },
                modifier = Modifier.testTag("notifications_bell_button"),
              ) {
                BadgedBox(
                  badge = {
                    if (unreadCount > 0) {
                      Badge { Text(unreadCount.coerceAtMost(99).toString()) }
                    }
                  }
                ) {
                  Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = s.notificationsTitle,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              ThemeToggleChip(
                isDarkTheme = isDarkTheme,
                s = s,
                onToggle = onToggleTheme,
              )
              LanguageToggleChip(
                s = s,
                onToggle = { controller.toggleLanguage() },
                modifier = Modifier.padding(end = 8.dp),
              )
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
              ),
          )
          if (selectedTab != 0 && memberships.isNotEmpty()) {
            BuildingContextStrip(
              activeBuildingId = building.id,
              memberships = memberships,
              s = s,
              onSelectBuilding = { bId -> controller.switchActiveBuilding(bId) },
              onCreateNewBuilding = { showCreateBuildingDialog = true },
            )
          }
          if (topBanner != null) {
            topBanner()
          }
        }
      },
      bottomBar = {
        if (!isWide) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            tabs.forEachIndexed { idx, (label, filledIcon, outlinedIcon) ->
              NavigationBarItem(
                selected = selectedTab == idx,
                onClick = { selectedTab = idx },
                icon = {
                  Icon(
                    if (selectedTab == idx) filledIcon else outlinedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag("nav_tab_$idx"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
        if (isWide) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            tabs.forEachIndexed { idx, (label, filledIcon, outlinedIcon) ->
              NavigationRailItem(
                selected = selectedTab == idx,
                onClick = { selectedTab = idx },
                icon = {
                  Icon(
                    if (selectedTab == idx) filledIcon else outlinedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
              )
            }
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            when (selectedTab) {
              0 ->
                HomeScreen(
                  s = s,
                  building = building,
                  myMember = myMember,
                  memberships = memberships,
                  buildingOverviews = buildingOverviews,
                  apartments = apartments,
                  payments = payments,
                  expenses = expenses,
                  works = works,
                  controller = controller,
                  onOpenCreateBuilding = { showCreateBuildingDialog = true },
                  onNavigateToTab = { tabIdx -> selectedTab = tabIdx },
                )
              1 ->
                FinanceScreen(
                  s = s,
                  building = building,
                  myMember = myMember,
                  apartments = apartments,
                  payments = payments,
                  expenses = expenses,
                  controller = controller,
                )
              2 ->
                IssuesScreen(
                  s = s,
                  building = building,
                  myMember = myMember,
                  issues = issues,
                  works = works,
                  controller = controller,
                )
              3 ->
                BuildingScreen(
                  s = s,
                  building = building,
                  myMember = myMember,
                  apartments = apartments,
                  payments = payments,
                  announcements = announcements,
                  polls = polls,
                  members = members,
                  contacts = contacts,
                  memberships = memberships,
                  controller = controller,
                  onOpenPortfolio = { showPortfolioDialog = true },
                  onSignOut = onSignOut,
                  signOutLabel = signOutLabel,
                  isDarkTheme = isDarkTheme,
                  onToggleTheme = onToggleTheme,
                )
              else ->
                CreateHubScreen(
                  s = s,
                  building = building,
                  myMember = myMember,
                  apartments = apartments,
                  controller = controller,
                  onOpenCreateBuilding = { showCreateBuildingDialog = true },
                  onJoinWithCode = onJoinWithCode,
                  onNavigateToTab = { tabIdx -> selectedTab = tabIdx },
                )
            }
          }
        }
      }
    }
  }

  if (showNotificationsDialog) {
    NotificationCenterDialog(
      s = s,
      buildingName = building.name,
      notifications = notifications,
      currentUserId = controller.currentUserId,
      hasPushPermission = hasPushPermission,
      onRequestPushPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pushPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      },
      onMarkAllRead = { controller.markAllNotificationsRead() },
      onDismiss = { showNotificationsDialog = false },
    )
  }

  if (showPortfolioDialog) {
    BuildingPortfolioDialog(
      s = s,
      activeBuildingId = building.id,
      memberships = memberships,
      onSelectBuilding = { bId -> controller.switchActiveBuilding(bId) },
      onCreateNewBuilding = { showCreateBuildingDialog = true },
      onJoinWithCode = onJoinWithCode,
      onDismiss = { showPortfolioDialog = false },
    )
  }

  if (showCreateBuildingDialog) {
    CreateBuildingDialog(
      s = s,
      onDismiss = { showCreateBuildingDialog = false },
      onSubmit = { name, city, neighborhood, floors, apts, fee, reserve, syndicApt ->
        controller.createBuilding(
          name = name,
          city = city,
          neighborhood = neighborhood,
          floorCount = floors,
          apartmentCount = apts,
          defaultFee = fee,
          reserveFund = reserve,
          syndicAptLabel = syndicApt,
        )
        showCreateBuildingDialog = false
      },
    )
  }
}
