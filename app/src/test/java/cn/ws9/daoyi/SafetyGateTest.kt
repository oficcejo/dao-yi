package cn.ws9.daoyi

import cn.ws9.daoyi.core.llm.HerbDose
import cn.ws9.daoyi.core.llm.LlmPrescription
import cn.ws9.daoyi.core.safety.SafetyGate
import cn.ws9.daoyi.core.tcm.Extraction
import cn.ws9.daoyi.core.tcm.PersonProfile
import cn.ws9.daoyi.core.tcm.SymptomExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGateTest {

    @Test
    fun doseAbovePharmacopoeiaIsClamped() {
        val p = LlmPrescription(herbs = listOf(HerbDose("麻黄", 50.0)))
        val r = SafetyGate.verify(p, PersonProfile())
        assertEquals(10.0, r.adjusted.first().grams, 1e-6)
        assertTrue(r.warnings.any { it.contains("超过药典常用上限") })
    }

    @Test
    fun incompatiblePairIsBlocked() {
        val p = LlmPrescription(herbs = listOf(HerbDose("甘草", 6.0), HerbDose("甘遂", 2.0)))
        val r = SafetyGate.verify(p, PersonProfile())
        assertTrue(r.blocked)
        assertNotNull(r.refusal)
    }

    @Test
    fun pregnancyForbiddenIsBlocked() {
        val p = LlmPrescription(herbs = listOf(HerbDose("桂枝", 9.0), HerbDose("麝香", 1.0)))
        val r = SafetyGate.verify(p, PersonProfile(pregnant = true))
        assertTrue(r.blocked)
    }

    @Test
    fun redFlagsRefuseBeforeInference() {
        val e: Extraction = SymptomExtractor.extract("胸痛伴呼吸困难，请开方")
        assertNotNull(SafetyGate.checkBefore(e, PersonProfile()))
    }

    @Test
    fun unknownHerbIsFlagged() {
        val r = SafetyGate.verify(
            LlmPrescription(herbs = listOf(HerbDose("某未收录草药", 9.0))), PersonProfile()
        )
        assertTrue(r.warnings.any { it.contains("未收录") })
    }

    @Test
    fun normalFormulaPasses() {
        val p = LlmPrescription(herbs = listOf(
            HerbDose("桂枝", 9.0), HerbDose("白芍", 9.0), HerbDose("炙甘草", 6.0)
        ))
        val r = SafetyGate.verify(p, PersonProfile())
        assertTrue(!r.blocked)
        assertEquals(3, r.adjusted.size)
        assertTrue(r.warnings.any { it.contains("非诊断") })
    }
}
