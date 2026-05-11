package io.github.visiongem.ledger.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.visiongem.ledger.core.ui.R
import io.github.visiongem.ledger.core.ui.theme.LedgerTheme

private val StatePagePadding = 32.dp
private val StatePageGap = 16.dp

@Composable
fun ViaLoadingPage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ViaEmptyPage(
    message: String,
    modifier: Modifier = Modifier,
    illustration: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(StatePagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (illustration != null) {
            illustration()
            Spacer(modifier = Modifier.height(StatePageGap))
        }
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ViaErrorPage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(StatePagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(StatePageGap))
            ViaOutlineButton(text = stringResource(R.string.core_ui_retry), onClick = onRetry)
        }
    }
}

@Preview(showBackground = true, heightDp = 320)
@Composable
private fun ViaLoadingPagePreview() {
    LedgerTheme {
        ViaLoadingPage()
    }
}

@Preview(showBackground = true, heightDp = 320)
@Composable
private fun ViaEmptyPagePreview() {
    LedgerTheme {
        ViaEmptyPage(message = "No records yet. Tap + to add your first one.")
    }
}

@Preview(showBackground = true, heightDp = 320)
@Composable
private fun ViaErrorPagePreview() {
    LedgerTheme {
        ViaErrorPage(
            message = "Couldn't load data. Check your connection and try again.",
            onRetry = {},
        )
    }
}
