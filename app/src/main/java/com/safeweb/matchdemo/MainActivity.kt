package com.safeweb.matchdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safeweb.matchdemo.ui.theme.SafeIDMatchDemoTheme

class MainActivity : ComponentActivity() {

    // --- FaceTec: usamos a sua função existente (substitua aqui pelo que você já tinha) ---
    private fun startMatchFaceToID() {
        // Se você já tinha a versão com reflexão, chame ela daqui; exemplo:
        // startMatchFaceTecWithReflection()
        Toast.makeText(this, "Match Face to ID disparado", Toast.LENGTH_SHORT).show()
        // TODO: troque pelo seu disparo real:
        // sdkInstance?.start3DLivenessThen3D2DPhotoIDMatch(this, SessionRequestProcessor())
    }

    private fun ensureCameraPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
        return granted
    }
    // ---------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SafeIDMatchDemoTheme {
                Scaffold(Modifier.fillMaxSize()) { inner ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(inner)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                if (!ensureCameraPermission()) return@Button
                                startMatchFaceToID()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Match Face to ID")
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                startActivity(Intent(this@MainActivity, PassportNfcActivity::class.java))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Read Passport NFC")
                        }
                    }
                }
            }
        }
    }
}
