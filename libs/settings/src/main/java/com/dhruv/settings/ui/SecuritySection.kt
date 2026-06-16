package com.dhruv.settings.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Security settings section — biometric toggle.
 *
 * Gates the biometric switch on [BiometricManager.canAuthenticate] with BIOMETRIC_STRONG.
 * When no eligible biometric is enrolled the toggle is disabled and an explanatory label is shown.
 *
 * No finance dependencies. Callback-driven.
 *
 * @param biometricEnabled Current stored preference.
 * @param onBiometricToggled Called when the user changes the switch (only when eligible).
 */
@Composable
fun SecuritySection(
    biometricEnabled: Boolean,
    onBiometricToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val biometricStatus = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }
    val canUseBiometric = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Biometric unlock",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!canUseBiometric) {
                    Text(
                        text = when (biometricStatus) {
                            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                                "No biometric hardware on this device."
                            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                                "Biometric hardware is currently unavailable."
                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                                "No biometrics enrolled. Enrol in device Settings first."
                            else -> "Biometric authentication is not available."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Switch(
                checked = biometricEnabled && canUseBiometric,
                onCheckedChange = { if (canUseBiometric) onBiometricToggled(it) },
                enabled = canUseBiometric
            )
        }
    }
}
