# Cleanova
**Less Clutter. More Life.**

Native Android storage utility. Foundation v0.1: device capacity, accessible media inventory, three-tab shell and local appearance preferences. No deletion, AI classification, payments, ads, accounts or onboarding in this milestone. No network permission.

## Build
JDK 17, Android SDK 35, Gradle 8.9. Run `gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Use the same Gradle version in Android Studio or the cloud IDE. GitHub Actions builds a debug APK. This is an initial development branch, not a release.

## Boundaries
UI -> ViewModel -> repository -> Android APIs. Device capacity is the internal data volume reported by StatFs, not advertised hardware capacity. Media results cover only granted access and are not estimated reclaimable space. No files are modified.
