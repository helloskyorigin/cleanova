# Foundation architecture and release gates

Cleanova is local-first: each installation processes its own media. A million or ten million installations do not create a shared scan workload. This does not guarantee performance or availability on every device. Any future cloud service requires separate capacity, privacy and cost planning.

## Dependency boundaries
- domain: immutable platform-neutral models and repository contracts; no UI or Android APIs.
- data: platform adapters, scoped permissions, cancellable queries; never called directly from screen composables.
- core/permissions: per-media grant mapping. Selected or mixed grants never mean complete gallery access.
- UI: activity hosts Compose, reusable components separate from screen shell, observable ViewModel state. Future feature screens get separate packages and state holders.
- Keep one app module initially; split by feature when ownership or build times warrant it. No heavy DI or persistence until needed.

## Invariants
No destructive action without a verified current URI, user review and applicable platform approval. Protected items must be excluded at execution time, not only in UI. Future cleanup engine needs idempotent operations, cancellation, failure reporting per item and measured recovered bytes. No fake progress, no media paths or private names in logs, no network permission for foundation.

## Gates before main merge
- CI compilation, JVM tests and Android lint pass.
- Test API 26, 33, 34, 35 and current target devices; full/partial/denied/revoked media access, rotation, resume, process recreation, rapid scan/cancel, empty storage and large libraries.
- Run emulator UI smoke test and real device checks. Test 10k/100k media metadata rows and memory usage; no full-resolution batch decode.
- Check 200% font size, TalkBack, contrast, system bars, gesture navigation and reduced motion.
- Use real benchmark traces before claiming frame-rate or scan-speed targets.
- Review GitHub branch protection, permissions and dependency updates; release signed AAB later. Never commit credentials. No direct production merge before verification.

## Out of scope
Onboarding, ads, billing, login, AI ranking, deletion, compression, background maintenance. Implement after stable foundation, screen by screen. Google AI Studio preview is manual and does not replace device validation.
