package de.familienwecker.famwake.ui.screens


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.aptabase.Aptabase
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    val mockupContent: (@Composable () -> Unit)? = null // Compose-Mockup (Slides 1–2)
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    language: String,       // from PreferencesRepository: "de", "en", "system"
    startAtWelcome: Boolean = false,
    initialTooltipsEnabled: Boolean = true,
    isLoggedIn: Boolean = false,
    onStartAnonymously: (Boolean) -> Unit,
    onLogin: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val notifPermission = Manifest.permission.POST_NOTIFICATIONS
    var isNotifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, notifPermission) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotifGranted = granted
        Aptabase.instance.trackEvent(
            "permission_notification_result",
            mapOf("granted" to granted)
        )
    }

    val slides = listOf(
        // Slide 0 – emotionale Einstiegs-Slide mit Panda-Lottie
        OnboardingSlide(
            titleRes  = R.string.onboarding_slide0_title,
            bodyRes   = R.string.onboarding_slide0_body,
            lottieRes = R.raw.panda
        ),
        // Slide 1: Visueller Zeitplan
        OnboardingSlide(
            titleRes      = R.string.onboarding_slide1_title,
            bodyRes       = R.string.onboarding_slide1_body,
            mockupContent = { Slide1ScheduleMockup() }
        ),
        // Slide 2: Berechtigungen & Start
        OnboardingSlide(
            titleRes      = R.string.onboarding_slide5_title,
            bodyRes       = R.string.onboarding_slide5_body,
            mockupContent = {
                SlidePermissionMockup(
                    isGranted = isNotifGranted,
                    onRequestPermission = {
                        Aptabase.instance.trackEvent("permission_notification_requested")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(notifPermission)
                        } else {
                            isNotifGranted = true
                        }
                    }
                )
            }
        )
    ).let { list ->
        if (isLoggedIn) list.dropLast(1) else list
    }

    val initialPage   = if (startAtWelcome) slides.size - 1 else 0
    val pagerState    = rememberPagerState(initialPage = initialPage, pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage    = pagerState.currentPage == slides.size - 1
    var isStarting    by remember { mutableStateOf(false) }
    var tooltipsEnabled by remember { mutableStateOf(initialTooltipsEnabled) }

    LaunchedEffect(Unit) {
        if (!startAtWelcome) {
            Aptabase.instance.trackEvent("onboarding_started")
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        Aptabase.instance.trackEvent("onboarding_slide_viewed", mapOf("slide" to pagerState.currentPage))
    }

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
                        .padding(top = 64.dp, bottom = 180.dp)
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

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text      = stringResource(slide.titleRes),
                        style     = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color     = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text       = stringResource(slide.bodyRes),
                        style      = MaterialTheme.typography.bodyLarge,
                        color      = Color.White.copy(alpha = 0.85f),
                        textAlign  = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Top Bar: Login Button für Bestandskunden auf allen Slides
        if (!isLoggedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        Aptabase.instance.trackEvent("onboarding_login_clicked")
                        onLogin(tooltipsEnabled)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(
                        text = stringResource(R.string.login_button),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
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

            AnimatedVisibility(
                visible = isLastPage,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(200))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { tooltipsEnabled = !tooltipsEnabled }.padding(horizontal = 8.dp)
                ) {
                    Checkbox(
                        checked = tooltipsEnabled,
                        onCheckedChange = { tooltipsEnabled = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            checkmarkColor = Color(0xFF1A237E),
                            uncheckedColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = stringResource(R.string.onboarding_enable_tooltips),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Button(
                onClick = {
                    if (isLastPage) {
                        if (!isStarting) {
                            isStarting = true
                            onStartAnonymously(tooltipsEnabled)
                        }
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
                        text       = if (isLastPage) {
                                         if (isLoggedIn) stringResource(R.string.close_desc)
                                         else stringResource(R.string.onboarding_done)
                                     } else stringResource(R.string.onboarding_next),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            }

            AnimatedVisibility(
                visible = isLastPage && !isLoggedIn,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(200))
            ) {
                TextButton(onClick = {
                    Aptabase.instance.trackEvent("onboarding_login_clicked")
                    onLogin(tooltipsEnabled)
                }) {
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
                    Aptabase.instance.trackEvent("onboarding_skipped", mapOf("from_slide" to pagerState.currentPage))
                    if (!isStarting) {
                        isStarting = true
                        onStartAnonymously(tooltipsEnabled)
                    }
                }) {
                    Text(
                        text     = stringResource(R.string.onboarding_skip),
                        color    = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
