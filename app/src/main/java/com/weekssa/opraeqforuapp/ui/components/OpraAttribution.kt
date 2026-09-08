package com.weekssa.opraeqforuapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R

const val OPRA_PROJECT_URL = "https://github.com/opra-project/OPRA"
const val OPRA_DATA_LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/"

@Composable
fun OpraAttribution(
    onOpenUrl: (String) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.opra_logo),
            contentDescription = stringResource(R.string.opra_logo_content_description),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (compact) 48.dp else 72.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = stringResource(R.string.opra_attribution_text),
            modifier = Modifier.padding(top = 8.dp),
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onOpenUrl(OPRA_PROJECT_URL) }) {
            Text(stringResource(R.string.opra_open_project))
        }
    }
}
