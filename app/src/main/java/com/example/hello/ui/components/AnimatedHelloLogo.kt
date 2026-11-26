package com.example.hello.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.MotionTokens

/**
 * Choreography modes for Hello Mark Motion ("Writing it down", ADR-0041).
 */
enum class HelloMarkAnimationMode {
    /** 0.65 s mark-only cold launch: band drops, outline traces, squiggle is jotted. */
    LAUNCH,

    /** 1.9 s horizontal lockup: mark + five hand-drawn "hello" letters for Onboarding & About. */
    WELCOME,

    /** 0.4 s save confirmation: band & outline static, squiggle re-written once inside Toast Pill. */
    SAVED
}

@Immutable
data class HelloMarkTimelineFrame(
    val bandAlpha: Float,
    val bandOffsetYUnits: Float,
    val outlineProgress: Float,
    val outlineAlpha: Float,
    val squiggleProgress: Float,
    val squiggleAlpha: Float,
    val letterProgress: List<Float>,
    val letterAlpha: List<Float>
)

private val ZERO_LETTERS = List(MotionTokens.HelloMarkMotion.WELCOME_LETTER_COUNT) { 0f }
private val FULL_LETTERS = List(MotionTokens.HelloMarkMotion.WELCOME_LETTER_COUNT) { 1f }

/**
 * Returns the total playback duration in milliseconds for the given [mode] and reduced-motion state.
 */
fun helloMarkTotalDurationMs(
    mode: HelloMarkAnimationMode,
    isReducedMotion: Boolean
): Int {
    if (isReducedMotion) return MotionTokens.HelloMarkMotion.REDUCED_MOTION_FADE_MS
    return when (mode) {
        HelloMarkAnimationMode.LAUNCH -> MotionTokens.HelloMarkMotion.LAUNCH_TOTAL_DURATION_MS
        HelloMarkAnimationMode.WELCOME -> MotionTokens.HelloMarkMotion.WELCOME_TOTAL_DURATION_MS
        HelloMarkAnimationMode.SAVED -> MotionTokens.HelloMarkMotion.SAVED_TOTAL_DURATION_MS
    }
}

/**
 * Pure gating rule deciding whether the cold-start App Launch overlay should play (ADR-0041).
 * Skips when already played in this process, on first-run onboarding (which plays Welcome next),
 * on notification deep-links, or under Reduced Motion.
 */
fun shouldPlayLaunchMarkAnimation(
    hasPlayedInProcess: Boolean,
    hasCompletedOnboarding: Boolean,
    hasInitialDeepLink: Boolean,
    isReducedMotion: Boolean
): Boolean {
    return !hasPlayedInProcess &&
        hasCompletedOnboarding &&
        !hasInitialDeepLink &&
        !isReducedMotion
}

/**
 * Pure timeline evaluator computing every element's progress, opacity, and drop offset
 * at [elapsedMs] using [easing] (defaulting to `MotionTokens.BrandMarkEasing`).
 */
fun computeHelloMarkTimelineFrame(
    mode: HelloMarkAnimationMode,
    elapsedMs: Float,
    isReducedMotion: Boolean,
    easing: (Float) -> Float = { MotionTokens.BrandMarkEasing.transform(it) }
): HelloMarkTimelineFrame {
    val tokens = MotionTokens.HelloMarkMotion
    val t = elapsedMs.coerceAtLeast(0f)

    if (isReducedMotion) {
        val fadeProgress = (t / tokens.REDUCED_MOTION_FADE_MS.toFloat()).coerceIn(0f, 1f)
        val hasLetters = mode == HelloMarkAnimationMode.WELCOME
        return HelloMarkTimelineFrame(
            bandAlpha = fadeProgress,
            bandOffsetYUnits = 0f,
            outlineProgress = 1f,
            outlineAlpha = fadeProgress,
            squiggleProgress = 1f,
            squiggleAlpha = fadeProgress,
            letterProgress = if (hasLetters) FULL_LETTERS else ZERO_LETTERS,
            letterAlpha = if (hasLetters) List(tokens.WELCOME_LETTER_COUNT) { fadeProgress } else ZERO_LETTERS
        )
    }

    return when (mode) {
        HelloMarkAnimationMode.SAVED -> {
            val sqRaw = rawSegmentProgress(t, tokens.SAVED_SQUIGGLE_DELAY_MS, tokens.SAVED_SQUIGGLE_DURATION_MS)
            HelloMarkTimelineFrame(
                bandAlpha = 1f,
                bandOffsetYUnits = 0f,
                outlineProgress = 1f,
                outlineAlpha = 1f,
                squiggleProgress = easing(sqRaw),
                squiggleAlpha = strokeRampAlpha(sqRaw),
                letterProgress = ZERO_LETTERS,
                letterAlpha = ZERO_LETTERS
            )
        }

        HelloMarkAnimationMode.LAUNCH -> {
            val bandRaw = rawSegmentProgress(t, tokens.LAUNCH_BAND_DELAY_MS, tokens.LAUNCH_BAND_DURATION_MS)
            val bandEased = easing(bandRaw)
            val outRaw = rawSegmentProgress(t, tokens.LAUNCH_OUTLINE_DELAY_MS, tokens.LAUNCH_OUTLINE_DURATION_MS)
            val sqRaw = rawSegmentProgress(t, tokens.LAUNCH_SQUIGGLE_DELAY_MS, tokens.LAUNCH_SQUIGGLE_DURATION_MS)
            HelloMarkTimelineFrame(
                bandAlpha = bandEased,
                bandOffsetYUnits = -tokens.BAND_DROP_OFFSET_Y_UNITS * (1f - bandEased),
                outlineProgress = easing(outRaw),
                outlineAlpha = strokeRampAlpha(outRaw),
                squiggleProgress = easing(sqRaw),
                squiggleAlpha = strokeRampAlpha(sqRaw),
                letterProgress = ZERO_LETTERS,
                letterAlpha = ZERO_LETTERS
            )
        }

        HelloMarkAnimationMode.WELCOME -> {
            val bandRaw = rawSegmentProgress(t, tokens.WELCOME_BAND_DELAY_MS, tokens.WELCOME_BAND_DURATION_MS)
            val bandEased = easing(bandRaw)
            val outRaw = rawSegmentProgress(t, tokens.WELCOME_OUTLINE_DELAY_MS, tokens.WELCOME_OUTLINE_DURATION_MS)
            val sqRaw = rawSegmentProgress(t, tokens.WELCOME_SQUIGGLE_DELAY_MS, tokens.WELCOME_SQUIGGLE_DURATION_MS)
            val letterProgress = List(tokens.WELCOME_LETTER_COUNT) { index ->
                val delay = tokens.WELCOME_LETTER_BASE_DELAY_MS + index * tokens.WELCOME_LETTER_STAGGER_MS
                easing(rawSegmentProgress(t, delay, tokens.WELCOME_LETTER_DURATION_MS))
            }
            val letterAlpha = List(tokens.WELCOME_LETTER_COUNT) { index ->
                val delay = tokens.WELCOME_LETTER_BASE_DELAY_MS + index * tokens.WELCOME_LETTER_STAGGER_MS
                strokeRampAlpha(rawSegmentProgress(t, delay, tokens.WELCOME_LETTER_DURATION_MS))
            }
            HelloMarkTimelineFrame(
                bandAlpha = bandEased,
                bandOffsetYUnits = -tokens.BAND_DROP_OFFSET_Y_UNITS * (1f - bandEased),
                outlineProgress = easing(outRaw),
                outlineAlpha = strokeRampAlpha(outRaw),
                squiggleProgress = easing(sqRaw),
                squiggleAlpha = strokeRampAlpha(sqRaw),
                letterProgress = letterProgress,
                letterAlpha = letterAlpha
            )
        }
    }
}

private fun rawSegmentProgress(elapsedMs: Float, delayMs: Int, durationMs: Int): Float {
    if (durationMs <= 0) return if (elapsedMs >= delayMs) 1f else 0f
    return ((elapsedMs - delayMs) / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun strokeRampAlpha(rawProgress: Float): Float {
    if (rawProgress <= 0f) return 0f
    return (rawProgress / MotionTokens.HelloMarkMotion.STROKE_ALPHA_RAMP_FRACTION).coerceIn(0f, 1f)
}

/**
 * Pre-parsed vector geometry and cached [PathMeasure] instances matching `ic_hello_mark.xml`,
 * `ic_hello_mark_small.xml`, and `ic_hello_horizontal.xml`.
 */
private object HelloMarkGeometry {
    // Standard mark cut (>= 32dp, Launch & Welcome)
    const val BAND_STANDARD =
        "M50,52 H206 A22,22 0 0 1 228,74 V112 H28 V74 A22,22 0 0 1 50,52 Z " +
            "M114,72 H142 A6,6 0 0 1 142,84 H114 A6,6 0 0 1 114,72 Z"
    const val OUTLINE_STANDARD =
        "M34,92 V176 A22,22 0 0 0 56,198 H200 A22,22 0 0 0 222,176 V92"
    const val SQUIGGLE_STANDARD =
        "M64,160 C80,138 92,178 108,156 S134,140 148,160 S176,150 192,154"
    const val OUTLINE_STANDARD_STROKE = 12f
    const val SQUIGGLE_STANDARD_STROKE = 11f

    // Small mark cut (< 32dp, Toast Pill save confirmation)
    const val BAND_SMALL =
        "M50,48 H206 A26,26 0 0 1 232,74 V116 H24 V74 A26,26 0 0 1 50,48 Z"
    const val OUTLINE_SMALL =
        "M34,96 V172 A26,26 0 0 0 60,198 H196 A26,26 0 0 0 222,172 V96"
    const val SQUIGGLE_SMALL =
        "M62,164 C82,142 96,176 116,156 S148,146 166,160 S182,154 194,156"
    const val OUTLINE_SMALL_STROKE = 20f
    const val SQUIGGLE_SMALL_STROKE = 18f

    // Wordmark ("hello") — letter 'h' split into stem + arch contours for clean PathMeasure tracing
    const val WORDMARK_STROKE = 18f
    const val WORDMARK_TRANSLATE_X = 256f
    const val WORDMARK_TRANSLATE_Y = 16f

    data class LetterSpec(
        val contours: List<String>,
        val rotationDegrees: Float,
        val pivotX: Float,
        val pivotY: Float
    )

    val WORDMARK_LETTERS = listOf(
        LetterSpec(
            contours = listOf(
                "M10,34 Q14,112 9,190",
                "M11.2,142 A40,50 0 0 1 91.2,142 Q93,168 90,190"
            ),
            rotationDegrees = -1.5f,
            pivotX = 50f,
            pivotY = 112f
        ),
        LetterSpec(
            contours = listOf("M138,142 Q182,136 226,140 A44,50 0 1 0 210,178"),
            rotationDegrees = 2f,
            pivotX = 182f,
            pivotY = 140f
        ),
        LetterSpec(
            contours = listOf("M274,34 Q279,112 273,190"),
            rotationDegrees = -1f,
            pivotX = 274f,
            pivotY = 112f
        ),
        LetterSpec(
            contours = listOf("M328,36 Q323,112 329,190"),
            rotationDegrees = 1.5f,
            pivotX = 328f,
            pivotY = 112f
        ),
        LetterSpec(
            contours = listOf("M426,90 C456,88 478,112 476,142 C474,172 452,192 424,190 C396,188 376,166 377,138 C378,112 398,92 428,90"),
            rotationDegrees = -2f,
            pivotX = 426f,
            pivotY = 140f
        )
    )
}

private class MeasuredStroke(pathData: String) {
    val path: Path = PathParser().parsePathString(pathData).toPath()
    val measure: PathMeasure = PathMeasure().apply { setPath(path, forceClosed = false) }
    val length: Float = measure.length
}

private class MeasuredLetter(
    val strokes: List<MeasuredStroke>,
    val rotationDegrees: Float,
    val pivotX: Float,
    val pivotY: Float
) {
    val totalLength: Float = strokes.sumOf { it.length.toDouble() }.toFloat().coerceAtLeast(1f)
}

private class HelloMarkDrawCache(useSmallCut: Boolean, includeWordmark: Boolean) {
    val bandPath: Path = PathParser()
        .parsePathString(if (useSmallCut) HelloMarkGeometry.BAND_SMALL else HelloMarkGeometry.BAND_STANDARD)
        .toPath()
        .apply {
            if (!useSmallCut) fillType = PathFillType.EvenOdd
        }
    val outlineStroke = MeasuredStroke(
        if (useSmallCut) HelloMarkGeometry.OUTLINE_SMALL else HelloMarkGeometry.OUTLINE_STANDARD
    )
    val squiggleStroke = MeasuredStroke(
        if (useSmallCut) HelloMarkGeometry.SQUIGGLE_SMALL else HelloMarkGeometry.SQUIGGLE_STANDARD
    )
    val outlineStrokeWidth = if (useSmallCut) HelloMarkGeometry.OUTLINE_SMALL_STROKE else HelloMarkGeometry.OUTLINE_STANDARD_STROKE
    val squiggleStrokeWidth = if (useSmallCut) HelloMarkGeometry.SQUIGGLE_SMALL_STROKE else HelloMarkGeometry.SQUIGGLE_STANDARD_STROKE
    val letters: List<MeasuredLetter> = if (includeWordmark) {
        HelloMarkGeometry.WORDMARK_LETTERS.map { spec ->
            MeasuredLetter(
                strokes = spec.contours.map(::MeasuredStroke),
                rotationDegrees = spec.rotationDegrees,
                pivotX = spec.pivotX,
                pivotY = spec.pivotY
            )
        }
    } else {
        emptyList()
    }
    val scratchPath = Path()
}

/**
 * Animated Hello name-tag mark rendered via Compose Canvas `PathMeasure` (ADR-0041).
 * Automatically uses the thicker small-size cut when [width] < `32.dp` or in [HelloMarkAnimationMode.SAVED].
 */
@Composable
fun AnimatedHelloMark(
    mode: HelloMarkAnimationMode,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = "Hello",
    playKey: Any = Unit,
    isReducedMotion: Boolean = LocalReducedMotion.current,
    onFinished: () -> Unit = {}
) {
    val clampedWidth = width.coerceAtLeast(16.dp)
    val useSmallCut = clampedWidth < 32.dp || mode == HelloMarkAnimationMode.SAVED
    val cache = remember(useSmallCut) { HelloMarkDrawCache(useSmallCut = useSmallCut, includeWordmark = false) }
    val elapsedMs = remember { Animatable(0f) }
    val latestOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(mode, playKey, isReducedMotion) {
        val totalMs = helloMarkTotalDurationMs(mode = mode, isReducedMotion = isReducedMotion)
        elapsedMs.snapTo(0f)
        if (totalMs > 0) {
            elapsedMs.animateTo(
                targetValue = totalMs.toFloat(),
                animationSpec = tween(durationMillis = totalMs, easing = LinearEasing)
            )
        }
        latestOnFinished()
    }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }

    val sizingModifier = if (useSmallCut) {
        modifier.size(clampedWidth)
    } else {
        modifier
            .width(clampedWidth)
            .aspectRatio(200f / 176f)
    }

    Canvas(modifier = sizingModifier.then(semanticsModifier)) {
        val frame = computeHelloMarkTimelineFrame(
            mode = mode,
            elapsedMs = elapsedMs.value,
            isReducedMotion = isReducedMotion
        )
        if (useSmallCut) {
            drawInViewBox(vbX = 0f, vbY = 0f, vbWidth = 256f, vbHeight = 256f) {
                drawMarkPaths(cache = cache, frame = frame, color = color, markTranslateX = 0f)
            }
        } else {
            drawInViewBox(vbX = 12f, vbY = 40f, vbWidth = 200f, vbHeight = 176f) {
                drawMarkPaths(cache = cache, frame = frame, color = color, markTranslateX = -16f)
            }
        }
    }
}

/**
 * Animated horizontal Hello lockup (name-tag mark + five hand-drawn "hello" letters)
 * for Onboarding Welcome and About Hello screens (ADR-0041).
 */
@Composable
fun AnimatedHelloHorizontalLogo(
    modifier: Modifier = Modifier,
    width: Dp = 180.dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = "Hello",
    playKey: Any = Unit,
    isReducedMotion: Boolean = LocalReducedMotion.current,
    onFinished: () -> Unit = {}
) {
    val clampedWidth = width.coerceAtLeast(96.dp)
    val cache = remember { HelloMarkDrawCache(useSmallCut = false, includeWordmark = true) }
    val elapsedMs = remember { Animatable(0f) }
    val latestOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(playKey, isReducedMotion) {
        val totalMs = helloMarkTotalDurationMs(
            mode = HelloMarkAnimationMode.WELCOME,
            isReducedMotion = isReducedMotion
        )
        elapsedMs.snapTo(0f)
        if (totalMs > 0) {
            elapsedMs.animateTo(
                targetValue = totalMs.toFloat(),
                animationSpec = tween(durationMillis = totalMs, easing = LinearEasing)
            )
        }
        latestOnFinished()
    }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .width(clampedWidth)
            .aspectRatio(760f / 256f)
            .then(semanticsModifier)
    ) {
        val frame = computeHelloMarkTimelineFrame(
            mode = HelloMarkAnimationMode.WELCOME,
            elapsedMs = elapsedMs.value,
            isReducedMotion = isReducedMotion
        )
        drawInViewBox(vbX = 0f, vbY = 0f, vbWidth = 760f, vbHeight = 256f) {
            drawMarkPaths(cache = cache, frame = frame, color = color, markTranslateX = -16f)
            withTransform({
                translate(
                    left = HelloMarkGeometry.WORDMARK_TRANSLATE_X,
                    top = HelloMarkGeometry.WORDMARK_TRANSLATE_Y
                )
            }) {
                cache.letters.forEachIndexed { index, letter ->
                    val progress = frame.letterProgress.getOrElse(index) { 0f }
                    val alpha = frame.letterAlpha.getOrElse(index) { 0f }
                    if (alpha > 0f && progress > 0f) {
                        withTransform({
                            rotate(
                                degrees = letter.rotationDegrees,
                                pivot = Offset(letter.pivotX, letter.pivotY)
                            )
                        }) {
                            drawMeasuredLetter(
                                letter = letter,
                                progress = progress,
                                color = color.copy(alpha = color.alpha * alpha),
                                strokeWidth = HelloMarkGeometry.WORDMARK_STROKE,
                                scratchPath = cache.scratchPath
                            )
                        }
                    }
                }
            }
        }
    }
}

private inline fun DrawScope.drawInViewBox(
    vbX: Float,
    vbY: Float,
    vbWidth: Float,
    vbHeight: Float,
    block: DrawScope.() -> Unit
) {
    if (vbWidth <= 0f || vbHeight <= 0f || size.width <= 0f || size.height <= 0f) return
    val uniformScale = minOf(size.width / vbWidth, size.height / vbHeight)
    val offsetX = (size.width - vbWidth * uniformScale) / 2f
    val offsetY = (size.height - vbHeight * uniformScale) / 2f
    withTransform({
        translate(left = offsetX, top = offsetY)
        scale(scaleX = uniformScale, scaleY = uniformScale, pivot = Offset.Zero)
        translate(left = -vbX, top = -vbY)
    }) {
        block()
    }
}

private fun DrawScope.drawMarkPaths(
    cache: HelloMarkDrawCache,
    frame: HelloMarkTimelineFrame,
    color: Color,
    markTranslateX: Float
) {
    withTransform({
        translate(left = markTranslateX, top = 0f)
    }) {
        if (frame.bandAlpha > 0f) {
            withTransform({
                translate(left = 0f, top = frame.bandOffsetYUnits)
            }) {
                drawPath(
                    path = cache.bandPath,
                    color = color.copy(alpha = color.alpha * frame.bandAlpha)
                )
            }
        }

        if (frame.outlineAlpha > 0f && frame.outlineProgress > 0f) {
            drawPartialStroke(
                stroke = cache.outlineStroke,
                progress = frame.outlineProgress,
                color = color.copy(alpha = color.alpha * frame.outlineAlpha),
                strokeWidth = cache.outlineStrokeWidth,
                scratchPath = cache.scratchPath
            )
        }

        if (frame.squiggleAlpha > 0f && frame.squiggleProgress > 0f) {
            drawPartialStroke(
                stroke = cache.squiggleStroke,
                progress = frame.squiggleProgress,
                color = color.copy(alpha = color.alpha * frame.squiggleAlpha),
                strokeWidth = cache.squiggleStrokeWidth,
                scratchPath = cache.scratchPath
            )
        }
    }
}

private fun DrawScope.drawPartialStroke(
    stroke: MeasuredStroke,
    progress: Float,
    color: Color,
    strokeWidth: Float,
    scratchPath: Path
) {
    val clamped = progress.coerceIn(0f, 1f)
    if (clamped <= 0f || stroke.length <= 0f) return
    val style = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    if (clamped >= 0.999f) {
        drawPath(path = stroke.path, color = color, style = style)
        return
    }
    scratchPath.reset()
    stroke.measure.getSegment(
        startDistance = 0f,
        stopDistance = stroke.length * clamped,
        destination = scratchPath,
        startWithMoveTo = true
    )
    drawPath(path = scratchPath, color = color, style = style)
}

private fun DrawScope.drawMeasuredLetter(
    letter: MeasuredLetter,
    progress: Float,
    color: Color,
    strokeWidth: Float,
    scratchPath: Path
) {
    val clamped = progress.coerceIn(0f, 1f)
    if (clamped <= 0f) return
    val targetDistance = letter.totalLength * clamped
    var consumed = 0f
    for (stroke in letter.strokes) {
        val remaining = targetDistance - consumed
        if (remaining <= 0f) break
        val strokeFraction = if (stroke.length > 0f) {
            (remaining / stroke.length).coerceIn(0f, 1f)
        } else {
            1f
        }
        drawPartialStroke(
            stroke = stroke,
            progress = strokeFraction,
            color = color,
            strokeWidth = strokeWidth,
            scratchPath = scratchPath
        )
        consumed += stroke.length
    }
}

@Preview(name = "AnimatedHelloHorizontalLogo Light", showBackground = true)
@Composable
private fun AnimatedHelloHorizontalLogoLightPreview() {
    HelloTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            AnimatedHelloHorizontalLogo(width = 200.dp, isReducedMotion = true)
        }
    }
}

@Preview(name = "AnimatedHelloHorizontalLogo Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnimatedHelloHorizontalLogoDarkPreview() {
    HelloTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            AnimatedHelloHorizontalLogo(width = 200.dp, isReducedMotion = true)
        }
    }
}

@Preview(name = "AnimatedHelloMark Saved Small", showBackground = true)
@Composable
private fun AnimatedHelloMarkSavedPreview() {
    HelloTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .padding(12.dp)
        ) {
            AnimatedHelloMark(
                mode = HelloMarkAnimationMode.SAVED,
                width = 22.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                isReducedMotion = true
            )
        }
    }
}
