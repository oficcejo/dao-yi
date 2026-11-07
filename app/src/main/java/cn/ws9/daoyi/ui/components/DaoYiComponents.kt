package cn.ws9.daoyi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.ws9.daoyi.core.bazi.BaziChart
import cn.ws9.daoyi.core.bazi.Ganzhi
import cn.ws9.daoyi.core.safety.SafetyGate
import cn.ws9.daoyi.ui.theme.CardBg
import cn.ws9.daoyi.ui.theme.Cinnabar
import cn.ws9.daoyi.ui.theme.CinnabarPale
import cn.ws9.daoyi.ui.theme.Gold
import cn.ws9.daoyi.ui.theme.HairLine
import cn.ws9.daoyi.ui.theme.Ink
import cn.ws9.daoyi.ui.theme.InkMuted
import cn.ws9.daoyi.ui.theme.InkSoft
import cn.ws9.daoyi.ui.theme.PaperDeep
import cn.ws9.daoyi.ui.theme.wuxingColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SectionHeader(no: String, title: String, sub: String = "") {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(no, style = MaterialTheme.typography.labelSmall, color = Cinnabar)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.weight(1f))
            Box(Modifier.width(40.dp).height(1.dp).background(Gold))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(4.dp).background(Gold, RoundedCornerShape(50)))
        }
        if (sub.isNotBlank()) {
            Text(sub, style = MaterialTheme.typography.bodySmall, color = InkMuted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun DaoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, HairLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun GoldDivider() {
    Box(
        Modifier.fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.55f), Gold.copy(alpha = 0.05f))))
    )
}

@Composable
fun Tag(text: String, color: Color = Gold) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) CinnabarPale else CardBg
    val fg = if (selected) Cinnabar else InkSoft
    val border = if (selected) Cinnabar else HairLine
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = fg,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(3.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.6.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    )
}

@Composable
fun KeyValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMuted, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
    }
}

@Composable
fun MeterBar(label: String, ratio: Float, trailing: String, color: Color = Gold) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            label, style = MaterialTheme.typography.bodyMedium, color = Ink,
            maxLines = 1, modifier = Modifier.width(136.dp)
        )
        Box(
            Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(PaperDeep)
        ) {
            Box(
                Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp)).background(color)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(trailing, style = MaterialTheme.typography.bodySmall, color = InkMuted,
            textAlign = TextAlign.End, modifier = Modifier.width(46.dp))
    }
}

@Composable
fun Disclaimer(text: String = SafetyGate.DISCLAIMER) {
    Row(
        Modifier.fillMaxWidth()
            .background(CinnabarPale, RoundedCornerShape(10.dp))
            .padding(13.dp)
    ) {
        Box(Modifier.width(3.dp).height(42.dp).background(Cinnabar, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(11.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Cinnabar, lineHeight = 19.sp)
    }
}

@Composable
fun PillarBoard(chart: BaziChart) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        chart.pillars.forEachIndexed { i, p ->
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (i == 2) Gold.copy(alpha = 0.08f) else CardBg)
                    .border(0.6.dp, if (i == 2) Gold else HairLine, RoundedCornerShape(10.dp))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(chart.labels[i], style = MaterialTheme.typography.labelSmall, color = InkMuted)
                Spacer(Modifier.height(8.dp))
                Text(
                    p.gan, fontFamily = FontFamily.Serif, fontSize = 30.sp,
                    color = wuxingColors[Ganzhi.wx(p.gan)] ?: Ink
                )
                Text(
                    p.zhi, fontFamily = FontFamily.Serif, fontSize = 30.sp,
                    color = wuxingColors[Ganzhi.wx(p.zhi)] ?: Ink
                )
                Spacer(Modifier.height(8.dp))
                Text(chart.tenGods[i], style = MaterialTheme.typography.bodySmall, color = Cinnabar)
                Text(
                    chart.hiddenStems[i].joinToString(" ") { it.gan },
                    style = MaterialTheme.typography.bodySmall, color = InkMuted, fontSize = 11.sp
                )
                Text(
                    chart.nayin[i], style = MaterialTheme.typography.bodySmall,
                    color = Gold, fontSize = 11.sp, maxLines = 1
                )
            }
        }
    }
}

@Composable
fun WuxingRadar(power: Map<String, Double>) {
    val keys = listOf("木", "火", "土", "金", "水")
    Canvas(Modifier.fillMaxWidth().height(206.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(size.width, size.height) * 0.36f
        fun pt(i: Int, ratio: Float): Offset {
            val a = Math.toRadians(-90.0 + i * 72.0)
            return Offset(
                cx + r * ratio * cos(a).toFloat(),
                cy + r * ratio * sin(a).toFloat()
            )
        }
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { level ->
            val grid = Path()
            for (i in 0..4) {
                val q = pt(i, level)
                if (i == 0) grid.moveTo(q.x, q.y) else grid.lineTo(q.x, q.y)
            }
            grid.close()
            drawPath(path = grid, color = HairLine, style = Stroke(width = 1.dp.toPx()))
        }
        for (i in 0..4) {
            drawLine(color = HairLine, start = Offset(cx, cy), end = pt(i, 1f), strokeWidth = 1.dp.toPx())
        }
        val shape = Path()
        val pts = keys.mapIndexed { i, k ->
            val ratio = ((power[k] ?: 0.0).toFloat() / 40f).coerceIn(0.05f, 1f)
            pt(i, ratio)
        }
        pts.forEachIndexed { i, q ->
            if (i == 0) shape.moveTo(q.x, q.y) else shape.lineTo(q.x, q.y)
        }
        shape.close()
        drawPath(path = shape, color = Gold.copy(alpha = 0.20f))
        drawPath(path = shape, color = Gold, style = Stroke(width = 2.dp.toPx()))
        pts.forEach { q -> drawCircle(color = Cinnabar, radius = 3.dp.toPx(), center = q) }
    }
}
