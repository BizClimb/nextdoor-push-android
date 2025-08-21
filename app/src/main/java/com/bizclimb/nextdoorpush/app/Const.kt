package com.bizclimb.nextdoorpush.app

object Const {
  // Your server endpoints
  // Replace if you host them elsewhere
  const val REGISTER_URL =
    "https://bb.bizclimb.com/nextdoor_hood_follower/notifications_register_device.php"

  // Keys used inside FCM data payload
  const val KEY_TEXT = "text_content"
  const val KEY_POST_LINK = "post_link"
  const val KEY_APPROVE_URL = "approve_url"
  const val KEY_CLOSE_URL = "close_url"
  const val KEY_TOKEN = "action_token"
  const val KEY_MATCHED_ID = "matched_id"
  const val KEY_ACCOUNT_ID = "account_id"

  const val CHANNEL_ID = "nextdoor_push_channel"
}
