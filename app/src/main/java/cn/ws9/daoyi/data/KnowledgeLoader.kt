package cn.ws9.daoyi.data

import android.content.Context
import cn.ws9.daoyi.core.tcm.Herb
import cn.ws9.daoyi.core.tcm.KnowledgeBase
import org.json.JSONObject

/**
 * 可选扩展：把 app/src/main/assets/knowledge_override.json 里的药材剂量表合并进内置知识库。
 * 格式：{"herbs":[{"name":"药名","min":3,"max":10,"toxic":false,"note":""}]}
 * 文件缺失或格式有误时静默使用内置数据，不影响主流程。
 */
object KnowledgeLoader {

    fun applyOverride(context: Context) {
        runCatching {
            val text = context.assets.open("knowledge_override.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val arr = JSONObject(text).optJSONArray("herbs") ?: return
            val extra = mutableListOf<Herb>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name", "").trim()
                if (name.isBlank()) continue
                extra += Herb(
                    name = name,
                    minGram = o.optInt("min", 3),
                    maxGram = o.optInt("max", 10),
                    toxic = o.optBoolean("toxic", false),
                    pregnancyAvoid = o.optBoolean("pregnancy_avoid", false),
                    decoctNote = o.optString("note", "")
                )
            }
            if (extra.isNotEmpty()) {
                val names = extra.map { it.name }.toSet()
                KnowledgeBase.herbs = KnowledgeBase.herbs.filterNot { it.name in names } + extra
            }
        }
    }
}
