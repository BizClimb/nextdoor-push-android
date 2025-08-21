package com.bizclimb.nextdoorpush.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        val scope = rememberCoroutineScope()
        var token by remember { mutableStateOf("tap to fetch FCM token") }
        var accountId by remember { mutableStateOf("1") }

        Surface {
          Column {
            Text("Nextdoor Push Helper")
            OutlinedTextField(
              value = accountId,
              onValueChange = { accountId = it },
              label = { Text("Account ID for registration") }
            )
            Button(onClick = {
              FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
                token = t
                scope.launch(Dispatchers.IO) {
                  Net.registerToken(accountId, t)
                }
              }
            }) { Text("Fetch and Register Token") }
            Text(token)
          }
        }
      }
    }
  }
}
