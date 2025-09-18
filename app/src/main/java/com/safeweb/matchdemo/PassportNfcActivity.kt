package com.safeweb.matchdemo

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.BACKey
import org.jmrtd.PassportService
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
class PassportNfcActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null

    // estados da UI
    private var docNumber by mutableStateOf("")
    private var dob by mutableStateOf("")   // YYMMDD
    private var doe by mutableStateOf("")   // YYMMDD
    private var status by mutableStateOf("Informe os dados e aproxime o passaporte na antena NFC.")

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

        try {
            val isoDep = IsoDep.get(tag) ?: throw IllegalStateException("Tag não é IsoDep")
            isoDep.connect()
            status = "Conectado. Autenticando..."

            // leitura (DG1 + DG2)
            val result = readPassportIsoDep(isoDep, docNumber.trim(), dob.trim(), doe.trim())
            val info = result.info
            val photo = result.photoJpeg

            runOnUiThread {
                val it = Intent(this, PassportDataActivity::class.java).apply {
                    putExtra("surname", info.surname)
                    putExtra("givenNames", info.givenNames)
                    putExtra("docNumber", info.docNumber)
                    putExtra("nationality", info.nationality)
                    putExtra("dob", info.dateOfBirth)
                    putExtra("doe", info.dateOfExpiry)
                    putExtra("sex", info.sex)
                    putExtra("issuingState", info.issuingState)
                    if (photo != null) putExtra("photoJpeg", photo)
                }
                startActivity(it)
            }
            isoDep.close()
        } catch (t: Throwable) {
            runOnUiThread {
                status = "Erro: ${t.message ?: t.javaClass.simpleName}"
                Toast.makeText(this, status, Toast.LENGTH_LONG).show()
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

        // DG2 (foto do rosto) – preferimos JPEG; se vier JP2, deixamos nulo por enquanto.
        var jpegPhotoBytes: ByteArray? = null
        try {
            val dg2In: InputStream = service.getInputStream(PassportService.EF_DG2)
            val dg2 = DG2File(dg2In)
            search@ for (fi in dg2.faceInfos) {
                for (img in fi.faceImageInfos) {
                    val mime = (img.mimeType ?: "").lowercase()
                    val isJpeg = mime.contains("jpeg") || mime.contains("jpg")
                    if (isJpeg) {
                        jpegPhotoBytes = img.imageInputStream.readAllBytesCompat()
                        break@search
                    }
                }
            }
        } catch (_: Throwable) {
            // DG2 ausente ou foto em JPEG2000 (não suportado nativamente no Android)
        }

        return PassportReadResult(info = info, photoJpeg = jpegPhotoBytes)
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
    val photoJpeg: ByteArray?
)
