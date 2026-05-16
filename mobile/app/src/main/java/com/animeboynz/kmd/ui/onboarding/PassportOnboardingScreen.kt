package com.animeboynz.kmd.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.animeboynz.kmd.R
import com.animeboynz.kmd.nfc.NfcTagDispatcher
import com.animeboynz.kmd.passport.PassportChipSummary
import com.animeboynz.kmd.passport.Td3Mrz
import com.animeboynz.kmd.preferences.GeneralPreferences
import com.animeboynz.kmd.presentation.Screen
import com.animeboynz.kmd.ui.home.HomeScreen
import org.jmrtd.lds.icao.MRZInfo
import org.koin.compose.koinInject
import java.io.File
import java.util.concurrent.Executors

object PassportOnboardingScreen : Screen() {
    private fun readResolve(): Any = PassportOnboardingScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val generalPreferences = koinInject<GeneralPreferences>()
        val screenModel = rememberScreenModel { PassportOnboardingScreenModel(generalPreferences) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        DisposableEffect(state) {
            when (state) {
                is PassportOnboardingScreenModel.State.WaitNfc -> {
                    NfcTagDispatcher.consumer = { tag -> screenModel.onNfcTag(tag) }
                }
                else -> {
                    NfcTagDispatcher.consumer = null
                }
            }
            onDispose {
                NfcTagDispatcher.consumer = null
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = context.getString(R.string.passport_onboarding_title)) },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val s = state) {
                    is PassportOnboardingScreenModel.State.Welcome -> WelcomeStep(
                        onStart = { screenModel.goToScan() },
                        onManual = { screenModel.openManualEntry() },
                    )

                    is PassportOnboardingScreenModel.State.ScanPhoto -> PassportCameraStep(
                        onCaptured = { screenModel.onPhotoCaptured(it) },
                        onManualInstead = { screenModel.openManualEntry() },
                    )

                    is PassportOnboardingScreenModel.State.ReviewMrz -> ReviewMrzStep(
                        mrz = s.mrz,
                        onConfirm = { screenModel.confirmMrzForNfc(s.mrz) },
                        onEdit = { screenModel.editMrzAgain(s.mrz) },
                    )

                    is PassportOnboardingScreenModel.State.ManualMrz -> ManualMrzStep(
                        line1 = s.line1,
                        line2 = s.line2,
                        error = s.error,
                        onLine1 = screenModel::updateManualLine1,
                        onLine2 = screenModel::updateManualLine2,
                        onSubmit = { screenModel.submitManualMrz() },
                        onBack = { screenModel.goToScan() },
                    )

                    is PassportOnboardingScreenModel.State.WaitNfc -> WaitNfcStep()

                    is PassportOnboardingScreenModel.State.ChipRead -> ChipResultStep(
                        summary = s.summary,
                        onContinue = {
                            screenModel.markOnboardingComplete()
                            navigator.replaceAll(HomeScreen)
                        },
                    )

                    is PassportOnboardingScreenModel.State.Fatal -> FatalNfcStep(
                        message = s.message,
                        onRetry = { screenModel.retryNfcAfterError() },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onStart: () -> Unit,
    onManual: () -> Unit,
) {
    val context = LocalContext.current
    Text(
        text = context.getString(R.string.passport_onboarding_intro),
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_scan_photo))
    }
    TextButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_enter_mrz_manual))
    }
}

@Composable
private fun PassportCameraStep(
    onCaptured: (android.graphics.Bitmap) -> Unit,
    onManualInstead: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, hasPermission) {
        if (!hasPermission) {
            return@DisposableEffect onDispose { }
        }
        val future = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = capture
        future.addListener(
            {
                try {
                    val provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture,
                    )
                } catch (_: Exception) {
                }
            },
            mainExecutor,
        )
        onDispose {
            imageCapture = null
            mainExecutor.execute {
                try {
                    if (future.isDone) {
                        future.get().unbindAll()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    Text(text = context.getString(R.string.passport_onboarding_scan_instructions))

    if (hasPermission) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            factory = { previewView },
        )

        Button(
            onClick = {
                val capture = imageCapture ?: return@Button
                val file = File(context.cacheDir, "passport_mrz_${System.currentTimeMillis()}.jpg")
                val output = ImageCapture.OutputFileOptions.Builder(file).build()
                val executor = Executors.newSingleThreadExecutor()
                capture.takePicture(
                    output,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val bmp = BitmapFactory.decodeFile(file.absolutePath)
                            if (bmp != null) {
                                ContextCompat.getMainExecutor(context).execute {
                                    onCaptured(bmp)
                                }
                            }
                            file.delete()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            file.delete()
                        }
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(context.getString(R.string.passport_onboarding_capture))
        }
    } else {
        Text(text = context.getString(R.string.passport_onboarding_camera_denied))
    }

    TextButton(onClick = onManualInstead, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_enter_mrz_manual))
    }
}

@Composable
private fun ReviewMrzStep(
    mrz: Td3Mrz,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    Text(text = context.getString(R.string.passport_onboarding_mrz_review))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = mrz.line1, fontFamily = FontFamily.Monospace)
            Text(text = mrz.line2, fontFamily = FontFamily.Monospace)
        }
    }
    Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_continue_nfc))
    }
    TextButton(onClick = onEdit) {
        Text(context.getString(R.string.passport_onboarding_edit_mrz))
    }
}

@Composable
private fun ManualMrzStep(
    line1: String,
    line2: String,
    error: String?,
    onLine1: (String) -> Unit,
    onLine2: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Text(text = context.getString(R.string.passport_onboarding_manual_hint))
    error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    OutlinedTextField(
        value = line1,
        onValueChange = onLine1,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(context.getString(R.string.passport_onboarding_mrz_line1)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
    OutlinedTextField(
        value = line2,
        onValueChange = onLine2,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(context.getString(R.string.passport_onboarding_mrz_line2)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
    Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_validate_mrz))
    }
    TextButton(onClick = onBack) {
        Text(context.getString(R.string.passport_onboarding_back_camera))
    }
}

@Composable
private fun WaitNfcStep() {
    val context = LocalContext.current
    Text(
        text = context.getString(R.string.passport_onboarding_nfc_instructions),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun ChipResultStep(
    summary: PassportChipSummary,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val m: MRZInfo = summary.mrzInfo
    val displayName = listOf(m.primaryIdentifier, m.secondaryIdentifier)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { m.nameOfHolder }
    Text(
        text = context.getString(R.string.passport_onboarding_chip_success),
        style = MaterialTheme.typography.titleMedium,
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyValueRow(context.getString(R.string.passport_field_names), displayName)
            KeyValueRow(context.getString(R.string.passport_field_doc_number), m.documentNumber)
            KeyValueRow(context.getString(R.string.passport_field_nationality), m.nationality)
            KeyValueRow(context.getString(R.string.passport_field_dob), m.dateOfBirth)
            KeyValueRow(context.getString(R.string.passport_field_expiry), m.dateOfExpiry)
            KeyValueRow(context.getString(R.string.passport_field_gender), m.gender.toString())
            KeyValueRow(context.getString(R.string.passport_field_issuer), m.issuingState)
            KeyValueRow(context.getString(R.string.passport_field_doc_type), m.documentCode)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = context.getString(R.string.passport_onboarding_raw_dg1),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = summary.rawMrzDisplay, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_enter_app))
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FatalNfcStep(
    message: String,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    Text(text = context.getString(R.string.passport_onboarding_nfc_failed), style = MaterialTheme.typography.titleMedium)
    Text(text = message, color = MaterialTheme.colorScheme.error)
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_try_nfc_again))
    }
}
