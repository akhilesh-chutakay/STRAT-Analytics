package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MarketType(val displayName: String, val shortDesc: String) {
    BLUE_OCEAN("Blue Ocean", "Untapped market with uncontested space & high differentiation"),
    RED_OCEAN("Red Ocean", "Established market with proven existing demand & fierce competition")
}

enum class BusinessModelCategory(val displayName: String) {
    B2B_SAAS("B2B SaaS"),
    MICRO_SAAS("Micro-SaaS"),
    B2B_DEVTOOL("DevTool & Infra"),
    AI_AGENT_TOOL("AI Agent / Workflow"),
    INTERNAL_TOOL("Internal Ops Tool"),
    API_SERVICE("API & Data Service"),
    CHROME_EXTENSION("Browser Extension"),
    MARKETPLACE("B2B Marketplace"),
    PRODUCTIZED_SERVICE("Productized Service"),
    NO_CODE_TOOL("No-Code & Template Tool"),
    MICRO_UTILITY("Simple Micro-Utility")
}

enum class PipelineStage(val displayName: String) {
    DISCOVERED("Discovered"),
    VALIDATING("Validating Demand"),
    PROTOTYPING("Prototyping"),
    MVP_READY("MVP Ready"),
    LAUNCHED("In Market"),
    ARCHIVED("Archived")
}

enum class DevComplexity(val displayName: String, val typicalWeeks: Int) {
    ZERO_CODE_NO_CODE("Zero-Code / No-Code (<3 Days)", 0),
    LOW_1_2_WEEKS("Low (1-2 Weeks)", 2),
    MEDIUM_3_6_WEEKS("Medium (3-6 Weeks)", 4),
    HIGH_2_4_MONTHS("High (2-4 Months)", 10),
    INTENSIVE_5_PLUS_MONTHS("Intensive (5+ Months)", 20)
}

enum class DataSourceType(val displayName: String) {
    FOUNDER_INTERVIEW("Founder Interview"),
    REDDIT("Reddit (r/SideProject, r/SaaS)"),
    X_TWITTER("X / Twitter Founder Pain"),
    G2_REVIEWS("G2 Negative Reviews"),
    CAPTERRA("Capterra Complaints"),
    PRODUCT_HUNT("Product Hunt Comments"),
    GITHUB_ISSUES("GitHub Feature Requests"),
    UPWORK_JOB("Upwork / Freelance Demand"),
    CHROME_STORE("Chrome Web Store Gap"),
    CUSTOMER_CALL("Customer Discovery Call")
}

@Entity(tableName = "business_ideas")
data class BusinessIdea(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val tagline: String,
    val category: BusinessModelCategory,
    val marketType: MarketType,
    val stage: PipelineStage = PipelineStage.DISCOVERED,
    val isImmediatelyActionable: Boolean = false, // Flag for fast-to-ship, high buyer urgency
    val isZeroInvestment: Boolean = false, // $0 upfront cost, 0 paid ads, zero capital needed
    val zeroCostPlaybook: String = "", // Exact step-by-step $0 acquisition and launch method
    val timeToFirstDollar: String = "48 Hours", // e.g. "24-48 Hours", "3-5 Days", "1 Week"
    val zeroCostStack: String = "Vercel Free, Supabase Free, Stripe Checkout, Organic Cold DMs",

    // Problem & Solution
    val problemStatement: String,
    val solutionDescription: String,
    val targetAudience: String, // e.g. "Mid-market Shopify store owners ($1M-$10M GMV)"
    val whyNowTrigger: String, // e.g. "Stripe API change, AI agent capability, new EU privacy mandate"

    // Revenue Engine Projections
    val pricingModel: String = "Monthly Subscription", // Tiered SaaS, Usage-based, Flat fee, Per seat
    val monthlyPricePerCustomer: Double = 99.0, // ARPU ($)
    val targetCustomersYear1: Int = 100,
    val estimatedMRR: Double = 9900.0,
    val estimatedARR: Double = 118800.0,
    val projectedYear1Revenue: Double = 85000.0,
    val projectedYear3Revenue: Double = 450000.0,
    val pricingTiersSummary: String = "Starter: $49/mo | Pro: $99/mo | Enterprise: $299/mo",

    // Effort & Work Required
    val devComplexity: DevComplexity = DevComplexity.MEDIUM_3_6_WEEKS,
    val weeksToMvp: Int = 4,
    val techStack: String = "Next.js, Supabase, Gemini API, Stripe, Tailwind",
    val estimatedMonthlyOperatingCost: Double = 120.0,
    val mvpFeatureScope: String = "1. Core scraper/extractor 2. Dashboard 3. Webhook delivery 4. Stripe checkout",

    // Competition & Moats
    val competitionLevel: String = "Medium", // Low, Medium, High
    val topCompetitors: String = "",
    val competitorWeaknesses: String = "", // e.g. "Expensive enterprise contracts ($10k+), no self-serve, complex setup"
    val unfairAdvantageMoat: String = "", // e.g. "Niche-specific workflow, 10x faster setup, native Slack integration"

    // Data Source & Ready Buyer Validation
    val dataSourceType: DataSourceType = DataSourceType.FOUNDER_INTERVIEW,
    val sourceUrlOrRef: String = "",
    val sourceQuote: String = "", // The raw founder/buyer complaint that inspired this
    val willingnessToPayRating: Int = 4, // 1 to 5 stars (5 = Urgent ready budget)
    val readyBuyerProfiles: String = "", // e.g. "B2B SaaS Founders with 10-50 employees who spend >$5k/mo on tooling"
    val actionableNextSteps: String = "1. DM 10 founders from source thread\n2. Set up pre-order landing page\n3. Build minimal prototype for first 3 pilot users",

    // Computed Opportunity Metrics
    val opportunityScore: Int = 78, // 0 - 100 composite score
    val notes: String = "",

    // Google Drive & Cloud Sync
    val isDriveSynced: Boolean = false,
    val driveFileId: String? = null,
    val lastDriveSyncTimestamp: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
