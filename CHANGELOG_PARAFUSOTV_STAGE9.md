# ParafusoTV — Stage 9

## IPTV list management

- Xtream and M3U/M3U8 are presented as the primary IPTV connection methods.
- Added a dedicated **Testar conexão** action before saving an Xtream or M3U/M3U8 source.
- M3U/M3U8 validation downloads/parses the playlist and reports the number of discovered items.
- Xtream validation authenticates the server and reports authentication/subscription status when available.
- Saving a changed Xtream/M3U configuration is blocked until its current connection data has passed the test.
- A successful test is invalidated automatically when the connection fields change or the provider type changes.
- Existing provider storage, multi-source merging and refresh behavior remain intact.

## Validation

The project was structurally checked after the changes. A full Gradle build remains pending because this environment cannot resolve/download the configured Gradle distribution from services.gradle.org.
