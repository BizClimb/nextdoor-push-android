package com.bizclimb.nextdoorpush.app

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.math.abs

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

    val text        = data[Const.KEY_TEXT].orEmpty()
    val approveUrl  = data[Const.KEY_APPROVE_URL].orEmpty()
    val closeUrl    = data[Const.KEY_CLOSE_URL].orEmpty()
    val matchedId   = data[Const.KEY_MATCHED_ID]?.ifBlank { null }
      ?: System.currentTimeMillis().toString()
    val actionToken = data[Const.KEY_ACTION_TOKEN].orEmpty()
    val accountLbl  = data[Const.KEY_ACCOUNT_LABEL].orEmpty()

    // Derive a stable, collision‑resistant base id
    // Prefer action_token (server generated, unique per post) then matchedId
    val baseId = safeId(actionToken.ifBlank { matchedId })

    val title = if (accountLbl.isNotBlank()) "ND • $accountLbl" else "Nextdoor Match"

    // Build explicit broadcast intents with unique data URIs so Android treats each as distinct
    val approveIntent = Intent(this, ActionReceiver::class.java).apply {
      action = "com.bizclimb.nextdoorpush.app.ACTION_CLICK"
      data = Uri.parse("ndp://action/$baseId?verb=post")  // uniqueness key
      putExtra("url", approveUrl)
      putExtra("matched_id", matchedId)
      putExtra("verb", "post")
      putExtra("notif_id", baseId)
    }
    val closeIntent = Intent(this, ActionReceiver::class.java).apply {
      action = "com.bizclimb.nextdoorpush.app.ACTION_CLICK"
      data = Uri.parse("ndp://action/$baseId?verb=close") // uniqueness key
      putExtra("url", closeUrl)
      putExtra("matched_id", matchedId)
      putExtra("verb", "close")
      putExtra("notif_id", baseId)
    }

    val piApprove = PendingIntent.getBroadcast(
      this,
      baseId xor 0xA5A5,               // distinct requestCode per action
      approveIntent,
      PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val piClose = PendingIntent.getBroadcast(
      this,
      baseId xor 0x5A5A,
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
      // keep order: Post then Close
      .apply {
        if (approveUrl.isNotBlank()) addAction(0, "Post",  piApprove)
        if (closeUrl.isNotBlank())   addAction(0, "Close", piClose)
      }

    Notif.notify(this, baseId, builder)
  }

  private fun safeId(key: String): Int {
    // Avoid rare Int hash collisions by forcing positive and mixing
    val h = key.hashCode()
    return abs(h xor (h shl 13) xor (h ushr 7))
  }
}
