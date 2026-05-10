package io.github.visiongem.ledger.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette — teal-driven (spec §10: pick a blue/green family).
private val LedgerTeal100 = Color(0xFFB2DFDB)
private val LedgerTeal500 = Color(0xFF00897B)
private val LedgerTeal700 = Color(0xFF00695C)
private val LedgerTeal900 = Color(0xFF004D40)

private val LedgerCoral500 = Color(0xFFFF7043)
private val LedgerCoral200 = Color(0xFFFFAB91)
private val LedgerCoral800 = Color(0xFFD84315)

val LedgerLightColorScheme = lightColorScheme(
    primary = LedgerTeal700,
    onPrimary = Color.White,
    primaryContainer = LedgerTeal100,
    onPrimaryContainer = LedgerTeal900,
    secondary = LedgerCoral500,
    onSecondary = Color.White,
    secondaryContainer = LedgerCoral200,
    onSecondaryContainer = LedgerCoral800,
)

val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerTeal500,
    onPrimary = Color.Black,
    primaryContainer = LedgerTeal900,
    onPrimaryContainer = LedgerTeal100,
    secondary = LedgerCoral200,
    onSecondary = LedgerCoral800,
    secondaryContainer = LedgerCoral800,
    onSecondaryContainer = LedgerCoral200,
)
