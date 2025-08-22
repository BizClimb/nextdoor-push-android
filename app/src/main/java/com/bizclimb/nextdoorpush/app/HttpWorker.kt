package com.bizclimb.nextdoorpush.app

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpWorker(appContext: Context, workerParams: WorkerParameters) :
  Worker(appContext, workerParams) {

  private val client = OkHttpClient()

  override fun doWork(): Result {
    val url = inputData.getString("url") ?: return Result.failure()
    val matchedId = inputData.getString("matched_id") ?: "0"
    val verb = inputData.getString("verb") ?: "action"

    return try {
      val req = Request.Builder().url(url).build()
      val resp = client.newCall(req).execute()

      if (!resp.isSuccessful) {
        val body = resp.body?.string()?.take(200) ?: "<empty>"
        notifyFailure("HTTP ${resp.code}", verb, matchedId, body)
        Result.retry()
      } else {
        // no success notification
        Result.success()
      }
    } catch (e: Exception) {
      notifyFailure("Exception: ${e.message}", verb, matchedId, e.stackTraceToString())
      Result.retry()
    }
  }

  private fun notifyFailure(reason: String, verb: String, matchedId: String, detail: String) {
    val notifId = matchedId.hashCode()

    val notif = NotificationCompat.Builder(applicationContext, "push_channel")
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentTitle("Action failed: $verb")
      .setContentText(reason)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$reason\n$detail"))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .build()

    NotificationManagerCompat.from(applicationContext).notify(notifId + 1000, notif)
  }
}
