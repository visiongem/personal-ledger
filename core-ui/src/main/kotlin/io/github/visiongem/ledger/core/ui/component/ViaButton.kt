package io.github.visiongem.ledger.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.visiongem.ledger.core.ui.theme.LedgerTheme

private val ViaButtonShape = RoundedCornerShape(12.dp)
private val ViaButtonMinHeight = 48.dp
private val ViaSpinnerSize = 20.dp

@Composable
fun ViaFillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ViaButtonMinHeight),
        enabled = enabled && !loading,
        shape = ViaButtonShape,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ViaSpinnerSize),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun ViaOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ViaButtonMinHeight),
        enabled = enabled,
        shape = ViaButtonShape,
    ) {
        Text(text)
    }
}

@Preview(showBackground = true)
@Composable
private fun ViaButtonPreview() {
    LedgerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ViaFillButton(text = "Save", onClick = {})
            ViaFillButton(text = "Saving", onClick = {}, loading = true)
            ViaFillButton(text = "Disabled", onClick = {}, enabled = false)
            ViaOutlineButton(text = "Cancel", onClick = {})
            ViaOutlineButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
