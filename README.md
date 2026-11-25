# Cleanova
**Less Clutter. More Life.**

Native Android, local-first storage utility. Foundation v0.1 provides capacity, accessible media inventory, three-tab shell and appearance preferences. No AI cleaning, deletion, payments, ads, accounts, network access or onboarding yet.

## Build
JDK 17, SDK 35, Gradle 8.9. Run `gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. CI uses the hosted runner's Android SDK and explicitly installs required packages. Gradle wrapper is not included yet; use the pinned Gradle version.

## Architecture
See [foundation contracts and quality gates](docs/FOUNDATION.md) and [roadmap](docs/ROADMAP.md). Internal data volume capacity is not advertised hardware capacity. Media totals are accessible inventory, not reclaimable bytes. Build/device validation is required before merge.
