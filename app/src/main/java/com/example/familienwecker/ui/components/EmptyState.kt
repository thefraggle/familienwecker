package com.example.familienwecker.ui.components

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
    imageRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    lottieRes: Int? = null,
    action: (@Composable () -> Unit)? = null
) {
    val composition by rememberLottieComposition(
        spec = if (lottieRes != null) LottieCompositionSpec.RawRes(lottieRes) 
               else LottieCompositionSpec.RawRes(imageRes) // Fallback (should not happen with logic below)
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
        } else {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(240.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
