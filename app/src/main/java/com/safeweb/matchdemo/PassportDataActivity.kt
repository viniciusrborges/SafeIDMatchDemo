package com.safeweb.matchdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safeweb.matchdemo.ui.theme.SafeIDMatchDemoTheme

class PassportDataActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = Bundle().apply { putAll(intent.extras) }

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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
