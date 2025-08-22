package com.bizclimb.nextdoorpush.app

import android.app.PendingIntent
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    try {
      val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        ?: java.util.UUID.randomUUID().toString()
      Net.registerTokenAllAccounts(deviceId, token)
    } catch (_: Throwable) { }
  }

  override fun onMessageReceived(msg: RemoteMessage) {
    val payload = msg.data

    val text = payload[Const.KEY_TEXT].orEmpty()
    val approveUrl = payload[Const.KEY_APPROVE_URL].orEmpty()
    val closeUrl = payload[Const.KEY_CLOSE_URL].orEmpty()
    val matchedId = payload[Const.KEY_MATCHED_ID]?.ifBlank { null }
      ?: System.currentTimeMillis().toString()
    val accountLabel = payload[Const.KEY_ACCOUNT_LABEL].orEmpty()

    val title = if (accountLabel.isNotBlank()) "ND • $accountLabel" else "Nextdoor Match"
    val notifId = matchedId.hashCode()

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
      this, notifId, approveIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val piClose = PendingIntent.getBroadcast(
      this, notifId + 1, closeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(this, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_notify_more)
      .setContentTitle(title)
      .setContentText(text.take(140))
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)

    if (approveUrl.isNotBlank()) builder.addAction(0, "Post", piApprove)
    if (closeUrl.isNotBlank()) builder.addAction(0, "Close", piClose)

    Notif.notify(this, notifId, builder)
  }
}
