# PENDENCIAS.md

Lista de pendências e itens futuros do projeto **SKY WATCH CG**:

1. **Testes do `syncFromRemote` e `SirosSyncWorker`**:
   - Os testes de integração do sincronizador (`SirosSyncIntegrationTest.kt`) e do Worker (`SirosSyncWorker`) foram isolados na pasta externa `C:\Users\janie\Downloads\skywatch-pendentes` para aguardar a decisão sobre a adição da dependência de teste do WorkManager (`androidx.work:work-testing`), que requer autorização explícita do usuário.
2. **Navegação por Data na Interface**:
   - Validação completa da seleção de datas diferentes no calendário da interface principal e sua integração reativa com o `FlightListViewModel`.
3. **Instalações Novas vs. Migradas do Room**:
   - Em instalações novas (schema v2), o Room cria a tabela `sync_metadata` sem a restrição `CHECK (id = 1)` (presente apenas em bancos migrados da v1). Avaliar se é necessário unificar via script de criação inicial.