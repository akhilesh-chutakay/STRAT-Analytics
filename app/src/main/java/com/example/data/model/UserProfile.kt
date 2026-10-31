package com.example.data.model

data class AvatarPreset(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val description: String
)

data class UserProfile(
    val id: String = "user_primary",
    val name: String = "Akhilesh Chutakay",
    val email: String = "akhilesh.chutakay8@gmail.com",
    val title: String = "Venture Architect & Angel Strategist",
    val avatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
    val bio: String = "Building high-conviction B2B SaaS & AI automation ventures with validated founder demand and data-driven moats.",
    val organization: String = "iA Strat Labs",
    val experienceLevel: String = "Venture Partner / Founder",
    val targetPortfolioARR: Double = 1_000_000.0,
    val currencySymbol: String = "$",
    val isGoogleDriveLinked: Boolean = true,
    val joinedDate: String = "Aug 2026",
    val tier: String = "Executive Pro"
) {
    val initials: String
        get() {
            val parts = name.trim().split(" ")
            return if (parts.size >= 2) {
                "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
            } else {
                name.take(2).uppercase()
            }
        }

    companion object {
        val AVATAR_PRESETS = listOf(
            AvatarPreset(
                id = "preset_tech_lead",
                name = "Venture Architect",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
                description = "Modern Tech Strategist"
            ),
            AvatarPreset(
                id = "preset_founder_male",
                name = "Executive Founder",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
                description = "B2B SaaS Builder"
            ),
            AvatarPreset(
                id = "preset_product_lead",
                name = "Product Strategist",
                avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400&auto=format&fit=crop&q=80",
                description = "Growth & AI Operator"
            ),
            AvatarPreset(
                id = "preset_ai_engineer",
                name = "AI Lead",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
                description = "Deep Tech & ML Pioneer"
            ),
            AvatarPreset(
                id = "preset_indie_hacker",
                name = "Indie Pioneer",
                avatarUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=400&auto=format&fit=crop&q=80",
                description = "Bootstrapped Scaler"
            ),
            AvatarPreset(
                id = "preset_cyber_minimal",
                name = "Angel Partner",
                avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80",
                description = "Capital & Portfolio Lead"
            )
        )
    }
}
