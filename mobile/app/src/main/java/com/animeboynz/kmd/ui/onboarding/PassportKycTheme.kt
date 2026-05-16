package com.animeboynz.kmd.ui.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Design tokens aligned with `frontend/src/styles.css` (KYC / verification flow). */
object PassportKycColors {
    val background = Color(0xFFF0F2FF)
    val primary = Color(0xFF4361EE)
    val text = Color(0xFF1A1D26)
    val muted = Color(0xFF6B7280)
    val border = Color(0xFFE5E7EB)
    val bannerBg = Color(0xFFEEF1FF)
    val bannerBorder = Color(0xFFC7D2FE)
    val surface = Color.White
    val nfcGradientTop = Color(0xFF0F1020)
    val nfcGradientBottom = Color(0xFF1A1A2E)
    val nfcSub = Color(0xFFA5B4FC)
    val nfcListBg = Color(0x0FFFFFFF)
    val nfcCheckDoneBg = Color(0x2634D399)
    val nfcCheckDoneText = Color(0xFFA7F3D0)
    val nfcListText = Color(0xFFC7D2FE)
    val stepIconBg = Color(0xFFEEF1FF)
    val stepDoneIconBg = Color(0xFFD1FAE5)
    val pillApprovedBg = Color(0xFFD1FAE5)
    val pillApprovedText = Color(0xFF047857)
    val pillProgressBg = Color(0xFFDBEAFE)
    val pillProgressText = Color(0xFF4361EE)
    val progressFaceStart = Color(0xFF312E81)
    val progressFaceEnd = Color(0xFF4338CA)
}

private val KycLightColorScheme = lightColorScheme(
    primary = PassportKycColors.primary,
    onPrimary = Color.White,
    primaryContainer = PassportKycColors.bannerBg,
    onPrimaryContainer = PassportKycColors.primary,
    secondary = Color(0xFF7C3AED),
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
