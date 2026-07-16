package com.rohan.mbtool.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.mbtool.ui.theme.Bg
import com.rohan.mbtool.ui.theme.Muted
import com.rohan.mbtool.ui.theme.Primary
import com.rohan.mbtool.ui.theme.PrimaryBg

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, tween(300))
        kotlinx.coroutines.delay(800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(PrimaryBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Code,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(52.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "MB Tool",
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "RenderDragon Shader Tool",
                fontSize = 13.sp,
                color    = Muted,
            )
        }
    }
}
