# PENDENCIAS.md

Lista de pendências e itens futuros do projeto **SKY WATCH CG**:

1. **Testes do `syncFromRemote` e `SirosSyncWorker`**:
   - Os testes de integração do sincronizador (`SirosSyncIntegrationTest.kt`) e do Worker (`SirosSyncWorker`) foram isolados na pasta externa `C:\Users\janie\Downloads\skywatch-pendentes` para aguardar a decisão sobre a adição da dependência de teste do WorkManager (`androidx.work:work-testing`), que requer autorização explícita do usuário.
   - **Pendente (Etapa A Item 1)**: Teste `testSyncFromRemoteSuccess` falhando devido a incompatibilidade de data de referência entre o normalizador (usa `LocalDate.now()`) e a data consultada no teste. 2 de 3 testes passando (`testSyncFromRemoteDownloadFailurePreservesGrid` ✅, `testSyncFromRemoteZeroSbcgFlightsPreservesGrid` ✅). O CSV de sucesso precisa ajustar a data de referência do normalizador ou a data consultada no teste.

2. **Navegação por Data na Interface**:
   - Validação completa da seleção de datas diferentes no calendário da interface principal e sua integração reativa com o `FlightListViewModel`.
   - **Concluído (Etapa A Item 2)**: Implementados métodos no `FlightListViewModel` (`goToPreviousDay()`, `goToToday()`, `goToNextDay()`) e 3 testes unitários em `FlightListViewModelTest` (`testGoToPreviousDayChangesDateAndLoadsFlights`, `testGoToTodayChangesDateAndLoadsFlights`, `testGoToNextDayChangesDateAndLoadsFlights`) — todos passando.
   - **Concluído (Etapa A Item 3)**: Virada do dia UTC — voo de 02:00 UTC exibido na data LOCAL (UTC-4 = 22:00 do dia anterior).
     - Adicionado teste `testUtcDayRolloverFlightAppearsOnLocalDate` em `FlightListViewModelTest` (falhou inicialmente, depois passou).
     - Corrigido `SirosRepository.getArrivalsForUi` e `getDeparturesForUi` para buscar janela de 3 dias e filtrar por data local computada a partir de UTC + timezone do aeroporto (`ZoneId`).
     - Adicionados métodos `getArrivalsByDateRange` e `getDeparturesByDateRange` no `FlightDao`.
     - 9 testes em `FlightListViewModelTest` — todos passando.

3. **Instalações Novas vs. Migradas do Room**:
   - Em instalações novas (schema v2), o Room cria a tabela `sync_metadata` sem a restrição `CHECK (id = 1)` (presente apenas em bancos migrados da v1). Avaliar se é necessário unificar via script de criação inicial.

4. **SirosSyncWorker (Etapa A Item 5)**:
   - Não adicionar dependência `androidx.work:work-testing` sem autorização. Registrado para futura implementação.

## Próxima Fase - Fase 6.3/6.4 (WorkManager + Notificações)
- **Fase 6.3**: Sincronização periódica em background com `WorkManager` (`SirosSyncWorker` com política de retentativa, `BackoffPolicy.EXPONENTIAL`, max 5 tentativas, constraint de rede).
- **Fase 6.4**: Agendamento de notificações locais (`AlarmManager`/`WorkManager`) para alertas de voo (ex: "Seu voo GOL 1234 parte em 2h"). Tratamento de reinicialização via `BOOT_COMPLETED` receiver.
- **Testes em dispositivo real** e geração de APK de debug.