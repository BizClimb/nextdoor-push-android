package com.bizclimb.nextdoorpush.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*

class ActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val url = intent.getStringExtra("url") ?: return
    val matchedId = intent.getStringExtra("matched_id") ?: "0"
    val verb = intent.getStringExtra("verb") ?: ""

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
