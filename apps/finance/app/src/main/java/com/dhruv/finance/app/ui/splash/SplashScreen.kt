package com.dhruv.finance.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.DhruvLogo
import com.dhruv.core.ui.components.DhruvWordmarkImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Top app bar wordmark height — the hero wordmark lands at exactly this size. */
private val TOP_BAR_WORDMARK_HEIGHT = 26.dp

/** Prominent centre size of the wordmark during the splash entrance. */
private val HERO_WORDMARK_HEIGHT = 56.dp

/** Intrinsic aspect ratio (w/h) of `ic_dhruv_wordmark`, mirrored from DhruvBrand. */
private const val WORDMARK_ASPECT = 944f / 256f

/**
 * Animated launch splash with a "hero" hand-off into the app bar.
 *
 * 1. The compass logo spins in (rotate −25°→0°, scale + fade).
 * 2. The "dhruv" wordmark zooms in at the centre.
 * 3. The compass fades while the wordmark **flies to the top-left and shrinks to the app-bar size**
 *    — landing where MainActivity's [DhruvWordmarkImage] sits, so it reads as one continuous mark.
 * 4. The splash fades out and [onFinished] reveals the app underneath.
 *
 * The splash owns its full timeline; the caller just flips a flag in [onFinished].
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    val compassRotation = remember { Animatable(-25f) }
    val compassScale = remember { Animatable(0.8f) }
    val compassAlpha = remember { Animatable(0f) }
    val wordmarkScaleIn = remember { Animatable(0.6f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val handoff = remember { Animatable(0f) } // 0 = centre/hero, 1 = top-left/app-bar
    val containerAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. compass spin-in
        launch { compassAlpha.animateTo(1f, tween(400, easing = LinearOutSlowInEasing)) }
        launch { compassScale.animateTo(1f, tween(600, easing = LinearOutSlowInEasing)) }
        launch { compassRotation.animateTo(0f, tween(600, easing = LinearOutSlowInEasing)) }
        // 2. wordmark zooms in shortly after
        delay(350)
        launch { wordmarkAlpha.animateTo(1f, tween(350)) }
        wordmarkScaleIn.animateTo(1f, tween(450, easing = LinearOutSlowInEasing))
        // brief hold
        delay(450)
        // 3. hand-off: compass fades out, wordmark flies to the top-left
        launch { compassAlpha.animateTo(0f, tween(300)) }
        launch { compassScale.animateTo(0.5f, tween(300)) }
        handoff.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        // 4. fade out and reveal the app
        delay(60)
        containerAlpha.animateTo(0f, tween(220))
        onFinished()
    }

    val scaleDown = TOP_BAR_WORDMARK_HEIGHT.value / HERO_WORDMARK_HEIGHT.value

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = containerAlpha.value }
            .background(Color.Black)
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val smallW = TOP_BAR_WORDMARK_HEIGHT * WORDMARK_ASPECT
        // Centre of the resting top-bar wordmark: ~16dp from the start, vertically centred in the
        // 64dp small TopAppBar that sits just below the status bar.
        val targetCenterX = 16.dp + smallW / 2
        val targetCenterY = statusTop + (64.dp - TOP_BAR_WORDMARK_HEIGHT) / 2 + TOP_BAR_WORDMARK_HEIGHT / 2

        // Compass — upper-centre, spins in then fades during the hand-off.
        DhruvLogo(
            modifier = Modifier
                .align(Alignment.Center)
                .size(140.dp)
                .graphicsLayer {
                    translationY = -(96.dp.toPx())
                    rotationZ = compassRotation.value
                    scaleX = compassScale.value
                    scaleY = compassScale.value
                    alpha = compassAlpha.value
                }
        )

        // Wordmark hero — laid out centred at hero size; transformed to the top-left on hand-off.
        DhruvWordmarkImage(
            height = HERO_WORDMARK_HEIGHT,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val p = handoff.value
                    val s = wordmarkScaleIn.value * (1f - p + p * scaleDown)
                    scaleX = s
                    scaleY = s
                    alpha = wordmarkAlpha.value
                    translationX = (targetCenterX - screenW / 2).toPx() * p
                    translationY = (targetCenterY - screenH / 2).toPx() * p
                }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SplashScreenPreview() {
    SplashScreen()
}
