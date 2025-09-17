package com.safeweb.matchdemo

import android.Manifest
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safeweb.matchdemo.ui.theme.SafeIDMatchDemoTheme

// >>> imports que estavam faltando <<<
import com.facetec.sdk.FaceTecSDK
import com.safeweb.matchdemo.SessionRequestProcessor
import com.safeweb.matchdemo.Config

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureCameraPermission()

        // (Opcional mas recomendado) aplica a customização vinda do Config
        FaceTecSDK.setCustomization(Config.retrieveConfigurationWizardCustomization())

        setContent {
            SafeIDMatchDemoTheme {
                MatchScreen(
                    onMatchClick = {
                        // Feedback rápido
                        Toast.makeText(this, "Match Face to ID (clicado)", Toast.LENGTH_SHORT).show()

                        // Dispara o fluxo Match Face to ID (UI do FaceTec + networking via SessionRequestProcessor)
                        FaceTecSDK.getInstance().start3DLivenessThen3D2DPhotoIDMatch(
                            this@MainActivity,
                            SessionRequestProcessor()
                        )
                    }
                )
            }
        }
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }
}

@Composable
fun MatchScreen(onMatchClick: () -> Unit) {
    Scaffold { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onMatchClick) {
                Text("Match Face to ID")
            }
        }
    }
}
