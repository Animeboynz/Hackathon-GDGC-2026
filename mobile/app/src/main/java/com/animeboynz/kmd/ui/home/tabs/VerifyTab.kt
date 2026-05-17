package com.animeboynz.kmd.ui.home.tabs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.animeboynz.kmd.R
import com.animeboynz.kmd.crypto.VerIdCredentialCrypto
import com.animeboynz.kmd.network.SupabaseUsersApi
import com.animeboynz.kmd.nfc.NfcTagDispatcher
import com.animeboynz.kmd.nfc.VerIdNfcCredentialReader
import com.animeboynz.kmd.preferences.GeneralPreferences
import com.animeboynz.kmd.presentation.util.Tab
import com.animeboynz.kmd.ui.onboarding.PassportKycColors
import com.animeboynz.kmd.ui.onboarding.PassportKycTheme
import com.animeboynz.kmd.ui.preferences.PreferencesScreen
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.koin.compose.koinInject

object VerifyTab : Tab {
    private fun readResolve(): Any = VerifyTab

    override val options: TabOptions
        @Composable
        get() {
            val image = rememberVectorPainter(Icons.Filled.QrCodeScanner)
            return TabOptions(
                index = 0u,
                title = "Verify",
                icon = image,
            )
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        PassportKycTheme {
            Scaffold(
                containerColor = PassportKycColors.page,
                topBar = {
                    TopAppBar(
                        title = { Text("Verify ID") },
                        actions = {
                            IconButton(onClick = { navigator.push(PreferencesScreen) }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        },
                    )
                },
            ) { paddingValues ->
                VerifyQrScreen(
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

private data class ScannedCredential(
    val name: String,
    val credentialId: String,
    val documentNumber: String,
    val dateOfBirth: String,
    val method: String,
    val imageBase64: String,
    val publicKeyBase64: String,
    val signedReliability: Long,
    val timePeriod: Long,
    val isExpired: Boolean,
    val rawPayload: String,
)

@Composable
private fun VerifyQrScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val okHttpClient = koinInject<OkHttpClient>()
    val preferences = koinInject<GeneralPreferences>()
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var scannedCredential by remember { mutableStateOf<ScannedCredential?>(null) }
    var reliabilityInfo by remember { mutableStateOf<SupabaseUsersApi.UserReliability?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var reputationMessage by remember { mutableStateOf<String?>(null) }
    var isVoting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentScannedCredential by rememberUpdatedState(scannedCredential)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        NfcTagDispatcher.consumer = { tag ->
            if (currentScannedCredential == null) {
                scope.launch {
                    VerIdNfcCredentialReader.read(tag).fold(
                        onSuccess = { raw ->
                            parseCredential(raw).fold(
                                onSuccess = {
                                    scannedCredential = it
                                    reliabilityInfo = null
                                    reputationMessage = "Received VerID over NFC."
                                    scanError = null
                                },
                                onFailure = { error ->
                                    scanError = error.message ?: "Could not decode NFC VerID"
                                },
                            )
                        },
                        onFailure = { error ->
                            scanError = error.message ?: "Could not read VerID over NFC"
                        },
                    )
                }
            }
        }
        onDispose {
            NfcTagDispatcher.consumer = null
        }
    }

    LaunchedEffect(scannedCredential?.publicKeyBase64) {
        val publicKey = scannedCredential?.publicKeyBase64 ?: return@LaunchedEffect
        reliabilityInfo = null
        reputationMessage = null
        SupabaseUsersApi.getReliabilityForPublicKey(okHttpClient, publicKey).fold(
            onSuccess = { reliabilityInfo = it },
            onFailure = { reputationMessage = it.message ?: "Could not load reliability from Supabase" },
        )
    }

    Column(modifier = modifier.padding(top = 16.dp, bottom = 24.dp)) {
        Text(
            text = "Scan another VerID",
            color = PassportKycColors.text,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
        Text(
            text = "Point your camera at another user's ID QR, or touch phones while they have NFC sharing open.",
            modifier = Modifier.padding(top = 6.dp),
            color = PassportKycColors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )

        if (scannedCredential == null) {
            QrScannerCard(
                hasCameraPermission = hasCameraPermission,
                onQrScanned = { raw ->
                    parseCredential(raw).fold(
                        onSuccess = {
                            scannedCredential = it
                            reliabilityInfo = null
                            reputationMessage = null
                            scanError = null
                        },
                        onFailure = { error ->
                            scanError = error.message ?: "Could not decode this QR"
                        },
                    )
                },
            )
            scanError?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 12.dp),
                    color = Color(0xFFB91C1C),
                    fontSize = 13.sp,
                )
            }
            if (!hasCameraPermission) {
                Text(
                    text = "Camera permission is required to scan ID QR codes.",
                    modifier = Modifier.padding(top = 12.dp),
                    color = PassportKycColors.muted,
                    fontSize = 13.sp,
                )
            }
        } else {
            VerifiedCredentialCard(
                credential = scannedCredential!!,
                reliabilityInfo = reliabilityInfo,
                reputationMessage = reputationMessage,
                isVoting = isVoting,
                onVote = { isReliable ->
                    val targetKey = scannedCredential?.publicKeyBase64
                    val authorKey = preferences.digitalIdPublicKeyBase64.get()
                    when {
                        targetKey == null -> {
                            reputationMessage = "Scan a VerID before rating."
                        }
                        authorKey.isBlank() -> {
                            reputationMessage = "Create your own VerID before rating another user."
                        }
                        authorKey == targetKey -> {
                            reputationMessage = "You cannot rate your own ID."
                        }
                        else -> {
                            isVoting = true
                            SupabaseUsersApi.submitReputationVote(
                                client = okHttpClient,
                                authorPublicKey = authorKey,
                                targetPublicKey = targetKey,
                                isReliable = isReliable,
                            ).fold(
                                onSuccess = {
                                    reliabilityInfo = it
                                    reputationMessage = if (isReliable) "Marked as reliable." else "Marked as unreliable."
                                },
                                onFailure = { reputationMessage = it.message ?: "Could not submit vote" },
                            )
                            isVoting = false
                        }
                    }
                },
                onScanAgain = {
                    scannedCredential = null
                    reliabilityInfo = null
                    reputationMessage = null
                    scanError = null
                },
            )
        }
    }
}

@Composable
private fun QrScannerCard(
    hasCameraPermission: Boolean,
    onQrScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, PassportKycColors.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center,
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { viewContext ->
                        PreviewView(viewContext).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            bindQrScannerCamera(
                                context = context,
                                lifecycleOwner = lifecycleOwner,
                                previewView = this,
                                onQrScanned = onQrScanned,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("Camera required", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .border(3.dp, PassportKycColors.lavender, RoundedCornerShape(24.dp)),
            )
        }
    }
}

@Composable
private fun VerifiedCredentialCard(
    credential: ScannedCredential,
    reliabilityInfo: SupabaseUsersApi.UserReliability?,
    reputationMessage: String?,
    isVoting: Boolean,
    onVote: suspend (Boolean) -> Unit,
    onScanAgain: () -> Unit,
) {
    val portraitBitmap = remember(credential.imageBase64) { credential.imageBase64.decodeBitmapOrNull() }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                PassportPhotoThumb(bitmap = portraitBitmap, sizeDp = 82)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (credential.isExpired) "Expired credential" else "Verified credential",
                        color = if (credential.isExpired) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(credential.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(credential.credentialId, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
                }
            }
            InfoPill("Credential status", if (credential.isExpired) "Expired ID" else "Current 30-second ID")
            InfoPill("Verification method", credential.method)
            InfoPill("Document", credential.documentNumber)
            InfoPill("Date of birth", credential.dateOfBirth)
            InfoPill(
                "Reliability",
                reliabilityInfo?.let { "${it.effectiveReliability}% (${it.baseReliability}% document score)" }
                    ?: "${credential.signedReliability}% signed claim",
            )
            InfoPill("Signature", "Verified with QR public key")
            InfoPill("Photo", "Not embedded in QR")
            InfoPill("Public key", credential.publicKeyBase64.take(16) + "...")

            Text("Rate this user", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { scope.launch { onVote(true) } }, enabled = !isVoting, modifier = Modifier.weight(1f)) {
                    Text("Thumbs up", color = PassportKycColors.lavender, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { scope.launch { onVote(false) } }, enabled = !isVoting, modifier = Modifier.weight(1f)) {
                    Text("Thumbs down", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                }
            }
            reputationMessage?.let {
                Text(it, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
            }
        }
    }

    TextButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text("Scan another ID", color = PassportKycColors.primary, fontWeight = FontWeight.Bold)
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

private fun bindQrScannerCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onQrScanned: (String) -> Unit,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
        {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), QrAnalyzer(onQrScanned))
                }

            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        },
        ContextCompat.getMainExecutor(context),
    )
}

private class QrAnalyzer(
    private val onQrScanned: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }
    private var lastResult: String? = null

    override fun analyze(image: ImageProxy) {
        try {
            val luminance = image.copyLuminancePlane()
            val result = decodeQr(luminance, image.width, image.height)
                ?: decodeQr(luminance.rotateClockwise(image.width, image.height), image.height, image.width)
                ?: decodeQr(luminance.rotateCounterClockwise(image.width, image.height), image.height, image.width)

            if (result != null && result != lastResult) {
                lastResult = result
                onQrScanned(result)
            }
        } catch (_: Exception) {
            reader.reset()
        } finally {
            image.close()
        }
    }

    private fun decodeQr(bytes: ByteArray, width: Int, height: Int): String? {
        return try {
            val source = PlanarYUVLuminanceSource(
                bytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false,
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            reader.decodeWithState(bitmap).text
        } catch (_: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }
}

private fun ImageProxy.copyLuminancePlane(): ByteArray {
    val plane = planes.first()
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val output = ByteArray(width * height)
    var outputOffset = 0

    for (row in 0 until height) {
        val rowStart = row * rowStride
        for (col in 0 until width) {
            output[outputOffset++] = buffer.get(rowStart + col * pixelStride)
        }
    }
    return output
}

private fun ByteArray.rotateClockwise(width: Int, height: Int): ByteArray {
    val rotated = ByteArray(size)
    var index = 0
    for (x in 0 until width) {
        for (y in height - 1 downTo 0) {
            rotated[index++] = this[y * width + x]
        }
    }
    return rotated
}

private fun ByteArray.rotateCounterClockwise(width: Int, height: Int): ByteArray {
    val rotated = ByteArray(size)
    var index = 0
    for (x in width - 1 downTo 0) {
        for (y in 0 until height) {
            rotated[index++] = this[y * width + x]
        }
    }
    return rotated
}

private fun parseCredential(raw: String): Result<ScannedCredential> = runCatching {
    val json = JSONObject(raw)
    val version = json.optInt("v", 1)
    if (json.optString("t") != "verid" || version < 2) {
        error("This is not a signed VerID QR")
    }

    val publicKey = json.getString("pub")
    val encodedData = json.getString("data")
    val signature = json.getString("sig")
    if (!VerIdCredentialCrypto.verify(publicKey, encodedData, signature)) {
        error("Could not verify this ID signature")
    }

    val data = JSONObject(VerIdCredentialCrypto.decodeText(encodedData))
    val timePeriod = data.optLong("tp", -1L)
    ScannedCredential(
        name = data.optString("n", "Unknown holder"),
        credentialId = data.optString("cid", "-"),
        documentNumber = data.optString("doc", "-"),
        dateOfBirth = data.optString("dob", "-"),
        method = data.optString("m", "QR"),
        imageBase64 = "",
        publicKeyBase64 = publicKey,
        signedReliability = data.optLong("rel", 0L).coerceIn(0L, 100L),
        timePeriod = timePeriod,
        isExpired = timePeriod != currentVerIdTimePeriod(),
        rawPayload = raw,
    )
}

private fun currentVerIdTimePeriod(): Long = System.currentTimeMillis() / 30_000L

private fun String.decodeBitmapOrNull(): Bitmap? {
    if (isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(this, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
