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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.familienwecker.famwake.ui.theme.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import de.familienwecker.famwake.R
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val titleRes: Int,
    val bodyRes: Int,
    val lottieRes: Int? = null,                        // Lottie-Animation (Slide 0)
    val mockupContent: (@Composable () -> Unit)? = null // Compose-Mockup (Slides 1–4)
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    language: String,       // from PreferencesRepository: "de", "en", "system"
    startAtWelcome: Boolean = false,
    onStartAnonymously: () -> Unit,
    onLogin: () -> Unit
) {
    val slides = listOf(
        // Slide 0 – emotionale Einstiegs-Slide mit Panda-Lottie
        OnboardingSlide(
            titleRes  = R.string.onboarding_slide0_title,
            bodyRes   = R.string.onboarding_slide0_body,
            lottieRes = R.raw.panda
        ),
        // Slides 1–4: Compose-Mockups → automatisch lokalisiert, keine PNG-Wartung nötig
        // Reihenfolge: Zeitplan → Wochentage → Invite → CTA (Nutzen-orientiert)
        OnboardingSlide(
            titleRes      = R.string.onboarding_slide1_title,
            bodyRes       = R.string.onboarding_slide1_body,
            mockupContent = { Slide1ScheduleMockup() }
        ),
        OnboardingSlide(
            titleRes      = R.string.onboarding_slide2_title,
            bodyRes       = R.string.onboarding_slide2_body,
            mockupContent = { Slide3DaySettingsMockup() }
        ),
        OnboardingSlide(
            titleRes      = R.string.onboarding_slide3_title,
            bodyRes       = R.string.onboarding_slide3_body,
            mockupContent = { Slide2InviteMockup() }
        ),
        OnboardingSlide(
            titleRes      = R.string.onboarding_slide4_title,
            bodyRes       = R.string.onboarding_slide4_body,
            mockupContent = { Slide4ReliableMockup() }
        ),
        OnboardingSlide(
            titleRes      = R.string.onboarding_slide5_title,
            bodyRes       = R.string.onboarding_slide5_body,
            lottieRes     = R.raw.wakeup
        )
    )

    val initialPage   = if (startAtWelcome) slides.size - 1 else 0
    val pagerState    = rememberPagerState(initialPage = initialPage, pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage    = pagerState.currentPage == slides.size - 1
    var isStarting    by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Globaler Hintergrund (onboarding_bg.png)
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.onboarding_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Scrim für bessere Lesbarkeit des weißen Textes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val slide = slides[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    horizontalAlignment  = Alignment.CenterHorizontally,
                    verticalArrangement  = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .padding(top = 56.dp, bottom = 180.dp)
                ) {
                    when {
                        slide.mockupContent != null -> {
                            // Compose-nativer Mockup: vollständig lokalisiert, kein PNG nötig
                            slide.mockupContent.invoke()
                        }
                        slide.lottieRes != null -> {
                            // Lottie-Animation (Panda auf Slide 0)
                            val composition by rememberLottieComposition(
                                LottieCompositionSpec.RawRes(slide.lottieRes)
                            )
                            LottieAnimation(
                                composition = composition,
                                iterations  = LottieConstants.IterateForever,
                                speed       = 0.7f,
                                modifier    = Modifier
                                    .sizeIn(maxWidth = 280.dp, maxHeight = 280.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }
                    }

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
                        if (!isStarting) {
                            isStarting = true
                            onStartAnonymously()
                        }
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
                enabled = !isStarting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = Color(0xFF1A237E),
                    disabledContainerColor = Color.White.copy(alpha = 0.5f),
                    disabledContentColor = Color(0xFF1A237E).copy(alpha = 0.5f)
                )
            ) {
                if (isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1A237E),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text       = if (isLastPage) stringResource(R.string.onboarding_done)
                                     else stringResource(R.string.onboarding_next),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            }

            AnimatedVisibility(
                visible = isLastPage,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(200))
            ) {
                TextButton(onClick = onLogin) {
                    Text(
                        text     = stringResource(R.string.onboarding_login_create),
                        color    = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = !isLastPage,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(200))
            ) {
                TextButton(onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(slides.size - 1)
                    }
                }) {
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
