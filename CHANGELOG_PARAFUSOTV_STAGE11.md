# ParafusoTV — Etapa 11

## TV ao vivo — experiência de troca rápida

- Adicionado botão **CANAIS** ao overlay do player ao vivo.
- Criado seletor rápido de canais sobre o vídeo, sem encerrar o player.
- Canal atual é marcado com **AGORA**.
- Numeração usa `tvgChno` quando disponível e fallback para a posição da lista.
- Seleção de outro canal reaproveita o fluxo de reprodução/failover existente.
- Mantidos zapping por DPAD ↑/↓, favoritos, EPG e preview já existentes.

## Validação

A estrutura do projeto e os arquivos alterados foram revisados. O build completo não pôde ser executado neste ambiente porque o Gradle Wrapper precisa baixar o Gradle 8.12 de `services.gradle.org`, que está inacessível no ambiente.
