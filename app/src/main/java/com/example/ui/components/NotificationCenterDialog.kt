package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BuildingNotification
import com.example.data.model.NotificationType
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalSyndicColors
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationCenterDialog(
  s: AppStrings,
  buildingName: String,
  notifications: List<BuildingNotification>,
  currentUserId: String,
  hasPushPermission: Boolean,
  onRequestPushPermission: () -> Unit,
  onMarkAllRead: () -> Unit,
  onDismiss: () -> Unit,
) {
  val semantic = LocalSyndicColors.current
  val unreadCount = notifications.count { !it.isReadBy(currentUserId) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(s.notificationsTitle, fontWeight = FontWeight.Bold)
          Text(
            text = buildingName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (unreadCount > 0) {
          TextButton(
            onClick = onMarkAllRead,
            modifier = Modifier.testTag("mark_all_notifications_read"),
          ) {
            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(s.markAllReadAction, style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!hasPushPermission) {
          FilledTonalButton(
            onClick = onRequestPushPermission,
            modifier = Modifier.fillMaxWidth().testTag("enable_push_notifications_button"),
          ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.enablePushNotifications)
          }
        } else {
          Surface(
            color = semantic.paidContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = s.pushEnabledBadge,
              color = semantic.paidText,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }

        if (notifications.isEmpty()) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Default.NotificationsNone,
              contentDescription = null,
              modifier = Modifier.size(40.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = s.noNotificationsYet,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(notifications, key = { it.id }) { notif ->
              val isUnread = !notif.isReadBy(currentUserId)
              val timeStr =
                remember(notif.createdAt) {
                  notif.createdAt?.toDate()?.let {
                    SimpleDateFormat("dd/MM HH:mm", Locale.US).format(it)
                  } ?: ""
                }
              val (icon, tint) =
                when (notif.typeEnum()) {
                  NotificationType.ANNOUNCEMENT -> Icons.Default.Campaign to MaterialTheme.colorScheme.primary
                  NotificationType.WORK -> Icons.Default.Construction to MaterialTheme.colorScheme.tertiary
                  NotificationType.ISSUE -> Icons.Default.ReportProblem to semantic.overdueText
                  NotificationType.PAYMENT -> Icons.Default.AccountBalanceWallet to semantic.paidText
                  NotificationType.MEMBER_JOIN -> Icons.Default.PersonAdd to MaterialTheme.colorScheme.secondary
                  NotificationType.GENERAL -> Icons.Default.NotificationsActive to MaterialTheme.colorScheme.primary
                }
              Surface(
                shape = MaterialTheme.shapes.medium,
                color =
                  if (isUnread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border =
                  BorderStroke(
                    width = 1.dp,
                    color = if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else semantic.hairline,
                  ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  Surface(
                    shape = CircleShape,
                    color = tint.copy(alpha = 0.14f),
                    modifier = Modifier.size(34.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    }
                  }
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = notif.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                      )
                      if (isUnread) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary)
                      }
                    }
                    Text(
                      text = notif.body,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = listOf(notif.senderName, timeStr).filter { it.isNotBlank() }.joinToString(" • "),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(s.cancelAction)
      }
    },
  )
}
