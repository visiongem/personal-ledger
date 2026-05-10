package io.github.visiongem.ledger.core.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.visiongem.ledger.core.ui.theme.LedgerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = actions,
    )
}

@Preview(showBackground = true)
@Composable
private fun ViaTopBarPreview() {
    LedgerTheme {
        ViaTopBar(
            title = "Records",
            onBack = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ViaTopBarNoBackPreview() {
    LedgerTheme {
        ViaTopBar(title = "Home")
    }
}
