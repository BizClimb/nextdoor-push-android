package com.bizclimb.nextdoorpush.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class ActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val url = intent.getStringExtra("url") ?: return
    val matchedId = intent.getStringExtra("matched_id") ?: return
    val verb = intent.getStringExtra("verb") ?: return
    val notifId = intent.getIntExtra("notif_id", matchedId.hashCode())

    // collapse the notification immediately to prevent double taps
    NotificationManagerCompat.from(context).cancel(notifId)

    // enqueue network work
    val data = workDataOf(
      "url" to url,
      "matched_id" to matchedId,
      "verb" to verb,
      "notif_id" to notifId
    )
    val req = OneTimeWorkRequestBuilder<HttpWorker>()
      .setInputData(data)
      .build()
    WorkManager.getInstance(context).enqueue(req)
  }
}
