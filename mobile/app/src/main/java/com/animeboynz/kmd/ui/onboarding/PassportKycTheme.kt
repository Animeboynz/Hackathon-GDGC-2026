package com.animeboynz.kmd.ui.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Design tokens aligned with the updated VerID frontend theme. */
object PassportKycColors {
    val background = Color(0xFFF3F0FA)
    val page = Color(0xFFF2F4F8)
    val primary = Color(0xFFAE0086)
    val primaryHover = Color(0xFF920071)
    val primaryBorder = Color(0xFF500040)
    val lavender = Color(0xFFCAA5FF)
    val lavender2 = Color(0xFFEDE9FE)
    val text = Color(0xFF111827)
    val muted = Color(0xFF374151)
    val border = Color(0xFFCBD5E1)
    val bannerBg = Color(0xFFF5DBE9)
    val bannerBorder = Color(0xFFD946B8)
    val surface = Color.White
    val nfcGradientTop = Color(0xFF0F172A)
    val nfcGradientBottom = Color(0xFF020617)
    val nfcSub = Color(0xFFCBD5E1)
    val nfcListBg = Color(0x0FFFFFFF)
    val nfcCheckDoneBg = Color(0x88AE0086)
    val nfcCheckDoneText = Color.White
    val nfcListText = Color(0xFFE2E8F0)
    val stepIconBg = Color(0xFFF5EEF9)
    val stepDoneIconBg = Color(0xFFCAA5FF)
    val pillApprovedBg = Color(0xFFF5DBE9)
    val pillApprovedText = Color(0xFFAE0086)
    val pillProgressBg = Color(0xFFEDE9FE)
    val pillProgressText = Color(0xFF4361EE)
    val progressFaceStart = Color(0xFF1E293B)
    val progressFaceEnd = Color(0xFF0F172A)
}

private val KycLightColorScheme = lightColorScheme(
    primary = PassportKycColors.primary,
    onPrimary = Color.White,
    primaryContainer = PassportKycColors.bannerBg,
    onPrimaryContainer = PassportKycColors.primary,
    secondary = PassportKycColors.lavender,
    onSecondary = Color.White,
    background = PassportKycColors.background,
    onBackground = PassportKycColors.text,
    surface = PassportKycColors.surface,
    onSurface = PassportKycColors.text,
    surfaceVariant = Color(0xFFF8F9FF),
    onSurfaceVariant = PassportKycColors.muted,
    outline = PassportKycColors.border,
    error = Color(0xFFDC2626),
)

private val KycTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 21.6.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.02).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.02).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.2.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.8.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun PassportKycTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KycLightColorScheme,
        typography = KycTypography,
        content = content,
    )
}
