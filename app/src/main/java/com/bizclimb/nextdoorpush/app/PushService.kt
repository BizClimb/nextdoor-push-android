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
    val data = msg.data
    val text = data[Const.KEY_TEXT].orEmpty()
    val approveUrl = data[Const.KEY_APPROVE_URL].orEmpty()
    val closeUrl = data[Const.KEY_CLOSE_URL].orEmpty()
    val matchedId = data[Const.KEY_MATCHED_ID]?.ifBlank { null }
      ?: System.currentTimeMillis().toString()
    val accountLabel = data[Const.KEY_ACCOUNT_LABEL].orEmpty()

    val title = if (accountLabel.isNotBlank()) "ND • $accountLabel" else "Nextdoor Match"

    // Use unique, collision resistant ids for the notification and each action
    val notifId = ("notif:$matchedId").hashCode()
    val approveReqCode = ("approve:$matchedId").hashCode()
    val closeReqCode = ("close:$matchedId").hashCode()

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

    // CANCEL_CURRENT ensures no stale extras are reused
    val flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    val piApprove = PendingIntent.getBroadcast(this, approveReqCode, approveIntent, flags)
    val piClose = PendingIntent.getBroadcast(this, closeReqCode, closeIntent, flags)

    val builder = NotificationCompat.Builder(this, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_notify_more)
      .setContentTitle(title)
      .setContentText(text.take(140))
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)

    // Approve first, then Close
    if (approveUrl.isNotBlank()) builder.addAction(0, "Post", piApprove)
    if (closeUrl.isNotBlank()) builder.addAction(0, "Close", piClose)

    Notif.notify(this, notifId, builder)
  }
}
