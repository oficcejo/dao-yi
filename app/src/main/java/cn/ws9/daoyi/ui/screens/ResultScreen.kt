package cn.ws9.daoyi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.ws9.daoyi.core.llm.HerbDose
import cn.ws9.daoyi.core.pipeline.RunStatus
import cn.ws9.daoyi.core.safety.SafetyGate
import cn.ws9.daoyi.ui.components.DaoCard
import cn.ws9.daoyi.ui.components.Disclaimer
import cn.ws9.daoyi.ui.components.GoldDivider
import cn.ws9.daoyi.ui.components.KeyValue
import cn.ws9.daoyi.ui.components.MeterBar
import cn.ws9.daoyi.ui.components.PillarBoard
import cn.ws9.daoyi.ui.components.SectionHeader
import cn.ws9.daoyi.ui.components.Tag
import cn.ws9.daoyi.ui.components.WuxingRadar
import cn.ws9.daoyi.ui.theme.Cinnabar
import cn.ws9.daoyi.ui.theme.CinnabarPale
import cn.ws9.daoyi.ui.theme.Gold
import cn.ws9.daoyi.ui.theme.Ink
import cn.ws9.daoyi.ui.theme.InkMuted
import cn.ws9.daoyi.ui.theme.Jade
import cn.ws9.daoyi.ui.theme.Paper
import cn.ws9.daoyi.ui.theme.wuxingColors
import cn.ws9.daoyi.viewmodel.UiState
import java.time.format.DateTimeFormatter

private fun g(v: Double): String = "${(v * 10).toInt() / 10.0} g"
private val TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
fun ResultScreen(state: UiState, onGoSettings: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 26.dp, bottom = 40.dp)
    ) {
        Text("推 演", style = MaterialTheme.typography.displayMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(26.dp).height(1.dp).background(Cinnabar))
            Spacer(Modifier.width(10.dp))
            Text("四柱 · 运气 · 证型后验 · 经方", style = MaterialTheme.typography.bodySmall, color = Gold)
        }
        Spacer(Modifier.height(16.dp))
        Disclaimer()

        if (state.loading) {
            Spacer(Modifier.height(40.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Cinnabar)
                Spacer(Modifier.height(16.dp))
                Text("正在排盘、汇总证据并检校方剂…", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
            return@Column
        }

        // 运行状态始终置顶：明确本次是否调用过模型、为什么降级，长错误收进可折叠详情
        if (state.status.headline.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            StatusCard(state.status, onGoSettings)
        }

        state.refusal?.let { r ->
            Spacer(Modifier.height(18.dp))
            DaoCard {
                Text("安全闸门 · 停止推演", style = MaterialTheme.typography.titleLarge, color = Cinnabar)
                Spacer(Modifier.height(10.dp))
                Text(r, style = MaterialTheme.typography.bodyLarge, color = Ink)
            }
        }

        if (state.chart == null && state.refusal == null) {
            Spacer(Modifier.height(18.dp))
            DaoCard {
                Text("尚无推演结果", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.height(10.dp))
                Text(
                    state.message.ifBlank { "请在「起局」页填写生辰与症候，然后点击起局推演。" },
                    style = MaterialTheme.typography.bodyLarge, color = InkMuted
                )
            }
            return@Column
        }

        if (state.message.isNotBlank() && state.prescription == null && state.fallback == null && state.refusal == null) {
            Spacer(Modifier.height(12.dp))
            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = Cinnabar)
        }

        state.chart?.let { chart ->
            SectionHeader("壹", "四柱命盘", "真太阳时校正 · 立春分年 · 节气分月")
            DaoCard {
                PillarBoard(chart)
                Spacer(Modifier.height(16.dp))
                GoldDivider()
                Spacer(Modifier.height(12.dp))
                KeyValue("北京时间", chart.beijingTime.format(TIME_FMT))
                KeyValue("真太阳时", chart.trueSolarTime.format(TIME_FMT))
                KeyValue("月令节气", chart.solarTermName)
                KeyValue("日主", "${chart.dayMaster}（${chart.strength.level}）")
                KeyValue("旺衰评分", "${chart.strength.score} / 100")
                KeyValue("纳音", chart.nayin.joinToString(" · "))
            }

            SectionHeader("贰", "五行与旺衰", "藏干本气/中气/余气加权 · 得令、通根、透干")
            DaoCard {
                WuxingRadar(chart.strength.power)
                listOf("木", "火", "土", "金", "水").forEach { k ->
                    MeterBar(
                        "${k}行力量", (chart.strength.power.getValue(k) / 50.0).toFloat(),
                        "${chart.strength.power.getValue(k).toInt()}%",
                        wuxingColors[k] ?: Gold
                    )
                }
                Spacer(Modifier.height(12.dp))
                GoldDivider()
                Spacer(Modifier.height(10.dp))
                chart.strength.items.forEach { item ->
                    KeyValue(item.label, "${if (item.delta >= 0) "+" else ""}${item.delta}  ${item.note}")
                }
                Spacer(Modifier.height(10.dp))
                Text(chart.strength.advice, style = MaterialTheme.typography.bodyMedium, color = Ink)
            }

            SectionHeader("叁", "大运流年 · 五运六气", "时空参考维度，权重低于四诊")
            DaoCard {
                KeyValue("起运", chart.dayun.note.substringBefore("；"))
                KeyValue("流年", "${chart.dayun.liuNianYear} ${chart.dayun.liuNian.name}")
                KeyValue("现行大运", chart.dayun.current)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    chart.dayun.steps.take(6).forEach { s ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.ganZhi, style = MaterialTheme.typography.bodySmall, color = Ink)
                            Text("${s.startAge}岁", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                GoldDivider()
                Spacer(Modifier.height(12.dp))
                KeyValue("岁运", "${chart.yunqi.ganZhi} · ${chart.yunqi.zhongYun}")
                KeyValue("司天", chart.yunqi.siTian)
                KeyValue("在泉", chart.yunqi.zaiQuan)
                KeyValue("主气/客气", "${chart.yunqi.zhuQi} / ${chart.yunqi.keQi}")
                Spacer(Modifier.height(8.dp))
                Text(chart.yunqi.note, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }

        state.bayes?.let { bayes ->
            SectionHeader("肆", "证型后验", "对数域分组加权 · 八字/运气只调制先验")
            DaoCard {
                bayes.ranked.take(6).forEachIndexed { i, p ->
                    MeterBar(
                        p.syndrome.name.substringBefore(" · ").ifBlank { p.syndrome.name }.take(7),
                        p.probability.toFloat(),
                        "${(p.probability * 100).toInt()}%",
                        if (i == 0) Cinnabar else Gold.copy(alpha = 0.65f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                GoldDivider()
                Spacer(Modifier.height(10.dp))
                SubTitle("首位证据链")
                bayes.ranked.first().contributions.take(5).forEach { c ->
                    KeyValue(c.label, "${if (c.delta >= 0) "+" else ""}${(c.delta * 100).toInt() / 100.0}  ${c.detail}")
                }
                Spacer(Modifier.height(8.dp))
                Text(bayes.explanation, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                Spacer(Modifier.height(6.dp))
                Text("候选证型共 ${bayes.ranked.size} 个且先验均等，绝对后验天然偏低；判读以相对排序与证据链为准。",
                    style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Spacer(Modifier.height(8.dp))
                Text(bayes.ranked.first().syndrome.classic, style = MaterialTheme.typography.bodySmall, color = Gold)
            }
        }

        // 「伍」只有一个出口：模型成功 → 方剂与调摄；模型失败 → 明确标注的降级参考卡。两者互斥。
        state.prescription?.let { p ->
            SectionHeader("伍", "方剂与调摄", "优先张仲景经方 · 剂量精确到克 · 大模型输出经硬规则校验")
            DaoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.formulaName.ifBlank { "参考方" }, style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Spacer(Modifier.width(10.dp))
                    Tag(p.confidence.ifBlank { "参考" }, Cinnabar)
                }
                if (p.source.isNotBlank()) Text(p.source, style = MaterialTheme.typography.bodySmall, color = Gold)
                Spacer(Modifier.height(12.dp))
                if (p.syndrome.isNotBlank()) KeyValue("证型", p.syndrome)
                if (p.therapy.isNotBlank()) KeyValue("治法", p.therapy)

                SectionLine()
                SubTitle("方药（现代参考量）")
                if (p.herbs.isEmpty()) {
                    Text("模型未返回可识别的药味，本项留空。", style = MaterialTheme.typography.bodySmall, color = Cinnabar)
                } else {
                    p.herbs.forEach { h -> HerbRow(h) }
                }
                if (p.decoction.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    KeyValue("煎服法", p.decoction)
                }

                SectionLine()
                SubTitle("中成药（须辨证）")
                BulletList(p.patentMedicines, "模型未给出中成药建议。")

                SectionLine()
                SubTitle("养生 · 补不足损有余")
                LabeledList("饮食", p.diet)
                LabeledList("作息", p.lifestyle)
                if (p.exercise.isNotBlank()) KeyValue("导引", p.exercise)
                if (p.emotional.isNotBlank()) KeyValue("情志", p.emotional)
                if (p.seasonal.isNotBlank()) KeyValue("节气", p.seasonal)

                SectionLine()
                SubTitle("条文依据")
                BulletList(p.references, "模型未给出条文依据。")

                if (p.cautions.any { it.isNotBlank() }) {
                    SectionLine()
                    SubTitle("禁忌与就医提示")
                    BulletList(p.cautions)
                }
            }
        }

        state.fallback?.let { f ->
            SectionHeader("伍", "本地典籍参考卡", "未调用大模型 · 仅作条文比对，不是本次症候的推演结果")
            DaoCard {
                Tag("降级结果 · 非本次方剂", Cinnabar)
                Spacer(Modifier.height(10.dp))
                Text(f.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
                Text(f.source, style = MaterialTheme.typography.bodySmall, color = Gold)
                Spacer(Modifier.height(10.dp))
                KeyValue("方证", f.syndrome)
                Spacer(Modifier.height(10.dp))
                GoldDivider()
                Spacer(Modifier.height(10.dp))
                f.herbs.forEach { (name, gram) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                        Text("$gram g", style = MaterialTheme.typography.bodyMedium, color = Cinnabar)
                    }
                }
                Spacer(Modifier.height(10.dp))
                KeyValue("煎服", f.method)
                state.fallbackPatent?.let { KeyValue("中成药", "${it.name} · ${it.caution}") }
                Spacer(Modifier.height(10.dp))
                Text("此为模型未返回时的典籍原文参考，不是针对当前症候的个体化推演；原因见顶部「运行状态」。",
                    style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }

        state.safety?.let { s ->
            SectionHeader("陆", "安全校验与免责", "剂量、配伍、特殊人群逐项复核")
            DaoCard {
                BulletList(s.warnings.filter { it != SafetyGate.DISCLAIMER })
                Spacer(Modifier.height(12.dp))
                Disclaimer()
            }
        }

        Spacer(Modifier.height(18.dp))
        GoldDivider()
        Spacer(Modifier.height(10.dp))
        Text(
            "本页所有内容为典籍整理与概率推演的学习记录，未经医师审校，不得作为诊疗或抓药依据。",
            style = MaterialTheme.typography.bodySmall, color = InkMuted
        )
    }
}

@Composable
private fun StatusCard(status: RunStatus, onGoSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val accent = if (status.modelUsed) Jade else Cinnabar
    DaoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(30.dp).background(accent, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(status.headline, style = MaterialTheme.typography.titleMedium, color = accent)
                if (status.reason.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(status.reason, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                }
            }
            Spacer(Modifier.width(8.dp))
            Tag(if (status.modelUsed) "模型" else "本地", accent)
        }
        if (status.technical.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (expanded) "收起技术详情 ▲" else "查看技术详情 ▼",
                style = MaterialTheme.typography.bodySmall, color = Gold,
                modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 4.dp)
            )
            if (expanded) {
                Spacer(Modifier.height(4.dp))
                Text(status.technical, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }
        if (!status.modelUsed && status.technical.contains("API Key")) {
            Spacer(Modifier.height(8.dp))
            Text(
                "前往「设置」填写 API Key ›",
                style = MaterialTheme.typography.bodySmall, color = Jade,
                modifier = Modifier.clickable(onClick = onGoSettings).padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionLine() {
    Spacer(Modifier.height(14.dp))
    GoldDivider()
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SubTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Ink,
        modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun LabeledList(label: String, items: List<String>) {
    if (items.isEmpty()) return
    Text(label, style = MaterialTheme.typography.bodySmall, color = Gold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
    BulletList(items)
}

@Composable
private fun HerbRow(h: HerbDose) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(h.name, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
        if (h.processing.isNotBlank()) {
            Text(h.processing, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier.background(CinnabarPale, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(g(h.grams), style = MaterialTheme.typography.bodyMedium, color = Cinnabar)
        }
    }
    if (h.note.isNotBlank()) {
        Text(h.note, style = MaterialTheme.typography.bodySmall, color = InkMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    }
}

@Composable
private fun BulletList(items: List<String>, emptyHint: String = "") {
    val list = items.filter { it.isNotBlank() }
    if (list.isEmpty()) {
        if (emptyHint.isNotBlank()) Text(emptyHint, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        return
    }
    list.forEach { s ->
        Row(Modifier.padding(vertical = 3.dp)) {
            Text("·", style = MaterialTheme.typography.bodyMedium, color = Gold)
            Spacer(Modifier.width(8.dp))
            Text(s, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
        }
    }
}
