package com.example.focuscontrol.ui.navigation

enum class MainNavDestination(
    val route: String,
    val label: String,
    val iconEmoji: String
) {
    HOME("home", "Home", "🏠"),
    APP_CONTROL("app_control", "App Limits", "📱"),
    STUDY("study", "Study Mode", "⏱️"),
    STATS("stats", "Usage Stats", "📊"),
    SETTINGS("settings", "Settings", "⚙️")
}
