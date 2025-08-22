package com.bizclimb.nextdoorpush.app

import android.app.PendingIntent
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    // Auto register this device for all accounts on your server
    // Server will fan out to all accounts except those listed in device_token_exclude
    try {
      val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        ?: java.util.UUID.randomUUID().toString()
      Net.registerTokenAllAccounts(deviceId, token)
    } catch (_: Throwable) {
      // ignore
    }
  }

  override fun onMessageReceived(msg: RemoteMessage) {
    val data = msg.data

    val text = data[Const.KEY_TEXT].orEmpty()
    val approveUrl = data[Const.KEY_APPROVE_URL].orEmpty()
    val closeUrl = data[Const.KEY_CLOSE_URL].orEmpty()
    val matchedId = data[Const.KEY_MATCHED_ID]?.ifBlank { null }
      ?: System.currentTimeMillis().toString()
    val accountLabel = data[Const.KEY_ACCOUNT_LABEL].orEmpty()

    val title = if (accountLabel.isNotBlank()) "ND • $accountLabel" else "Nextdoor Match"
    val notifId = matchedId.hashCode()

    // Build broadcast intents for actions
    val approveIntent = Intent(this, ActionReceiver::class.java).apply {
      action = "com.bizclimb.nextdoorpush.app.ACTION_CLICK"
      putExtra("url", approveUrl)
      putExtra("matched_id", matchedId)
      putExtra("verb", "post")
      putExtra("notif_id", notifId)
    }
    val closeIntent = Intent(this, ActionReceiver::class.java).apply {
      action = "com.bizclimb.nextdoorpush.app.ACTION_CLICK"
      putExtra("url", closeUrl)
      putExtra("matched_id", matchedId)
      putExtra("verb", "close")
      putExtra("notif_id", notifId)
    }

    val piApprove = PendingIntent.getBroadcast(
      this,
      notifId,
      approveIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or BuildConfig.PI_IMMUTABLE
    )
    val piClose = PendingIntent.getBroadcast(
      this,
      notifId + 1,
      closeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or BuildConfig.PI_IMMUTABLE
    )

    val notif = NotificationCompat.Builder(this, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_notify_more)
      .setContentTitle(title)
      .setContentText(text.take(140))
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .addAction(0, "Close", piClose)
      .addAction(0, "Post", piApprove)
      .setAutoCancel(true)
      .build()

    NotificationManagerCompat.from(this).notify(notifId, notif)
  }
}
