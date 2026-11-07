package cn.ws9.daoyi

import cn.ws9.daoyi.core.bayes.BayesEngine
import cn.ws9.daoyi.core.tcm.SymptomExtractor
import cn.ws9.daoyi.core.tcm.SymptomHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BayesEngineTest {

    private fun zero() = mapOf("木" to 0.0, "火" to 0.0, "土" to 0.0, "金" to 0.0, "水" to 0.0)

    @Test
    fun probabilitiesSumToOne() {
        val e = SymptomExtractor.extract("恶寒发热无汗，身痛，脉浮紧，苔薄白")
        val r = BayesEngine.infer(e.symptoms, zero(), zero(), lambda = 0.0)
        assertEquals(1.0, r.ranked.sumOf { it.probability }, 1e-6)
        assertTrue(r.confidence > 0.0)
    }

    @Test
    fun groupedEvidenceDoesNotOverCount() {
        val single = BayesEngine.infer(
            listOf(SymptomHit("恶寒", "主症", 1.0)), zero(), zero(), 0.0
        )
        val triple = BayesEngine.infer(
            listOf(
                SymptomHit("恶寒", "主症", 1.0),
                SymptomHit("无汗", "主症", 1.0),
                SymptomHit("脉紧", "舌脉", 0.8)
            ), zero(), zero(), 0.0
        )
        assertTrue("同组多证据取均值，不应造成后验爆炸", triple.confidence < 0.99)
        assertTrue(triple.confidence >= single.confidence)
    }

    @Test
    fun lambdaZeroDisablesBaziPrior() {
        val e = SymptomExtractor.extract("口苦咽干，往来寒热，胸胁苦满，脉弦")
        val bias = mapOf("木" to 1.0, "火" to 0.0, "土" to -0.5, "金" to 0.0, "水" to 0.0)
        val off = BayesEngine.infer(e.symptoms, bias, zero(), lambda = 0.0)
        val on = BayesEngine.infer(e.symptoms, bias, zero(), lambda = 0.15)
        assertTrue("λ>0 时先验应产生影响",
            kotlin.math.abs(off.ranked.first().logScore - on.ranked.first().logScore) > 0.001)
    }

    @Test
    fun negativeEvidencePenalizes() {
        val hits = listOf(
            SymptomHit("口渴", "主症", 1.0),
            SymptomHit("汗出", "主症", 1.0),
            SymptomHit("脉数", "舌脉", 0.8)
        )
        val r = BayesEngine.infer(hits, zero(), zero(), 0.0)
        assertTrue("太阳伤寒证应被「汗出」强负证据压低",
            r.ranked.first { it.syndrome.id == "S02" }.probability <
                    r.ranked.first { it.syndrome.id == "S05" }.probability)
    }

    @Test
    fun tooFewSymptomsTriggerLowConfidence() {
        val r = BayesEngine.infer(listOf(SymptomHit("乏力", "主症", 0.8)), zero(), zero(), 0.0)
        assertTrue(r.lowConfidence)
    }
}
