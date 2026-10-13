package com.yourapp.whatsappclone.ui.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourapp.whatsappclone.data.CobaltManager
import com.yourapp.whatsappclone.data.PhoneLinkState
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.Muted
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaTealDark

/**
 * Phone-number linking: request a verification code, then submit it.
 * The state machine lives in [CobaltManager].
 */
@Composable
fun PhoneLinkPanel() {
    val colors = LocalWaExtendedColors.current
    val state by CobaltManager.phoneLinkState.collectAsState()
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Link with your phone number",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter your number with country prefix (e.g. 15551234567). " +
                "WhatsApp will send you a verification code.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))

        when (val current = state) {
            is PhoneLinkState.Idle, is PhoneLinkState.Error -> {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { input -> phone = input.filter { it.isDigit() }.take(15) },
                    label = { Text("Phone number") },
                    placeholder = { Text("15551234567", color = Muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = outlinedColors(colors.textPrimary, colors.textSecondary),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (current is PhoneLinkState.Error) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { CobaltManager.requestVerificationCode(phone) },
                    enabled = phone.length >= 7,
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreen, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text("Request verification code", style = MaterialTheme.typography.titleMedium)
                }
            }

            is PhoneLinkState.RequestingCode -> {
                CircularProgressIndicator(color = WaGreen, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Requesting code from WhatsApp…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            is PhoneLinkState.CodeRequested -> {
                Text(
                    text = "Code sent to +${current.phoneNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WaTealDark,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { input -> code = input.filter { it.isDigit() }.take(8) },
                    label = { Text("Verification code") },
                    placeholder = { Text("6-digit code", color = Muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedColors(colors.textPrimary, colors.textSecondary),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { CobaltManager.submitVerificationCode(code.trim()) },
                    enabled = code.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreen, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text("Verify and link", style = MaterialTheme.typography.titleMedium)
                }
            }

            is PhoneLinkState.Verifying -> {
                CircularProgressIndicator(color = WaGreen, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Verifying code…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            is PhoneLinkState.Linked -> {
                Text(
                    text = "Device linked ✓",
                    style = MaterialTheme.typography.titleMedium,
                    color = WaTealDark,
                )
            }
        }
    }
}

@Composable
private fun outlinedColors(textPrimary: Color, textSecondary: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedBorderColor = WaGreen,
        unfocusedBorderColor = textSecondary.copy(alpha = 0.4f),
        focusedLabelColor = WaGreen,
        unfocusedLabelColor = textSecondary,
        cursorColor = WaGreen,
        focusedPlaceholderColor = textSecondary,
        unfocusedPlaceholderColor = textSecondary,
    )
