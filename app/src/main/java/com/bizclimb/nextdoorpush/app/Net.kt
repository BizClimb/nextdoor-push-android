package com.bizclimb.nextdoorpush.app

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import java.util.concurrent.TimeUnit

object Net {
  val http = OkHttpClient.Builder()
    .callTimeout(20, TimeUnit.SECONDS)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

  fun get(url: String): Boolean {
    val req = Request.Builder().url(url).get().build()
    http.newCall(req).execute().use { resp -> return resp.isSuccessful }
  }

  // old per account registration (still available for manual tests)
  fun registerToken(accountId: String, token: String): Boolean {
    val form = FormBody.Builder()
      .add("account_id", accountId)
      .add("fcm_token", token)
      .build()
    val req = Request.Builder()
      .url(Const.REGISTER_URL)
      .post(form)
      .build()
    http.newCall(req).execute().use { resp -> return resp.isSuccessful }
  }

  // new global registration used by PushService.onNewToken
  fun registerTokenAllAccounts(deviceId: String, token: String): Boolean {
    val form = FormBody.Builder()
      .add("device_id", deviceId)
      .add("fcm_token", token)
      .build()
    val req = Request.Builder()
      .url(Const.REGISTER_URL)
      .post(form)
      .build()
    http.newCall(req).execute().use { resp -> return resp.isSuccessful }
  }
}
