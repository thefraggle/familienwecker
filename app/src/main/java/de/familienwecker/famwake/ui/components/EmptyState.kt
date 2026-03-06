package de.familienwecker.famwake.ui.components

import com.airbnb.lottie.compose.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    imageRes: Int? = null,
    lottieRes: Int? = null,
    action: (@Composable () -> Unit)? = null
) {
    val composition by rememberLottieComposition(
        spec = when {
            lottieRes != null -> LottieCompositionSpec.RawRes(lottieRes)
            imageRes != null -> LottieCompositionSpec.RawRes(imageRes)
            else -> LottieCompositionSpec.RawRes(0) // Should not be reached
        }
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (lottieRes != null) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(240.dp)
            )
        } else if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(240.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
