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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.ws9.daoyi.core.tcm.KnowledgeBase
import cn.ws9.daoyi.ui.components.DaoCard
import cn.ws9.daoyi.ui.components.Disclaimer
import cn.ws9.daoyi.ui.components.GoldDivider
import cn.ws9.daoyi.ui.components.KeyValue
import cn.ws9.daoyi.ui.components.SectionHeader
import cn.ws9.daoyi.ui.components.Tag
import cn.ws9.daoyi.ui.theme.Cinnabar
import cn.ws9.daoyi.ui.theme.Gold
import cn.ws9.daoyi.ui.theme.Ink
import cn.ws9.daoyi.ui.theme.InkMuted
import cn.ws9.daoyi.ui.theme.Paper

@Composable
fun ClassicsScreen() {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val formulas = KnowledgeBase.formulas.filter {
        query.isBlank() || it.name.contains(query) || it.syndrome.contains(query) || it.source.contains(query)
    }

    Column(
        Modifier.fillMaxSize().background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 26.dp, bottom = 40.dp)
    ) {
        Text("典 籍", style = MaterialTheme.typography.displayMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(26.dp).height(1.dp).background(Cinnabar))
            Spacer(Modifier.width(10.dp))
            Text("伤寒论 · 金匮要略 · 中成药", style = MaterialTheme.typography.bodySmall, color = Gold)
        }
        Spacer(Modifier.height(16.dp))
        Disclaimer()
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("检索方名 / 方证 / 出处") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("经方 ${KnowledgeBase.formulas.size} 首 · 药材 ${KnowledgeBase.herbs.size} 味 · 证型 ${KnowledgeBase.syndromes.size} 类",
            style = MaterialTheme.typography.bodySmall, color = InkMuted)

        SectionHeader("壹", "经方索引", "点击展开组成与煎服法 · 克数为现代参考量")
        formulas.forEach { f ->
            DaoCard(
                modifier = Modifier.padding(bottom = 10.dp).clickable {
                    expanded = if (expanded == f.name) null else f.name
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.name, style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.weight(1f))
                    Tag(f.source.take(8), Gold)
                }
                Spacer(Modifier.height(6.dp))
                Text(f.syndrome, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                if (expanded == f.name) {
                    Spacer(Modifier.height(12.dp))
                    GoldDivider()
                    Spacer(Modifier.height(10.dp))
                    f.herbs.forEach { (name, gram) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                            Text("$gram g", style = MaterialTheme.typography.bodyMedium, color = Cinnabar)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    KeyValue("煎服法", f.method)
                    KeyValue("出处", f.source)
                    if (f.note.isNotBlank()) KeyValue("备注", f.note)
                }
            }
        }

        SectionHeader("贰", "中成药对照", "须辨证使用，证型不符则无效甚至有害")
        DaoCard {
            KnowledgeBase.patents.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(p.name, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.width(116.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.syndrome, style = MaterialTheme.typography.bodySmall, color = Gold)
                        Text(p.caution, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    }
                }
            }
        }

        SectionHeader("叁", "剂量说明", "古今折算的争议与本研究取法")
        DaoCard {
            Text(
                "汉代一两的折算历来有 15.625g（柯雪帆，据光和大司农铜权）、13.75g（丘光明）、" +
                        "1.3g（日本汉方）等不同结论。且仲景原方多「一剂分三服、一煎」，现代为两煎，" +
                        "郝万山主张「今天一剂 ≈ 经方一次治疗量」。\n\n" +
                        "本工具同时保留两种信息：典籍折算量（按原方 × 15.625g ÷ 服法次数，仅作学习展示），" +
                        "与现代参考量（按《中国药典》常用日用量区间）。程序输出的克数为后者，且由安全闸门" +
                        "保证不越过药典上限；任何超出上限的模型输出都会被自动校正并标注。",
                style = MaterialTheme.typography.bodyMedium, color = Ink
            )
        }
    }
}
