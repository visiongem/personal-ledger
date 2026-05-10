package io.github.visiongem.ledger.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Material 3 defaults override — custom font deferred to keep v1 APK light.
private val baseTypography = Typography()

val LedgerTypography = baseTypography.copy(
    titleLarge = baseTypography.titleLarge.copy(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = baseTypography.titleMedium.copy(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = baseTypography.bodyLarge.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = baseTypography.labelLarge.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

// Reserved for cases where direct TextStyle access is preferable to Typography lookup.
val MoneyAmountStyle: TextStyle = baseTypography.headlineMedium.copy(
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
)
