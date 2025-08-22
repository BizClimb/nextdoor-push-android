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
  override fun onReceive(context: Context, intent: Intent) {
    val url       = intent.getStringExtra("url").orEmpty()
    val matchedId = intent.getStringExtra("matched_id").orEmpty()
    val verb      = intent.getStringExtra("verb").orEmpty()
    val notifId   = intent.getIntExtra("notif_id", 0)

    // Close the visible notification right away
    if (notifId != 0) NotificationManagerCompat.from(context).cancel(notifId)

    if (url.isBlank()) {
      Log.w("NDPush", "ActionReceiver missing url for matched_id=$matchedId verb=$verb")
      return
    }

    val data = workDataOf(
      "url" to url,
      "matched_id" to matchedId,
      "verb" to verb
    )
    val req = OneTimeWorkRequestBuilder<HttpWorker>()
      .setInputData(data)
      .build()
    WorkManager.getInstance(context).enqueue(req)
  }
}
