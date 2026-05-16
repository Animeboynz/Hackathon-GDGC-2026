package com.animeboynz.kmd.ui.home.tabs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.animeboynz.kmd.R
import com.animeboynz.kmd.preferences.GeneralPreferences
import com.animeboynz.kmd.presentation.util.Tab
import com.animeboynz.kmd.ui.onboarding.PassportKycColors
import com.animeboynz.kmd.ui.onboarding.PassportKycTheme
import com.animeboynz.kmd.ui.preferences.PreferencesScreen
import org.koin.compose.koinInject

object MyId : Tab {
    private fun readResolve(): Any = MyId

    override val options: TabOptions
        @Composable
        get() {
            val image = rememberVectorPainter(Icons.AutoMirrored.Filled.ManageSearch)
            return TabOptions(
                index = 0u,
                title = stringResource(R.string.my_id),
                icon = image,
            )
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val preferences = koinInject<GeneralPreferences>()

        PassportKycTheme {
            Scaffold(
                containerColor = PassportKycColors.page,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(stringResource(R.string.my_id))
                        },
                        actions = {
                            IconButton(onClick = { navigator.push(PreferencesScreen) }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PassportKycColors.page)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    DigitalIdDashboard(
                        preferences = preferences,
                        onPresent = { tabNavigator.current = ToolsTab },
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalIdDashboard(
    preferences: GeneralPreferences,
    onPresent: () -> Unit,
) {
    val generated = preferences.digitalIdGenerated.get()
    val holderName = preferences.digitalIdHolderName.get()
    val credentialId = preferences.digitalIdCredentialId.get()
    val documentNumber = preferences.digitalIdDocumentNumber.get()
    val dateOfBirth = preferences.digitalIdDateOfBirth.get()
    val portraitBase64 = preferences.digitalIdPortraitBase64.get()
    val portraitBitmap = remember(portraitBase64) { portraitBase64.decodeBitmapOrNull() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PassportPhotoThumb(bitmap = portraitBitmap, sizeDp = 44)
            Column {
                Text(text = "Hello,", style = MaterialTheme.typography.bodyMedium, color = PassportKycColors.muted)
                Text(
                    text = if (generated) holderName else "Traveler",
                    style = MaterialTheme.typography.titleMedium,
                    color = PassportKycColors.text,
                    maxLines = 1,
                )
            }
        }
    }

    if (!generated) {
        EmptyDigitalIdCard()
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, PassportKycColors.border),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Passport · NFC",
                style = MaterialTheme.typography.labelMedium,
                color = PassportKycColors.muted,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = "Verified",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 36.sp,
                color = PassportKycColors.text,
            )
            Text(
                text = "Last chip read: ready for travel",
                style = MaterialTheme.typography.bodyMedium,
                color = PassportKycColors.muted,
            )
        }
    }

    EmergencyIdPass(
        holderName = holderName,
        credentialId = credentialId,
        documentNumber = documentNumber,
        dateOfBirth = dateOfBirth,
        portraitBitmap = portraitBitmap,
        onPresent = onPresent,
    )
}

@Composable
private fun EmptyDigitalIdCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, PassportKycColors.border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🛡", fontSize = 44.sp, color = PassportKycColors.primary)
            Text(
                text = "No digital ID yet",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = PassportKycColors.text,
            )
            Text(
                text = "Complete passport NFC and selfie verification to generate your VerID credential.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = PassportKycColors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmergencyIdPass(
    holderName: String,
    credentialId: String,
    documentNumber: String,
    dateOfBirth: String,
    portraitBitmap: Bitmap?,
    onPresent: () -> Unit,
) {
    Spacer(modifier = Modifier.height(28.dp))
    Text(
        text = "Digital ID",
        style = MaterialTheme.typography.titleMedium,
        color = PassportKycColors.text,
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A)),
                    ),
                )
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(PassportKycColors.primary.copy(alpha = 0.35f)),
            )
            Column {
                PassportPhotoThumb(bitmap = portraitBitmap, sizeDp = 76)
                Text(
                    text = "ACTIVE",
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(PassportKycColors.lavender)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    color = PassportKycColors.text,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "Crisis credential",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )
                Text(
                    text = "Passport chip + photo + selfie",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    CredentialField("Holder", holderName, Modifier.weight(1f))
                    CredentialField("DOB", dateOfBirth, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    CredentialField("Document", documentNumber, Modifier.weight(1f))
                    CredentialField("ID", credentialId, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .clickable(onClick = onPresent)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "PRESENT",
                            style = MaterialTheme.typography.labelMedium,
                            color = PassportKycColors.muted,
                            letterSpacing = 1.1.sp,
                        )
                        Text(
                            text = "QR + NFC",
                            style = MaterialTheme.typography.titleMedium,
                            color = PassportKycColors.text,
                        )
                    }
                    Text("▣", fontSize = 44.sp, color = PassportKycColors.text)
                }
            }
        }
    }
}

@Composable
private fun CredentialField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.72f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
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

private fun String.decodeBitmapOrNull(): Bitmap? {
    if (isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(this, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
