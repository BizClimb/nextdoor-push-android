package com.bizclimb.nextdoorpush.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import android.util.Log

class ActionReceiver : BroadcastReceiver() {
  // ActionReceiver.kt snippet for reference
  override fun onReceive(context: Context, intent: Intent) {
    val url      = intent.getStringExtra("url") ?: return
    val matched  = intent.getStringExtra("matched_id") ?: "0"
    val verb     = intent.getStringExtra("verb") ?: "action"
    val notifId  = intent.getIntExtra("notif_id", 0)

    // Close the notification immediately on tap
    NotificationManagerCompat.from(context).cancel(notifId)

    val data = workDataOf(
      "url" to url,
      "matched_id" to matched,
      "verb" to verb
    )
    val req = OneTimeWorkRequestBuilder<HttpWorker>()
      .setInputData(data)
      .build()
    WorkManager.getInstance(context).enqueue(req)
  }
}
