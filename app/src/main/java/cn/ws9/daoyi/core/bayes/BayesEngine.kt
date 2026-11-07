package cn.ws9.daoyi.core.bayes

import cn.ws9.daoyi.core.tcm.KnowledgeBase
import cn.ws9.daoyi.core.tcm.SymptomHit
import cn.ws9.daoyi.core.tcm.Syndrome
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

data class Contribution(val label: String, val delta: Double, val detail: String)

data class Posterior(
    val syndrome: Syndrome,
    val logScore: Double,
    val probability: Double,
    val contributions: List<Contribution>
)

data class BayesResult(
    val ranked: List<Posterior>,
    val confidence: Double,
    val lowConfidence: Boolean,
    val explanation: String
)

/**
 * 朴素贝叶斯（对数域、证据分组加权）。
 * log P(H|E) = log P0(H) + Σ_g [ Σ_e w_e·log(P(e|H)/0.5) / √n_g ] + λ·cos(B八字, BiasH) + μ·cos(B运气, BiasH)
 * 同组证据用 1/√n 折扣代替独立连乘；八字偏性只调制先验，不作为症状证据；λ 可置 0 关闭。
 */
object BayesEngine {

    private const val P_MAIN = 0.90
    private const val P_OPTIONAL = 0.60
    private const val P_NEGATIVE = 0.06
    private const val BASE = 0.50
    private const val MU_YUNQI = 0.10

    fun infer(
        symptoms: List<SymptomHit>,
        baziBias: Map<String, Double>,
        yunqiBias: Map<String, Double>,
        lambda: Double = 0.15
    ): BayesResult {
        val hypotheses = KnowledgeBase.syndromes
        val groups = symptoms.groupBy { it.group }
        val scored = hypotheses.map { h ->
            val contributions = mutableListOf<Contribution>()
            var log = ln(1.0 / hypotheses.size)

            for ((group, hits) in groups) {
                // 同组证据彼此相关，不能按独立事件连乘，否则后验会被推到虚假的 0.99；
                // 用 1/√n 做组内折扣，既保留「命中条数越多越可信」，又避免重复计数。
                // 中性证据（似然=BASE）必须留在组内：旧实现把它们丢掉，
                // 会让「只命中 2/4 且全是主症」的证型反超「命中 4/4 但含或然症」的证型，
                // 例如恶寒发热无汗头痛曾被误判为外寒内饮而非太阳伤寒。
                val perHit = hits.map { hit ->
                    hit.weight * ln(likelihood(h, hit.text) / BASE)
                }
                val groupLog = perHit.sum() / sqrt(perHit.size.toDouble())
                log += groupLog
                val hitCount = hits.count { likelihood(h, it.text) != BASE }
                contributions += Contribution("证据 · $group", groupLog, "$hitCount/${hits.size} 条命中")
            }

            val simBazi = cosine(baziBias, h.bias)
            val deltaBazi = lambda * 2.0 * simBazi
            log += deltaBazi
            contributions += Contribution("本命偏性先验", deltaBazi, "λ=$lambda，相似=${r2(simBazi)}")

            val simYunqi = cosine(yunqiBias, h.bias)
            val deltaYunqi = MU_YUNQI * 2.0 * simYunqi
            log += deltaYunqi
            contributions += Contribution("运气时空先验", deltaYunqi, "μ=$MU_YUNQI，相似=${r2(simYunqi)}")

            Posterior(h, log, 0.0, contributions.sortedByDescending { kotlin.math.abs(it.delta) })
        }

        val maxLog = scored.maxOf { it.logScore }
        val exps = scored.map { exp(it.logScore - maxLog) }
        val total = exps.sum()
        val ranked = scored.mapIndexed { i, p -> p.copy(probability = exps[i] / total) }
            .sortedByDescending { it.probability }

        val top = ranked.first()
        val second = ranked.getOrNull(1)?.probability ?: 0.0
        val ratio = if (second > 0.0) top.probability / second else Double.POSITIVE_INFINITY
        // 22 个候选证型使绝对后验天然偏低，因此以「相对优势 + 证据数量」为主判据
        val low = symptoms.size < 2 || ratio < 1.4 || top.probability < 0.12
        val explanation = "共 ${symptoms.size} 条证据、${groups.size} 个证据组；首位「${top.syndrome.name}」后验 " +
                "${(top.probability * 100).toInt()}%，与第二位之比 ${if (ratio.isInfinite()) "—" else r2(ratio).toString()}。" +
                if (low) " 证据不足或证型不集中，建议补充四诊信息，不宜直接出方。"
                else " 证据链可用于下一步研讨。"

        return BayesResult(ranked, top.probability, low, explanation)
    }

    private fun likelihood(h: Syndrome, symptom: String): Double {
        fun List<String>.matches() = any { symptom.contains(it) || it.contains(symptom) }
        return when {
            h.main.matches() -> P_MAIN
            h.optional.matches() -> P_OPTIONAL
            h.negative.matches() -> P_NEGATIVE
            else -> BASE
        }
    }

    private fun cosine(a: Map<String, Double>, b: Map<String, Double>): Double {
        val keys = (a.keys + b.keys).toSet()
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        keys.forEach { k ->
            val x = a[k] ?: 0.0
            val y = b[k] ?: 0.0
            dot += x * y
            na += x * x
            nb += y * y
        }
        if (na == 0.0 || nb == 0.0) return 0.0
        return (dot / (sqrt(na) * sqrt(nb))).coerceIn(-1.0, 1.0)
    }

    private fun r2(v: Double): Double = (v * 100.0).toInt() / 100.0
}
