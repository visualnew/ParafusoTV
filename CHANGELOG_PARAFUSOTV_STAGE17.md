# ParafusoTV — Stage 17

## Gravações e agendamento

- Gerenciador de gravações para TV com estados Agendada, Em andamento, Concluída, Falhou e Cancelada.
- Cancelamento de gravações agendadas com remoção dos alarmes.
- Detalhes de horário, canal e armazenamento.
- DAO ganhou exclusão de registros.
- Mantido o agendamento por AlarmManager e restauração após reboot.

## Limitação explícita

A infraestrutura desta etapa gerencia o agendamento e o estado da gravação, mas o backend atual do projeto ainda não implementa captura persistente de um stream ao vivo em arquivo para todos os formatos (TS/HLS/DASH). Portanto esta etapa não declara que um arquivo de vídeo foi gravado quando a captura não ocorreu.
