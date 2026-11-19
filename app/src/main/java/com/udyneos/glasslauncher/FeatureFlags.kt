package com.udyneos.glasslauncher

/**
 * Whitelist of everything planned for the next ten updates (see ROADMAP.md).
 * A feature only exists at runtime once it is listed in [FeatureFlags.unlocked],
 * so half-finished work can live in the tree without reaching users.
 */
enum class Feature(val update: Int, val title: String) {
    ICON_PACKS(1, "Icon pack support"),
    FOLDERS(2, "Home screen folders"),
    WIDGETS(3, "Widget host"),
    GESTURES(4, "Swipe down search and double tap to lock"),
    NOTIFICATION_BADGES(5, "Notification badges"),
    HIDDEN_APPS(6, "Hidden apps"),
    ICON_SIZE_GRID(7, "Custom grid size and icon size"),
    BACKUP_RESTORE(8, "Layout backup and restore"),
    DYNAMIC_WALLPAPER_TINT(9, "Wallpaper-aware glass tint"),
    IN_APP_UPDATE(10, "In-app update from GitHub"),
}

object FeatureFlags {

    // Move a feature in here in the same commit that finishes it.
    private val unlocked: Set<Feature> = emptySet()

    fun isEnabled(feature: Feature): Boolean = feature in unlocked

    /** Features that are planned but not shipped yet, in update order. */
    fun upcoming(): List<Feature> =
        Feature.entries.filter { it !in unlocked }.sortedBy { it.update }
}
