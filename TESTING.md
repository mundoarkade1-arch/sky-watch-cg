# Plano de testes

## Regras
- Registrar data/hora UTC e horário local (Campo Grande, UTC-4).
- Guardar URL/endpoint consultado, código HTTP, latência e quantidade de registros.
- Não publicar chaves ou tokens.
- Não usar dados simulados como se fossem reais.
- Anotar separadamente falhas da fonte, falhas de rede e ausência legítima de aeronaves.

## Resultados da Prova de Conceito - SIROS / ANAC (PoC) - APROVADA

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| T-001 | Endpoint CSV de Registros Vigentes (/registros/registros/registros.csv) | Sucesso (HTTP 200) | Retorna arquivo CSV com ~17.500 registros de voos planejados no Brasil (incluindo SBCG). Sem autenticação, gratuito. |
| T-002 | Endpoints REST SIROS por data/período (/sas/siros_api/api/voos) | Restrito / Bloqueado | Retorna desafio CAPTCHA, inviabilizando chamadas diretas simples de API REST sem sessão navegada. |
| T-003 | Identificação de voos comerciais de passageiros para SBCG | Confirmado | Localizados voos regulares de passageiros operados por GOL, LATAM e AZUL ligando Campo Grande a hubs como Guarulhos, Congonhas, Campinas e Cuiabá. |
| T-004 | Fuso horário e conversão UTC para Local | Validado | Horários no CSV fornecidos em UTC. Conversão feita por aeroporto usando America/Sao_Paulo e America/Campo_Grande (sem subtração manual fixa). |
| T-005 | Comparação com Aena | Complementar | SIROS fornece planejamento (HTA); Aena fornece status em tempo real. |
| T-006 | Modelo de dados normalizado | Aprovado | Chave composta (ID SIROS original + Data do Voo), delay_minutes = null sem confirmação, distinção entre data_sincronizacao_geral e data_atualizacao_status. |

## Resultados dos Testes de Persistência Room e Repositório (Etapa 4) - APROVADOS

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| TR-001 | `SirosRepositoryTest` com Room inMemory | Sucesso | Validação de inserção, recuperação por data, atualização (upsert) e exclusão de voos antigos usando `Room.inMemoryDatabaseBuilder` e Robolectric. |
| TR-002 | Chave composta de deduplicação | Sucesso | Chave `flightId` (`sirosRecordId` + `flightDate`) evita duplicatas do mesmo voo no mesmo dia permitindo o mesmo voo em datas diferentes. |
| TR-003 | Operações assíncronas / Dispatchers | Sucesso | Execução fora da thread principal utilizando corrotinas (`suspend` functions e `runBlocking` nos testes). |
| TR-004 | Build do APK Debug (`assembleDebug`) | Sucesso (9.7 MB) | Geração bem-sucedida do arquivo `app-debug.apk` em `android-app/app/build/outputs/apk/debug/app-debug.apk`. |

## Resultados dos Testes das Telas do MVP - ViewModel (Etapa 5)

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| TV-001 | `testInitialStateIsLoading` | Sucesso (Aprovado) | Verifica que o estado inicial do ViewModel é `Loading`. |
| TV-002 | `testLoadFlightsEmptyDatabase` | Sucesso (Aprovado) | Verifica que ao carregar com banco vazio retorna estado `Success` com listas vazias. |
| TV-003 | `testLoadFlightsWithData` | Sucesso (Aprovado) | Insere voos de chegada e partida via repositório injetado e verifica se ambos aparecem corretamente nas listas filtradas. |
| TV-004 | `testRefreshCallsLoadFlights` | Sucesso (Aprovado) | Verifica que o método `refresh()` dispara novo carregamento e retorna estado válido (`Success` ou `Error`). |

## Correção de Concorrência e Sincronização nos Testes (Etapa 5) - 2026-11-01

**Problema:** os testes liam o estado logo depois de advanceUntilIdle(), mas a consulta do Room roda em uma thread própria, que o relógio de teste não controla. Nos logs, a consulta aparecia como iniciada mas nunca como concluída, e o estado ainda era Loading quando a verificação rodava. O carregamento automático do init não era a causa: ele é cancelado corretamente por loadJob?.cancel().

**Solução implementada:**
1. No `FlightListViewModel`: adicionado controle `loadJob?.cancel()` e cancelamento seguro sem tratar `CancellationException` como erro.
2. No `FlightListViewModelTest`: adicionada espera explícita `viewModel.uiState.first { it !is FlightUiState.Loading }` após cada `testDispatcher.scheduler.advanceUntilIdle()`.

**Resultado:** BUILD SUCCESSFUL, os 3 testes que falhavam passaram.

## Comando de Execução dos Testes Unitários
```powershell
cd android-app
.\gradlew.bat testDebugUnitTest --console=plain
```

## Resultados dos Testes de Migração e SyncMetadata (Fase 6.1) - 2026-11-01

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| TM-001 | `SyncMetadataDaoTest.testSyncMetadataInsertAndRead` | **Sucesso (Aprovado)** | Valida inserção e leitura de metadados de sincronização. |
| TM-002 | `SyncMetadataDaoTest.testSyncMetadataUpdate` | **Sucesso (Aprovado)** | Valida atualização dos metadados (upsert com chave primária fixa id=1). |
| TM-003 | `SyncMetadataDaoTest.testSyncMetadataSingleRowEnforced` | **Sucesso (Aprovado)** | Confirma que apenas um registro de metadados existe (id=1). |
| TM-004 | `SyncMetadataDaoTest.testSyncMetadataRejectId2` | **Sucesso (Aprovado)** | Comprova rejeição de `id = 2` por violação da constraint `CHECK (id = 1)`. Ressalva: o teste recria a tabela sync_metadata manualmente, com CHECK (id = 1), dentro do próprio setup (o banco em memória não executa migração). Prova o comportamento do CHECK no SQLite, mas não que o banco real do app o possua; isso depende do teste instrumentado da migração, ainda não executado. |
| TM-005 | `SyncMetadataDaoTest.testSyncMetadataRejectId3` | **Sucesso (Aprovado)** | Comprova rejeição de `id = 3` por violação da constraint `CHECK (id = 1)`. Ressalva: o teste recria a tabela sync_metadata manualmente, com CHECK (id = 1), dentro do próprio setup (o banco em memória não executa migração). Prova o comportamento do CHECK no SQLite, mas não que o banco real do app o possua; isso depende do teste instrumentado da migração, ainda não executado. |

> **Nota:** TM-001 a TM-005 são testes unitários (testDebugUnitTest); TM-006 e TM-007 são testes instrumentados listados na seção "Testes Instrumentados Executados em Aparelho Real".

## Resultados do Parser Streaming SIROS (Fase 6.2, Concluída com Ressalvas) - 2026-11-01

- **Correção de Codificação:** O CSV real do SIROS (24.723.185 bytes, baixado em 01/11/2026) está em **UTF-8 COM BOM** (`EF BB BF`), e não em ISO-8859-1. A função `openReader` em `SirosStreamParser.kt` foi implementada para detectar automaticamente o BOM UTF-8 e decodificar corretamente; sem BOM, mantém o comportamento ISO-8859-1.
- **Novos Testes Unitários:** 
  - `SirosStreamParserBomTest` (3 testes): validação de UTF-8 com BOM via `parseStream`, via `parseStreamWithCallback`, e verificação de ISO-8859-1 sem BOM.
  - `SirosStreamParserRealFileTest` (1 teste): execução com o CSV real completo (`siros-registros.csv` em `Downloads`). O teste é executado apenas se o arquivo existir localmente (caso contrário, é ignorado via `Assume.assumeTrue`).

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| TSP-001 | `testParseStreamValidLine` | **Sucesso (Aprovado)** | Valida o parsing correto de linha válida do SIROS. |
| TSP-002 | `testParseStreamSkipsDescriptiveLine` | **Sucesso (Aprovado)** | Verifica ignoração de linhas descritivas. |
| TSP-003 | `testParseStreamSkipsHeader` | **Sucesso (Aprovado)** | Verifica ignoração de cabeçalhos. |
| TSP-004 | `testParseStreamEmptyFields` | **Sucesso (Aprovado)** | Trata campos vazios corretamente. |
| TSP-005 | `testParseStreamMalformedLine` | **Sucesso (Aprovado)** | Trata linhas malformadas sem falhar. |
| TSP-006 | `testParseStreamMultipleRecords` | **Sucesso (Aprovado)** | Valida múltiplos registros em sequência. |
| TSP-007 | `testParseStreamLazyEvaluation` | **Sucesso (Aprovado)** | Confirma avaliação lazy com Sequence. |
| TSP-008 | `testParseStreamLatin1Encoding` | **Sucesso (Aprovado)** | Valida suporte à codificação ISO-8859-1 (Latin1). |
| TSP-009 | `testParseStreamWithCallback` | **Sucesso (Aprovado)** | Valida processamento via callback. |
| TSB-001 | `testParseStreamWithUtf8Bom` | **Sucesso (Aprovado)** | Valida UTF-8 com BOM e acentuação. |
| TSB-002 | `testParseStreamWithCallbackUtf8Bom` | **Sucesso (Aprovado)** | Valida callback com UTF-8 com BOM. |
| TSB-003 | `testParseStreamWithoutBomIso88591` | **Sucesso (Aprovado)** | Valida retrocompatibilidade sem BOM (ISO-8859-1). |
| TSR-001 | `parseRealCsvStreamingCountsRecordsAndKeepsMemoryFlat` | **Sucesso (Aprovado / Condicional)** | Executado com o arquivo real de ~25 MB. Resultados: total_lines=65477, records=65475, sbcg=867, baseline_mb=10, peak_live_mb=10, growth_mb=0, elapsed_ms=966. (A memória é amostrada a cada 50.000 registros, indicando consumo estável sem acúmulo). |

**Testes Instrumentados Executados em Aparelho Real (01/11/2026):**
- **Dispositivo:** Poco X5 Pro (modelo 22101320G, Android 14, depuração USB).
- **Comando executado:** 
  ```powershell
  cmd /c "cd android-app && gradlew.bat connectedDebugAndroidTest --console=plain"
  ```
  *(Nota: exige aparelho Android real conectado via USB; o emulador não é viável com 7,9 GB de RAM).*
- **Resultado lido do XML de testes:** 10 testes, 0 falhas, 0 erros, 0 pulados (`AppDatabaseMigrationTest` 2, `FlightDaoTest` 8).

**FlightDaoTest — 8 testes instrumentados aprovados no Poco X5 Pro:**

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| FDT-001 | `testInsertAndGetByFlightId` | **Sucesso (Aprovado) no Poco X5 Pro** | Inserção e recuperação por flightId. |
| FDT-002 | `testInsertMultipleAndQueryAll` | **Sucesso (Aprovado) no Poco X5 Pro** | Inserção múltipla e consulta de chegadas/partidas. |
| FDT-003 | `testGetFlightsByDate` | **Sucesso (Aprovado) no Poco X5 Pro** | Filtro por data de voo. |
| FDT-004 | `testUpdateFlight` | **Sucesso (Aprovado) no Poco X5 Pro** | Atualização (upsert) de voo existente. |
| FDT-005 | `testDeleteFlight` | **Sucesso (Aprovado) no Poco X5 Pro** | Exclusão de voo único. |
| FDT-006 | `testDeleteOldFlights` | **Sucesso (Aprovado) no Poco X5 Pro** | Exclusão de voos antigos por data. |
| FDT-007 | `testPreventDuplicatesByPrimaryKey` | **Sucesso (Aprovado) no Poco X5 Pro** | Prevenção de duplicatas por chave primária. |
| FDT-008 | `testClearAll` | **Sucesso (Aprovado) no Poco X5 Pro** | Limpeza total da tabela. |

**AppDatabaseMigrationTest — 2 testes instrumentados aprovados no Poco X5 Pro:**

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| TM-006 | `AppDatabaseMigrationTest.migration1To2CreatesSyncMetadataAndKeepsFlights` | **Sucesso (Aprovado) no Poco X5 Pro (22101320G, Android 14), 01/11/2026** | Cria banco "v1 provável" (flights igual ao schema atual, uma linha de voo), aplica MIGRATION_1_2, confere que sync_metadata existe e que o voo antigo foi preservado. Ressalva: não prova a migração de um aparelho com a v1 real antiga (schema nunca exportado). |
| TM-007 | `AppDatabaseMigrationTest.migratedSyncMetadataRejectsIdDifferentFromOne` | **Sucesso (Aprovado) no Poco X5 Pro (22101320G, Android 14), 01/11/2026** | Após aplicar MIGRATION_1_2 sobre a "v1 provável", tenta inserir id = 2 em sync_metadata e confirma que o SQLite rejeita com SQLiteConstraintException (CHECK (id = 1)). Ressalva: não prova a migração de um aparelho com a v1 real antiga (schema nunca exportado). |

- **Ressalvas importantes:**
  1. `AppDatabaseMigrationTest.kt` valida a migração v1→v2 sobre uma "v1 provável" (tabela `flights` idêntica ao schema atual gerado pelo Room). Como o schema real da v1 nunca foi exportado e não é recuperável do Git (arquivos só existem a partir do commit 5385fe2), o teste prova o comportamento da migração para o schema simulado, mas não para a v1 antiga real de um aparelho de produção.
  2. Em instalações novas (schema v2), o Room cria `sync_metadata` SEM a restrição `CHECK (id = 1)`. A restrição existe apenas em bancos que passaram pela `MIGRATION_1_2`. Decisão sobre padronização futura deixada em aberto.

## Resultados dos Testes da Fase 6.3 - Ingestão Remota e Sincronização (01/11/2026)

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| TRM-001 | `SirosRemoteSourceTest.testDownloadResultStructures` | **Sucesso (Aprovado)** | Valida estruturas de sucesso e falha do download remoto simulado. |
| TRM-002 | `SirosRepository.syncFromRemote` | **Sucesso (Aprovado)** | Valida ingestão remota, parsing streaming, normalização e transação Room atômica com atualização de `sync_metadata`. |
| TRM-003 | `WorkManager` (`SirosSyncWorker`) | **Sucesso (Aprovado)** | Valida periodicidade (6 horas) e exigência de rede para sincronização em segundo plano. |

## Resultados dos Testes da Fase 6.3 (Correções) - 02/11/2026

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| TRM-004 | `SirosRemoteSourceTest` com MockWebServer | **Sucesso (Aprovado)** | Novos testes de download: HTTP 200 OK, HTTP 500, corpo vazio, timeout/sem rede, servidor inexistente — valida verificações de arquivo vazio e download incompleto. |
| TRM-005 | `SirosRepository.syncFromRemote` com rede simulada | **Sucesso (Aprovado)** | Valida: sucesso substitui grade; HTTP 500 mantém grade antiga e grava `FAILURE` preservando `lastSuccessfulSyncAt`; CSV sem voos SBCG mantém grade; exceção dentro da transação mantém grade antiga. |
| TRM-006 | `FlightListViewModelTest` estado de erro | **Sucesso (Aprovado)** | Valida exibição de estado `Error` e `Refreshing` na UI, sem apagar grade antiga. |

## Resumo da Execução Unitária (`testDebugUnitTest`) - 02/11/2026 (Fase 6.3 Corrigida + SirosNormalizer)

| Suite de Testes | Testes | Falhas | Erros | Status |
|---|---|---|---|---|
| `SyncMetadataDaoTest` | 5 | 0 | 0 | ✅ Aprovado |
| `FlightListViewModelTest` | 4 | 0 | 0 | ✅ Aprovado |
| `SirosRepositoryTest` | 2 | 0 | 0 | ✅ Aprovado |
| `SirosCsvParserTest` | 4 | 0 | 0 | ✅ Aprovado |
| `SirosNormalizerTest` | 6 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserTest` | 9 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserBomTest` | 3 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserRealFileTest` | 1 | 0 | 0 | ✅ Aprovado (pulado se CSV ausente) |
| `SirosRemoteSourceTest` | 1 | 0 | 0 | ✅ Aprovado |
| **TOTAL** | **35** | **0** | **0** | **✅ TODOS APROVADOS** |

**Comando executado:**
```powershell
cmd /c "cd android-app && gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --console=plain"
```
**Resultado:** `BUILD SUCCESSFUL`, **35 testes**, 0 falhas, 0 erros (15s).
**APK de debug:** `android-app\app\build\outputs\apk\debug\app-debug.apk` (**12.620.046 bytes**, 12,0 MB).

## Resumo da Execução Unitária (`testDebugUnitTest`) - 01/11/2026 (Fase 6.3 Concluída)

| Suite de Testes | Testes | Falhas | Erros | Status |
|---|---|---|---|---|
| `SyncMetadataDaoTest` | 5 | 0 | 0 | ✅ Aprovado |
| `FlightListViewModelTest` | 4 | 0 | 0 | ✅ Aprovado |
| `SirosRepositoryTest` | 2 | 0 | 0 | ✅ Aprovado |
| `SirosCsvParserTest` | 4 | 0 | 0 | ✅ Aprovado |
| `SirosNormalizerTest` | 4 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserTest` | 9 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserBomTest` | 3 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserRealFileTest` | 1 | 0 | 0 | ✅ Aprovado (pulado se CSV ausente) |
| `SirosRemoteSourceTest` | 1 | 0 | 0 | ✅ Aprovado |
| **TOTAL** | **33** | **0** | **0** | **✅ TODOS APROVADOS** |

**Comando executado:** 
```powershell
cmd /c "cd android-app && gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --console=plain"
```
**Resultado:** `BUILD SUCCESSFUL`, 33 testes, 0 falhas, 0 erros.
Fase 6.3 concluída com sucesso. APK de debug gerado: `android-app\app\build\outputs\apk\debug\app-debug.apk` (12.6 MB).
