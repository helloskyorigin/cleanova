# Cleanova — engineering instructions

These are repository-wide instructions for all coding agents and contributors.

## Operating procedure
1. Inspect the repository, current branch, existing implementation, requirements and CI status before changing code. Read actual files, not only prior summaries.
2. Define a focused implementation plan and acceptance criteria. Fix foundation/build failures before adding screen features. Respect the approved product scope.
3. Edit the smallest coherent set of files. Preserve existing working behavior and public contracts; avoid speculative abstractions, unrelated rewrites and dependency churn.
4. Review the diff for correctness, cancellation, lifecycle, security, accessibility, performance, privacy and edge cases.
5. Run the closest available build, unit tests and lint. Inspect actual CI logs, fix root causes, and rerun. Never invent a test result or describe an unverified build as working.
6. Commit small, meaningful changes to a feature branch and record verification results. Keep main stable; do not merge without passing checks and founder approval. Never force-push over user changes.
7. Explain exactly what changed, what was tested, what remains pending, and give the next actionable step. Do not promise unattended work.

## Product constraints
- Cleanova: AI Phone Cleaner. Tagline: Less Clutter. More Life.
- Android-first, Kotlin + Jetpack Compose, original iOS-inspired visual language, approved logo. Home | Media | Storage.
- Foundation first; then one screen/working vertical feature at a time. No onboarding, ads, billing, login or unrelated features until explicitly approved.
- Local-first. No media uploads, telemetry or network permission in this foundation. Future cloud capabilities need separate review.
- Real storage readings and real scan states only. Do not invent recoverable space or fake AI.
- No destructive operations without a current review, explicit user approval and required Android confirmation. Protect user-marked memories; do not promise infallible AI.
- Handle denied, selected, mixed and revoked media access. Never infer permission from cached UI. No unrestricted file access shortcuts.
- Use cancellable background work and bounded memory. No full-resolution batch decoding. Respect process death, rotation, accessibility and low-memory devices.
- Keep UI, domain contracts and platform data adapters separate. One module while appropriate, split when justified. Use dependency injection seams, typed routes, reusable design tokens, resource strings and deterministic state.
- Never commit secrets, customer media, generated build outputs or signing keys.

## Verification gate
Compile, unit tests, lint, emulator/device smoke checks, lifecycle/permissions cases and performance checks appropriate to the change. A passing CI build is not a substitute for real-device or scale testing. Report unavailable checks explicitly. See docs/FOUNDATION.md.
