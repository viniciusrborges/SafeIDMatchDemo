package com.safeweb.matchdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facetec.sdk.FaceTecSDK
import com.safeweb.matchdemo.ui.theme.SafeIDMatchDemoTheme

class MainActivity : ComponentActivity() {
    private var sdkInstance: FaceTecSDK? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (opcional) customização visual
        FaceTecSDK.setCustomization(Config.retrieveConfigurationWizardCustomization())

        // === 1) INICIALIZAÇÃO (escolha a variante que aparecer no autocomplete) ===
        // A)
        // FaceTecSDK.initializeInDevelopmentMode(this, Config.DeviceKeyIdentifier) { status -> ... }
        // B)
        // FaceTecSDK.initializeInProductionMode(this, Config.DeviceKeyIdentifier, Config.ProductionKeyText) { status -> ... }
        // C)
        // FaceTecSDK.initialize(this, Config.DeviceKeyIdentifier, Config.ProductionKeyText) { status -> ... }

        // Exemplo usando a (A) – troque pelos nomes do seu AAR:
        FaceTecSDK.initializeInDevelopmentMode(this, Config.DeviceKeyIdentifier) { status ->
            // Se sua callback não tem 'isCompletelyInitialized', teste pelo enum/constante que aparecer.
            // Ex.: if (status.isSuccessful) { ... } ou if (status == FaceTecSDK.Status.OK) { ... }
            // Use o autocomplete no 'status.' para ver o que existe.

            // === 2) PEGAR A INSTÂNCIA ===
            // Troque 'create' pelo método que seu AAR expõe (create/newInstance/constructor).
            sdkInstance = FaceTecSDK.create(this@MainActivity)
        }

        setContent {
            SafeIDMatchDemoTheme {
                Scaffold(Modifier.fillMaxSize()) { inner ->
                    Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                        Button(onClick = {
                            if (!ensureCameraPermission()) return@Button
                            val sdk = sdkInstance ?: run {
                                Toast.makeText(this@MainActivity, "SDK ainda inicializando…", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // === 3) MATCH FACE TO ID ===
                            sdk.start3DLivenessThen3D2DPhotoIDMatch(
                                this@MainActivity,
                                SessionRequestProcessor()
                            )
                        }) { Text("Match Face to ID") }
                    }
                }
            }
        }
    }

    private fun ensureCameraPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!granted) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
        return granted
    }
}
