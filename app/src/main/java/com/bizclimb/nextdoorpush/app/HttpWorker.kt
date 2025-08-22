package com.bizclimb.nextdoorpush.app

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HttpWorker(appContext: Context, workerParams: WorkerParameters) :
  Worker(appContext, workerParams) {

  private val client = OkHttpClient()

  override fun doWork(): Result {
    val url = inputData.getString("url") ?: return Result.failure()
    val matchedId = inputData.getString("matched_id") ?: "0"
    val verb = inputData.getString("verb") ?: "action"

    return try {
      val req = Request.Builder().url(url).build()
      client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) {
          val body = resp.body?.string()?.take(500) ?: "<empty>"
          // Treat already handled states as success to avoid noisy retries
          val lower = body.lowercase()
          val terminal =
            resp.code in 400..499 ||
            "already_handled" in lower ||
            "already handled" in lower

          notifyFailure("HTTP ${resp.code}", verb, matchedId, "$url\n$body")
          if (terminal) Result.success() else Result.retry()
        } else {
          // Success: no toast
          Result.success()
        }
      }
    } catch (e: IOException) {
      notifyFailure("Network error", verb, matchedId, "${e.message}\n$url")
      // Network issues can be transient: allow retry
      Result.retry()
    } catch (e: Exception) {
      notifyFailure("Exception", verb, matchedId, "${e.message}\n$url\n${e.stackTraceToString()}")
      // Unexpected logic errors should not loop forever
      Result.failure()
    }
  }

  private fun notifyFailure(reason: String, verb: String, matchedId: String, detail: String) {
    val builder = NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentTitle("Action failed: $verb")
      .setContentText(reason)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$reason\n$detail"))
      .setPriority(NotificationCompat.PRIORITY_HIGH)

    // Offset id so we don't clash with the main notification id
    Notif.notify(applicationContext, ("fail:$matchedId").hashCode(), builder)
  }
}
