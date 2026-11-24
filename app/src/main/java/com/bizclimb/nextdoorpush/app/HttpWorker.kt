package com.bizclimb.nextdoorpush.app

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.IOException

class HttpWorker(appContext: Context, workerParams: WorkerParameters) :
  Worker(appContext, workerParams) {

  private val client = OkHttpClient()

  override fun doWork(): Result {
    val url = inputData.getString("url") ?: return Result.failure()
    val matchedId = inputData.getString("matched_id") ?: "0"
    val verb = inputData.getString("verb") ?: "action"
    val link = url.toHttpUrlOrNull() ?: return Result.failure()
    if (BidGreatAction.isApprovalLink(link)) {
      val action = BidGreatAction.request(link, matchedId, verb)
      if (action == null) {
        notifyFailure("Invalid BidGreat approval link", verb, matchedId, "Use the latest alert.")
        return Result.failure()
      }
      return submitBidGreat(action, matchedId, verb)
    }

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

          notifyFailure("HTTP ${resp.code}", verb, matchedId, "Open Nextdoor HQ to review.")
          if (terminal) Result.success() else Result.retry()
        } else {
          // Success: no toast
          Result.success()
        }
      }
    } catch (e: IOException) {
      notifyFailure("Network error", verb, matchedId, "Check your connection.")
      // Network issues can be transient: allow retry
      Result.retry()
    } catch (e: Exception) {
      notifyFailure("Action could not be completed", verb, matchedId, "Open Nextdoor HQ to review.")
      // Unexpected logic errors should not loop forever
      Result.failure()
    }
  }

  private fun submitBidGreat(req: Request, matchedId: String, verb: String): Result {
    fun problem(reason: String, detail: String, retry: Boolean = false): Result {
      if (retry && runAttemptCount < 2) return Result.retry()
      notifyFailure(reason, verb, matchedId, detail)
      return Result.failure()
    }
    return try {
      val explicitClient = client.newBuilder().followRedirects(false).followSslRedirects(false).build()
      explicitClient.newCall(req).execute().use { resp ->
        when {
          resp.code in 500..599 -> problem("HQ is temporarily unavailable", "Try the alert again.", true)
          resp.code == 404 || resp.code == 401 -> problem("This approval link is no longer valid", "Use the latest alert.")
          resp.code == 409 || resp.code == 422 -> problem("This item is no longer approvable from this alert", "Review it in HQ.")
          !resp.isSuccessful -> problem("HQ did not accept this action", "Open the approval link to review.")
          else -> {
            val ack = JSONObject(resp.body?.string().orEmpty())
            val decision = if (verb == "post") "approve" else "close"
            if (!ack.optBoolean("ok", false) || ack.optString("decision") != decision) {
              problem("HQ did not confirm the decision", "Check HQ before trying again.", true)
            } else {
              val notifId = inputData.getInt("notif_id", matchedId.hashCode())
              NotificationManagerCompat.from(applicationContext).cancel(notifId)
              Result.success()
            }
          }
        }
      }
    } catch (_: IOException) {
      problem("Network error", "Check HQ before trying the alert again.", true)
    } catch (_: Exception) {
      problem("HQ did not confirm the decision", "Check HQ before trying again.", true)
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
