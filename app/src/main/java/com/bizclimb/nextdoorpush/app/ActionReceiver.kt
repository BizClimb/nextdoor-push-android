package com.bizclimb.nextdoorpush.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val url      = intent.getStringExtra("url") ?: return
    val matched  = intent.getStringExtra("matched_id") ?: "0"
    val verb     = intent.getStringExtra("verb") ?: "action"
    val notifId  = intent.getIntExtra("notif_id", 0)

    // BidGreat alerts stay available until HQ acknowledges the decision.
    val signedApproval = url.toHttpUrlOrNull()?.let(BidGreatAction::isApprovalLink) == true
    if (!signedApproval) NotificationManagerCompat.from(context).cancel(notifId)

    val data = workDataOf(
      "url" to url,
      "matched_id" to matched,
      "verb" to verb,
      "notif_id" to notifId
    )
    val req = OneTimeWorkRequestBuilder<HttpWorker>()
      .setInputData(data)
      .build()
    WorkManager.getInstance(context).enqueue(req)
  }
}
