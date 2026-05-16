package com.animeboynz.kmd.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.animeboynz.kmd.BuildConfig
import com.animeboynz.kmd.R
import com.animeboynz.kmd.nfc.NfcTagDispatcher
import com.animeboynz.kmd.passport.PassportChipSummary
import com.animeboynz.kmd.passport.Td3Mrz
import com.animeboynz.kmd.preferences.GeneralPreferences
import com.animeboynz.kmd.presentation.Screen
import com.animeboynz.kmd.ui.home.HomeScreen
import com.dynamsoft.mrzscannerbundle.ui.MRZScanResult
import com.dynamsoft.mrzscannerbundle.ui.MRZScannerActivity
import com.dynamsoft.mrzscannerbundle.ui.MRZScannerConfig
import kotlinx.coroutines.delay
import org.jmrtd.lds.icao.MRZInfo
import org.koin.compose.koinInject
import java.io.File

private val KycRadius = 16.dp
private val TapMin = 52.dp

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
        val nfcConsumerActive = state is PassportOnboardingScreenModel.State.WaitNfc

        DisposableEffect(nfcConsumerActive) {
            if (nfcConsumerActive) {
                NfcTagDispatcher.consumer = { tag -> screenModel.onNfcTag(tag) }
            } else {
                NfcTagDispatcher.consumer = null
            }
            onDispose {
                if (nfcConsumerActive) {
                    NfcTagDispatcher.consumer = null
                }
            }
        }

        PassportKycTheme {
            val isNfcWait = state is PassportOnboardingScreenModel.State.WaitNfc

            Scaffold(
                containerColor = if (isNfcWait) {
                    PassportKycColors.nfcGradientBottom
                } else {
                    MaterialTheme.colorScheme.background
                },
                topBar = {
                    if (!isNfcWait) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = context.getString(R.string.passport_onboarding_title),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                            ),
                        )
                    }
                },
            ) { padding ->
                if (isNfcWait) {
                    val s = state as PassportOnboardingScreenModel.State.WaitNfc
                    WaitNfcStep(
                        isReading = s.isReading,
                        onBack = { screenModel.cancelWaitNfc() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        VerificationProgress(state)

                        when (val s = state) {
                            is PassportOnboardingScreenModel.State.Welcome -> WelcomeStep(
                                onStart = { screenModel.goToScan() },
                                onManual = { screenModel.openManualEntry() },
                            )

                            is PassportOnboardingScreenModel.State.ScanPhoto -> PassportDynamsoftScanStep(
                                onScanResult = { screenModel.onDynamsoftMrzResult(it) },
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

                            is PassportOnboardingScreenModel.State.WaitNfc -> Unit

                            is PassportOnboardingScreenModel.State.ChipRead -> ChipResultStep(
                                summary = s.summary,
                                onContinue = { screenModel.beginSelfieCheck(s.summary) },
                            )

                            is PassportOnboardingScreenModel.State.SelfieCheck -> SelfieCheckStep(
                                summary = s.summary,
                                onBack = { screenModel.backToPassportSummary(s.summary) },
                                onFaceMatch = { screenModel.confirmSelfieMatch(s.summary) },
                            )

                            is PassportOnboardingScreenModel.State.DigitalIdIssue -> DigitalIdIssueStep(
                                summary = s.summary,
                                onDone = {
                                    screenModel.completeDigitalId(s.summary)
                                    navigator.replaceAll(HomeScreen)
                                },
                            )

                            is PassportOnboardingScreenModel.State.Fatal -> FatalNfcStep(
                                message = s.message,
                                onRetry = { screenModel.retryNfcAfterError() },
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = context.getString(R.string.passport_onboarding_footer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationProgress(state: PassportOnboardingScreenModel.State) {
    val progress = when (state) {
        PassportOnboardingScreenModel.State.Welcome -> 0.16f
        PassportOnboardingScreenModel.State.ScanPhoto -> 0.3f
        is PassportOnboardingScreenModel.State.ReviewMrz -> 0.38f
        is PassportOnboardingScreenModel.State.ManualMrz -> 0.3f
        is PassportOnboardingScreenModel.State.WaitNfc -> 0.44f
        is PassportOnboardingScreenModel.State.ChipRead -> 0.52f
        is PassportOnboardingScreenModel.State.SelfieCheck -> 0.72f
        is PassportOnboardingScreenModel.State.DigitalIdIssue -> 0.9f
        is PassportOnboardingScreenModel.State.Fatal -> 0.44f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE2E8F0))
            .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.67f), RoundedCornerShape(999.dp)),
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = PassportKycColors.primary,
            trackColor = Color.Transparent,
        )
    }
}

@Composable
private fun KycElevatedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KycRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        content()
    }
}

@Composable
private fun KycPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(TapMin),
        shape = RoundedCornerShape(KycRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        ),
        border = BorderStroke(2.dp, PassportKycColors.primaryBorder),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
    }
}

@Composable
private fun KycSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(TapMin),
        shape = RoundedCornerShape(KycRadius),
        border = BorderStroke(2.dp, PassportKycColors.muted),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PassportKycColors.text),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun WelcomeStep(
    onStart: () -> Unit,
    onManual: () -> Unit,
) {
    val context = LocalContext.current
    Text(
        text = context.getString(R.string.passport_onboarding_welcome_subtitle),
        style = MaterialTheme.typography.titleMedium,
        fontSize = 17.5.sp,
        lineHeight = 24.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PassportKycColors.bannerBg)
            .border(1.dp, PassportKycColors.bannerBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.passport_onboarding_intro),
            style = MaterialTheme.typography.bodySmall,
            color = PassportKycColors.primary,
            lineHeight = 20.sp,
        )
    }

    KycElevatedCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📱", fontSize = 30.sp)
                Text("📘", fontSize = 24.sp, modifier = Modifier.padding(start = 4.dp))
            }
            BulletLine(context.getString(R.string.passport_onboarding_bullet_chip))
            BulletLine(context.getString(R.string.passport_onboarding_bullet_remove))
            BulletLine(context.getString(R.string.passport_onboarding_bullet_hold))
        }
    }

    KycPrimaryButton(text = context.getString(R.string.passport_onboarding_scan_photo), onClick = onStart)
    TextButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = context.getString(R.string.passport_onboarding_enter_mrz_manual),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun PassportDynamsoftScanStep(
    onScanResult: (MRZScanResult?) -> Unit,
    onManualInstead: () -> Unit,
) {
    val context = LocalContext.current
    val scannerConfig = remember {
        MRZScannerConfig().apply {
            setLicense(BuildConfig.DYNAMSOFT_LICENSE_KEY)
            setCameraToggleButtonVisible(true)
        }
    }
    val launcher = rememberLauncherForActivityResult(
        MRZScannerActivity.ResultContract(),
        onScanResult,
    )

    Text(
        text = context.getString(R.string.passport_onboarding_scan_title),
        style = MaterialTheme.typography.headlineLarge,
    )
    Text(
        text = context.getString(R.string.passport_onboarding_scan_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    KycPrimaryButton(
        text = context.getString(R.string.passport_onboarding_open_mrz_scanner),
        onClick = { launcher.launch(scannerConfig) },
        enabled = BuildConfig.DYNAMSOFT_LICENSE_KEY.isNotBlank(),
    )
    if (BuildConfig.DYNAMSOFT_LICENSE_KEY.isBlank()) {
        Text(
            text = context.getString(R.string.passport_onboarding_dynamsoft_license_hint),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    KycSecondaryButton(
        text = context.getString(R.string.passport_onboarding_enter_mrz_manual),
        onClick = onManualInstead,
    )
}

@Composable
private fun ReviewMrzStep(
    mrz: Td3Mrz,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    Text(
        text = context.getString(R.string.passport_onboarding_mrz_review),
        style = MaterialTheme.typography.headlineLarge,
        fontSize = 20.sp,
    )
    KycElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = mrz.line1, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text(text = mrz.line2, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
    KycPrimaryButton(text = context.getString(R.string.passport_onboarding_continue_nfc), onClick = onConfirm)
    TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_edit_mrz), fontWeight = FontWeight.SemiBold)
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
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFFAFBFF),
        unfocusedContainerColor = Color(0xFFFAFBFF),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    )
    Text(
        text = context.getString(R.string.passport_onboarding_manual_hint),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    error?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    OutlinedTextField(
        value = line1,
        onValueChange = onLine1,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(context.getString(R.string.passport_onboarding_mrz_line1)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
    OutlinedTextField(
        value = line2,
        onValueChange = onLine2,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(context.getString(R.string.passport_onboarding_mrz_line2)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
    KycPrimaryButton(text = context.getString(R.string.passport_onboarding_validate_mrz), onClick = onSubmit)
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.passport_onboarding_back_scan), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NfcScanStage(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "nfcHero")
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        val ringSizes = listOf(48.dp, 72.dp, 96.dp)
        ringSizes.forEachIndexed { index, size ->
            val delayMs = index * 250
            val progress by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delayMs),
                ),
                label = "ring$index",
            )
            val alpha = (1f - progress) * 0.75f
            val scale = 0.9f + progress * 0.28f
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .border(
                        width = 2.dp,
                        color = Color(0x594361EE),
                        shape = CircleShape,
                    ),
            )
        }
        Text(text = "📘", fontSize = 56.sp)
    }
}

@Composable
private fun WaitNfcStep(
    isReading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var nfcPhase by remember { mutableIntStateOf(0) }
    var elapsedSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(isReading) {
        if (!isReading) {
            nfcPhase = 0
            elapsedSec = 0
            return@LaunchedEffect
        }
        nfcPhase = 1
        delay(900)
        nfcPhase = 2
        delay(850)
        nfcPhase = 3
    }

    LaunchedEffect(isReading) {
        if (!isReading) {
            elapsedSec = 0
            return@LaunchedEffect
        }
        elapsedSec = 0
        while (true) {
            delay(1_000)
            elapsedSec++
        }
    }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(PassportKycColors.nfcGradientTop, PassportKycColors.nfcGradientBottom),
                ),
            ),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        ) {
            Text("✕", color = Color(0xFFF4F4F5), fontSize = 18.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            NfcScanStage()
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = context.getString(R.string.passport_onboarding_nfc_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.2.sp, color = Color(0xFFF4F4F5)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Text(
                text = context.getString(R.string.passport_onboarding_nfc_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = PassportKycColors.nfcSub,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp),
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            NfcChecklistItem(
                label = context.getString(R.string.passport_onboarding_nfc_check_detected),
                done = isReading && nfcPhase >= 1,
            )
            NfcChecklistItem(
                label = context.getString(R.string.passport_onboarding_nfc_check_reading),
                done = isReading && nfcPhase >= 2,
            )
            NfcChecklistItem(
                label = context.getString(R.string.passport_onboarding_nfc_check_securing),
                done = isReading && nfcPhase >= 3,
            )

            if (isReading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PassportKycColors.nfcSub,
                    trackColor = Color.White.copy(alpha = 0.15f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = PassportKycColors.nfcSub,
                        strokeWidth = 3.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.passport_onboarding_nfc_reading_progress_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE0E7FF),
                        )
                        Text(
                            text = context.getString(R.string.passport_onboarding_nfc_elapsed, elapsedSec),
                            style = MaterialTheme.typography.bodySmall,
                            color = PassportKycColors.nfcSub,
                        )
                    }
                }
                Text(
                    text = context.getString(R.string.passport_onboarding_nfc_reading_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = PassportKycColors.nfcSub.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 14.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.passport_onboarding_nfc_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE0E7FF),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = context.getString(R.string.passport_onboarding_nfc_ready_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = PassportKycColors.nfcSub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun NfcChecklistItem(label: String, done: Boolean) {
    val bg = if (done) PassportKycColors.nfcCheckDoneBg else PassportKycColors.nfcListBg
    val fg = if (done) PassportKycColors.nfcCheckDoneText else PassportKycColors.nfcListText
    val prefix = if (done) "✓" else "○"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(prefix, color = fg, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = fg)
    }
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

    KycElevatedCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = context.getString(R.string.passport_onboarding_overview_head),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.4.sp,
            )
            Text(
                text = context.getString(R.string.passport_onboarding_overview_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
            OverviewStepRow(emoji = "📘", title = context.getString(R.string.passport_onboarding_step_chip_title), subtitle = context.getString(R.string.passport_onboarding_step_chip_meta), done = true)
            OverviewStepRow(emoji = "✓", title = context.getString(R.string.passport_onboarding_step_checks_title), subtitle = context.getString(R.string.passport_onboarding_step_checks_meta), done = true, withDivider = true)
        }
    }

    KycElevatedCard {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PassportKycColors.progressFaceStart, PassportKycColors.progressFaceEnd),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("📘", fontSize = 26.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPillRow(
                    label = context.getString(R.string.passport_onboarding_status_passport_label),
                    pillText = context.getString(R.string.passport_onboarding_status_approved),
                    approved = true,
                )
                StatusPillRow(
                    label = context.getString(R.string.passport_onboarding_status_nfc_label),
                    pillText = context.getString(R.string.passport_onboarding_status_verified),
                    approved = true,
                )
            }
        }
    }

    summary.portrait?.let { bmp ->
        Text(
            text = context.getString(R.string.passport_onboarding_chip_portrait),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            shape = RoundedCornerShape(KycRadius),
            elevation = CardDefaults.cardElevation(3.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = context.getString(R.string.passport_onboarding_chip_portrait),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
    }

    KycElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = context.getString(R.string.passport_onboarding_chip_success),
                style = MaterialTheme.typography.titleMedium,
            )
            KeyValueRow(context.getString(R.string.passport_field_names), displayName)
            KeyValueRow(context.getString(R.string.passport_field_doc_number), m.documentNumber)
            KeyValueRow(context.getString(R.string.passport_field_nationality), m.nationality)
            KeyValueRow(context.getString(R.string.passport_field_dob), m.dateOfBirth)
            KeyValueRow(context.getString(R.string.passport_field_expiry), m.dateOfExpiry)
            KeyValueRow(context.getString(R.string.passport_field_gender), m.gender.toString())
            KeyValueRow(context.getString(R.string.passport_field_issuer), m.issuingState)
            KeyValueRow(context.getString(R.string.passport_field_doc_type), m.documentCode)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = context.getString(R.string.passport_onboarding_raw_dg1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = summary.rawMrzDisplay,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    KycPrimaryButton(text = context.getString(R.string.passport_onboarding_enter_app), onClick = onContinue)
}

@Composable
private fun SelfieCheckStep(
    summary: PassportChipSummary,
    onBack: () -> Unit,
    onFaceMatch: () -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedSelfie by remember { mutableStateOf<Bitmap?>(null) }
    var isComparing by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    val holderName = summary.mrzInfo.nameOfHolder.ifBlank {
        listOf(summary.mrzInfo.primaryIdentifier, summary.mrzInfo.secondaryIdentifier)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }.ifBlank { context.getString(R.string.passport_onboarding_selfie_default_holder) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(isComparing) {
        if (!isComparing) return@LaunchedEffect
        delay(2_400)
        onFaceMatch()
    }

    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = context.getString(R.string.passport_onboarding_back),
            color = PassportKycColors.muted,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Text(
        text = context.getString(R.string.passport_onboarding_selfie_title),
        style = MaterialTheme.typography.headlineLarge,
        color = PassportKycColors.text,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = context.getString(R.string.passport_onboarding_selfie_body),
        style = MaterialTheme.typography.bodyMedium,
        color = PassportKycColors.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    SelfieScanVisual(
        passportBitmap = summary.portrait,
        capturedSelfie = capturedSelfie,
        isComparing = isComparing,
        hasCameraPermission = hasCameraPermission,
        onImageCaptureReady = { imageCapture = it },
        modifier = Modifier.padding(top = 14.dp),
    )

    KycElevatedCard {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = context.getString(R.string.passport_onboarding_selfie_compare_title),
                style = MaterialTheme.typography.titleMedium,
            )
            MatchRow(
                label = context.getString(R.string.passport_onboarding_selfie_compare_passport),
                value = holderName,
                done = true,
            )
            MatchRow(
                label = context.getString(R.string.passport_onboarding_selfie_compare_face),
                value = when {
                    isComparing -> context.getString(R.string.passport_onboarding_selfie_comparing)
                    capturedSelfie != null -> context.getString(R.string.passport_onboarding_selfie_captured)
                    else -> context.getString(R.string.passport_onboarding_selfie_waiting)
                },
                done = isComparing || capturedSelfie != null,
            )
        }
    }

    KycPrimaryButton(
        text = when {
            isComparing -> context.getString(R.string.passport_onboarding_selfie_comparing_button)
            capturedSelfie != null -> context.getString(R.string.passport_onboarding_selfie_retake)
            else -> context.getString(R.string.passport_onboarding_selfie_capture)
        },
        enabled = hasCameraPermission && imageCapture != null && !isComparing,
        onClick = {
            val capture = imageCapture
            if (capture != null) {
                val outputFile = File(context.cacheDir, "verid-selfie-${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            capturedSelfie = outputFile.decodeBitmapRespectingExif()
                            isComparing = true
                        }

                        override fun onError(exception: ImageCaptureException) {
                            capturedSelfie = null
                            isComparing = false
                        }
                    },
                )
            }
        },
    )

    if (!hasCameraPermission) {
        Text(
            text = context.getString(R.string.passport_onboarding_selfie_permission),
            style = MaterialTheme.typography.bodySmall,
            color = PassportKycColors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelfieScanVisual(
    passportBitmap: Bitmap?,
    capturedSelfie: Bitmap?,
    isComparing: Boolean,
    hasCameraPermission: Boolean,
    onImageCaptureReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val infinite = rememberInfiniteTransition(label = "selfieScan")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "selfieScanRotation",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isComparing) 300.dp else 250.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isComparing && capturedSelfie != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FaceCompareImage(label = "Passport", bitmap = passportBitmap, placeholder = "👤")
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .border(3.dp, PassportKycColors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↔", color = PassportKycColors.primary, fontWeight = FontWeight.Bold)
                }
                FaceCompareImage(label = "Selfie", bitmap = capturedSelfie, placeholder = "◉")
            }
        } else {
            Box(
                modifier = Modifier
                    .size(224.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, PassportKycColors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    capturedSelfie != null -> {
                        Image(
                            bitmap = capturedSelfie.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    hasCameraPermission -> {
                        AndroidView(
                            factory = { viewContext ->
                                PreviewView(viewContext).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    bindSelfieCamera(
                                        context = context,
                                        lifecycleOwner = lifecycleOwner,
                                        previewView = this,
                                        onImageCaptureReady = onImageCaptureReady,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        Text("◉", fontSize = 68.sp, color = PassportKycColors.primary)
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { rotationZ = rotation }
                        .border(2.dp, PassportKycColors.primaryHover, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun FaceCompareImage(label: String, bitmap: Bitmap?, placeholder: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(112.dp)
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
                Text(placeholder, fontSize = 42.sp, color = PassportKycColors.primary)
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = PassportKycColors.muted)
    }
}

private fun bindSelfieCamera(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onImageCaptureReady: (ImageCapture) -> Unit,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
        {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val cameraSelector = runCatching {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            }.getOrDefault(CameraSelector.DEFAULT_BACK_CAMERA)

            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                onImageCaptureReady(imageCapture)
            }
        },
        ContextCompat.getMainExecutor(context),
    )
}

private fun File.decodeBitmapRespectingExif(): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(absolutePath) ?: return null
    val orientation = ExifInterface(absolutePath).getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL,
    )
    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (rotation == 0f) return bitmap

    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
        if (it != bitmap) bitmap.recycle()
    }
}

@Composable
private fun MatchRow(label: String, value: String, done: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = PassportKycColors.muted)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = PassportKycColors.text)
        }
        Text(
            text = if (done) "✓" else "○",
            color = if (done) PassportKycColors.primary else PassportKycColors.muted,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun DigitalIdIssueStep(
    summary: PassportChipSummary,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val documentNumber = summary.mrzInfo.documentNumber.ifBlank { "UNKNOWN" }
    val credentialId = "EID-${documentNumber.takeLast(4).padStart(4, '0')}"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(PassportKycColors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 52.sp)
        }
        Text(
            text = context.getString(R.string.passport_onboarding_id_ready_title),
            modifier = Modifier.padding(top = 22.dp),
            style = MaterialTheme.typography.headlineLarge,
            color = PassportKycColors.text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = context.getString(R.string.passport_onboarding_id_ready_body),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = PassportKycColors.muted,
            textAlign = TextAlign.Center,
        )
    }

    KycElevatedCard {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = context.getString(R.string.passport_onboarding_id_credential_label),
                style = MaterialTheme.typography.labelMedium,
                color = PassportKycColors.muted,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = context.getString(R.string.passport_onboarding_id_credential_title),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                color = PassportKycColors.text,
            )
            Text(
                text = context.getString(R.string.passport_onboarding_id_credential_meta, credentialId),
                style = MaterialTheme.typography.bodyMedium,
                color = PassportKycColors.muted,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PassportKycColors.bannerBg)
                    .border(2.dp, PassportKycColors.bannerBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = context.getString(R.string.passport_onboarding_id_qr_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = PassportKycColors.text,
                )
                Text("▣", fontSize = 42.sp, color = PassportKycColors.text)
            }
        }
    }

    KycPrimaryButton(
        text = context.getString(R.string.passport_onboarding_open_id),
        onClick = onDone,
    )
}

@Composable
private fun OverviewStepRow(
    emoji: String,
    title: String,
    subtitle: String,
    done: Boolean,
    withDivider: Boolean = false,
) {
    if (withDivider) {
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Spacer(modifier = Modifier.height(12.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (done) PassportKycColors.stepDoneIconBg else PassportKycColors.stepIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, fontSize = 15.2.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusPillRow(label: String, pillText: String, approved: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        val bg = if (approved) PassportKycColors.pillApprovedBg else PassportKycColors.pillProgressBg
        val fg = if (approved) PassportKycColors.pillApprovedText else PassportKycColors.pillProgressText
        Text(
            text = pillText.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            color = fg,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
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
    KycElevatedCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = context.getString(R.string.passport_onboarding_nfc_failed),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
    KycPrimaryButton(text = context.getString(R.string.passport_onboarding_try_nfc_again), onClick = onRetry)
}
