package com.example.charlobeats.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.charlobeats.ui.navigation.Screen
import com.example.charlobeats.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    var loadingText by remember { mutableStateOf("Initializing DSP Engine...") }

    LaunchedEffect(Unit) {
        delay(500)
        loadingText = "Loading 8D Audio & Biquad Filters..."
        delay(500)
        loadingText = "Powering Offline AI Audio Models..."
        delay(500)
        loadingText = "Ready to Boost!"
        delay(250)
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas),
        contentAlignment = Alignment.Center
    ) {
        // Glowing animated circular rings
        Canvas(modifier = Modifier.size(240.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PurpleBrand.copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = size.width / 2f
                )
            )
            drawCircle(
                color = CyanNeon.copy(alpha = 0.35f),
                radius = (size.width / 2.2f) * pulseScale,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = PurpleNeon.copy(alpha = 0.3f),
                radius = (size.width / 3.0f) * (2f - pulseScale),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing Purple App Icon Box
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(pulseScale)
                    .background(DarkCardBgElevated, RoundedCornerShape(20.dp))
                    .border(1.5.dp, PurpleBrand, RoundedCornerShape(20.dp))
                    .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = PurpleBrand, spotColor = PurpleNeon),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PurpleNeon,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "BOOM-BOOM",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .background(CyanNeon.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "AUDIO ENHANCER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = CyanNeon
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = loadingText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }
    }
}
