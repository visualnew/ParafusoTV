# ParafusoTV — Etapa 18

## Configurações, backup e idiomas

- Mantida a arquitetura existente de configurações, incluindo idioma da interface, áudio e legendas.
- Backup continua exportando provedores, servidores, favoritos, EPG, grupos, histórico e agendamentos.
- Adicionada proteção opcional por senha para backups.
- Backups protegidos usam AES-256-GCM com chave derivada por PBKDF2-HMAC-SHA256 e salt/IV aleatórios.
- Backups JSON antigos continuam importáveis sem senha.
- Importação de backup protegido solicita a senha apenas quando necessário.
- Falhas de senha inválida/corrupção são reportadas em vez de serem tratadas como uma importação vazia.
- Cancelamento do seletor de arquivos limpa o estado temporário do backup.
- Compatibilidade mantida com dispositivos sem seletor de documentos, usando fallback em armazenamento privado do aplicativo.

## Observação

O build completo ainda precisa ser executado em um ambiente com acesso às dependências do Gradle.
