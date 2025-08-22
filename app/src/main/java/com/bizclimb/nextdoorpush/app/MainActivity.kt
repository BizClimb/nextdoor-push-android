package com.bizclimb.nextdoorpush.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme { AppScreen() }
    }
  }
}

@Composable
private fun AppScreen() {
  val scope = rememberCoroutineScope()
  var token by remember { mutableStateOf("tap to fetch FCM token") }
  var deviceId by remember { mutableStateOf("") }

  Surface {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("Nextdoor Push Helper", style = MaterialTheme.typography.titleLarge)

      OutlinedTextField(
        value = deviceId,
        onValueChange = { deviceId = it },
        label = { Text("Device ID") },
        modifier = Modifier.padding(top = 12.dp)
      )

      Button(
        onClick = {
          FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
            token = t
            val did = if (deviceId.isNotBlank()) deviceId
                      else Settings.Secure.getString(
                        androidx.compose.ui.platform.LocalContext.current.contentResolver,
                        Settings.Secure.ANDROID_ID
                      ) ?: java.util.UUID.randomUUID().toString()
            scope.launch(Dispatchers.IO) {
              Net.registerTokenAllAccounts(did, t)
            }
          }
        },
        modifier = Modifier.padding(top = 12.dp)
      ) { Text("Fetch and Register Token") }

      Text(token, modifier = Modifier.padding(top = 12.dp))
    }
  }
}
