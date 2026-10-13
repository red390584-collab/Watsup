package com.yourapp.whatsappclone.ui.onboarding

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourapp.whatsappclone.R
import com.yourapp.whatsappclone.data.SessionStore
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaTealDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mandatory first-launch disclaimer about WhatsApp ToS / ban risk.
 * Acceptance is persisted in DataStore under `disclaimer_accepted`.
 */
@Composable
fun DisclaimerScreen(onAccepted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .size(96.dp)
                .background(WaGreen.copy(alpha = 0.12f), CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = WaTealDark,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.disclaimer_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.disclaimer_body),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 23.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "This is a personal-use, self-hosted client. Not affiliated with WhatsApp or Meta.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = {
                if (saving) return@Button
                saving = true
                scope.launch {
                    withContext(Dispatchers.IO) {
                        SessionStore.get(context).setDisclaimerAccepted(true)
                    }
                    onAccepted()
                }
            },
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = WaGreen, contentColor = Color.White),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = stringResource(R.string.disclaimer_continue),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                (context as? Activity)?.finish()
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = stringResource(R.string.disclaimer_exit),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
