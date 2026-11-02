# PENDENCIAS.md

Lista de pendências e itens futuros do projeto **SKY WATCH CG**:

1. **Testes do `syncFromRemote` e `SirosSyncWorker`**:
   - Os testes de integração do sincronizador (`SirosSyncIntegrationTest.kt`) e do Worker (`SirosSyncWorker`) foram isolados na pasta externa `C:\Users\janie\Downloads\skywatch-pendentes` para aguardar a decisão sobre a adição da dependência de teste do WorkManager (`androidx.work:work-testing`), que requer autorização explícita do usuário.
   - **Pendente (Etapa A Item 1)**: Teste `testSyncFromRemoteSuccess` falhando devido a formato de CSV - 2 de 3 testes passando (`testSyncFromRemoteDownloadFailurePreservesGrid` ✅, `testSyncFromRemoteZeroSbcgFlightsPreservesGrid` ✅). O CSV de sucesso precisa ajustar o formato exato esperado pelo parser SIROS.

2. **Navegação por Data na Interface**:
   - Validação completa da seleção de datas diferentes no calendário da interface principal e sua integração reativa com o `FlightListViewModel`.
   - **Concluído (Etapa A Item 2)**: Implementados métodos no `FlightListViewModel` (`goToPreviousDay()`, `goToToday()`, `goToNextDay()`) e 3 testes unitários em `FlightListViewModelTest` (`testGoToPreviousDayChangesDateAndLoadsFlights`, `testGoToTodayChangesDateAndLoadsFlights`, `testGoToNextDayChangesDateAndLoadsFlights`) — todos passando.
   - **Pendente (Etapa A Item 3)**: Virada do dia UTC: voo exibido na data LOCAL (UTC-4) e ordenado pelo horário local. Escrever primeiro um teste que falhe com um voo de 02:00 UTC e depois corrigir.

3. **Instalações Novas vs. Migradas do Room**:
   - Em instalações novas (schema v2), o Room cria a tabela `sync_metadata` sem a restrição `CHECK (id = 1)` (presente apenas em bancos migrados da v1). Avaliar se é necessário unificar via script de criação inicial.

4. **SirosSyncWorker (Etapa A Item 5)**:
   - Não adicionar dependência `androidx.work:work-testing` sem autorização. Registrado para futura implementação.