package com.bizclimb.nextdoorpush.app

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID
import android.os.SystemClock

class PushService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    try {
      val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        ?: UUID.randomUUID().toString()
      Net.registerTokenAllAccounts(deviceId, token)
    } catch (_: Throwable) { }
  }

  override fun onMessageReceived(msg: RemoteMessage) {
    val data = msg.data

    val text        = data[Const.KEY_TEXT].orEmpty()
    val approveUrl  = data[Const.KEY_APPROVE_URL].orEmpty()
    val closeUrl    = data[Const.KEY_CLOSE_URL].orEmpty()
    val accountLabel= data[Const.KEY_ACCOUNT_LABEL].orEmpty()

    // Always have a strong unique id
    val midRaw = data[Const.KEY_MATCHED_ID].orEmpty()
    val matchedId = if (midRaw.isNotBlank()) midRaw else UUID.randomUUID().toString()

    val title   = if (accountLabel.isNotBlank()) "ND • $accountLabel" else "Nextdoor Match"
    val notifId = matchedId.hashCode()

    // Unique data Uri makes PI identity unique even if requestCode collides
    val nonce = SystemClock.elapsedRealtimeNanos()

    val approveIntent = Intent(this, ActionReceiver::class.java).apply {
      action = "com.bizclimb.nextdoorpush.app.ACTION_CLICK"
      data = Uri.parse("app://ndpush/action?verb=post&mid=$matchedId&n=$nonce")
      putExtra("url", approveUrl)
      putExtra("matched_id", matchedId)
      putExtra("verb", "post")
      putExtra("notif_id", notifId)
    }
    val closeIntent = Intent(this, ActionReceiver::class.java).apply {
      action = "com.bizclimb.nextdoorpush.app.ACTION_CLICK"
      data = Uri.parse("app://ndpush/action?verb=close&mid=$matchedId&n=$nonce")
      putExtra("url", closeUrl)
      putExtra("matched_id", matchedId)
      putExtra("verb", "close")
      putExtra("notif_id", notifId)
    }

    val piApprove = PendingIntent.getBroadcast(
      this,
      notifId, // stable per matched id
      approveIntent,
      PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val piClose = PendingIntent.getBroadcast(
      this,
      notifId xor 0x7F4A11, // different but deterministic for second action
      closeIntent,
      PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(this, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_notify_more)
      .setContentTitle(title)
      .setContentText(text.take(140))
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      // order is Post then Close
      .apply {
        if (approveUrl.isNotBlank()) addAction(0, "Post", piApprove)
        if (closeUrl.isNotBlank()) addAction(0, "Close", piClose)
      }

    Notif.notify(this, notifId, builder)
  }
}
