package com.shizuku.filemanager.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class DustParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val velocityX: Float,
    val velocityY: Float
)

@Composable
fun CarProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "CarProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "CarMotion")
    
    // Bumpy road effect
    val verticalOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BumpyMotion"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CarRotation"
    )

    var particles by remember { mutableStateOf(listOf<DustParticle>()) }
    var lastProgress by remember { mutableFloatStateOf(0f) }
    var particleIdCounter by remember { mutableLongStateOf(0L) }

    val density = LocalDensity.current

    LaunchedEffect(animatedProgress) {
        if (animatedProgress > lastProgress || (animatedProgress > 0 && animatedProgress < 1)) {
            // Spawn particles behind the car
            val newParticles = List(2) {
                DustParticle(
                    id = particleIdCounter++,
                    x = animatedProgress,
                    y = 0.8f,
                    size = Random.nextFloat() * 8f + 4f,
                    alpha = 0.8f,
                    velocityX = -(Random.nextFloat() * 0.015f + 0.005f),
                    velocityY = (Random.nextFloat() - 0.7f) * 0.01f
                )
            }
            particles = (particles + newParticles).takeLast(40)
            lastProgress = animatedProgress
        }
    }

    // Particle update loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            particles = particles.mapNotNull { p ->
                if (p.alpha <= 0.05f) null
                else p.copy(
                    x = p.x + p.velocityX,
                    y = p.y + p.velocityY,
                    alpha = p.alpha * 0.96f,
                    size = p.size * 0.99f
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.height(80.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val roadY = height * 0.9f
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Road line
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, roadY),
                end = Offset(width, roadY),
                strokeWidth = 2f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
            )
            
            // Particles
            particles.forEach { p ->
                drawCircle(
                    color = Color.Gray.copy(alpha = p.alpha * 0.4f),
                    radius = p.size.dp.toPx(),
                    center = Offset(p.x * width, roadY + p.velocityY * height)
                )
            }
        }

        val carSize = 32.dp
        val carSizePx = with(density) { carSize.toPx() }
        val xPos = animatedProgress * (width - carSizePx)

        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(carSize)
                .graphicsLayer {
                    translationX = xPos
                    translationY = (roadY - carSizePx) + verticalOffset.dp.toPx()
                    rotationZ = rotation
                }
        )
    }
}
