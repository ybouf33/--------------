package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.util.LocalSokanProfile
import com.example.ui.components.SokanAuthAndRecoveryDialog
import com.example.ui.components.ThemeToggleChip
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.stringsFor
import com.example.ui.theme.LocalSyndicColors

@Composable
fun AuthScreen(
  language: AppLanguage,
  isDarkTheme: Boolean = false,
  deepLinkInviteCode: String? = null,
  onToggleLanguage: () -> Unit,
  onToggleTheme: () -> Unit = {},
  onAuthSuccess: () -> Unit,
  onGuestSignIn: () -> Unit,
  onSokanSignIn: (LocalSokanProfile, Boolean) -> Unit = { _, _ -> onGuestSignIn() },
  onJoinViaInviteGuest: (String) -> Unit = { _ -> onGuestSignIn() },
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val s = stringsFor(language)
  val semantic = LocalSyndicColors.current
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var showSokanDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 560.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Adaptive Light/Dark Hero Panel
      Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        border = BorderStroke(1.dp, semantic.heroBorder),
        shadowElevation = if (semantic.isLight) 2.dp else 6.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier =
            Modifier
              .background(
                Brush.linearGradient(
                  listOf(semantic.heroStart, semantic.heroEnd)
                )
              )
              .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(50),
              color = semantic.heroChipBg,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Gavel,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                  tint = semantic.goldAccent,
                )
                Text(
                  text = s.lawBadge,
                  style = MaterialTheme.typography.labelMedium,
                  color = semantic.heroContent,
                )
              }
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              ThemeToggleChip(isDarkTheme = isDarkTheme, s = s, onToggle = onToggleTheme)
              OutlinedButton(
                onClick = onToggleLanguage,
                border = BorderStroke(1.dp, semantic.heroContent.copy(alpha = 0.28f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("auth_language_toggle"),
              ) {
                Icon(
                  Icons.Default.Language,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = semantic.heroContent,
                )
                Spacer(Modifier.width(6.dp))
                Text(s.switchLanguageLabel, color = semantic.heroContent)
              }
            }
          }

          Image(
            painter = painterResource(id = R.drawable.img_onboarding_auth),
            contentDescription = stringResource(R.string.cd_onboarding_art),
            modifier =
              Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop,
          )

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = s.appName,
              style = MaterialTheme.typography.displaySmall,
              fontWeight = FontWeight.ExtraBold,
              color = semantic.heroContent,
            )
            Text(
              text = s.authHeadline,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = semantic.heroContent.copy(alpha = 0.92f),
            )
            Text(
              text = s.authSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = semantic.heroSubtleContent,
            )
          }
        }
      }

      if (!deepLinkInviteCode.isNullOrBlank()) {
        FilledTonalButton(
          onClick = { onJoinViaInviteGuest(deepLinkInviteCode) },
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_invite_link_join_button"),
        ) {
          Icon(Icons.Default.Key, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("${s.joinWithLinkButton} ($deepLinkInviteCode)", fontWeight = FontWeight.Bold)
        }
      }

      // Clean Features Card
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.hairline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          FeatureRow(Icons.Default.AccountBalanceWallet, s.authFeatureFinances, MaterialTheme.colorScheme.primary)
          FeatureRow(Icons.Default.Construction, s.authFeatureIssues, MaterialTheme.colorScheme.secondary)
          FeatureRow(Icons.Default.Groups, s.authFeatureGovernance, MaterialTheme.colorScheme.tertiary)
        }
      }

      if (errorMsg != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMsg ?: "",
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Spacer(Modifier.weight(1f, fill = false))

      Button(
        onClick = {
          errorMsg = null
          isLoading = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isLoading = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isLoading = false
              errorMsg = msg
            },
            scope = scope,
            onAuthCancelled = {
              isLoading = false
              errorMsg = context.getString(R.string.error_sign_in_cancelled)
            },
          )
        },
        enabled = !isLoading,
        shape = MaterialTheme.shapes.medium,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
          Spacer(Modifier.width(10.dp))
          Text(s.signingIn)
        } else {
          Icon(Icons.Default.Login, contentDescription = null)
          Spacer(Modifier.width(10.dp))
          Text(s.signInWithGoogle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
      }

      FilledTonalButton(
        onClick = { showSokanDialog = true },
        enabled = !isLoading,
        shape = MaterialTheme.shapes.medium,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("sokan_phone_sign_in_button"),
      ) {
        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(s.sokanLoginTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = onGuestSignIn,
        enabled = !isLoading,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier =
          Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("guest_sign_in_button"),
      ) {
        Icon(Icons.Default.PersonOutline, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(s.continueAsGuest, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      }
    }
  }

  if (showSokanDialog) {
    SokanAuthAndRecoveryDialog(
      s = s,
      onDismiss = { showSokanDialog = false },
      onLoginSuccess = { profile, wasRecovered, _ ->
        showSokanDialog = false
        onSokanSignIn(profile, wasRecovered)
      },
    )
  }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String, accent: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = accent.copy(alpha = 0.12f),
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
    )
  }
}
