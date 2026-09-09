# ParafusoTV — Contexto do Projeto

## Status atual

A ParafusoTV 1.0 é a versão estável e funcional do aplicativo.

A pasta atual é uma cópia da versão 1.0 destinada exclusivamente ao desenvolvimento da ParafusoTV 2.0.

NUNCA alterar a pasta original da ParafusoTV 1.0 durante o desenvolvimento da 2.0.

## Identidade

Nome do aplicativo: ParafusoTV

Objetivo: aplicativo IPTV premium para Android TV, Amazon Fire TV e smartphones Android.

Estética:
- premium
- cinematográfica
- escura
- metálica
- aço escovado/prata
- identidade visual baseada em um parafuso
- evitar aparência genérica ou excessivamente cinza/plana

Logo:
- símbolo metálico inspirado em parafuso
- incorpora elemento de reprodução/play
- visual tridimensional
- acabamento metálico premium

## Plataforma técnica

Projeto baseado no Lumora.

Application ID:
com.parafusotv

Namespace atual:
com.lumora

Compile SDK:
36

Target SDK:
36

Min SDK:
25

Java/Kotlin:
17

Gradle:
8.12

Build principal:
./gradlew :app:assembleDebug

## ParafusoTV 1.0 — recursos existentes

A versão 1.0 possui, entre outros:

- perfis de usuário
- até 4 perfis
- PIN opcional
- perfil infantil
- controle parental
- listas IPTV
- Xtream Codes
- M3U/M3U8
- múltiplas fontes
- mesclagem de fontes
- favoritos
- histórico
- progresso de reprodução
- TV ao vivo
- EPG
- filmes
- séries
- busca
- player baseado em Media3/ExoPlayer
- suporte a PiP quando disponível
- backup/importação local
- atualização automática das listas
- menu lateral
- interface para Android TV/Fire TV
- interface para smartphones Android
- splash com logo ParafusoTV
- reprodução em tela cheia

Menu principal atual:

- TV ao vivo
- Filmes
- Séries
- Favoritos
- Gravações
- Guia de TV
- Configurações

## Gravações

A interface de Gravações existe, mas a captura completa do stream ainda não está implementada.

Não criar uma falsa funcionalidade de gravação.

Quando a função ainda não estiver implementada, informar claramente ao usuário que a funcionalidade está em desenvolvimento.

## Objetivo principal da ParafusoTV 2.0

A versão 2.0 deve evoluir principalmente a experiência de usuário.

Não devemos simplesmente adicionar funções aleatoriamente.

A prioridade é criar uma interface significativamente mais elegante, moderna, intuitiva e adaptada a cada tipo de dispositivo.

## TV — Android TV e Fire TV

A interface de TV deve ser pensada para controle remoto/D-pad.

Não depender de mouse ou touchscreen para funções importantes.

O foco selecionado deve ser sempre visualmente evidente.

A experiência desejada é inspirada no conceito de aplicativos modernos de streaming, com navegação por conteúdo e carrosséis horizontais.

Não copiar código ou interface proprietária de outros aplicativos.

Desejamos reduzir a sensação atual de:

Menu → categoria → aba → lista.

Preferimos uma experiência mais próxima de:

Menu → conteúdo relevante → carrosséis horizontais → detalhes → reprodução.

Exemplos de possíveis linhas:
- Continuar assistindo
- Favoritos
- Mais assistidos
- Filmes
- Séries
- Canais
- Categorias

Se uma seção estiver vazia, ela não deve aparecer.

## Smartphone

O mesmo APK deve funcionar em smartphones Android.

Porém, a interface precisa ser realmente responsiva e pensada para touchscreen.

Problemas conhecidos da versão 1.0:

1. Home/catálogo em portrait está pequeno, deslocado e mal adaptado.
2. Em landscape fica melhor, mas ainda existem problemas de posicionamento.
3. O menu lateral pode rolar, mas o toque/click não funciona corretamente em algumas áreas.
4. O player em landscape ficou muito bom, mas a orientação do telefone não retorna corretamente após sair da reprodução/rotacionar.
5. Alguns elementos da interface antes da reprodução ficam pequenos ou mal posicionados.

Esses problemas devem ser tratados como conjunto na 2.0, e não como pequenos patches isolados.

## Experiência desejada no smartphone

Portrait:
- catálogo principal
- navegação por toque
- cards maiores
- grids/carrosséis adequados à largura da tela
- menu e botões próprios para toque
- layout centralizado

Landscape:
- reprodução de vídeo otimizada
- controles apropriados
- aproveitamento da tela

Ao sair do player, a orientação deve voltar corretamente para o modo apropriado da interface.

## Player

O player da versão 1.0 funciona muito bem em tela cheia na Fire TV.

Esse comportamento não deve ser quebrado.

No smartphone, o player deve continuar priorizando landscape quando apropriado.

O player usa Media3/ExoPlayer como mecanismo de reprodução.

A interface visual do player pertence à ParafusoTV.

## IPTV

Adicionar lista:

1. Xtream Codes como método principal
2. M3U/M3U8 como alternativa

Permitir múltiplas listas.

Cada lista deve possuir:
- nome
- status de conexão
- última atualização
- quantidade de canais
- quantidade de filmes
- quantidade de séries
- prioridade
- ativar/desativar
- atualizar manualmente
- editar
- excluir

Testar conexão antes de salvar.

Usar cache local.

Atualizar listas automaticamente em segundo plano.

## Canais

Apresentação desejada:
- logo
- número
- nome
- indicador AO VIVO
- qualidade quando disponível

Troca de canais pelo D-pad deve ser rápida.

Ao entrar em TV ao vivo:
- abrir o último canal utilizado quando disponível
- caso contrário, abrir o primeiro canal disponível

## Busca

A busca deve ser global e local.

Pesquisar:
- canais
- filmes
- séries

Desejamos resultados enquanto o usuário digita, quando tecnicamente adequado.

Favoritos e itens recentemente utilizados podem receber prioridade.

A busca deve tolerar pequenas diferenças de escrita.

## Filmes

Desejamos:
- posters
- categorias
- grids/carrosséis
- detalhes
- capa
- sinopse
- gênero
- ano
- botão de reprodução

## Séries

Desejamos:
- posters
- categorias
- detalhes
- temporadas
- episódios
- continuar assistindo
- retomada do episódio
- reprodução automática do próximo episódio quando apropriado

## Perfis

Até 4 perfis.

Cada perfil possui independentemente:
- favoritos
- histórico
- progresso
- último canal
- preferências

O último perfil usado deve abrir automaticamente.

Troca de perfil pelo menu.

Avatar padrão:
- parafuso metálico

Permitir:
- nome
- avatar
- cor/estilo por perfil

## Backup

Tudo deve continuar local.

Não criar servidor ou conta própria da ParafusoTV.

Backup/importação deve poder preservar:
- perfis
- PINs
- favoritos
- histórico
- progresso
- listas IPTV
- preferências

Backups protegidos por senha devem ser criptografados.

## Privacidade e arquitetura

A ParafusoTV não possui servidor próprio nem conta própria obrigatória.

Dados do usuário devem permanecer localmente no dispositivo, salvo aquilo que seja naturalmente necessário para acessar os provedores IPTV configurados pelo usuário.

## Atualizador

Não reativar o atualizador apontando para o repositório original do Lumora.

A ParafusoTV não deve buscar automaticamente atualizações do Lumora.

## Compatibilidade

A 2.0 deve manter suporte a:
- Amazon Fire TV
- Android TV
- smartphones Android

O objetivo é um único APK com interfaces adaptadas ao tipo de dispositivo.

Não criar dois aplicativos separados sem uma necessidade técnica real.

## Regra fundamental de desenvolvimento

Antes de modificar arquivos importantes:

1. entender a arquitetura existente;
2. preservar as funcionalidades que já funcionam;
3. evitar alterações desnecessárias;
4. testar compilação;
5. não substituir uma funcionalidade existente por uma implementação falsa;
6. não remover funcionalidades sem motivo;
7. não alterar a versão estável original.

A versão 1.0 é nossa referência funcional.

## Processo desejado

O desenvolvimento da 2.0 deve ser feito em grupos coerentes de mudanças.

Evitar ciclos desnecessários de:
alterar uma linha → compilar → instalar → testar → repetir.

Primeiro analisar e planejar conjuntos de mudanças.

Quando houver decisões de produto/UX que realmente precisem da aprovação do proprietário do projeto, perguntar antes de implementar.

Não fazer perguntas triviais que possam ser decididas tecnicamente.

## Prioridade inicial da 2.0

A primeira grande frente deve ser a arquitetura de interface responsiva:

1. separar claramente as necessidades de TV e smartphone;
2. manter a mesma lógica/conteúdo;
3. criar experiência de TV baseada em D-pad;
4. criar experiência mobile baseada em touchscreen;
5. melhorar home/catalogo;
6. migrar gradualmente para navegação por carrosséis/linhas de conteúdo;
7. corrigir orientação do player no smartphone;
8. corrigir interação touch do menu mobile.

Antes de implementar, analisar o projeto inteiro e apresentar um plano.

## Regra para o Codex

Neste primeiro contato com o projeto 2.0:

NÃO ALTERAR NENHUM ARQUIVO.

Primeiro ler e analisar a arquitetura existente e este documento.

Depois apresentar um plano técnico para a ParafusoTV 2.0, especialmente para a separação entre interface de TV e smartphone.

A implementação somente deve começar após aprovação do plano.
