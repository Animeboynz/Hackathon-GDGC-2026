package com.animeboynz.kmd.ui.home.tabs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.util.Base64
import android.util.Base64.NO_WRAP
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.animeboynz.kmd.R
import com.animeboynz.kmd.preferences.GeneralPreferences
import com.animeboynz.kmd.presentation.util.Tab
import com.animeboynz.kmd.ui.onboarding.PassportKycColors
import com.animeboynz.kmd.ui.onboarding.PassportKycTheme
import com.animeboynz.kmd.ui.preferences.PreferencesScreen
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.QRCodeWriter
import org.koin.compose.koinInject
import java.io.ByteArrayOutputStream

object ToolsTab : Tab {
    private fun readResolve(): Any = ToolsTab

    override val options: TabOptions
        @Composable
        get() {
            val image = rememberVectorPainter(Icons.Filled.Construction)
            return TabOptions(
                index = 0u,
                title = stringResource(R.string.tools_tab),
                icon = image,
            )
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val preferences = koinInject<GeneralPreferences>()

        PassportKycTheme {
            Scaffold(
                containerColor = PassportKycColors.page,
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.tools_tab)) },
                        actions = {
                            IconButton(onClick = { navigator.push(PreferencesScreen) }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        },
                    )
                },
            ) { paddingValues ->
                VerificationPresenter(
                    preferences = preferences,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PassportKycColors.page)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

private enum class VerificationMode {
    QR,
    NFC,
}

@Composable
private fun VerificationPresenter(
    preferences: GeneralPreferences,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(VerificationMode.QR) }
    val holderName = preferences.digitalIdHolderName.get()
    val credentialId = preferences.digitalIdCredentialId.get()
    val documentNumber = preferences.digitalIdDocumentNumber.get()
    val dateOfBirth = preferences.digitalIdDateOfBirth.get()
    val portraitBase64 = preferences.digitalIdPortraitBase64.get()
    val portraitBitmap = remember(portraitBase64) { portraitBase64.decodeBitmapOrNull() }
    val qrPhotoBase64 = remember(portraitBitmap) { portraitBitmap.toCompressedQrPhotoBase64() }
    val method = if (mode == VerificationMode.QR) "QR Verification" else "NFC Verification"
    val qrPayload = remember(holderName, credentialId, documentNumber, dateOfBirth, qrPhotoBase64) {
        buildQrPayload(
            holderName = holderName,
            credentialId = credentialId,
            documentNumber = documentNumber,
            dateOfBirth = dateOfBirth,
            qrPhotoBase64 = qrPhotoBase64,
        )
    }

    Column(modifier = modifier.padding(top = 16.dp, bottom = 24.dp)) {
        Text(
            text = "Present Digital ID",
            color = PassportKycColors.text,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
        Text(
            text = "Choose how someone should verify this locally issued VerID credential.",
            modifier = Modifier.padding(top = 6.dp),
            color = PassportKycColors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(2.dp, PassportKycColors.border, RoundedCornerShape(18.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ModeButton("QR", mode == VerificationMode.QR, Modifier.weight(1f)) { mode = VerificationMode.QR }
            ModeButton("NFC", mode == VerificationMode.NFC, Modifier.weight(1f)) { mode = VerificationMode.NFC }
        }

        if (mode == VerificationMode.QR) {
            QrVerificationCard(
                holderName = holderName,
                credentialId = credentialId,
                documentNumber = documentNumber,
                dateOfBirth = dateOfBirth,
                qrPhotoBase64 = qrPhotoBase64,
                portraitBitmap = portraitBitmap,
                payload = qrPayload,
                method = method,
            )
        } else {
            NfcVerificationCard(
                holderName = holderName,
                credentialId = credentialId,
                portraitBitmap = portraitBitmap,
                method = method,
            )
        }
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, if (selected) PassportKycColors.primaryBorder else Color.Transparent),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) PassportKycColors.primary else Color.Transparent,
            contentColor = if (selected) Color.White else PassportKycColors.muted,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QrVerificationCard(
    holderName: String,
    credentialId: String,
    documentNumber: String,
    dateOfBirth: String,
    qrPhotoBase64: String,
    portraitBitmap: Bitmap?,
    payload: String,
    method: String,
) {
    val qrBitmap = remember(payload) { payload.toQrBitmap(size = 900) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, PassportKycColors.border),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                PassportPhotoThumb(bitmap = portraitBitmap, sizeDp = 72)
                Column(modifier = Modifier.weight(1f)) {
                    Text("QR Verification", color = PassportKycColors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(holderName, color = PassportKycColors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(credentialId, color = PassportKycColors.muted, fontSize = 13.sp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(PassportKycColors.bannerBg)
                    .border(2.dp, PassportKycColors.bannerBorder, RoundedCornerShape(22.dp))
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Digital ID QR code",
                    modifier = Modifier
                        .size(320.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(10.dp),
                )
            }

            InfoRow("Name", holderName)
            InfoRow("Verification method", method)
            InfoRow("Document", documentNumber)
            InfoRow("Date of birth", dateOfBirth)
            InfoRow("Photo encoding", if (qrPhotoBase64.isBlank()) "No portrait available" else "Compressed Base64 JPEG (${qrPhotoBase64.length} chars)")

            Text("Encoded payload", color = PassportKycColors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = payload,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PassportKycColors.page)
                    .padding(12.dp),
                color = PassportKycColors.text,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NfcVerificationCard(
    holderName: String,
    credentialId: String,
    portraitBitmap: Bitmap?,
    method: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PassportPhotoThumb(bitmap = portraitBitmap, sizeDp = 86)
            Text("Tap to verify", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Hold a verifier device near this phone to receive this credential over NFC.",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            NfcRings()
            InfoPill("Name", holderName)
            InfoPill("Verification method", method)
            InfoPill("Credential ID", credentialId)
        }
    }
}

@Composable
private fun NfcRings() {
    Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(42.dp).clip(CircleShape).border(2.dp, PassportKycColors.lavender, CircleShape))
        Box(Modifier.size(78.dp).clip(CircleShape).border(2.dp, PassportKycColors.lavender.copy(alpha = 0.55f), CircleShape))
        Box(Modifier.size(112.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape))
        Box(Modifier.size(18.dp).clip(CircleShape).background(PassportKycColors.primary))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PassportKycColors.muted, fontSize = 13.sp)
        Text(value, color = PassportKycColors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PassportPhotoThumb(bitmap: Bitmap?, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(3.dp, PassportKycColors.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("👤", fontSize = (sizeDp / 2).sp)
        }
    }
}

private fun buildQrPayload(
    holderName: String,
    credentialId: String,
    documentNumber: String,
    dateOfBirth: String,
    qrPhotoBase64: String,
): String {
    return """
        {"t":"verid","v":1,"n":"${holderName.jsonEscape()}","cid":"${credentialId.jsonEscape()}","doc":"${documentNumber.jsonEscape()}","dob":"${dateOfBirth.jsonEscape()}","m":"QR","img":"${qrPhotoBase64.jsonEscape()}"}
    """.trimIndent()
}

private fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")

private fun String.decodeBitmapOrNull(): Bitmap? {
    if (isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(this, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun String.toQrBitmap(size: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = try {
        QRCodeWriter().encode(this, BarcodeFormat.QR_CODE, size, size, hints)
    } catch (e: WriterException) {
        QRCodeWriter().encode("""{"type":"verid.digital_id","v":1,"error":"payload-too-large"}""", BarcodeFormat.QR_CODE, size, size, hints)
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
    }
}

private fun Bitmap?.toCompressedQrPhotoBase64(maxChars: Int = 1_250): String {
    if (this == null) return ""

    val source = centerCropSquare()
    var best = ""
    val sides = listOf(40, 36, 32, 28, 24, 20)
    val qualities = listOf(42, 35, 28, 22, 16, 10)

    for (side in sides) {
        val scaled = Bitmap.createScaledBitmap(source, side, side, true)
        for (quality in qualities) {
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            val encoded = Base64.encodeToString(output.toByteArray(), NO_WRAP)
            best = encoded
            if (encoded.length <= maxChars) {
                if (source != this) source.recycle()
                if (scaled != source) scaled.recycle()
                return encoded
            }
        }
        if (scaled != source) scaled.recycle()
    }

    if (source != this) source.recycle()
    return best
}

private fun Bitmap.centerCropSquare(): Bitmap {
    val side = minOf(width, height)
    val left = (width - side) / 2
    val top = (height - side) / 2
    return Bitmap.createBitmap(this, left, top, side, side)
}
