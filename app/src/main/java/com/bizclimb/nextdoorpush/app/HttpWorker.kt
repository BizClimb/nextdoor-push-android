package com.bizclimb.nextdoorpush.app

import android.content.Context
import android.util.Log
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
      val finalUrl = if (url.contains("?")) "$url&src=android" else "$url?src=android"
      val req = Request.Builder().url(finalUrl).get().build()
      client.newCall(req).execute().use { resp ->
        val code = resp.code
        val ok = resp.isSuccessful
        Log.d("NDPush", "HttpWorker verb=$verb mid=$matchedId code=$code ok=$ok url=${url.take(120)}")

        if (ok) return Result.success()

        val body = resp.body?.string()?.take(300) ?: "<empty>"
        notifyFailure(matchedId, verb, "HTTP $code", body)
        // retry only for 5xx
        if (code in 500..599) Result.retry() else Result.failure()
      }
    } catch (io: IOException) {
      notifyFailure(matchedId, verb, "Network error", io.message ?: "")
      Result.retry()
    } catch (t: Throwable) {
      notifyFailure(matchedId, verb, "Exception", t.message ?: "")
      Result.failure()
    }
  }

  private fun notifyFailure(matchedId: String, verb: String, reason: String, detail: String) {
    val builder = NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_notify_error)
      .setContentTitle("Action failed: $verb")
      .setContentText(reason)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$reason\n$detail"))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)

    Notif.notify(applicationContext, matchedId.hashCode() + 1000, builder)
  }
}
