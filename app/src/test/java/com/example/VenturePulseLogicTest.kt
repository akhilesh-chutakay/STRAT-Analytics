package com.example

import com.example.data.local.SeedData
import com.example.data.model.BusinessIdea
import com.example.data.model.BusinessModelCategory
import com.example.data.model.MarketType
import com.example.data.model.PipelineStage
import org.junit.Assert.*
import org.junit.Test

class VenturePulseLogicTest {

    @Test
    fun testSeedDataCompleteness() {
        val ideas = SeedData.getInitialIdeas()
        val signals = SeedData.getInitialSignals()

        assertTrue("Seed ideas should not be empty", ideas.isNotEmpty())
        assertTrue("Seed signals should not be empty", signals.isNotEmpty())

        // Verify Blue Ocean vs Red Ocean representation
        val blueOceanIdeas = ideas.filter { it.marketType == MarketType.BLUE_OCEAN }
        val redOceanIdeas = ideas.filter { it.marketType == MarketType.RED_OCEAN }
        assertTrue("Should contain Blue Ocean ideas", blueOceanIdeas.isNotEmpty())
        assertTrue("Should contain Red Ocean ideas", redOceanIdeas.isNotEmpty())

        // Verify actionable ideas
        val actionable = ideas.filter { it.isImmediatelyActionable }
        assertTrue("Should contain immediately actionable ideas", actionable.isNotEmpty())

        // Verify all ideas have positive revenue and effort estimations
        ideas.forEach { idea ->
            assertTrue("Estimated ARR should be positive for ${idea.title}", idea.estimatedARR > 0)
            assertTrue("Weeks to MVP should be non-negative for ${idea.title}", idea.weeksToMvp >= 0)
            assertTrue("Opportunity score should be between 15 and 99", idea.opportunityScore in 15..99)
            assertTrue("Problem statement must not be blank", idea.problemStatement.isNotBlank())
            assertTrue("Solution description must not be blank", idea.solutionDescription.isNotBlank())
        }
    }

    @Test
    fun testRevenueCalculationMath() {
        val pricePerMonth = 99.0
        val targetCustomers = 50
        val mrr = pricePerMonth * targetCustomers
        val arr = mrr * 12.0

        assertEquals(4950.0, mrr, 0.01)
        assertEquals(59400.0, arr, 0.01)
    }

    @Test
    fun testSignalConversionIntegrity() {
        val signals = SeedData.getInitialSignals()
        val firstSignal = signals.first()

        assertNotNull(firstSignal.problemTitle)
        assertTrue(firstSignal.rawProblemQuote.isNotBlank())
        assertTrue(firstSignal.estimatedWillingnessToPayMonthly > 0)
    }
}
