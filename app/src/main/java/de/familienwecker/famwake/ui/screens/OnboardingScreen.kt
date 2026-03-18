package de.familienwecker.famwake.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.familienwecker.famwake.R
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val titleRes: Int,
    val bodyRes: Int,
    val gradient: List<Color>,
    @DrawableRes val imageRes: Int,                    // DE image
    @DrawableRes val imageResEn: Int = imageRes        // EN image (falls abweichend)
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    language: String,       // from PreferencesRepository: "de", "en", "system"
    onFinished: () -> Unit
) {
    // "de" und "system" (Deutsch als Standard) → DE-Screenshots, sonst EN
    val isEnglish = language == "en"

    val slides = listOf(
        OnboardingSlide(
            titleRes   = R.string.onboarding_slide1_title,
            bodyRes    = R.string.onboarding_slide1_body,
            gradient   = listOf(Color(0xFF1A237E), Color(0xFF283593)),
            imageRes   = R.drawable.onboarding_slide1   // DE & EN identical
        ),
        OnboardingSlide(
            titleRes   = R.string.onboarding_slide2_title,
            bodyRes    = R.string.onboarding_slide2_body,
            gradient   = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)),
            imageRes   = R.drawable.onboarding_slide2_de,
            imageResEn = R.drawable.onboarding_slide2_en
        ),
        OnboardingSlide(
            titleRes   = R.string.onboarding_slide3_title,
            bodyRes    = R.string.onboarding_slide3_body,
            gradient   = listOf(Color(0xFF4A148C), Color(0xFF6A1B9A)),
            imageRes   = R.drawable.onboarding_slide3_de,
            imageResEn = R.drawable.onboarding_slide3_en
        ),
        OnboardingSlide(
            titleRes   = R.string.onboarding_slide4_title,
            bodyRes    = R.string.onboarding_slide4_body,
            gradient   = listOf(Color(0xFF880E4F), Color(0xFFAD1457)),
            imageRes   = R.drawable.onboarding_slide4   // DE & EN identical
        )
    )

    val pagerState    = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage    = pagerState.currentPage == slides.size - 1

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val slide    = slides[page]
            val imageRes = if (isEnglish) slide.imageResEn else slide.imageRes

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(slide.gradient))
            ) {
                Column(
                    horizontalAlignment  = Alignment.CenterHorizontally,
                    verticalArrangement  = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .padding(top = 56.dp, bottom = 180.dp)
                ) {
                    // Floating screenshot card
                    androidx.compose.foundation.Image(
                        painter            = painterResource(imageRes),
                        contentDescription = null,
                        contentScale       = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation       = 24.dp,
                                shape           = RoundedCornerShape(20.dp),
                                ambientColor    = Color.Black.copy(alpha = 0.6f),
                                spotColor       = Color.Black.copy(alpha = 0.6f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text      = stringResource(slide.titleRes),
                        style     = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color     = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text       = stringResource(slide.bodyRes),
                        style      = MaterialTheme.typography.bodyLarge,
                        color      = Color.White.copy(alpha = 0.85f),
                        textAlign  = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(slides.size) { index ->
                    val isActive = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                            .size(if (isActive) 10.dp else 7.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = Color(0xFF1A237E)
                )
            ) {
                Text(
                    text       = if (isLastPage) stringResource(R.string.onboarding_done)
                                 else stringResource(R.string.onboarding_next),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }

            AnimatedVisibility(
                visible = !isLastPage,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(200))
            ) {
                TextButton(onClick = onFinished) {
                    Text(
                        text     = stringResource(R.string.onboarding_skip),
                        color    = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
