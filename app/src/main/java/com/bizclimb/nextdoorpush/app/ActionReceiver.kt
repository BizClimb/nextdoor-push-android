package com.bizclimb.nextdoorpush.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class ActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val url = intent.getStringExtra("url") ?: ""
    val matchedId = intent.getStringExtra("matched_id") ?: "0"
    val verb = intent.getStringExtra("verb") ?: ""
    val notifId = intent.getIntExtra("notif_id", matchedId.hashCode())

    Log.d("NDPush", "ActionReceiver got tap verb=$verb matched=$matchedId urlLen=${url.length}")

    // cancel the banner right away so the user sees feedback
    NotificationManagerCompat.from(context).cancel(notifId)

    if (url.isBlank()) {
      Toast.makeText(context, "No action url", Toast.LENGTH_SHORT).show()
      return
    }

    val data = workDataOf(
      "url" to url,
      "matched_id" to matchedId,
      "verb" to verb
    )
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val req = OneTimeWorkRequestBuilder<HttpWorker>()
      .setInputData(data)
      .setConstraints(constraints)
      .addTag("action_${matchedId}_${verb.lowercase()}")
      .build()

    WorkManager.getInstance(context).enqueue(req)
    Toast.makeText(context, "Sending $verb", Toast.LENGTH_SHORT).show()
  }
}
