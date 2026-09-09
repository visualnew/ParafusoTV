# ParafusoTV — Etapa 6

## Dados separados por perfil

Esta etapa transforma o sistema de perfis em isolamento real para dados de uso local.

### Dados com namespace por perfil
- Favoritos de filmes/séries
- Favoritos de canais
- Canais recentemente reproduzidos
- Marcações de conteúdo assistido
- Posições de reprodução / Continuar assistindo

Cada conjunto usa o ID do perfil ativo para evitar que o histórico de um perfil apareça em outro.

### Compatibilidade
A migração inicial preserva dados antigos existentes no primeiro perfil quando encontrados no formato anterior.

### Troca de perfil
Ao selecionar outro perfil, os caches em memória dos stores são invalidados imediatamente, evitando vazamento visual do perfil anterior durante a mesma sessão.

## Observação de validação
A estrutura Kotlin/XML foi revisada localmente. A compilação Android completa ainda depende de um ambiente que tenha acesso à distribuição/dependências do Gradle.
