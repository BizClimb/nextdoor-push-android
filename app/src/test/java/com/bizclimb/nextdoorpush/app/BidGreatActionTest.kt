package com.bizclimb.nextdoorpush.app

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class BidGreatActionTest {
  private val token = "a".repeat(43)
  private val base = "https://bb.bizclimb.com/nextdoor/project-approval.php"
  private val approve = "$base?id=65&v=1&t=$token&intent=approve&tap=1"

  @Test fun approveUsesCookieFreePostWithScopedFields() {
    val req = requireNotNull(BidGreatAction.request(approve.toHttpUrl(), "65", "post"))
    assertEquals("POST", req.method)
    assertEquals("https://bb.bizclimb.com/nextdoor/project-approval-mobile.php", req.url.toString())
    assertEquals("1", req.header("X-Nextdoor-Notification"))
    assertNull(req.header("Cookie"))
    val body = req.body as FormBody
    assertEquals(mapOf("id" to "65", "revision" to "1", "token" to token, "action" to "approve"),
      (0 until body.size).associate { body.name(it) to body.value(it) })
  }

  @Test fun closeRespectsItsOwnAction() {
    val close = approve.replace("intent=approve", "intent=close")
    val req = requireNotNull(BidGreatAction.request(close.toHttpUrl(), "65", "close"))
    assertEquals("close", (req.body as FormBody).value(3))
    assertNull(BidGreatAction.request(close.toHttpUrl(), "65", "post"))
  }

  @Test fun requiresValidSignedVersionAndCorrectHostIdAndIntent() {
    for (link in listOf(
      approve.replace("https:", "http:"), approve.replace("bb.bizclimb.com", "example.org"),
      approve.replace("bb.bizclimb.com", "bb.bizclimb.com:444"), approve.replace("v=1", "v=0"),
      approve.replace(token, "not-signed"), approve.replace("intent=approve", "intent=edit"),
      "$approve&v=2"
    )) assertNull(BidGreatAction.request(link.toHttpUrl(), "65", "post"))
    assertNull(BidGreatAction.request(approve.toHttpUrl(), "66", "post"))
  }

  @Test fun unrelatedSiSLinkDoesNotUseBidGreatRoute() {
    assertFalse(BidGreatAction.isApprovalLink("https://bb.bizclimb.com/sis_ppb/approval.php?id=11".toHttpUrl()))
  }
}
