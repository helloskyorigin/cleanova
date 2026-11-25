# Cleanova (Android)

Cleanova — *Less Clutter. More Life.* An iOS-inspired phone storage cleaner and declutter companion built for Android with Kotlin and Jetpack Compose.

## Features
- **Storage Overview & Health Score**: Real-time device capacity telemetry (`StatFs`), animated segmented storage ring, session recovery counter, and local workspace cache sweep.
- **Zero-Permission Media Audit**: Uses the Android Photo Picker (`PickMultipleVisualMedia`) to inspect photo and video metadata, flagging oversized videos, screenshots, and potential duplicates.
- **Deep Folder Scan (SAF)**: Uses Android's Storage Access Framework (`OpenDocumentTree` + `DocumentsContract`) to inspect folders such as Downloads, highlight APKs/archives, large files, and exact-size duplicates, and safely delete user-selected files.
- **AI Declutter Advisor**: Routed through `AiCleanerAdvisor` (currently backed by `PlaceholderAiCleanerAdvisor` with sample responses until Firebase AI Logic is enabled).
