package de.familienwecker.famwake.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.viewmodel.PurchaseState

data class LangEntry(val code: String, val label: String)

data class ThemeOption(val code: String, val icon: ImageVector, val description: String)

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
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text(stringResource(R.string.settings_donate_loading))
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

/**
 * Universelle Warn-Karte für Berechtigungen (z. B. Akku, Exakter Alarm, Fullscreen).
 */
@Composable
fun PermissionWarningCard(
    title: String,
    body: String,
    buttonLabel: String,
    isDarkTheme: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            val interactionSource = remember { MutableInteractionSource() }
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().bounceClick(interactionSource),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(buttonLabel) }
        }
    }
}

/**
 * Footer mit Version, Copyright und Links zu Nutzungsbedingungen / Datenschutz / Impressum.
 */
@Composable
fun SettingsLegalFooter(
    context: Context,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "FamWake ${stringResource(R.string.app_name_short)} v${de.familienwecker.famwake.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.settings_footer_copyright),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_terms_of_use),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val url = context.getString(R.string.settings_terms_of_use_url)
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    } catch (_: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, context.getString(R.string.error_no_app_found), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Text(
                text = stringResource(R.string.settings_privacy_policy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val url = context.getString(R.string.settings_privacy_policy_url)
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    } catch (_: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, context.getString(R.string.error_no_app_found), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Text(
                text = stringResource(R.string.settings_imprint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val url = context.getString(R.string.settings_imprint_url)
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    } catch (_: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, context.getString(R.string.error_no_app_found), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
