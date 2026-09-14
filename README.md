# ParafusoTV

**ParafusoTV** é um cliente de IPTV e mídia pessoal para **Android, Android TV e Fire TV**, desenvolvido com foco em desempenho, navegação por controle remoto e uma experiência consistente entre celular e televisão.

O aplicativo é baseado no projeto **Lumora** e continua utilizando parte de sua arquitetura interna. O ParafusoTV acrescenta adaptações próprias de interface, experiência mobile e TV, navegação, identidade visual, estabilidade e outras melhorias desenvolvidas pela **Visualnew / Jonathan Tavares**.

> **O ParafusoTV é apenas um player.**
> Ele não fornece, vende, hospeda ou distribui canais, filmes, séries, listas IPTV ou assinaturas. O usuário deve utilizar suas próprias fontes e serviços legalmente obtidos.

## Destaques

- Interface adaptada para celular e televisão
- Navegação otimizada para D-pad em Android TV e Fire TV
- Suporte a provedores IPTV, incluindo Xtream Codes e M3U/M3U8
- TV ao vivo com EPG
- Catálogo de Filmes e Séries
- Busca integrada
- Favoritos, histórico e progresso de reprodução
- Perfis e controles parentais
- Player baseado em AndroidX Media3 / ExoPlayer
- Áudio, legendas e velocidade de reprodução
- Picture-in-Picture em dispositivos compatíveis
- Google Cast
- Downloads em dispositivos compatíveis
- Integrações com Jellyfin e Plex
- Sistema de plugins
- Backup e restauração
- Interface traduzida para vários idiomas
- Atualizações através das GitHub Releases

## Recursos

### TV ao vivo

O ParafusoTV oferece navegação por canais e categorias com interface adaptada para celular e televisão.

Entre os recursos disponíveis estão:

- provedores Xtream Codes e playlists M3U/M3U8;
- múltiplas fontes configuráveis;
- EPG com programação atual e próxima;
- favoritos;
- agrupamento e organização de canais;
- reprodução em tela cheia;
- zapping;
- suporte a catch-up quando disponibilizado pelo provedor;
- seleção de fontes alternativas quando disponíveis.

A área de **gravações/DVR ainda está em desenvolvimento** e não deve ser considerada um recurso concluído nesta versão.

### Filmes e Séries

- Navegação por categorias
- Capas e páginas de detalhes
- Temporadas e episódios
- Histórico de reprodução
- Continue assistindo
- Retomada de progresso
- Favoritos
- Busca pelo catálogo
- Reprodução integrada ao player

### Jellyfin e Plex

O projeto inclui integração com servidores pessoais Jellyfin e Plex.

Esses recursos dependem da configuração e das capacidades do servidor utilizado. O ParafusoTV não fornece servidores, contas ou conteúdo para esses serviços.

### Reprodução

O player utiliza **AndroidX Media3 (ExoPlayer)** e oferece recursos como HLS, DASH e RTSP, controle de progresso, velocidade de reprodução, seleção de áudio e legendas, Picture-in-Picture, retomada de reprodução e Google Cast.

### Perfis

O ParafusoTV permite manter diferentes perfis no mesmo dispositivo. Favoritos, histórico, progresso e outras preferências podem permanecer separados entre os perfis.

Também há suporte a controles parentais e proteção por PIN.

### Plugins

O aplicativo mantém um mecanismo de plugins baseado em JavaScript para extensões e fontes adicionais.

Alguns componentes desse sistema ainda utilizam infraestrutura herdada do projeto Lumora. Plugins e fontes de terceiros são independentes do ParafusoTV e podem possuir seus próprios termos, disponibilidade e limitações.

### Android Auto

O código contém suporte experimental relacionado ao Android Auto para utilização com o veículo estacionado.

> **Nunca assista a vídeos enquanto estiver dirigindo.**

Esse recurso pode depender da versão do Android Auto, dispositivo, veículo e políticas da plataforma e não faz parte dos principais ambientes atualmente validados para lançamento do ParafusoTV.

## Dispositivos testados

A versão atual foi validada diretamente em hardware real, incluindo:

- **Moto G56 5G — Android**
- **Amazon Fire TV Stick**

A aplicação possui **minSdk 25 (Android 7.1)**. Funcionamento em todos os dispositivos e fabricantes não pode ser garantido.

## Tecnologias

- **Linguagem:** Kotlin
- **Interface:** Android Views / XML
- **Player:** AndroidX Media3 / ExoPlayer
- **Persistência:** Room
- **Tarefas em segundo plano:** WorkManager
- **Rede:** OkHttp e Retrofit
- **Serialização:** Gson e Kotlin Serialization
- **HTML:** jsoup
- **JavaScript:** Rhino e QuickJS
- **QR Code:** ZXing
- **Torrent:** libtorrent4j
- **Car:** AndroidX Car App Library
- **Min SDK:** 25
- **Target SDK:** 36

## Instalação

As versões públicas são disponibilizadas através das [GitHub Releases](https://github.com/visualnew/ParafusoTV/releases).

Baixe o APK correspondente à versão desejada e instale-o manualmente no dispositivo Android ou Fire TV.

Como o aplicativo não é distribuído pela Google Play Store, pode ser necessário permitir a instalação de aplicativos provenientes de fontes externas.

No primeiro uso, configure uma fonte própria, como uma conta Xtream Codes, playlist M3U/M3U8 ou servidor pessoal compatível.

**Nenhum conteúdo ou assinatura IPTV acompanha o ParafusoTV.**

## Compilando o código-fonte

```bash
git clone https://github.com/visualnew/ParafusoTV.git
cd ParafusoTV
./gradlew :app:assembleDebug
```

O APK de desenvolvimento será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Builds de release

Para builds assinadas, utilize seu próprio keystore.

O projeto espera um arquivo local `keystore.properties`, que não deve ser enviado ao GitHub.

```properties
storeFile=path/to/your.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Depois:

```bash
./gradlew :app:assembleRelease
```

Nunca publique seu arquivo `.jks`, `.keystore`, senhas ou `keystore.properties`.

## Estrutura do projeto

O namespace interno continua utilizando `com.lumora` por razões de compatibilidade com a base original e componentes existentes. Isso é uma decisão técnica e não representa a identidade pública do aplicativo.

```text
app/src/main/java/com/lumora/

├── adapter/       Adaptadores e listas
├── anime/         Recursos relacionados a catálogo
├── auto/          Componentes relacionados ao Android Auto
├── cache/         Cache, favoritos, histórico e progresso
├── data/          Banco, provedores, backup e sincronização
├── download/      Downloads offline
├── model/         Modelos de dados
├── pairing/       Pareamento por QR Code
├── parser/        Xtream Codes e M3U
├── player/        Player e Media3
├── plugin/        Engine e loja de plugins
├── recording/     Infraestrutura relacionada a gravações
├── reminder/      Lembretes de programação
├── scraper/       Fontes externas e descoberta
├── torrent/       Streaming baseado em torrent
└── util/          Utilitários compartilhados
```

## Contribuindo

Issues e Pull Requests são bem-vindos.

Antes de propor mudanças grandes, recomenda-se abrir uma Issue descrevendo a ideia, o problema e o impacto esperado.

Contribuições devem respeitar a licença do projeto e das dependências utilizadas.

## Aviso legal

**O ParafusoTV não fornece conteúdo.**

O aplicativo funciona como um player e cliente de mídia. Ele não hospeda, vende ou fornece canais, filmes, séries, assinaturas IPTV ou serviços de streaming.

Todo conteúdo reproduzido depende de uma fonte configurada pelo próprio usuário. O usuário é responsável por garantir que possui autorização para acessar e reproduzir o conteúdo utilizado e que seu uso está de acordo com as leis aplicáveis e os termos dos respectivos serviços.

### Fontes de terceiros

Recursos que utilizam sites, plugins, servidores ou índices externos dependem de serviços que não são operados nem controlados pelo ParafusoTV ou pela Visualnew.

Disponibilidade, conteúdo e legalidade dessas fontes são responsabilidade dos respectivos operadores e usuários.

### Garantia

O software é fornecido **"como está"**, sem garantias expressas ou implícitas, dentro dos limites permitidos pela legislação aplicável.

Consulte também o arquivo [LICENSE](LICENSE).

## Origem e licença

O **ParafusoTV é um trabalho derivado do projeto Lumora**.

Projeto original: https://github.com/disclosurez/Lumora

O código-base recebido está licenciado sob a **PolyForm Noncommercial License 1.0.0**. Consulte o arquivo [LICENSE](LICENSE).

O ParafusoTV é disponibilizado exclusivamente para **uso não comercial**, de acordo com os termos dessa licença.

O aviso obrigatório do projeto original é preservado:

```text
Required Notice: Copyright (c) 2026 Lumora (https://github.com/disclosurez/Lumora)
```

As modificações, identidade visual e trabalho adicional do ParafusoTV são mantidos pela **Visualnew / Jonathan Tavares**, sem remover ou substituir os direitos e avisos pertencentes aos autores e projetos dos quais o software deriva.

## Créditos

**ParafusoTV**

Desenvolvido e mantido por **Jonathan Tavares / Visualnew**.

- Desenvolvedor: https://jonathan.ws
- Site oficial: https://visualnew.com/parafusotv
- GitHub: https://github.com/visualnew/ParafusoTV
- Instagram: https://instagram.com/visualnew
- Facebook: https://facebook.com/visualnew
- YouTube: https://youtube.com/@visualnew

Baseado no projeto **Lumora**, com agradecimento aos seus autores e colaboradores.
