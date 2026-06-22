package de.familienwecker.famwake.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.viewmodel.PurchaseState

/**
 * Dialog zur Anzeige der In-App-Kaufoptionen (RevenueCat Offerings).
 */
@Composable
fun DonationDialog(
    onDismiss: () -> Unit,
    onDonate: (Package) -> Unit,
    offerings: Offerings?,
    purchaseState: PurchaseState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_support_donate))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (purchaseState is PurchaseState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    Text(stringResource(R.string.settings_donate_purchase_loading))
                } else if (purchaseState is PurchaseState.Error) {
                    Text(
                        text = purchaseState.uiText.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    val currentOffering = offerings?.current
                    if (currentOffering != null && currentOffering.availablePackages.isNotEmpty()) {
                        currentOffering.availablePackages.forEach { pkg ->
                            val displayName = if (pkg.product.name.isNotBlank()) pkg.product.name
                                else pkg.product.title.substringBeforeLast(" (").trim()
                            val label = "$displayName (${pkg.product.price.formatted})"

                            val interactionSource = remember { MutableInteractionSource() }
                            Button(
                                onClick = { onDonate(pkg) },
                                modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(interactionSource),
                                interactionSource = interactionSource,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Text(label)
                            }
                        }
                    } else if (offerings != null) {
                        Text(
                            text = stringResource(R.string.settings_donate_no_offers),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_donate_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

/**
 * Einzelner Bullet-Point für den Hilfe-Bereich im SettingsScreen.
 */
@Composable
fun HelpBulletPoint(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            modifier = Modifier.padding(end = 12.dp, top = 2.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = text.replace("<b>", "").replace("</b>", ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
