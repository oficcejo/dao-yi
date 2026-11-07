package cn.ws9.daoyi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.ws9.daoyi.core.bazi.Shichen
import cn.ws9.daoyi.ui.theme.CardBg
import cn.ws9.daoyi.ui.theme.Cinnabar
import cn.ws9.daoyi.ui.theme.CinnabarPale
import cn.ws9.daoyi.ui.theme.Gold
import cn.ws9.daoyi.ui.theme.GoldPale
import cn.ws9.daoyi.ui.theme.HairLine
import cn.ws9.daoyi.ui.theme.Ink
import cn.ws9.daoyi.ui.theme.InkMuted
import cn.ws9.daoyi.ui.theme.White
import java.time.LocalDate
import java.time.LocalTime

/** 与宣纸墨金体系一致的可点击选择字段。 */
@Composable
fun PickerField(
    label: String,
    value: String,
    hint: String = "",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
                .border(0.8.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(value, fontFamily = FontFamily.Serif, fontSize = 18.sp, color = Ink,
                    modifier = Modifier.weight(1f))
                Text("选择 ›", style = MaterialTheme.typography.labelSmall, color = Gold)
            }
        }
        if (hint.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, style = MaterialTheme.typography.labelSmall, color = InkMuted)
        }
    }
}

@Composable
private fun DialogShell(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        // 三键导航机型上弹窗同样可能贴到系统栏下，外层单独吃掉上下 insets，
        // 不改动卡片本身的圆角与内边距；系统栏已避让时 padding 为 0，外观不变。
        Box(Modifier.statusBarsPadding().navigationBarsPadding()) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardBg)
                    .border(0.8.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Text(title, fontFamily = FontFamily.Serif, fontSize = 21.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun DialogAction(text: String, primary: Boolean = false, onClick: () -> Unit) {
    Text(
        text, style = MaterialTheme.typography.titleSmall,
        color = if (primary) Cinnabar else InkMuted,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

/** 公历日期选择：年月切换 + 年份网格 + 当月日历，不依赖任何实验性 Material3 API。 */
@Composable
fun DatePickerDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val maxYear = if (initial.year > today.year) initial.year else today.year
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    var picked by remember { mutableStateOf(initial) }
    var chooseYear by remember { mutableStateOf(false) }

    fun shiftMonth(delta: Int) {
        var m = month + delta
        if (m < 1) { m = 12; year -= 1 }
        if (m > 12) { m = 1; year += 1 }
        month = m
    }

    DialogShell("公历出生日期", "点击年份可快速跳转 · 八字以节气与立春分界，日期须为公历", onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            DialogAction("‹") { shiftMonth(-1) }
            Text(
                "${year} 年 ${month} 月",
                fontFamily = FontFamily.Serif, fontSize = 18.sp, color = Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { chooseYear = !chooseYear }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (chooseYear) "返回日历" else "选年份",
                style = MaterialTheme.typography.labelSmall, color = Gold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable { chooseYear = !chooseYear }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
            DialogAction("›") { shiftMonth(1) }
        }
        Spacer(Modifier.height(10.dp))

        if (chooseYear) {
            YearGrid(1900, maxYear, year) { y -> year = y; chooseYear = false }
        } else {
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                    Text(w, style = MaterialTheme.typography.labelSmall, color = InkMuted,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))
            val first = LocalDate.of(year, month, 1)
            val cells: List<Int?> = List<Int?>(first.dayOfWeek.value - 1) { null } +
                    (1..first.lengthOfMonth()).toList()
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { d ->
                        val isPicked = d != null && LocalDate.of(year, month, d) == picked
                        val isToday = d != null && LocalDate.of(year, month, d) == today
                        Box(
                            Modifier.weight(1f).height(42.dp).padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isPicked -> Cinnabar
                                        isToday -> GoldPale
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = d != null) {
                                    if (d != null) picked = LocalDate.of(year, month, d)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (d != null) {
                                Text(d.toString(), style = MaterialTheme.typography.bodyMedium,
                                    color = if (isPicked) White else Ink)
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GoldDivider()
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            DialogAction("取消") { onDismiss() }
            Spacer(Modifier.weight(1f))
            DialogAction("回到今天") {
                picked = today; year = today.year; month = today.monthValue; chooseYear = false
            }
            DialogAction("确 定", primary = true) { onPick(picked) }
        }
    }
}

@Composable
private fun YearGrid(fromYear: Int, toYear: Int, selected: Int, onYear: (Int) -> Unit) {
    val rows = (fromYear..toYear).toList().chunked(4)
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = (((selected - fromYear) / 4) - 2)
            .coerceIn(0, (rows.size - 1).coerceAtLeast(0))
    )
    LazyColumn(state = state, modifier = Modifier.fillMaxWidth().height(276.dp)) {
        items(rows) { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                row.forEach { y ->
                    Box(
                        Modifier.weight(1f).padding(3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (y == selected) Cinnabar else CardBg)
                            .border(0.6.dp, if (y == selected) Cinnabar else HairLine,
                                RoundedCornerShape(8.dp))
                            .clickable { onYear(y) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$y", style = MaterialTheme.typography.bodyMedium,
                            color = if (y == selected) White else Ink)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** 十二时辰选择；选后自动写入该时辰代表时刻，精确分钟仍可在输入框微调。 */
@Composable
fun ShichenPickerDialog(current: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val selected = Shichen.indexOf(current)
    DialogShell("出生时辰", "十二时辰各两小时 · 选定后自动填入代表时刻", onDismiss) {
        Shichen.NAMES.withIndex().chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { iv ->
                    val i = iv.index
                    Box(
                        Modifier.weight(1f).padding(3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (i == selected) CinnabarPale else CardBg)
                            .border(0.6.dp, if (i == selected) Cinnabar else HairLine,
                                RoundedCornerShape(10.dp))
                            .clickable { onPick(Shichen.defaultTime(i)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(iv.value, fontFamily = FontFamily.Serif, fontSize = 18.sp,
                                color = if (i == selected) Cinnabar else Ink)
                            Spacer(Modifier.height(3.dp))
                            Text(Shichen.RANGES[i], fontSize = 10.sp, color = InkMuted, maxLines = 1)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "子时 23:00–01:00 跨日：默认取晚子时 23:30；若生于凌晨 00:00–01:00，请在下方把精确时刻改为 00:xx。",
            style = MaterialTheme.typography.bodySmall, color = InkMuted
        )
        Spacer(Modifier.height(10.dp))
        GoldDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
            DialogAction("取消") { onDismiss() }
        }
    }
}
