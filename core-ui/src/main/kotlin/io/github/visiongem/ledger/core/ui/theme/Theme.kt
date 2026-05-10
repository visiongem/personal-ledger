package io.github.visiongem.ledger.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

// dynamicColor defaults to false so Android 12+ wallpaper-derived colors don't
// override the brand palette; opt in per-screen if desired.
@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LedgerDarkColorScheme
        else -> LedgerLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LedgerTypography,
        content = content,
    )
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun LedgerThemeLightPreview() {
    LedgerTheme(darkTheme = false) {
        Surface { Text("Personal Ledger · Light") }
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun LedgerThemeDarkPreview() {
    LedgerTheme(darkTheme = true) {
        Surface { Text("Personal Ledger · Dark") }
    }
}
