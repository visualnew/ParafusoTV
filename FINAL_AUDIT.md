# ParafusoTV — Final Audit (Stage 20)

## Verified statically
- Application ID: `com.parafusotv`
- Application label: `ParafusoTV`
- Version: `1.0.0` / versionCode 1
- Target SDK 36 / min SDK 25
- Launcher and Android TV launcher declarations present
- Picture-in-Picture capability declared
- ParafusoTV playback orientation integration present
- Core ParafusoTV profile, parental, IPTV list, EPG, search, backup and player files present
- No TODO/Not-yet-implemented markers were found in the core `com/lumora` package outside scraper providers.
- Automatic update checker no longer points at the upstream Lumora repository. It is disabled until a ParafusoTV release repository is configured.

## Build limitation
The supplied Gradle wrapper requires Gradle 8.12 from `services.gradle.org`. This environment has no network access and no local Gradle 8.12 distribution, so `:app:assembleDebug --offline` could not start the build. Therefore this audit does **not** certify compilation or APK runtime behavior.

## Recording limitation
The existing recording scheduler persists scheduled states and alarms, but the current code does not contain a complete stream-capture pipeline that writes live IPTV media to a recording file. Recording remains the principal functional item requiring implementation and device testing before a production 1.0 claim.

## License
The bundled source license is PolyForm Noncommercial 1.0.0 for the supplied Lumora-derived version. Personal/noncommercial use is permitted under that license; commercial distribution requires separate legal review/permission.
