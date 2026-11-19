# Roadmap

Whitelist for the next ten updates. Each item matches an entry in
`FeatureFlags.kt`; it only becomes visible once it is added to `unlocked`.

| Update | Feature | Notes |
|-------:|---------|-------|
| 1 | Icon pack support | Read ADW/Nova style packs, fall back to the app's own icon |
| 2 | Home screen folders | Drag one icon onto another, glass folder preview |
| 3 | Widget host | AppWidgetHost on the home pages |
| 4 | Gestures | Swipe down for search, double tap to lock the screen |
| 5 | Notification badges | Needs notification listener access, opt-in |
| 6 | Hidden apps | Managed from the hidden settings, still reachable through search |
| 7 | Grid and icon size | Replace the fixed 4x6 grid with a setting |
| 8 | Backup and restore | Export layout and settings to a file |
| 9 | Wallpaper-aware tint | Glass tint follows the dominant wallpaper colour |
| 10 | In-app update | Check GitHub releases, debug or release APK choice |

## Rules

- Nothing outside this list ships before update 10.
- A feature is switched on in `FeatureFlags.unlocked` in the commit that finishes it.
- Hidden settings stay hidden: no icon, no menu entry, only the clock gesture.
