package de.familienwecker.famwake.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.familienwecker.famwake.R
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val emoji: String,
    val titleRes: Int,
    val bodyRes: Int,
    val gradient: List<Color>
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val slides = listOf(
        OnboardingSlide(
            emoji = "⏰",
            titleRes = R.string.onboarding_slide1_title,
            bodyRes = R.string.onboarding_slide1_body,
            gradient = listOf(Color(0xFF1A237E), Color(0xFF283593))
        ),
        OnboardingSlide(
            emoji = "👨‍👩‍👧‍👦",
            titleRes = R.string.onboarding_slide2_title,
            bodyRes = R.string.onboarding_slide2_body,
            gradient = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
        ),
        OnboardingSlide(
            emoji = "📅",
            titleRes = R.string.onboarding_slide3_title,
            bodyRes = R.string.onboarding_slide3_body,
            gradient = listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))
        ),
        OnboardingSlide(
            emoji = "🚀",
            titleRes = R.string.onboarding_slide4_title,
            bodyRes = R.string.onboarding_slide4_body,
            gradient = listOf(Color(0xFF880E4F), Color(0xFFAD1457))
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == slides.size - 1

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val slide = slides[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(slide.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp)
                        .padding(bottom = 160.dp)
                ) {
                    Text(
                        text = slide.emoji,
                        fontSize = 80.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Text(
                        text = stringResource(slide.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(slide.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
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

            // Next / Get Started button
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1A237E)
                )
            ) {
                Text(
                    text = if (isLastPage) stringResource(R.string.onboarding_done)
                           else stringResource(R.string.onboarding_next),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Skip text (hidden on last page)
            AnimatedVisibility(
                visible = !isLastPage,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                TextButton(onClick = onFinished) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
