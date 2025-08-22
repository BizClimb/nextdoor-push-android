package com.bizclimb.nextdoorpush.app

import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Worker
import android.content.Context

class HttpWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

  override suspend fun doWork(): Result {
    val url = inputData.getString("url") ?: return Result.failure()
    val matchedId = inputData.getString("matched_id") ?: "0"
    val verb = inputData.getString("verb") ?: ""

    val ok = try {
      Net.get(url).also {
        Log.d("NDPush", "HttpWorker verb=$verb matched=$matchedId ok=$it url=${url.take(120)}")
      }
    } catch (t: Throwable) {
      Log.e("NDPush", "HttpWorker error verb=$verb matched=$matchedId msg=${t.message}")
      false
    }

    val text = if (ok) "Action $verb sent" else "Action failed"

    val notif = NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_upload_done)
      .setContentTitle("Nextdoor push")
      .setContentText(text)
      .setAutoCancel(true)
      .build()

    NotificationManagerCompat.from(applicationContext)
      .notify(matchedId.hashCode() + 1000, notif)

    return if (ok) Result.success() else Result.retry()
  }
}
