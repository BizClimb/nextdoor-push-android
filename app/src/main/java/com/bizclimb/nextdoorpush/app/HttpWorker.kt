package com.bizclimb.nextdoorpush.app

import android.content.Context
import androidx.core.app.NotificationCompat
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
    val notifId = inputData.getInt("notif_id", matchedId.hashCode())

    return try {
      val req = Request.Builder().url(url).build()
      client.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) {
          // nothing to show on success
          return Result.success()
        }

        val code = resp.code
        val body = resp.body?.string()?.take(500) ?: "<empty>"

        // do not retry for tokens already handled or conflict
        if (code == 410 || code == 409) {
          notifyFailure("HTTP $code", verb, matchedId, body, noRetry = true)
          return Result.success()
        }

        notifyFailure("HTTP $code", verb, matchedId, body, noRetry = false)
        return Result.retry()
      }
    } catch (e: Exception) {
      notifyFailure("Exception ${e.javaClass.simpleName}", verb, matchedId, e.message ?: "", noRetry = false)
      return Result.retry()
    }
  }

  private fun notifyFailure(reason: String, verb: String, matchedId: String, detail: String, noRetry: Boolean) {
    val builder = NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentTitle("Action failed: $verb")
      .setContentText(reason)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$reason\n$detail"))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)

    // use a separate id so it does not resurrect the original notification
    Notif.notify(applicationContext, matchedId.hashCode() + 1000, builder)
  }
}
