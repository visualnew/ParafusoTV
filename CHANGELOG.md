## 2.0.0

Primeira versão estável da série 2.0. A nova interface e a atualização sobre a beta foram validadas no Moto G56 e no Amazon Fire TV Stick.

### Interface

- Os botões agora usam grafite escuro, com texto e ícones brancos para melhorar a legibilidade. O contorno claro destaca o botão em foco ao navegar pelo controle remoto.
- As funcionalidades existentes foram preservadas.

### Integrações

- Trakt e TMDB não estão configurados nesta distribuição.

### Atualização da beta

- Baixe `ParafusoTV.apk` e instale manualmente por cima da `2.0.0-beta.1`, sem desinstalar o aplicativo, para preservar os dados.
- O verificador de atualizações da `2.0.0-beta.1` não reconhece a `2.0.0` como uma versão mais recente.

## 2.0.0-beta.1

### Highlights
- First public Beta of ParafusoTV.
- Public branding and release workflow updated from Lumora to ParafusoTV.
- Signed release pipeline configured for GitHub Actions.
- CI simplified to validate Android buildability on GitHub runners.
- Release APK validated on Moto G56 5G and Amazon Fire TV Stick.
- About screen includes Lumora origin and PolyForm Noncommercial 1.0.0 attribution.

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
