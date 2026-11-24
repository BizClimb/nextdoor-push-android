package com.bizclimb.nextdoorpush.app

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request

/** Turns a signed BidGreat notification tap into a request that doesn't need cookies. */
object BidGreatAction {
  private const val HOST = "bb.bizclimb.com"
  private const val REVIEW_PATH = "/nextdoor/project-approval.php"
  private const val ACTION_URL = "https://bb.bizclimb.com/nextdoor/project-approval-mobile.php"
  private val number = Regex("[1-9][0-9]{0,9}")
  private val tokenPattern = Regex("[A-Za-z0-9_-]{43}")

  fun isApprovalLink(url: HttpUrl): Boolean = url.encodedPath == REVIEW_PATH

  fun request(link: HttpUrl, matchedId: String, verb: String): Request? {
    if (!isApprovalLink(link) || !link.isHttps || link.host != HOST || link.port != 443 ||
        link.username.isNotEmpty() || link.password.isNotEmpty() || link.fragment != null) return null
    val id = link.queryParameterValues("id").singleOrNull() ?: return null
    val revision = link.queryParameterValues("v").singleOrNull() ?: return null
    val token = link.queryParameterValues("t").singleOrNull() ?: return null
    val action = when (verb) {
      "post" -> "approve"
      "close" -> "close"
      else -> return null
    }
    if (link.queryParameterValues("intent").singleOrNull() != action ||
        id != matchedId || !number.matches(id) || !number.matches(revision) || !tokenPattern.matches(token)) return null
    val body = FormBody.Builder().add("id", id).add("revision", revision).add("token", token).add("action", action).build()
    return Request.Builder().url(ACTION_URL)
      .header("Accept", "application/json").header("X-Nextdoor-Notification", "1")
      .post(body).build()
  }
}
