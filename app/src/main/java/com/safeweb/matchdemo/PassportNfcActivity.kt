package com.safeweb.matchdemo

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.scuba.smartcards.IsoDepCardService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jmrtd.BACKey
import org.jmrtd.PassportService
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
class PassportNfcActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null

    // UI state
    private var docNumber by mutableStateOf("")
    private var dob by mutableStateOf("")   // YYMMDD
    private var doe by mutableStateOf("")   // YYMMDD
    private var status by mutableStateOf("Informe os dados e aproxime o passaporte na antena NFC.")

    // >>> Ajuste esta URL para a sua rota real (case-insensitive no ASP.NET Core).
    // Pelo seu controller: [Route("api/[controller]")] + "decode-base64"  =>  .../api/JP2Decoder/decode-base64
    private val JP2_DECODER_URL = "https://api.ovvo.com.br/safeid/api/JP2Decoder/decode-base64"

    // OkHttp reutilizável
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Read Passport NFC") }) }) { inner ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = docNumber, onValueChange = { docNumber = it.uppercase() },
                        label = { Text("Passport Number") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dob, onValueChange = { dob = it.filter(Char::isDigit) },
                        label = { Text("Date of Birth (YYMMDD)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = doe, onValueChange = { doe = it.filter(Char::isDigit) },
                        label = { Text("Date of Expiry (YYMMDD)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = status, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Encoste a página da foto (onde fica o chip) na parte de trás do telefone.",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val a = nfcAdapter ?: run { status = "NFC não disponível neste dispositivo."; return }
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        a.enableReaderMode(this, this, flags, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        if (docNumber.isBlank() || dob.length != 6 || doe.length != 6) {
            runOnUiThread {
                Toast.makeText(this, "Preencha número, DOB e DOE (YYMMDD).", Toast.LENGTH_LONG).show()
            }
            return
        }

        // Fazemos tudo em background (coroutine IO)
        lifecycleScope.launchWhenResumed {
            withContext(Dispatchers.Main) { status = "Conectando..." }
            try {
                val isoDep = IsoDep.get(tag) ?: throw IllegalStateException("Tag não é IsoDep")
                isoDep.connect()

                val read = withContext(Dispatchers.IO) {
                    readPassportIsoDep(isoDep, docNumber.trim(), dob.trim(), doe.trim())
                }

                // Se não veio JPEG mas veio JP2 → pedir à API para converter (retorna PNG/JPEG)
                val photoBytes: ByteArray? = when {
                    read.photoJpeg != null -> read.photoJpeg
                    read.photoJp2 != null -> {
                        withContext(Dispatchers.Main) { status = "Convertendo foto (JP2 → PNG)..." }
                        decodeJp2Remote(read.photoJp2)
                    }
                    else -> null
                }

                withContext(Dispatchers.Main) {
                    val it = Intent(this@PassportNfcActivity, PassportDataActivity::class.java).apply {
                        putExtra("surname", read.info.surname)
                        putExtra("givenNames", read.info.givenNames)
                        putExtra("docNumber", read.info.docNumber)
                        putExtra("nationality", read.info.nationality)
                        putExtra("dob", read.info.dateOfBirth)
                        putExtra("doe", read.info.dateOfExpiry)
                        putExtra("sex", read.info.sex)
                        putExtra("issuingState", read.info.issuingState)
                        if (photoBytes != null) putExtra("photoJpeg", photoBytes)
                    }
                    startActivity(it)
                    status = "Leitura concluída."
                }

                isoDep.close()
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    status = "Erro: ${t.message ?: t.javaClass.simpleName}"
                    Toast.makeText(this@PassportNfcActivity, status, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- leitura DG1 + DG2 via JMRTD ---
    private fun readPassportIsoDep(
        isoDep: IsoDep,
        documentNumber: String,
        dateOfBirthYYMMDD: String,
        dateOfExpiryYYMMDD: String
    ): PassportReadResult {
        val cardService = IsoDepCardService(isoDep)
        val service = PassportService(
            cardService,
            PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
            PassportService.DEFAULT_MAX_BLOCKSIZE,
            false, false
        )

        service.open()
        service.sendSelectApplet(false)

        // BAC (derivado da MRZ)
        val bacKey = BACKey(documentNumber, dateOfBirthYYMMDD, dateOfExpiryYYMMDD)
        service.doBAC(bacKey)

        // DG1 (MRZ textual)
        val dg1In: InputStream = service.getInputStream(PassportService.EF_DG1)
        val dg1 = DG1File(dg1In)
        val mrz = dg1.mrzInfo

        val info = PassportInfo(
            surname = mrz.primaryIdentifier.replace("<", " ").trim(),
            givenNames = mrz.secondaryIdentifier.replace("<", " ").trim(),
            docNumber = mrz.documentNumber,
            nationality = mrz.nationality,
            dateOfBirth = mrz.dateOfBirth,     // YYMMDD
            dateOfExpiry = mrz.dateOfExpiry,   // YYMMDD
            sex = mrz.gender.toString(),
            issuingState = mrz.issuingState
        )

        // DG2 (foto do rosto): preferimos JPEG; se vier JP2 guardamos para a API
        var jpegPhotoBytes: ByteArray? = null
        var jp2PhotoBytes: ByteArray? = null

        try {
            val dg2In: InputStream = service.getInputStream(PassportService.EF_DG2)
            val dg2 = DG2File(dg2In)
            outer@ for (fi in dg2.faceInfos) {
                for (img in fi.faceImageInfos) {
                    val mime = (img.mimeType ?: "").lowercase()
                    when {
                        mime.contains("jpeg") || mime.contains("jpg") -> {
                            jpegPhotoBytes = img.imageInputStream.readAllBytesCompat()
                            break@outer
                        }
                        mime.contains("jp2") || mime.contains("jpeg2000") || mime.contains("j2k") -> {
                            jp2PhotoBytes = img.imageInputStream.readAllBytesCompat()
                            // continua o loop para ver se existe também uma cópia em JPEG;
                            // se não houver, usaremos o JP2 com a API
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // DG2 ausente ou erro de leitura
        }

        return PassportReadResult(info = info, photoJpeg = jpegPhotoBytes, photoJp2 = jp2PhotoBytes)
    }

    // --- Chamada à sua API para converter JP2 em PNG/JPEG ---
    private suspend fun decodeJp2Remote(jp2Bytes: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        // Corpo JSON do seu endpoint DecodeBase64: { "Jp2Base64": "...", "OutputFormat": "png" }
        val json = """
            {
              "Jp2Base64": "${Base64.encodeToString(jp2Bytes, Base64.NO_WRAP)}",
              "OutputFormat": "png"
            }
        """.trimIndent()

        val body: RequestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(JP2_DECODER_URL)
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            return@use resp.body?.bytes()
        }
    }

    private fun InputStream.readAllBytesCompat(): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val r = this.read(buf)
            if (r <= 0) break
            out.write(buf, 0, r)
        }
        return out.toByteArray()
    }
}

// DTOs
data class PassportInfo(
    val surname: String,
    val givenNames: String,
    val docNumber: String,
    val nationality: String,
    val dateOfBirth: String,
    val dateOfExpiry: String,
    val sex: String,
    val issuingState: String
)

data class PassportReadResult(
    val info: PassportInfo,
    val photoJpeg: ByteArray?,
    val photoJp2: ByteArray?
)
