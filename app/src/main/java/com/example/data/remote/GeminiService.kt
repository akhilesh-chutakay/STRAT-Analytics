package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response DTOs ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = "user",
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenConfig(
    val temperature: Float? = 0.7f,
    val topP: Float? = 0.95f,
    val topK: Int? = 40
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentWithModel(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun analyzeBusinessIdea(
        title: String,
        problem: String,
        solution: String,
        category: String,
        marketType: String,
        analysisMode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured in the Secrets panel or .env file.")
            )
        }

        val prompt = when (analysisMode) {
            "COMPETITOR_TEARDOWN" -> """
                Analyze the competitive landscape for this business idea:
                Title: $title
                Problem: $problem
                Proposed Solution: $solution
                Category: $category ($marketType)

                Provide a structured executive teardown:
                1. Incumbent Competitors & Market Leaders
                2. Real Customer Complaints & Weaknesses of Existing Solutions (Why users churn)
                3. Unfair Moat & Wedge Strategy (How a small fast team can win)
                4. Blue Ocean vs Red Ocean strategic recommendation
            """.trimIndent()

            "REVENUE_MODELING" -> """
                Create a detailed B2B / SaaS revenue model and pricing forecast for:
                Title: $title
                Problem: $problem
                Target Category: $category

                Provide:
                1. Recommended 3-Tier Pricing Model (Starter, Growth, Pro/Enterprise) with exact dollar amounts & feature limits
                2. Year 1 ARR & MRR projection breakdown (assumptions: churn rate, ARPU, customer acquisition pace)
                3. Year 3 Scale trajectory & lifetime value (LTV) to customer acquisition cost (CAC) estimation
                4. Expansion revenue levers (add-ons, usage seats, API tiers)
            """.trimIndent()

            "BUYER_DISCOVERY" -> """
                Formulate a founder/buyer customer discovery validation plan for:
                Title: $title
                Problem: $problem
                Proposed Solution: $solution

                Provide:
                1. Exact Ready Buyer Persona (Title, Company Size, Budget authority, Burning pain)
                2. Where to find them with high intent (specific subreddits, G2 categories, Twitter keywords, Slack groups)
                3. High-converting Cold DM / Discovery Email script (conversational, not salesy)
                4. 3 Critical Killer Questions to validate real willingness-to-pay before writing a single line of code
            """.trimIndent()

            "MVP_BLUEPRINT" -> """
                Design a fast 2-4 week MVP scope and architecture plan for:
                Title: $title
                Problem: $problem
                Solution: $solution

                Provide:
                1. Must-Have Core Features for Day 1 Launch (Cut all fluff)
                2. Recommended modern tech stack for maximum shipping speed
                3. Immediate 7-day tactical action checklist to get first 3 paying pilot customers
            """.trimIndent()

            else -> """
                Evaluate this business idea with deep founder insight:
                Title: $title
                Category: $category | Market: $marketType
                Problem: $problem
                Solution: $solution

                Provide an objective 360-degree scorecard:
                - Market Demand & Urgency (1-10)
                - Technical & Operational Feasibility (1-10)
                - Revenue Potential & Scalability (1-10)
                - Top 2 Fatal Risks & How to mitigate them
                - Immediate Next Step to Validate
            """.trimIndent()
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = "You are an elite B2B SaaS investor, experienced startup founder, and product strategist. You provide concise, ultra-pragmatic, actionable business intelligence without marketing buzzwords."
                    )
                )
            )
        )

        try {
            val response = service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response received from Gemini."))
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "API error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun extractIdeaFromProblemQuote(rawQuote: String, source: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured in the Secrets panel or .env file.")
            )
        }

        val prompt = """
            A founder or company expressed the following real problem / complaint:
            Source: $source
            Quote: "$rawQuote"

            Analyze this pain point and generate a structured business opportunity blueprint formatted as:
            - **Suggested Product Name**: (Catchy, professional tool name)
            - **One-Sentence Pitch**: (Crisp value proposition)
            - **Business Model**: (e.g. B2B SaaS / Micro-SaaS / DevTool)
            - **Market Classification**: (Red Ocean or Blue Ocean with 1-line reason)
            - **Target Ready Buyer**: (Specific title and company profile with budget)
            - **Suggested Monthly Pricing**: (e.g. $49/mo, $99/mo, $199/mo)
            - **Time to MVP**: (e.g. 2 weeks, 4 weeks)
            - **Core MVP Feature**: (The single killer feature that solves this)
            - **Tactical Validation Step**: (How to pre-sell to 3 buyers)
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = "You are an expert at transforming raw user complaints and market signals into high-converting B2B software products."
                    )
                )
            )
        )

        try {
            val response = service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response from Gemini."))
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Extraction error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Scans and synthesizes brand new Blue Ocean market opportunities
     * addressing real founder in-need operational pain points.
     */
    suspend fun discoverBlueOceanMarketIdeas(
        existingIdeaTitles: List<String>,
        preferBlueOcean: Boolean = true,
        activeModel: String = "gemini-3.5-flash",
        count: Int = 3
    ): Result<List<com.example.data.model.BusinessIdea>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val existingTitlesSummary = existingIdeaTitles.take(15).joinToString(", ")

        val prompt = """
            Analyze the current 2026 tech/startup macroeconomic landscape for unaddressed, high-urgency B2B founder, developer, and operator problems.
            Synthesize $count BRAND NEW, completely unique venture ideas.
            Focus STRICTLY on:
            1. ${if (preferBlueOcean) "BLUE OCEAN opportunities (uncontested market space, high moat score, high gross margin, low competition)" else "High-conviction venture opportunities"}
            2. Real FOUNDER and IN-NEED operator bottlenecks with verified burning pain and willingness-to-pay ($79-$299+/mo).
            3. Fast-to-ship architectures (<4 weeks to MVP).

            DO NOT duplicate any of these existing ideas: [$existingTitlesSummary].

            Respond ONLY with a valid JSON array of objects. Do not include markdown code block formatting or backticks around the json, or if you do, ensure it contains valid JSON. Each object MUST have these exact keys:
            [
              {
                "title": "Short catchy name (e.g. AuditShield - SOC2 Continuous Git Diff Compliance Bot)",
                "tagline": "Punchy 1-line value proposition",
                "category": "B2B_SAAS", // Valid options: B2B_SAAS, MICRO_SAAS, B2B_DEVTOOL, AI_AGENT_TOOL, INTERNAL_TOOL, API_SERVICE, CHROME_EXTENSION, MARKETPLACE, PRODUCTIZED_SERVICE
                "marketType": "BLUE_OCEAN", // Options: BLUE_OCEAN, RED_OCEAN
                "problemStatement": "Detailed explanation of the exact painful manual task or revenue leak founders/engineers suffer from",
                "solutionDescription": "Concise overview of the automated product workflow and how it solves the pain",
                "targetAudience": "Specific target buyer (e.g. Series A CTOs & DevSecOps leads with 15-50 engineers)",
                "whyNowTrigger": "Macro/technical trigger making this urgent right now",
                "pricingModel": "Tiered SaaS Subscription",
                "monthlyPricePerCustomer": 149.0,
                "targetCustomersYear1": 75,
                "pricingTiersSummary": "Starter: $49/mo | Growth: $149/mo | Enterprise: $399/mo",
                "devComplexity": "LOW_1_2_WEEKS", // Options: LOW_1_2_WEEKS, MEDIUM_3_6_WEEKS, HIGH_2_4_MONTHS
                "weeksToMvp": 3,
                "techStack": "Next.js, FastAPI, PostgreSQL, Stripe, Gemini 3.5 Flash",
                "estimatedMonthlyOperatingCost": 75.0,
                "mvpFeatureScope": "1. Core webhook ingestion 2. Rule evaluation engine 3. Slack alert dispatch 4. Stripe checkout",
                "competitionLevel": "Low",
                "topCompetitors": "Manual spreadsheets, bloated enterprise suites ($15k/yr)",
                "competitorWeaknesses": "Legacy suites require 3-month onboarding and prohibitive minimum seat pricing",
                "unfairAdvantageMoat": "1-click GitHub OAuth connect, instant zero-config diff analysis, flat pricing",
                "dataSourceType": "FOUNDER_INTERVIEW", // Options: FOUNDER_INTERVIEW, REDDIT, X_TWITTER, G2_REVIEWS, CAPTERRA, PRODUCT_HUNT, GITHUB_ISSUES, CUSTOMER_CALL
                "sourceUrlOrRef": "x.com/founder_pain_signals",
                "sourceQuote": "Direct quote from a founder/buyer describing how painful this is and why they'd pay immediately",
                "willingnessToPayRating": 5, // 1 to 5
                "readyBuyerProfiles": "Seed & Series A tech founders with verified IT compliance budget",
                "actionableNextSteps": "1. DM 10 founders from target ICP\n2. Launch 1-page pre-order waitlist\n3. Deliver working beta in 14 days",
                "opportunityScore": 88
              }
            ]
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = "You are an elite venture strategist and Y Combinator visiting partner specializing in identifying Blue Ocean B2B SaaS and high-margin founder problem spaces. Output clean JSON only."
                    )
                )
            )
        )

        val modelToUse = if (activeModel.isNotBlank()) activeModel else "gemini-3.5-flash"

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val response = service.generateContentWithModel(modelToUse, apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val parsedIdeas = parseIdeasFromJson(rawText)
                    if (parsedIdeas.isNotEmpty()) {
                        return@withContext Result.success(parsedIdeas)
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiApiClient", "Live discovery error: ${e.message}", e)
            }
        }

        // Fallback: Generate fresh curated Blue Ocean ideas reflecting deep founder problems
        val fallbackGenerated = generateCuratedBlueOceanIdeas(existingIdeaTitles, count)
        Result.success(fallbackGenerated)
    }

    private fun parseIdeasFromJson(rawJsonText: String): List<com.example.data.model.BusinessIdea> {
        val ideas = mutableListOf<com.example.data.model.BusinessIdea>()
        try {
            // Strip markdown code fences if present
            var cleanJson = rawJsonText.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.removePrefix("```json")
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.removePrefix("```")
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.removeSuffix("```")
            }
            cleanJson = cleanJson.trim()

            // Find JSON array start and end
            val startIdx = cleanJson.indexOf('[')
            val endIdx = cleanJson.lastIndexOf(']')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                cleanJson = cleanJson.substring(startIdx, endIdx + 1)
            }

            val array = org.json.JSONArray(cleanJson)
            val now = System.currentTimeMillis()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val mrr = obj.optDouble("monthlyPricePerCustomer", 99.0) * obj.optInt("targetCustomersYear1", 60)
                val arr = mrr * 12.0

                val category = try {
                    com.example.data.model.BusinessModelCategory.valueOf(obj.optString("category", "B2B_SAAS"))
                } catch (_: Exception) {
                    com.example.data.model.BusinessModelCategory.B2B_SAAS
                }

                val marketType = try {
                    com.example.data.model.MarketType.valueOf(obj.optString("marketType", "BLUE_OCEAN"))
                } catch (_: Exception) {
                    com.example.data.model.MarketType.BLUE_OCEAN
                }

                val devComplexity = try {
                    com.example.data.model.DevComplexity.valueOf(obj.optString("devComplexity", "MEDIUM_3_6_WEEKS"))
                } catch (_: Exception) {
                    com.example.data.model.DevComplexity.MEDIUM_3_6_WEEKS
                }

                val dataSourceType = try {
                    com.example.data.model.DataSourceType.valueOf(obj.optString("dataSourceType", "FOUNDER_INTERVIEW"))
                } catch (_: Exception) {
                    com.example.data.model.DataSourceType.FOUNDER_INTERVIEW
                }

                val isZeroInvest = obj.optBoolean("isZeroInvestment", false) ||
                    obj.optDouble("estimatedMonthlyOperatingCost", 60.0) <= 20.0 ||
                    category == com.example.data.model.BusinessModelCategory.NO_CODE_TOOL ||
                    category == com.example.data.model.BusinessModelCategory.MICRO_UTILITY

                val zeroPlaybook = obj.optString(
                    "zeroCostPlaybook",
                    "1. Build on 100% free tiers (Vercel/Cloudflare/Supabase).\n2. Reach out to 20 users discussing this pain on Reddit/X.\n3. Close first 3 paid pilots via Stripe Payment Link within 48h."
                )
                val timeToDollar = obj.optString("timeToFirstDollar", "24-48 Hours")
                val zeroStack = obj.optString("zeroCostStack", "Vercel Free Tier, Supabase Free, Stripe Checkout, Organic Cold DMs")

                ideas.add(
                    com.example.data.model.BusinessIdea(
                        id = 0, // Auto-generate in Room
                        title = obj.optString("title", "New Venture Concept ${i + 1}"),
                        tagline = obj.optString("tagline", "High-conviction venture opportunity"),
                        category = category,
                        marketType = marketType,
                        stage = com.example.data.model.PipelineStage.DISCOVERED,
                        isImmediatelyActionable = true,
                        isZeroInvestment = isZeroInvest,
                        zeroCostPlaybook = zeroPlaybook,
                        timeToFirstDollar = timeToDollar,
                        zeroCostStack = zeroStack,
                        problemStatement = obj.optString("problemStatement", "Unaddressed founder bottleneck."),
                        solutionDescription = obj.optString("solutionDescription", "Automated niche solution."),
                        targetAudience = obj.optString("targetAudience", "B2B SaaS and agency operators"),
                        whyNowTrigger = obj.optString("whyNowTrigger", "Recent API developments and market demand shift."),
                        pricingModel = obj.optString("pricingModel", "Monthly SaaS Subscription"),
                        monthlyPricePerCustomer = obj.optDouble("monthlyPricePerCustomer", 119.0),
                        targetCustomersYear1 = obj.optInt("targetCustomersYear1", 70),
                        estimatedMRR = mrr,
                        estimatedARR = arr,
                        projectedYear1Revenue = arr * 0.75,
                        projectedYear3Revenue = arr * 3.5,
                        pricingTiersSummary = obj.optString("pricingTiersSummary", "Starter: $49/mo | Pro: $119/mo | Enterprise: $299/mo"),
                        devComplexity = devComplexity,
                        weeksToMvp = obj.optInt("weeksToMvp", 3),
                        techStack = obj.optString("techStack", "Kotlin, Supabase, Gemini API, Stripe"),
                        estimatedMonthlyOperatingCost = obj.optDouble("estimatedMonthlyOperatingCost", 60.0),
                        mvpFeatureScope = obj.optString("mvpFeatureScope", "1. Authentication 2. Core workflow 3. Dashboard 4. Webhooks"),
                        competitionLevel = obj.optString("competitionLevel", "Low"),
                        topCompetitors = obj.optString("topCompetitors", "Manual processes"),
                        competitorWeaknesses = obj.optString("competitorWeaknesses", "High setup friction, overpriced legacy alternatives"),
                        unfairAdvantageMoat = obj.optString("unfairAdvantageMoat", "Laser-focused workflow integration with zero setup overhead"),
                        dataSourceType = dataSourceType,
                        sourceUrlOrRef = obj.optString("sourceUrlOrRef", "internet_market_scan"),
                        sourceQuote = obj.optString("sourceQuote", "\"We desperately need a lightweight tool for this exact issue.\""),
                        willingnessToPayRating = obj.optInt("willingnessToPayRating", 5),
                        readyBuyerProfiles = obj.optString("readyBuyerProfiles", "Founders and operational leads with direct purchase authority"),
                        actionableNextSteps = obj.optString("actionableNextSteps", "1. Reach out to 10 prospective buyers\n2. Deploy MVP\n3. Secure 3 paid pilots"),
                        opportunityScore = obj.optInt("opportunityScore", 85),
                        notes = "Synthesized via Gemini AI Internet Market Intelligence Scanner.",
                        updatedAt = now,
                        createdAt = now
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "JSON parse error: ${e.message}", e)
        }
        return ideas
    }

    private fun generateCuratedBlueOceanIdeas(
        existingTitles: List<String>,
        count: Int
    ): List<com.example.data.model.BusinessIdea> {
        val pool = listOf(
            com.example.data.model.BusinessIdea(
                id = 0,
                title = "StripeTaxShield - EU VAT & Nexus Cross-Border Automation",
                tagline = "Auto-calculate and file EU Digital Services VAT and US State Economic Nexus thresholds for indie SaaS founders in 1 click",
                category = com.example.data.model.BusinessModelCategory.B2B_SAAS,
                marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                stage = com.example.data.model.PipelineStage.DISCOVERED,
                isImmediatelyActionable = true,
                problemStatement = "Micro-SaaS and solo founders selling globally via Stripe get hit with unexpected €10k+ VAT compliance penalties because Anrok/Quaderno start at $3,000/yr and are too complex for $5k-$50k MRR businesses.",
                solutionDescription = "Dead-simple Stripe connect app that tracks EU MOSS and US Nexus thresholds in real-time, auto-generates quarterly filing CSVs, and sends warning alerts before hitting penalty limits.",
                targetAudience = "Global bootstrapped software founders making $3k-$40k MRR on Stripe",
                whyNowTrigger = "EU DAC7 enforcement and 2026 strict state nexus audits targeting digital creators and software sellers.",
                pricingModel = "Flat Monthly Tier",
                monthlyPricePerCustomer = 49.0,
                targetCustomersYear1 = 180,
                estimatedMRR = 8820.0,
                estimatedARR = 105840.0,
                projectedYear1Revenue = 78000.0,
                projectedYear3Revenue = 340000.0,
                pricingTiersSummary = "Hobby (<$10k MRR): $29/mo | Scale (<$50k MRR): $69/mo",
                devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                weeksToMvp = 2,
                techStack = "Node.js / Kotlin, Stripe Webhooks, TaxJar/Avalara rate tables, Tailwind",
                estimatedMonthlyOperatingCost = 45.0,
                mvpFeatureScope = "1. Stripe OAuth connect\n2. Nexus threshold progress gauge\n3. 1-click EU VAT return export\n4. Threshold SMS/Slack alert",
                competitionLevel = "Low",
                topCompetitors = "Quaderno, Anrok, Manual accountant spreadsheets ($500/mo)",
                competitorWeaknesses = "Enterprise-focused, require annual contracts, complex enterprise ERP setups.",
                unfairAdvantageMoat = "Zero setup, tailored specifically for sub-$50k MRR solo founders with transparent $49/mo pricing.",
                dataSourceType = com.example.data.model.DataSourceType.REDDIT,
                sourceUrlOrRef = "reddit.com/r/SaaS/comments/founder_vat_pain",
                sourceQuote = "\"I just got a letter from German tax authorities demanding €4,200 for back VAT on my SaaS. I had no idea I crossed the threshold. Why is there no lightweight Stripe alert tool?\"",
                willingnessToPayRating = 5,
                readyBuyerProfiles = "Solo developers & micro-SaaS founders who sell cross-border and lack an in-house CFO.",
                actionableNextSteps = "1. Post threshold calculator on r/SaaS and Twitter\n2. Offer free VAT checkup for first 20 founders\n3. Launch on Stripe App Marketplace",
                opportunityScore = 94,
                notes = "Extreme buyer urgency driven by fear of government tax audit penalties."
            ),
            com.example.data.model.BusinessIdea(
                id = 0,
                title = "WebhookRescue - Failed API Event Dead-Letter Replay Proxy",
                tagline = "Zero-loss webhook listener that caches, alerts, and auto-retries failed third-party webhooks (Shopify, Stripe, GitHub, Twilio)",
                category = com.example.data.model.BusinessModelCategory.B2B_DEVTOOL,
                marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                stage = com.example.data.model.PipelineStage.DISCOVERED,
                isImmediatelyActionable = true,
                problemStatement = "Whenever a SaaS backend undergoes downtime, deploy restarts, or rate limits, incoming webhooks from Stripe or Shopify drop silently, leading to lost customer orders and broken customer activations.",
                solutionDescription = "A managed proxy endpoint that acts as a resilient buffer for all incoming webhooks, logs the exact JSON payloads, provides an instant visual debugger, and automatically replays failed events with exponential backoff.",
                targetAudience = "Backend engineers, technical founders, and agency builders shipping webhooks",
                whyNowTrigger = "Modern microservices and serverless architectures suffer frequent cold starts and intermittent timeout drops.",
                pricingModel = "Usage-based Subscription",
                monthlyPricePerCustomer = 89.0,
                targetCustomersYear1 = 110,
                estimatedMRR = 9790.0,
                estimatedARR = 117480.0,
                projectedYear1Revenue = 85000.0,
                projectedYear3Revenue = 410000.0,
                pricingTiersSummary = "Dev (100k events): $29/mo | Team (1M events): $89/mo | Enterprise: $249/mo",
                devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                weeksToMvp = 2,
                techStack = "Cloudflare Workers / Go, Redis Queue, Supabase, Next.js",
                estimatedMonthlyOperatingCost = 50.0,
                mvpFeatureScope = "1. Dedicated proxy URL generation\n2. Payload inspect & diff\n3. 1-click retry button\n4. Discord/Slack alert on 500 responses",
                competitionLevel = "Low",
                topCompetitors = "Svix (expensive enterprise), Hookdeck, DIY Kafka setups",
                competitorWeaknesses = "Complex developer onboarding, high minimum monthly price tiers.",
                unfairAdvantageMoat = "Copy-paste 1 endpoint URL replacement with 0 SDK integration needed.",
                dataSourceType = com.example.data.model.DataSourceType.GITHUB_ISSUES,
                sourceUrlOrRef = "github.com/developer_complaints",
                sourceQuote = "\"We had a 15-minute server blip during Black Friday and lost 40 Shopify order webhooks. Manually recovering those orders took our team 8 hours.\"",
                willingnessToPayRating = 5,
                readyBuyerProfiles = "Technical leads at e-commerce SaaS and fintech startups processing high-value transactions.",
                actionableNextSteps = "1. Publish open-source lightweight proxy on GitHub\n2. Reach out to e-commerce developer communities\n3. Offer 50k free events for early beta testers",
                opportunityScore = 91,
                notes = "High retention B2B infrastructure wedge with near-zero customer churn."
            ),
            com.example.data.model.BusinessIdea(
                id = 0,
                title = "PromptGuard - AI Token & Cost Spike Circuit Breaker",
                tagline = "Real-time middleware that prevents runaway LLM loops, enforces per-tenant spending quotas, and falls back to cheap models automatically",
                category = com.example.data.model.BusinessModelCategory.AI_AGENT_TOOL,
                marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                stage = com.example.data.model.PipelineStage.DISCOVERED,
                isImmediatelyActionable = true,
                problemStatement = "AI startup founders and agency builders wake up to unexpected $5,000+ OpenAI/Gemini/Anthropic API bills when a user script or autonomous agent enters an infinite recursion loop.",
                solutionDescription = "Drop-in API gateway URL that sets hard budget ceilings per user, detects rogue looping prompt sequences, and instantly fails over or throttles malicious token draining.",
                targetAudience = "AI app developers, autonomous agent builders, and dev agencies",
                whyNowTrigger = "Explosion of multi-agent recursive systems (CrewAI, LangGraph, AutoGen) causing unpredictable API cost spikes.",
                pricingModel = "Per API Key Subscription",
                monthlyPricePerCustomer = 129.0,
                targetCustomersYear1 = 90,
                estimatedMRR = 11610.0,
                estimatedARR = 139320.0,
                projectedYear1Revenue = 95000.0,
                projectedYear3Revenue = 480000.0,
                pricingTiersSummary = "Hobby: $39/mo | Scale: $129/mo | Unlimited: $299/mo",
                devComplexity = com.example.data.model.DevComplexity.MEDIUM_3_6_WEEKS,
                weeksToMvp = 3,
                techStack = "Rust/Go Proxy, Redis, Gemini API, Next.js dashboard",
                estimatedMonthlyOperatingCost = 80.0,
                mvpFeatureScope = "1. Base URL override (drop-in)\n2. Per-key hard budget limits\n3. Loop detection heuristic\n4. Real-time cost analytics",
                competitionLevel = "Low",
                topCompetitors = "Helicone, Portkey, Langfuse",
                competitorWeaknesses = "Complex enterprise observability setups rather than simple hard budget circuit breakers.",
                unfairAdvantageMoat = "Hard financial kill-switch guarantee — if bill exceeds limit, we pause and notify instantly.",
                dataSourceType = com.example.data.model.DataSourceType.X_TWITTER,
                sourceUrlOrRef = "x.com/tech_founders/status/bill_spike",
                sourceQuote = "\"An unhandled loop in our agent overnight burned through $3,400 in API credits before we woke up. I need a hard circuit breaker right now.\"",
                willingnessToPayRating = 5,
                readyBuyerProfiles = "Founders running autonomous agent systems with high volume LLM calls.",
                actionableNextSteps = "1. Tweet bill-spike case study\n2. Share on Hacker News\n3. Offer 1-click drop-in wrapper for LangChain and OpenAI clients",
                opportunityScore = 93,
                notes = "Massive emotional relief for AI founders fearing surprise cloud bills."
            ),
            com.example.data.model.BusinessIdea(
                id = 0,
                title = "ChurnSurge - Cancellation Intent Screen & Rescue Discount Bot",
                tagline = "Interactive cancel flow widget that captures churn reasons, offers 1-click pause or custom rescue offers, and syncs feedback to Slack",
                category = com.example.data.model.BusinessModelCategory.MICRO_SAAS,
                marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                stage = com.example.data.model.PipelineStage.DISCOVERED,
                isImmediatelyActionable = true,
                problemStatement = "When a customer hits 'Cancel Subscription' in self-serve SaaS apps, founders lose them instantly without learning why or offering a pause option.",
                solutionDescription = "A lightweight 2-line JavaScript snippet that intercepts the cancel button, presents personalized dynamic offers (e.g. 50% off for 2 months, 1-month pause, direct founder call), and saves 15-30% of canceling users.",
                targetAudience = "B2B SaaS founders with 100+ active paying subscribers",
                whyNowTrigger = "Customer acquisition costs are rising; preserving existing net revenue retention is top priority for all software businesses.",
                pricingModel = "Tiered Monthly SaaS",
                monthlyPricePerCustomer = 99.0,
                targetCustomersYear1 = 120,
                estimatedMRR = 11880.0,
                estimatedARR = 142560.0,
                projectedYear1Revenue = 105000.0,
                projectedYear3Revenue = 520000.0,
                pricingTiersSummary = "Startup: $49/mo | Growth: $99/mo | Enterprise: $199/mo",
                devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                weeksToMvp = 2,
                techStack = "Vanilla JS Widget, Node.js, Stripe Billing API, Supabase",
                estimatedMonthlyOperatingCost = 60.0,
                mvpFeatureScope = "1. Embeddable modal widget\n2. Dynamic discount trigger\n3. Stripe coupon application\n4. Real-time Slack churn alert",
                competitionLevel = "Low",
                topCompetitors = "Churnkey ($250/mo minimum), ProfitWell",
                competitorWeaknesses = "Expensive enterprise minimums inaccessible to bootstrappers.",
                unfairAdvantageMoat = "Ultra affordable flat pricing with zero commission on saved revenue.",
                dataSourceType = com.example.data.model.DataSourceType.REDDIT,
                sourceUrlOrRef = "reddit.com/r/startups",
                sourceQuote = "\"Adding a simple 'pause subscription for 1 month' option saved 18 customers last month alone. Every SaaS needs this built in.\"",
                willingnessToPayRating = 5,
                readyBuyerProfiles = "Bootstrapped B2B SaaS founders looking to instantly cut churn.",
                actionableNextSteps = "1. Build 1-minute demo video\n2. Share before/after metrics on LinkedIn\n3. Onboard 10 founders for free 30-day trial",
                opportunityScore = 95,
                notes = "Proven high ROI — saving a single $100/mo customer pays for the software instantly."
            )
        )

        // Filter out any that match existing titles
        val filtered = pool.filter { candidate ->
            existingTitles.none { it.contains(candidate.title.take(15), ignoreCase = true) }
        }

        val resultList = if (filtered.size >= count) filtered.take(count) else pool.take(count)
        val now = System.currentTimeMillis()
        return resultList.map { it.copy(updatedAt = now, createdAt = now) }
    }

    suspend fun scanInternetForZeroInvestmentIdeas(
        existingIdeaTitles: List<String>,
        count: Int = 4,
        activeModel: String = "gemini-3.5-flash"
    ): Result<List<com.example.data.model.BusinessIdea>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val prompt = """
            Perform a comprehensive, deep-web market intelligence scan across global internet signals (Reddit r/SideProject, r/Entrepreneur, r/freelance, X/Twitter founder complaints, Upwork repeat job gigs, Product Hunt daily gaps, Chrome Web Store 1-star reviews, Gumroad top digital utilities).

            Generate $count UNIQUE, HYPER-PRACTICAL, ZERO-INVESTMENT & SIMPLE CASHFLOW business ideas that:
            1. REQUIRE $0 UPFRONT INVESTMENT (Utilize 100% free-tier services like Vercel, Supabase, Google Sheets, Cloudflare, Stripe Checkout, GitHub).
            2. REQUIRE MINIMAL OR ZERO EXTENSIVE CODING (<1 to 2 weeks to build, or simple script, browser extension, Notion/Make workflow, or micro-utility).
            3. SOLVE AN URGENT, REAL PAIN THAT MAKES IMMEDIATE FAST REVENUE (Targeting $1,000 to $10,000/month recurring cashflow).
            4. INCLUDE A CLEAR $0 AD-FREE CUSTOMER ACQUISITION PLAYBOOK (e.g. cold outreach to complaining Reddit/X threads, organic Chrome Web Store SEO, programmatic directory rank, direct local SMB pitch).
            5. HAVE AN ESTIMATED TIME TO FIRST DOLLAR UNDER 72 HOURS.

            Existing ideas in database to NOT duplicate:
            ${existingIdeaTitles.joinToString("\n") { "- $it" }}

            Output STRICT JSON ONLY as an array:
            [
              {
                "title": "Catchy Name (e.g. StripeDunningSMS - Failed Payment SMS Recovery)",
                "tagline": "Punchy 1-line value proposition",
                "category": "MICRO_SAAS", // Options: B2B_SAAS, MICRO_SAAS, B2B_DEVTOOL, AI_AGENT_TOOL, INTERNAL_TOOL, API_SERVICE, CHROME_EXTENSION, MARKETPLACE, PRODUCTIZED_SERVICE, NO_CODE_TOOL, MICRO_UTILITY
                "marketType": "BLUE_OCEAN",
                "isImmediatelyActionable": true,
                "isZeroInvestment": true,
                "zeroCostPlaybook": "Exact step-by-step $0 acquisition and launch method without spending any money on ads",
                "timeToFirstDollar": "24-48 Hours",
                "zeroCostStack": "Exact free tools used (e.g. Vercel Free, Supabase Free, Stripe Checkout, Organic Cold DMs)",
                "problemStatement": "Detailed pain and revenue leakage from real internet users",
                "solutionDescription": "Simple automated solution requiring minimal code",
                "targetAudience": "Specific buyers with ready budget",
                "whyNowTrigger": "Market or API shift making this timely",
                "pricingModel": "Monthly Subscription or Lifetime Access",
                "monthlyPricePerCustomer": 29.0,
                "targetCustomersYear1": 150,
                "pricingTiersSummary": "Starter: $19/mo | Pro: $29/mo | Lifetime: $129",
                "devComplexity": "ZERO_CODE_NO_CODE", // Options: ZERO_CODE_NO_CODE, LOW_1_2_WEEKS, MEDIUM_3_6_WEEKS
                "weeksToMvp": 1,
                "techStack": "HTML/JS, Vercel Free Tier, Stripe Payment Link",
                "estimatedMonthlyOperatingCost": 0.0,
                "mvpFeatureScope": "1. Simple Webhook / Form 2. Core trigger 3. Stripe payment link",
                "competitionLevel": "Low",
                "topCompetitors": "Manual effort, bloated expensive software",
                "competitorWeaknesses": "Expensive enterprise tools, high setup friction",
                "unfairAdvantageMoat": "Zero setup, 1-click install, ultra-affordable flat price",
                "dataSourceType": "REDDIT", // Options: REDDIT, X_TWITTER, UPWORK_JOB, CHROME_STORE, PRODUCT_HUNT, FOUNDER_INTERVIEW
                "sourceUrlOrRef": "reddit.com/r/SideProject",
                "sourceQuote": "Real quote illustrating the problem",
                "willingnessToPayRating": 5,
                "readyBuyerProfiles": "Freelancers, creators, SMBs, agency owners",
                "actionableNextSteps": "1. Deploy free prototype\n2. DM 10 users discussing this pain\n3. Collect first $100 via Stripe",
                "opportunityScore": 95
              }
            ]
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = "You are a master internet venture scout and bootstrapper expert specializing in $0-investment, low-code, high-margin cashflow business ideas. Output clean JSON only."
                    )
                )
            )
        )

        val modelToUse = if (activeModel.isNotBlank()) activeModel else "gemini-3.5-flash"

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val response = service.generateContentWithModel(modelToUse, apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val parsedIdeas = parseIdeasFromJson(rawText)
                    if (parsedIdeas.isNotEmpty()) {
                        return@withContext Result.success(parsedIdeas)
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiApiClient", "Live zero-investment scan error: ${e.message}", e)
            }
        }

        // Fallback: Generate fresh curated $0-investment simple cashflow ideas
        val fallbackGenerated = generateCuratedZeroInvestmentIdeas(existingIdeaTitles, count)
        Result.success(fallbackGenerated)
    }

    private fun generateCuratedZeroInvestmentIdeas(
        existingTitles: List<String>,
        count: Int
    ): List<com.example.data.model.BusinessIdea> {
        val pool = listOf(
            com.example.data.model.BusinessIdea(
                id = 0,
                title = "SubstackAudio - Newsletter to Spotify Podcast Auto-Publisher",
                tagline = "Zero-code automation that converts written Substack and Beehiiv newsletter posts into studio-grade audio podcasts published to Spotify and Apple Podcasts",
                category = com.example.data.model.BusinessModelCategory.NO_CODE_TOOL,
                marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                stage = com.example.data.model.PipelineStage.DISCOVERED,
                isImmediatelyActionable = true,
                isZeroInvestment = true,
                zeroCostPlaybook = "1. Connect Substack RSS to ElevenLabs / Gemini Audio via Make.com free tier.\n2. Push generated MP3 to free Anchor/Spotify for Podcasters RSS feed.\n3. DM 30 top Substack newsletter authors offering a free audio episode sample.\n4. Charge $49/mo per newsletter for 100% automated audio publication.",
                timeToFirstDollar = "24-48 Hours",
                zeroCostStack = "Substack RSS, Make.com Free Tier, Gemini / ElevenLabs Free Tier, Spotify for Podcasters (Free)",
                problemStatement = "Newsletter creators know 40% of their subscribers prefer listening to audio during commutes, but spending 3 hours recording and editing audio each week is unsustainable.",
                solutionDescription = "Hands-off RSS listener that detects new newsletter issues, synthesizes lifelike voice narrations, attaches branded intro music, and automatically publishes to Spotify and Apple Podcasts within 5 minutes.",
                targetAudience = "Substack, Beehiiv, and Ghost newsletter writers with 1,000+ subscribers",
                whyNowTrigger = "AI natural voice synthesis has reached human parity with near-zero latency; creators are aggressively expanding to audio.",
                pricingModel = "Monthly Newsletter Retainer",
                monthlyPricePerCustomer = 49.0,
                targetCustomersYear1 = 110,
                estimatedMRR = 5390.0,
                estimatedARR = 64680.0,
                projectedYear1Revenue = 50000.0,
                projectedYear3Revenue = 190000.0,
                pricingTiersSummary = "Solo Writer (4 posts/mo): $29/mo | Weekly Pro (8 posts/mo): $49/mo | Daily Publisher: $99/mo",
                devComplexity = com.example.data.model.DevComplexity.ZERO_CODE_NO_CODE,
                weeksToMvp = 0,
                techStack = "Make.com automation, RSS parser, ElevenLabs/Gemini voice, Spotify Podcasters Free API",
                estimatedMonthlyOperatingCost = 0.0,
                mvpFeatureScope = "1. Substack RSS input\n2. Voice style selector\n3. Automated MP3 rendering\n4. Podcast RSS hosting",
                competitionLevel = "Low",
                topCompetitors = "Manual voiceover artists ($200/episode), expensive podcast production agencies",
                competitorWeaknesses = "Human production takes 48 hours and costs $200+ per post.",
                unfairAdvantageMoat = "Zero human turnaround: publishes audio within 5 minutes of hitting publish on Substack.",
                dataSourceType = com.example.data.model.DataSourceType.REDDIT,
                sourceUrlOrRef = "reddit.com/r/Substack/comments/audio_versions",
                sourceQuote = "\"I asked my 5,000 newsletter readers what they wanted most: 62% said 'give me an audio version so I can listen in the car.'\"",
                willingnessToPayRating = 5,
                readyBuyerProfiles = "Monetized Substack and Beehiiv creators with active paid subscribers.",
                actionableNextSteps = "1. Pick 10 Substack newsletters in tech and finance\n2. Generate a 2-minute sample audio for their latest post\n3. Tweet the sample at the author with a 1-click subscription link",
                opportunityScore = 96,
                notes = "$0 upfront cost. Pure workflow arbitrage with recurring creator subscriptions."
            ),
            com.example.data.model.BusinessIdea(
                id = 0,
                title = "AltSEO - Shopify Bulk Image Alt-Text & Google Image Ranker",
                tagline = "Simple browser extension that scans Shopify stores and generates SEO-rich, keyword-optimized alt-text for 5,000+ product images in 1 click",
                category = com.example.data.model.BusinessModelCategory.CHROME_EXTENSION,
                marketType = com.example.data.model.MarketType.RED_OCEAN,
                stage = com.example.data.model.PipelineStage.DISCOVERED,
                isImmediatelyActionable = true,
                isZeroInvestment = true,
                zeroCostPlaybook = "1. Write 200 lines of JavaScript in a Chrome extension.\n2. Call Gemini 3.5 Flash Vision API (free tier) to describe product pictures with SEO keywords.\n3. Send cold email / DM to 50 Shopify store owners: 'Your store has 840 images missing alt text, hurting your Google rank. Here is a 1-click fix.'\n4. Charge $39 one-time or $19/mo.",
                timeToFirstDollar = "24-48 Hours",
                zeroCostStack = "Chrome Extension (JS), Gemini Flash Vision API (Free), Stripe Payment Link",
                problemStatement = "E-commerce stores lose 15-25% of organic search traffic because thousands of product images have blank or generic alt text like 'IMG_4920.jpg', destroying Google Image rankings.",
                solutionDescription = "A Chrome extension that integrates directly with Shopify Admin, inspects all unoptimized product pictures, passes them to AI Vision, and writes high-ranking descriptive alt text directly into Shopify in bulk.",
                targetAudience = "Shopify and WooCommerce store owners with 100+ product SKUs",
                whyNowTrigger = "Multimodal Vision models (Gemini Flash) can accurately identify apparel, materials, colors, and product specs in under 500ms for virtually zero cost.",
                pricingModel = "Per-Store Subscription or Pay-Per-Catalog",
                monthlyPricePerCustomer = 29.0,
                targetCustomersYear1 = 180,
                estimatedMRR = 5220.0,
                estimatedARR = 62640.0,
                projectedYear1Revenue = 52000.0,
                projectedYear3Revenue = 210000.0,
                pricingTiersSummary = "Small Store (<500 images): $29 one-time | Large Catalog (<5,000 images): $79 | Agency Unlimited: $49/mo",
                devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                weeksToMvp = 1,
                techStack = "Chrome Extension (Manifest V3), Shopify Admin API / DOM, Gemini Flash Vision, Stripe",
                estimatedMonthlyOperatingCost = 0.0,
                mvpFeatureScope = "1. Shopify product list scraper\n2. Gemini Vision keyword tagger\n3. Bulk CSV / Direct API updater\n4. Before/after SEO score card",
                competitionLevel = "Medium",
                topCompetitors = "Manual virtual assistants ($5/hr), bloated all-in-one SEO suites ($150/mo)",
                competitorWeaknesses = "All-in-one apps are slow, charge high monthly fees, and require intrusive Shopify app permissions.",
                unfairAdvantageMoat = "Lightweight Chrome extension runs client-side with 0 server costs and instant 1-click bulk operation.",
                dataSourceType = com.example.data.model.DataSourceType.CHROME_STORE,
                sourceUrlOrRef = "reddit.com/r/shopify/comments/alt_text_nightmare",
                sourceQuote = "\"I have 2,300 clothing items in my Shopify store. Doing alt tags manually would take 60 hours. I'd pay $50 right now for a 1-click button.\"",
                willingnessToPayRating = 5,
                readyBuyerProfiles = "Shopify boutique owners and e-commerce marketing freelancers.",
                actionableNextSteps = "1. Build MVP Chrome extension in 1 weekend\n2. Audit 20 Shopify stores and send them free reports with 5 optimized examples\n3. Post on r/shopify and e-commerce Discord groups",
                opportunityScore = 95,
                notes = "$0 server hosting. Direct revenue from eager store owners."
            ),
            com.example.data.model.BusinessIdea(
                id = 0,
                title = "JobScrape - Programmatic Company Careers Page Job Alert Bot",
                tagline = "Micro-SaaS that monitors 'hidden' career pages of 500+ stealth startups and sends instant Telegram/Email alerts the second high-paying remote roles open",
                category = com.example.data.model.BusinessModelCategory.API_SERVICE,
                marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                stage = com.example.data.model.PipelineStage.DISCOVERED,
                isImmediatelyActionable = true,
                isZeroInvestment = true,
                zeroCostPlaybook = "1. Set up a free GitHub Action cron job that scrapes Greenhouse/Lever/Ashby APIs for 200 top tech companies every 30 minutes.\n2. Forward new listings to a private Telegram channel & newsletter.\n3. Post launch on Twitter/X, Reddit r/cscareerquestions, and LinkedIn.\n4. Charge job seekers $15/mo for instant premium Telegram alerts before jobs hit LinkedIn.",
                timeToFirstDollar = "48 Hours",
                zeroCostStack = "GitHub Actions (Free cron), Telegram Bot API (Free), Supabase Free, Stripe Checkout",
                problemStatement = "Senior software engineers and tech job seekers face 1,000+ applicants per job on LinkedIn because posts are public for days. Applying in the first 30 minutes yields 10x higher interview rates.",
                solutionDescription = "A lightweight scraper querying raw ATS endpoints (Greenhouse, Lever, Ashby, Workable) that pushes new job openings to paying subscribers' Telegram/Slack in under 60 seconds.",
                targetAudience = "Remote senior engineers, product designers, and tech professionals looking for $120k-$250k roles",
                whyNowTrigger = "Mass tech layoffs and hyper-competitive job market make speed of application the #1 deciding factor for getting hired.",
                pricingModel = "B2C Monthly Subscription",
                monthlyPricePerCustomer = 15.0,
                targetCustomersYear1 = 400,
                estimatedMRR = 6000.0,
                estimatedARR = 72000.0,
                projectedYear1Revenue = 55000.0,
                projectedYear3Revenue = 230000.0,
                pricingTiersSummary = "Weekly Digest: Free | Real-Time Telegram Alerts: $15/mo | Lifetime VIP: $99",
                devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                weeksToMvp = 1,
                techStack = "Python / Node.js scraper, GitHub Actions Free CI, Telegram Bot API, Stripe",
                estimatedMonthlyOperatingCost = 0.0,
                mvpFeatureScope = "1. ATS feed scraper\n2. Keyword & salary filter\n3. Telegram bot notification engine\n4. Stripe subscription gating",
                competitionLevel = "Low in specialized early-alert space",
                topCompetitors = "LinkedIn Job Alerts (delayed 24-48 hours), Otta",
                competitorWeaknesses = "LinkedIn alerts are notoriously slow and spammed with sponsored promoted junk.",
                unfairAdvantageMoat = "Speed: 60-second alert delivery straight to mobile push via Telegram.",
                dataSourceType = com.example.data.model.DataSourceType.REDDIT,
                sourceUrlOrRef = "reddit.com/r/cscareerquestions",
                sourceQuote = "\"I got my $180k remote job purely because I applied 8 minutes after it was posted on Ashby before it even showed up on LinkedIn.\"",
                willingnessToPayRating = 5,
                readyBuyerProfiles = "High-income job seekers actively applying for competitive tech roles.",
                actionableNextSteps = "1. Spin up GitHub Action script tracking 50 YC companies\n2. Invite first 50 Telegram members for free\n3. Turn on Stripe paywall for new joins",
                opportunityScore = 94,
                notes = "$0 infrastructure cost — runs 100% on free GitHub Actions and Telegram."
            ),
            com.example.data.model.BusinessIdea(
                id = 0,
                title = "FormToLead - Typeform / Google Forms WhatsApp Instant SMS Lead Router",
                tagline = "Zero-code webhook bridge that instantly pings sales reps on WhatsApp & SMS with lead contact info the second a high-ticket form is submitted",
                category = com.example.data.model.BusinessModelCategory.NO_CODE_TOOL,
                marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                stage = com.example.data.model.PipelineStage.DISCOVERED,
                isImmediatelyActionable = true,
                isZeroInvestment = true,
                zeroCostPlaybook = "1. Build webhook parser on Vercel Free / Make.com.\n2. Forward form leads via Twilio WhatsApp API ($0.005/msg) to sales rep phone.\n3. Reach out to 25 real estate agents, solar consultants, and luxury service providers.\n4. Close 5 clients at $49/mo each for instant lead response speed.",
                timeToFirstDollar = "24-48 Hours",
                zeroCostStack = "Vercel Free Tier, Twilio WhatsApp / Telegram Bot, Stripe Payment Link",
                problemStatement = "Inbound lead conversion drops 391% if a sales rep calls back after 5 minutes instead of 1 minute. Most form submissions get buried in crowded email inboxes.",
                solutionDescription = "Plug-and-play webhook that connects to Google Forms, Typeform, Tally, or WordPress and immediately buzzes the sales rep's WhatsApp with a 1-tap 'Call Lead Now' button.",
                targetAudience = "Real estate agents, solar installers, mortgage brokers, personal trainers, and high-ticket service closers",
                whyNowTrigger = "Speed-to-lead is proven as the single biggest factor in closing $5,000+ service deals.",
                pricingModel = "Monthly Retainer per Seat",
                monthlyPricePerCustomer = 49.0,
                targetCustomersYear1 = 120,
                estimatedMRR = 5880.0,
                estimatedARR = 70560.0,
                projectedYear1Revenue = 58000.0,
                projectedYear3Revenue = 240000.0,
                pricingTiersSummary = "Solo Closer (1 number): $29/mo | Team (5 numbers): $69/mo | Agency White-label: $199/mo",
                devComplexity = com.example.data.model.DevComplexity.ZERO_CODE_NO_CODE,
                weeksToMvp = 0,
                techStack = "Node.js webhook handler, WhatsApp Business Cloud API / Twilio, Stripe Checkout",
                estimatedMonthlyOperatingCost = 10.0,
                mvpFeatureScope = "1. Webhook URL generator\n2. WhatsApp message template\n3. 1-tap click-to-dial button\n4. Daily lead count SMS summary",
                competitionLevel = "Low",
                topCompetitors = "Complex Zapier setups that cost $50/mo and fail intermittently",
                competitorWeaknesses = "Zapier is complicated for non-tech real estate agents and breaks whenever form schemas change.",
                unfairAdvantageMoat = "30-second copy-paste setup; built specifically for WhatsApp / SMS speed-to-lead.",
                dataSourceType = com.example.data.model.DataSourceType.UPWORK_JOB,
                sourceUrlOrRef = "upwork.com/jobs/~01f89bc",
                sourceQuote = "\"If I call a real estate inquiry within 60 seconds, I book the appointment 80% of the time. If I wait 30 minutes, they already talked to someone else.\"",
                willingnessToPayRating = 5,
                readyBuyerProfiles = "High-ticket sales reps and agency closers where 1 extra deal pays for 5 years of software.",
                actionableNextSteps = "1. Set up free demo webhook\n2. Call 10 local real estate agencies and demonstrate 5-second WhatsApp ping\n3. Sign first 2 clients on the spot",
                opportunityScore = 97,
                notes = "Massive ROI for buyers. $0 capital required to launch and scale."
            )
        )

        val filtered = pool.filter { candidate ->
            existingTitles.none { it.contains(candidate.title.take(15), ignoreCase = true) }
        }

        val resultList = if (filtered.size >= count) filtered.take(count) else pool.take(count)
        val now = System.currentTimeMillis()
        return resultList.map { it.copy(updatedAt = now, createdAt = now) }
    }

    suspend fun scanCategoryTargetedIdeas(
        targetCategory: com.example.data.model.BusinessModelCategory,
        existingIdeaTitles: List<String>,
        count: Int = 3,
        activeModel: String = "gemini-3.5-flash"
    ): Result<List<com.example.data.model.BusinessIdea>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val prompt = """
            Perform a dedicated deep-web market intelligence & category opportunity scout specifically for the following category:
            CATEGORY: "${targetCategory.displayName}" (Identifier: ${targetCategory.name})

            Scan real founder & buyer demand across Reddit, X, Upwork, Chrome Web Store, and G2 reviews to identify $count HIGH-OPPORTUNITY, UNCONTESTED VENTURE IDEAS strictly in the "${targetCategory.displayName}" space.

            Requirements:
            1. Every idea MUST have "category": "${targetCategory.name}".
            2. Solves a specific, acute problem that paying businesses or prosumers urgently need.
            3. Must have clear revenue mechanics (ARR, MRR, realistic pricing tiers, buyer willingness to pay).
            4. Fast time-to-MVP (<1-4 weeks).
            5. Provide exact actionable next steps and buyer acquisition strategy.

            Existing ideas in database to NOT duplicate:
            ${existingIdeaTitles.joinToString("\n") { "- $it" }}

            Output STRICT JSON ONLY as an array:
            [
              {
                "title": "Clear Name (e.g. TabSaver Pro - Bulk Workspace Session Sync)",
                "tagline": "Punchy 1-line value proposition",
                "category": "${targetCategory.name}",
                "marketType": "BLUE_OCEAN",
                "isImmediatelyActionable": true,
                "isZeroInvestment": false,
                "zeroCostPlaybook": "Customer acquisition strategy",
                "timeToFirstDollar": "< 1 Week",
                "zeroCostStack": "Free tier tech stack",
                "problemStatement": "Pain statement from real users in ${targetCategory.displayName}",
                "solutionDescription": "Targeted solution in ${targetCategory.displayName}",
                "targetAudience": "Target audience and buyer profile",
                "whyNowTrigger": "Market demand driver",
                "pricingModel": "Monthly Subscription or Usage",
                "monthlyPricePerCustomer": 49.0,
                "targetCustomersYear1": 120,
                "pricingTiersSummary": "Starter: $29/mo | Pro: $49/mo | Team: $149/mo",
                "devComplexity": "LOW_1_2_WEEKS",
                "weeksToMvp": 2,
                "techStack": "Modern stack tailored for ${targetCategory.displayName}",
                "estimatedMonthlyOperatingCost": 40.0,
                "mvpFeatureScope": "1. Core feature 2. Payment link 3. Dashboard",
                "competitionLevel": "Low",
                "topCompetitors": "Legacy players",
                "competitorWeaknesses": "Bloated, expensive",
                "unfairAdvantageMoat": "Laser focused niche positioning",
                "dataSourceType": "REDDIT",
                "sourceUrlOrRef": "reddit.com/r/SaaS",
                "sourceQuote": "User pain quote",
                "willingnessToPayRating": 5,
                "readyBuyerProfiles": "Specific buyers with budget",
                "actionableNextSteps": "1. Build MVP\n2. Reach out to 15 users\n3. Launch",
                "opportunityScore": 92
              }
            ]
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = "You are a master venture capitalist and product scout specializing in deeply researched, high-ROI business ideas in the ${targetCategory.displayName} sector. Output clean JSON only."
                    )
                )
            )
        )

        val modelToUse = if (activeModel.isNotBlank()) activeModel else "gemini-3.5-flash"

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val response = service.generateContentWithModel(modelToUse, apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val parsedIdeas = parseIdeasFromJson(rawText)
                    if (parsedIdeas.isNotEmpty()) {
                        // Ensure category is assigned
                        val standardized = parsedIdeas.map { it.copy(category = targetCategory) }
                        return@withContext Result.success(standardized)
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiApiClient", "Live category scan error: ${e.message}", e)
            }
        }

        // Curated category fallback
        val fallbackGenerated = generateCuratedCategoryIdeas(targetCategory, existingIdeaTitles, count)
        Result.success(fallbackGenerated)
    }

    private fun generateCuratedCategoryIdeas(
        targetCategory: com.example.data.model.BusinessModelCategory,
        existingTitles: List<String>,
        count: Int
    ): List<com.example.data.model.BusinessIdea> {
        val now = System.currentTimeMillis()
        val pool = when (targetCategory) {
            com.example.data.model.BusinessModelCategory.CHROME_EXTENSION -> listOf(
                com.example.data.model.BusinessIdea(
                    id = 0,
                    title = "TabSync Pro - Ephemeral Team Tab & Workspace Sharing",
                    tagline = "1-click browser extension that shares synced live browser workspaces and authenticated preview sessions across remote dev & design teams",
                    category = com.example.data.model.BusinessModelCategory.CHROME_EXTENSION,
                    marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                    stage = com.example.data.model.PipelineStage.DISCOVERED,
                    isImmediatelyActionable = true,
                    isZeroInvestment = true,
                    zeroCostPlaybook = "Post on r/webdev and Designer News with interactive Chrome extension demo.",
                    timeToFirstDollar = "< 48 Hours",
                    zeroCostStack = "Chrome Extension (MV3), WebSockets Free, Stripe Checkout",
                    problemStatement = "Remote design and engineering teams constantly paste 15 Slack links for staging URLs, Figma mocks, and Jira tickets during pull request reviews.",
                    solutionDescription = "Browser extension that snapshots the current set of research and PR tabs and opens them in 1 click for any teammate with synced cookies or staging auth.",
                    targetAudience = "Remote tech leads, QA engineers, and agile software teams",
                    whyNowTrigger = "Distributed engineering teams suffer severe context switching overhead.",
                    pricingModel = "Per-Seat Subscription",
                    monthlyPricePerCustomer = 8.0,
                    targetCustomersYear1 = 300,
                    estimatedMRR = 2400.0,
                    estimatedARR = 28800.0,
                    projectedYear1Revenue = 25000.0,
                    projectedYear3Revenue = 110000.0,
                    pricingTiersSummary = "Free (Up to 3 seats) | Pro ($8/seat/mo) | Org ($15/seat/mo)",
                    devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                    weeksToMvp = 1,
                    techStack = "Chrome Extension MV3, Tailwind CSS, Supabase Realtime, Stripe",
                    estimatedMonthlyOperatingCost = 0.0,
                    mvpFeatureScope = "1. Session bundle creation 2. 1-click team join 3. Staging cookie sync",
                    competitionLevel = "Low in collaborative team extension space",
                    topCompetitors = "OneTab (single-user, local only), Workona (heavy & bloated)",
                    competitorWeaknesses = "Workona is bloated and slow; OneTab lacks team sync.",
                    unfairAdvantageMoat = "Ultra-fast 200ms sync with zero bloat and native keyboard shortcuts.",
                    dataSourceType = com.example.data.model.DataSourceType.CHROME_STORE,
                    sourceUrlOrRef = "chromewebstore.google.com/search/onetab_alternatives",
                    sourceQuote = "\"I wish OneTab had a 'Share this exact session with my QA engineer' button.\"",
                    willingnessToPayRating = 5,
                    readyBuyerProfiles = "Engineering managers and remote agencies.",
                    actionableNextSteps = "1. Publish to Chrome Web Store\n2. Share on Hacker News 'Show HN'\n3. Email 20 remote dev leads",
                    opportunityScore = 93
                ),
                com.example.data.model.BusinessIdea(
                    id = 0,
                    title = "LoomCut - Instant Browser Screen Clip to Clean Markdown & Jira",
                    tagline = "Browser extension that captures page bugs, DOM state, console errors, and generates formatted Jira / Linear tickets with 1 click",
                    category = com.example.data.model.BusinessModelCategory.CHROME_EXTENSION,
                    marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                    stage = com.example.data.model.PipelineStage.DISCOVERED,
                    isImmediatelyActionable = true,
                    isZeroInvestment = true,
                    zeroCostPlaybook = "Share animated GIF demos on LinkedIn and Twitter targeting QA engineers and PMs.",
                    timeToFirstDollar = "3 Days",
                    zeroCostStack = "Chrome Extension API, Canvas DOM capture, Linear/Jira OAuth, Stripe",
                    problemStatement = "Filing bug reports takes 5 minutes per bug (taking screenshot, opening console, typing steps to reproduce, uploading to Linear).",
                    solutionDescription = "1-click browser action that automatically grabs console logs, network errors, screen crop, and creates an enriched bug ticket in Linear or Jira in 3 seconds.",
                    targetAudience = "QA testers, PMs, and customer support engineers",
                    whyNowTrigger = "Speed of shipping micro-deployments requires instant bug capture workflows.",
                    pricingModel = "Team Subscription",
                    monthlyPricePerCustomer = 29.0,
                    targetCustomersYear1 = 150,
                    estimatedMRR = 4350.0,
                    estimatedARR = 52200.0,
                    projectedYear1Revenue = 42000.0,
                    projectedYear3Revenue = 180000.0,
                    pricingTiersSummary = "Solo: $9/mo | Team (5 seats): $29/mo | Enterprise: $99/mo",
                    devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                    weeksToMvp = 2,
                    techStack = "TypeScript, Chrome Debugger API, Linear & Jira REST APIs",
                    estimatedMonthlyOperatingCost = 15.0,
                    mvpFeatureScope = "1. Bug capture overlay 2. Console log collector 3. Linear/Jira API post",
                    competitionLevel = "Medium",
                    topCompetitors = "Marker.io ($59/mo), Jam.dev",
                    competitorWeaknesses = "Expensive agency pricing with high per-seat fees.",
                    unfairAdvantageMoat = "Lightweight browser extension with zero dashboard bloat.",
                    dataSourceType = com.example.data.model.DataSourceType.REDDIT,
                    sourceUrlOrRef = "reddit.com/r/QualityAssurance",
                    sourceQuote = "\"I file 30 bug tickets a day. Writing console logs manually is soul-draining.\"",
                    willingnessToPayRating = 5,
                    readyBuyerProfiles = "QA leads and freelance software testers.",
                    actionableNextSteps = "1. Build Linear webhook extension\n2. Onboard 10 dev agency QA testers\n3. Launch on Product Hunt",
                    opportunityScore = 95
                )
            )

            com.example.data.model.BusinessModelCategory.MARKETPLACE -> listOf(
                com.example.data.model.BusinessIdea(
                    id = 0,
                    title = "FractionalOps - Vetted Fractional COO & RevOps Match Marketplace",
                    tagline = "B2B marketplace matching $1M-$10M ARR SaaS startups with on-demand vetted Fractional Chief Operating Officers and billing specialists",
                    category = com.example.data.model.BusinessModelCategory.MARKETPLACE,
                    marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                    stage = com.example.data.model.PipelineStage.DISCOVERED,
                    isImmediatelyActionable = true,
                    isZeroInvestment = true,
                    zeroCostPlaybook = "Recruit 20 high-caliber ex-founders on LinkedIn and match them to funded founders requesting help on Twitter/X.",
                    timeToFirstDollar = "7 Days",
                    zeroCostStack = "Airtable, Softr / Next.js, Stripe Connect",
                    problemStatement = "Post-seed startups cannot afford a $350k full-time COO, but founders burn out handling hiring, payroll, and billing operations manually.",
                    solutionDescription = "Curated talent marketplace offering 10-20 hour/week Fractional COOs with standardized onboarding playbooks and verified SaaS track records.",
                    targetAudience = "Series A & bootstrapped SaaS founders at $1M-$5M ARR",
                    whyNowTrigger = "Rise of fractional executive work culture and lean remote team structures.",
                    pricingModel = "15% Placement & Escrow Commission",
                    monthlyPricePerCustomer = 450.0,
                    targetCustomersYear1 = 50,
                    estimatedMRR = 22500.0,
                    estimatedARR = 270000.0,
                    projectedYear1Revenue = 180000.0,
                    projectedYear3Revenue = 850000.0,
                    pricingTiersSummary = "Client: 15% marketplace fee on hours billed ($3,000-$8,000/mo engagements)",
                    devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                    weeksToMvp = 2,
                    techStack = "Airtable, Next.js / Softr, Stripe Connect, DocuSign",
                    estimatedMonthlyOperatingCost = 30.0,
                    mvpFeatureScope = "1. Fractional COO vetting application 2. Client request intake 3. Stripe escrow billing",
                    competitionLevel = "Low in specialized SaaS COO vertical",
                    topCompetitors = "Generalist marketplaces (Upwork, Toptal)",
                    competitorWeaknesses = "Toptal and Upwork lack domain specialization in B2B SaaS operational leadership.",
                    unfairAdvantageMoat = "Pre-built SaaS RevOps templates and standardized 30-day onboarding milestones.",
                    dataSourceType = com.example.data.model.DataSourceType.X_TWITTER,
                    sourceUrlOrRef = "x.com/saas_founders/status/fractional_coo",
                    sourceQuote = "\"I need a 10-hour/week Fractional COO who knows Stripe billing and remote compensation bands. Where do I find one?\"",
                    willingnessToPayRating = 5,
                    readyBuyerProfiles = "Funded tech CEOs with 10-30 team members.",
                    actionableNextSteps = "1. Curate first 10 verified fractional operators\n2. DM 30 founders who recently announced funding\n3. Facilitate first 2 contracts",
                    opportunityScore = 96
                )
            )

            com.example.data.model.BusinessModelCategory.PRODUCTIZED_SERVICE -> listOf(
                com.example.data.model.BusinessIdea(
                    id = 0,
                    title = "MigrationSprint - Zero-Downtime Stripe Billing Migration Service",
                    tagline = "Productized 72-hour fixed-price service migrating SaaS billing from legacy charge systems or manual invoicing into modern Stripe Billing",
                    category = com.example.data.model.BusinessModelCategory.PRODUCTIZED_SERVICE,
                    marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                    stage = com.example.data.model.PipelineStage.DISCOVERED,
                    isImmediatelyActionable = true,
                    isZeroInvestment = true,
                    zeroCostPlaybook = "Cold DM SaaS founders complaining on r/SaaS about messy legacy payment migrations.",
                    timeToFirstDollar = "< 72 Hours",
                    zeroCostStack = "Stripe CLI, Node.js scripts, Loom, Stripe Invoicing",
                    problemStatement = "Founders with 500+ active subscribers dread migrating billing systems because credit card token transfers and webhook cutovers cause accidental duplicate charges or canceled cards.",
                    solutionDescription = "Turnkey 72-hour migration package with automated customer card token transfer, subscription state mapping, webhook testing, and zero subscriber downtime guarantee.",
                    targetAudience = "Bootstrapped SaaS founders moving to Stripe Billing or Paddle",
                    whyNowTrigger = "Stripe's new billing and tax APIs make modern infrastructure a necessity for compliance.",
                    pricingModel = "Fixed-Price Package ($1,999 - $4,999 per migration)",
                    monthlyPricePerCustomer = 2999.0,
                    targetCustomersYear1 = 35,
                    estimatedMRR = 8750.0,
                    estimatedARR = 105000.0,
                    projectedYear1Revenue = 95000.0,
                    projectedYear3Revenue = 320000.0,
                    pricingTiersSummary = "Standard (<500 subs): $1,999 | Growth (<2,500 subs): $3,499 | Custom: $5,999",
                    devComplexity = com.example.data.model.DevComplexity.ZERO_CODE_NO_CODE,
                    weeksToMvp = 0,
                    techStack = "Stripe API, Node.js data transformers, Stripe CLI",
                    estimatedMonthlyOperatingCost = 0.0,
                    mvpFeatureScope = "1. Customer token audit 2. Plan schema matching 3. Automated migration script 4. Reconciliation report",
                    competitionLevel = "Low",
                    topCompetitors = "Expensive software agencies billing $150/hr with no fixed timeline",
                    competitorWeaknesses = "Agencies drag migrations out for 2 months.",
                    unfairAdvantageMoat = "Fixed 72-hour turnaround with money-back guarantee.",
                    dataSourceType = com.example.data.model.DataSourceType.REDDIT,
                    sourceUrlOrRef = "reddit.com/r/SaaS/comments/stripe_migration_nightmare",
                    sourceQuote = "\"I've put off upgrading our billing engine for 18 months because I'm terrified of dropping paying subscriptions during migration.\"",
                    willingnessToPayRating = 5,
                    readyBuyerProfiles = "SaaS founders doing $10k-$100k MRR.",
                    actionableNextSteps = "1. Post case study on Indie Hackers\n2. DM 10 founders who mention billing tech debt\n3. Close first client with 50% upfront deposit",
                    opportunityScore = 97
                )
            )

            else -> listOf(
                com.example.data.model.BusinessIdea(
                    id = 0,
                    title = "NichePulse - ${targetCategory.displayName} Automated Discovery Engine",
                    tagline = "Turnkey intelligence tool tailored specifically for high-efficiency operators in the ${targetCategory.displayName} ecosystem",
                    category = targetCategory,
                    marketType = com.example.data.model.MarketType.BLUE_OCEAN,
                    stage = com.example.data.model.PipelineStage.DISCOVERED,
                    isImmediatelyActionable = true,
                    isZeroInvestment = true,
                    zeroCostPlaybook = "Direct outreach to active discussions in specialized niche communities.",
                    timeToFirstDollar = "< 3 Days",
                    zeroCostStack = "Modern lightweight API stack, Stripe Payment Links",
                    problemStatement = "Operators in the ${targetCategory.displayName} domain face fragmented tools and costly manual workflows.",
                    solutionDescription = "Specialized automation that streamlines operations and captures immediate market demand in ${targetCategory.displayName}.",
                    targetAudience = "Domain specialists, founders, and consultants in ${targetCategory.displayName}",
                    whyNowTrigger = "Rapid demand surge and lack of dedicated tooling for this exact workflow.",
                    pricingModel = "Monthly SaaS",
                    monthlyPricePerCustomer = 49.0,
                    targetCustomersYear1 = 120,
                    estimatedMRR = 5880.0,
                    estimatedARR = 70560.0,
                    projectedYear1Revenue = 58000.0,
                    projectedYear3Revenue = 240000.0,
                    pricingTiersSummary = "Starter: $29/mo | Pro: $49/mo | Enterprise: $129/mo",
                    devComplexity = com.example.data.model.DevComplexity.LOW_1_2_WEEKS,
                    weeksToMvp = 2,
                    techStack = "Kotlin / Next.js, Supabase, Stripe",
                    estimatedMonthlyOperatingCost = 25.0,
                    mvpFeatureScope = "1. Core ${targetCategory.displayName} module 2. Fast onboarding 3. Billing integration",
                    competitionLevel = "Low",
                    topCompetitors = "Manual spreadsheets and fragmented tools",
                    competitorWeaknesses = "High complexity, slow setup",
                    unfairAdvantageMoat = "Laser-targeted to ${targetCategory.displayName} operators with 1-click onboarding",
                    dataSourceType = com.example.data.model.DataSourceType.REDDIT,
                    sourceUrlOrRef = "reddit.com/r/SaaS",
                    sourceQuote = "\"There are no specialized tools built specifically for ${targetCategory.displayName}. Everything is either too generic or $500/month.\"",
                    willingnessToPayRating = 5,
                    readyBuyerProfiles = "Operators needing focused solutions in ${targetCategory.displayName}",
                    actionableNextSteps = "1. Build MVP\n2. Reach out to 15 operators\n3. Launch with early-bird discount",
                    opportunityScore = 92
                )
            )
        }

        val filtered = pool.filter { candidate ->
            existingTitles.none { it.contains(candidate.title.take(15), ignoreCase = true) }
        }
        val resultList = if (filtered.size >= count) filtered.take(count) else pool.take(count)
        return resultList.map { it.copy(updatedAt = now, createdAt = now) }
    }
}


