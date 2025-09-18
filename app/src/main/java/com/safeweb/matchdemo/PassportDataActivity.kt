package com.safeweb.matchdemo

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.safeweb.matchdemo.ui.theme.SafeIDMatchDemoTheme

class PassportDataActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = Bundle().apply { putAll(intent.extras) }
        // Obs: usaremos a mesma chave "photoJpeg" mesmo quando vier PNG da API — o BitmapFactory
        // entende ambos (PNG/JPEG).
        val photoBytes = intent.getByteArrayExtra("photoJpeg")

        setContent {
            SafeIDMatchDemoTheme {
                Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Passport Data") }) }) { inner ->
                    PassportDataContent(
                        surname = data.getString("surname").orEmpty(),
                        givenNames = data.getString("givenNames").orEmpty(),
                        docNumber = data.getString("docNumber").orEmpty(),
                        nationality = data.getString("nationality").orEmpty(),
                        dob = data.getString("dob").orEmpty(),
                        doe = data.getString("doe").orEmpty(),
                        sex = data.getString("sex").orEmpty(),
                        issuingState = data.getString("issuingState").orEmpty(),
                        photoBytes = photoBytes,
                        modifier = Modifier.padding(inner).padding(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PassportDataContent(
    surname: String,
    givenNames: String,
    docNumber: String,
    nationality: String,
    dob: String,
    doe: String,
    sex: String,
    issuingState: String,
    photoBytes: ByteArray? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Foto (pode ser JPEG vindo do chip OU PNG/JPEG vindo da API)
        if (photoBytes != null) {
            val bmp = remember(photoBytes) { BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size) }
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Passport photo",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)             // ocupa só 70% da largura da tela
                        .wrapContentHeight()            // mantém a proporção (sem crop)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Text("Não foi possível decodificar a foto (bytes inválidos).")
            }
        } else {
            Text("Foto do passaporte não disponível.")
        }

        Text("Surname: $surname")
        Text("Given Names: $givenNames")
        Text("Document Number: $docNumber")
        Text("Nationality: $nationality")
        Text("Date of Birth (YYMMDD): $dob")
        Text("Date of Expiry (YYMMDD): $doe")
        Text("Sex: $sex")
        Text("Issuing State: $issuingState")
    }
}
