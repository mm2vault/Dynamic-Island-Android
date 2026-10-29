package com.mm2vault.dynamicisland.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mm2vault.dynamicisland.R

/** Hero preview. Uses clearly labelled sample values (84 %, charging), never real data. */
@Composable
fun IslandPreview(modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow),
        label = "expand"
    )
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val measurer = rememberTextMeasurer()
    val cd = stringResource(R.string.preview_hint)
    val label = stringResource(R.string.preview_sample_status)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .clipToBounds()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
            .semantics { contentDescription = cd }
    ) {
        val maxW = maxWidth
        val density = LocalDensity.current
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val collapsedW = 126.dp.toPx()
            val expandedW = minOf(maxW.toPx() - 24.dp.toPx(), 300.dp.toPx())
            val w = collapsedW + (expandedW - collapsedW) * progress
            val h = 36.dp.toPx() + (84.dp.toPx() - 36.dp.toPx()) * progress
            val radius = (h / 2f).coerceAtMost(20.dp.toPx() + (1f - progress) * 20.dp.toPx())
            val topLeft = Offset((size.width - w) / 2f, 26.dp.toPx())
            drawGlow(topLeft, w, h, radius, breathe)
            drawRoundRect(Color(0xFF030206), topLeft, Size(w, h), CornerRadius(radius))
            drawRoundRect(
                Brush.linearGradient(listOf(Palette.Neon.copy(alpha = 0.9f), Palette.Violet.copy(alpha = 0.25f), Palette.Lavender.copy(alpha = 0.6f)), topLeft, Offset(topLeft.x + w, topLeft.y + h)),
                topLeft, Size(w, h), CornerRadius(radius), style = Stroke(1.2.dp.toPx())
            )
            drawContent(measurer, topLeft, w, h, progress, label)
        }
    }
}

private fun DrawScope.drawGlow(tl: Offset, w: Float, h: Float, r: Float, breathe: Float) {
    for (i in 1..5) {
        val grow = i * 4.dp.toPx()
        drawRoundRect(
            Palette.Neon.copy(alpha = 0.075f * breathe / i * 2f),
            Offset(tl.x - grow, tl.y - grow), Size(w + grow * 2, h + grow * 2), CornerRadius(r + grow),
            style = Stroke(4.dp.toPx())
        )
    }
}

private fun DrawScope.drawContent(m: TextMeasurer, tl: Offset, w: Float, h: Float, p: Float, label: String) {
    val cy = tl.y + h / 2f
    val cAlpha = (1f - p * 2f).coerceIn(0f, 1f)
    val eAlpha = (p * 2f - 1f).coerceIn(0f, 1f)
    if (cAlpha > 0f) {
        drawCircle(Palette.Lavender.copy(alpha = cAlpha), 3.dp.toPx(), Offset(tl.x + 17.dp.toPx(), cy))
        val t = m.measure("84%", TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Palette.Text.copy(alpha = cAlpha)))
        drawText(t, topLeft = Offset(tl.x + w - 14.dp.toPx() - t.size.width, cy - t.size.height / 2f))
    }
    if (eAlpha > 0f) {
        val rr = 19.dp.toPx()
        val c = Offset(tl.x + 16.dp.toPx() + rr, cy)
        val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
        drawArc(Palette.Neon.copy(alpha = 0.22f * eAlpha), 0f, 360f, false, Offset(c.x - rr, c.y - rr), Size(rr * 2, rr * 2), style = stroke)
        drawArc(Palette.Neon.copy(alpha = eAlpha), -90f, 360f * 0.84f, false, Offset(c.x - rr, c.y - rr), Size(rr * 2, rr * 2), style = stroke)
        val big = m.measure("84%", TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Palette.Text.copy(alpha = eAlpha)))
        val small = m.measure(label, TextStyle(fontSize = 12.sp, color = Palette.TextDim.copy(alpha = eAlpha)))
        val x = c.x + rr + 14.dp.toPx()
        val top = cy - (big.size.height + small.size.height) / 2f
        drawText(big, topLeft = Offset(x, top))
        drawText(small, topLeft = Offset(x, top + big.size.height))
    }
}
