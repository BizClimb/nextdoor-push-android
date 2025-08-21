package com.bizclimb.nextdoorpush.app

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    // You can call Net.registerToken here if you want auto registration
    // You would need a stored account id for that
  }

  override fun onMessageReceived(msg: RemoteMessage) {
    val data = msg.data
    val text = data[Const.KEY_TEXT] ?: "(no text)"
    val approve = data[Const.KEY_APPROVE_URL] ?: ""
    val close = data[Const.KEY_CLOSE_URL] ?: ""
    val matchedId = data[Const.KEY_MATCHED_ID] ?: System.currentTimeMillis().toString()

    val approveIntent = Intent(this, ActionReceiver::class.java).apply {
      action = "com.bizclimb.nextdoorpush.app.ACTION_CLICK"
      putExtra("url", approve)
      putExtra("matched_id", matchedId)
      putExtra("verb", "POSTED")
    }
    val closeIntent = Intent(this, ActionReceiver::class.java).apply {
      action = "com.bizclimb.nextdoorpush.app.ACTION_CLICK"
      putExtra("url", close)
      putExtra("matched_id", matchedId)
      putExtra("verb", "CLOSED")
    }

    val piApprove = PendingIntent.getBroadcast(
      this, matchedId.hashCode(), approveIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or BuildConfig.PI_IMMUTABLE
    )
    val piClose = PendingIntent.getBroadcast(
      this, matchedId.hashCode() + 1, closeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or BuildConfig.PI_IMMUTABLE
    )

    val notif = NotificationCompat.Builder(this, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_notify_more)
      .setContentTitle("Nextdoor match")
      .setContentText(text)
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .addAction(0, "Post", piApprove)
      .addAction(0, "Close", piClose)
      .setAutoCancel(true)
      .build()

    with(NotificationManagerCompat.from(this)) {
      notify(matchedId.hashCode(), notif)
    }
  }
}
