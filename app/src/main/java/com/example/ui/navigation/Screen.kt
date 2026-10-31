package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Venture Radar")
    object Actionable : Screen("actionable", "Action Ready")
    object Signals : Screen("signals", "Problem Vault")
    object Analytics : Screen("analytics", "Portfolio Stats")
    object Detail : Screen("detail/{ideaId}", "Idea Details") {
        fun createRoute(ideaId: Long) = "detail/$ideaId"
    }
    object AddEdit : Screen("add_edit?ideaId={ideaId}", "Plan Studio") {
        fun createRoute(ideaId: Long? = null) = if (ideaId != null) "add_edit?ideaId=$ideaId" else "add_edit"
    }
    object Settings : Screen("settings", "Settings")
    object WebCompanion : Screen("web_companion", "Web Platform")
}
