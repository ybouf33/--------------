package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.util.LocalSokanProfile
import com.example.data.util.ReceiptGenerator
import com.example.data.util.SokanAccountStore
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors

@Composable
fun SokanAuthAndRecoveryDialog(
  s: AppStrings,
  initialTab: Int = 0,
  initialPhone: String = "",
  initialName: String = "",
  onDismiss: () -> Unit,
  onLoginSuccess: (profile: LocalSokanProfile, wasRecovered: Boolean, newPlainPass: String) -> Unit,
) {
  val context = LocalContext.current
  val semantic = LocalSyndicColors.current
  var activeTab by remember { mutableIntStateOf(initialTab) }

  // Tab 0: Phone + WhatsApp OTP / Password Login
  var phone by remember { mutableStateOf(initialPhone) }
  var displayName by remember { mutableStateOf(initialName) }
  var password by remember { mutableStateOf("") }
  var enteredOtp by remember { mutableStateOf("") }
  var generatedOtpPreview by remember { mutableStateOf<String?>(null) }

  // Tab 1: Recover Account / Change Phone / Forgot Password via WhatsApp
  var oldPhone by remember { mutableStateOf(initialPhone) }
  var newPhone by remember { mutableStateOf("") }
  var recoverySecretInput by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var recoveryPreview by remember { mutableStateOf<Pair<String, String>?>(null) }

  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = if (activeTab == 0) s.sokanLoginTitle else s.recoverAccountTitle,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = activeTab == 0,
            onClick = {
              activeTab = 0
              errorText = null
            },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
            modifier = Modifier.testTag("sokan_tab_login"),
          ) {
            Text(s.signInNowAction, maxLines = 1)
          }
          SegmentedButton(
            selected = activeTab == 1,
            onClick = {
              activeTab = 1
              errorText = null
            },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
            modifier = Modifier.testTag("sokan_tab_recovery"),
          ) {
            Text(s.recoverDataAction.substringBefore(" ").ifBlank { "استرجاع" }, maxLines = 1)
          }
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!errorText.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorText.orEmpty(),
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        if (activeTab == 0) {
          Text(
            text = s.sokanLoginSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = s.sokanDemoHint,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(s.sokanPhoneLabel) },
            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("sokan_phone_input"),
          )
          OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(s.residentNameLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(s.sokanPasswordLabel) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("sokan_password_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = enteredOtp,
              onValueChange = { enteredOtp = it.filter { ch -> ch.isDigit() }.take(6) },
              label = { Text(s.sokanOtpLabel) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("sokan_otp_input"),
            )
            FilledTonalButton(
              onClick = {
                errorText = null
                if (SokanAccountStore.normalizePhoneKey(phone).length < 6) {
                  errorText = s.sokanPhoneLabel
                  return@FilledTonalButton
                }
                val code = SokanAccountStore.issueWhatsAppAuthCode(context, phone)
                generatedOtpPreview = code
                runCatching {
                  ReceiptGenerator.sendWhatsAppAuthCode(context, phone, code)
                }
              },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
              modifier = Modifier.testTag("send_whatsapp_otp_button"),
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(s.sendWhatsappOtpAction, style = MaterialTheme.typography.labelSmall)
            }
          }

          if (generatedOtpPreview != null) {
            Surface(
              color = semantic.paidContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "${s.otpSentHint} $generatedOtpPreview",
                color = semantic.paidText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(10.dp),
              )
            }
          }

          TextButton(
            onClick = {
              oldPhone = phone
              activeTab = 1
              errorText = null
            },
            modifier = Modifier.testTag("forgot_or_changed_phone_link"),
          ) {
            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.forgotOrChangedPhoneLink)
          }
        } else {
          OutlinedTextField(
            value = oldPhone,
            onValueChange = { oldPhone = it },
            label = { Text(s.oldPhoneLabel) },
            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("recovery_old_phone_input"),
          )
          OutlinedTextField(
            value = newPhone,
            onValueChange = { newPhone = it },
            label = { Text(s.newPhoneOptionalLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("recovery_new_phone_input"),
          )
          FilledTonalButton(
            onClick = {
              errorText = null
              if (SokanAccountStore.normalizePhoneKey(oldPhone).length < 6) {
                errorText = s.oldPhoneLabel
                return@FilledTonalButton
              }
              val creds = SokanAccountStore.issueRecoveryCredentials(context, oldPhone)
              recoveryPreview = creds
              val existing = SokanAccountStore.getAccountByPhone(context, oldPhone)
              runCatching {
                ReceiptGenerator.sendWhatsAppRecoveryCredentials(
                  context = context,
                  targetPhone = newPhone.ifBlank { oldPhone },
                  oldPhone = oldPhone,
                  recoveryCode = creds.first,
                  tempPassword = creds.second,
                  residentName = existing?.displayName ?: displayName,
                )
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("send_recovery_whatsapp_button"),
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.sendRecoveryWhatsappAction)
          }

          if (recoveryPreview != null) {
            Surface(
              color = semantic.paidContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "${s.otpSentHint} ${recoveryPreview!!.first} | ${recoveryPreview!!.second}",
                color = semantic.paidText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(10.dp),
              )
            }
          }

          OutlinedTextField(
            value = recoverySecretInput,
            onValueChange = { recoverySecretInput = it },
            label = { Text(s.recoveryCodeOrPassLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("recovery_secret_input"),
          )
          OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(s.newPasswordLabel) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("recovery_new_password_input"),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          errorText = null
          if (activeTab == 0) {
            val phoneKey = SokanAccountStore.normalizePhoneKey(phone)
            if (phoneKey.length < 6) {
              errorText = s.sokanPhoneLabel
              return@Button
            }
            val existing = SokanAccountStore.getAccountByPhone(context, phone)
            val otpValid =
              enteredOtp.isNotBlank() && SokanAccountStore.verifyWhatsAppAuthCode(context, phone, enteredOtp)
            val passValid =
              password.isNotBlank() && SokanAccountStore.verifyPassword(context, phone, password)

            if (existing != null) {
              if (!otpValid && !passValid) {
                errorText = "${s.sokanOtpLabel} / ${s.sokanPasswordLabel}"
                return@Button
              }
              val updated =
                if (password.isNotBlank() && otpValid) {
                  SokanAccountStore.registerOrUpdateAccount(
                    context = context,
                    rawPhone = phone,
                    displayName = displayName.ifBlank { existing.displayName },
                    plainPassword = password,
                  )
                } else {
                  existing
                }
              SokanAccountStore.setActiveSokanPhone(context, updated.phoneKey)
              onLoginSuccess(updated, false, password)
            } else {
              // First-time Sokan phone account requires verified WhatsApp OTP
              if (!otpValid) {
                errorText = s.sokanOtpLabel
                return@Button
              }
              val created =
                SokanAccountStore.registerOrUpdateAccount(
                  context = context,
                  rawPhone = phone,
                  displayName = displayName.ifBlank { "ساكن" },
                  plainPassword = password.ifBlank { "1234" },
                )
              SokanAccountStore.setActiveSokanPhone(context, created.phoneKey)
              onLoginSuccess(created, false, password.ifBlank { "1234" })
            }
          } else {
            val res =
              SokanAccountStore.recoverAndMigrateAccount(
                context = context,
                oldPhone = oldPhone,
                newPhone = newPhone,
                enteredSecret = recoverySecretInput,
                newPassword = newPassword,
                newDisplayName = displayName,
              )
            res.onSuccess { migrated ->
              onLoginSuccess(migrated, true, newPassword)
            }.onFailure { err ->
              errorText = err.localizedMessage ?: "Error"
            }
          }
        },
        modifier = Modifier.testTag("sokan_confirm_auth_button"),
      ) {
        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (activeTab == 0) s.verifyAndEnterAction else s.recoverDataAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(s.cancelAction) }
    },
  )
}
