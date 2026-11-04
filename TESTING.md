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
| T-005 | Comparação com Aena | Fonte complementar apenas como conceito | SIROS fornece planejamento (HTA). Para CGR, ainda não foi confirmada API nem autorização para reutilizar status operacional Aena; ver pesquisa em `docs/API_SOURCES.md`. |
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

## Resultados dos Testes de ViewModel (Fase 1 + Fase 2) - 03/11/2026

| ID | Teste | Resultado | Detalhes |
|---|---|---|---|
| TV-001 | `testInitialStateIsLoading` | **Sucesso (Aprovado)** | Verifica que o estado inicial do ViewModel é `Loading`. |
| TV-002 | `testLoadFlightsEmptyDatabase` | **Sucesso (Aprovado)** | Verifica que ao carregar com banco vazio retorna estado `Success` com listas vazias. |
| TV-003 | `testLoadFlightsWithData` | **Sucesso (Aprovado)** | Insere voos de chegada e partida via repositório injetado e verifica se ambos aparecem corretamente nas listas filtradas. |
| TV-004 | `testRefreshCallsLoadFlights` | **Sucesso (Aprovado)** | Verifica que o método `refresh()` dispara novo carregamento e retorna estado válido (`Success` ou `Error`). |
| TV-005 | `testRefreshStateEmitsRefreshing` | **Sucesso (Aprovado)** | Valida que a chamada a `refresh()` emite o estado `Refreshing` (com spinner/mantendo dados anteriores) e conclui com sucesso simulando uma fonte remota local sem acesso à internet real. |
| TV-006 | `testGoToPreviousDayChangesDateAndLoadsFlights` | **Sucesso (Aprovado)** | Verifica navegação para dia anterior. |
| TV-007 | `testGoToNextDayChangesDateAndLoadsFlights` | **Sucesso (Aprovado)** | Verifica navegação para próximo dia. |
| TV-008 | `testGoToTodayChangesDateAndLoadsFlights` | **Sucesso (Aprovado)** | Verifica navegação para hoje. |
| TV-009 | `testUtcDayRolloverFlightAppearsOnLocalDate` | **Sucesso (Aprovado)** | Verifica que voo 02:00 UTC aparece na data local correta (ex: 14/10 no fuso UTC-4). |

**Resumo da Execução Unitária (`testDebugUnitTest`) — 03/11/2026 (44 testes):**
- Total de testes: **44**
- Falhas: **0**
- Erros: **0**
- Ignorados: **1** (`SirosStreamParserRealFileTest` — precisa do CSV real)
- `BUILD SUCCESSFUL`

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
  2. Naquele estado (schema v2), instalações novas criavam `sync_metadata` sem `CHECK (id = 1)`. A observação é histórica; o schema atual é v6 e aplica a regra `id = 1` por gatilhos tanto em bancos novos como migrados.

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

## Resumo da Execução Unitária (`testDebugUnitTest`) - 03/11/2026 (Fase 1 Verificada)

| Suite de Testes | Testes | Falhas | Erros | Status |
|---|---|---|---|---|
| `SyncMetadataDaoTest` | 5 | 0 | 0 | ✅ Aprovado |
| `FlightListViewModelTest` | 9 | 0 | 0 | ✅ Aprovado |
| `SirosRepositoryTest` | 5 | 0 | 0 | ✅ Aprovado |
| `SirosCsvParserTest` | 4 | 0 | 0 | ✅ Aprovado |
| `SirosNormalizerTest` | 6 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserTest` | 9 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserBomTest` | 3 | 0 | 0 | ✅ Aprovado |
| `SirosStreamParserRealFileTest` | 1 | 0 | 0 | ✅ Aprovado (pulado se CSV ausente) |
| `SirosRemoteSourceTest` | 2 | 0 | 0 | ✅ Aprovado |
| **TOTAL** | **44** | **0** | **0** | **✅ TODOS APROVADOS** |

**Comando executado:**
```powershell
cmd /c "cd android-app && gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --console=plain"
```
**Resultado:** `BUILD SUCCESSFUL`, **44 testes**, 0 falhas, 0 erros (11.7s).
**APK de debug:** `android-app\app\build\outputs\apk\debug\app-debug.apk` (gerado com sucesso).

---

## Testes Instrumentados (`connectedAndroidTest`) — NÃO EXECUTADOS
Precisam de dispositivo/emulador físico. Não rodados neste ambiente.

---

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

## Correção do CI e da tela Sobre — 2026-11-04

**Falha investigada:** a execução CI #38 (commit `64f4c77`) terminou com 53 testes, 2 falhas e 1 teste ignorado. Os testes de atualização dependiam do download remoto SIROS real, que roda fora do relógio controlado por `runTest`; por isso o estado podia continuar em atualização quando o teste fazia a verificação.

**Alterações verificadas:** os testes de atualização usam uma fonte remota falsa controlada pelo teste. O ViewModel preserva o estado de atualização durante emissões do Room, impede duas sincronizações manuais simultâneas e conclui em sucesso ou erro. A tela Sobre usa ícone de retorno acessível, cores do tema e conteúdo rolável.

**Execução local:** `2026-11-04 18:59 UTC / 14:59 Campo Grande`.
```powershell
cd android-app
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
```
**Resultado reproduzível:** `BUILD SUCCESSFUL`; 53 testes contabilizados: 52 executados, 0 falhas, 0 erros e 1 ignorado (`SirosStreamParserRealFileTest`, depende de CSV real local). APK Debug gerado em `android-app\app\build\outputs\apk\debug\app-debug.apk` (11.019.269 bytes).

**CI remoto:** execução #39, commit `bccfd48` na branch `wip-fase3`, concluída com sucesso em 2026-11-04. [Ver execução no GitHub Actions](https://github.com/mundoarkade1-arch/sky-watch-cg/actions/runs/41892849679).

**Escopo ainda não coberto por esta execução:** teste manual ponta a ponta das notificações após reinicialização do dispositivo. A conclusão do CI não representa esse teste físico.

## Revalidação dos testes de atualização — 2026-11-04

**Comando local** (executado a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.skywatch.cg.ui.viewmodel.FlightListViewModelTest" --console=plain
```
**Resultado:** `BUILD SUCCESSFUL`; 11 testes, 0 falhas, 0 erros e 0 ignorados. O código de saída foi 0.

**Suíte completa e APK Debug:** executados `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` a partir de `android-app`. Resultado: `BUILD SUCCESSFUL`; 53 testes contabilizados, 0 falhas, 0 erros e 1 ignorado (`SirosStreamParserRealFileTest`, depende de CSV real local). APK gerado em `android-app\app\build\outputs\apk\debug\app-debug.apk` (11.019.269 bytes).

Durante essa validação foi corrigido `android-app/gradlew.bat`: o script caía sempre no caminho de falha ao terminar, mesmo quando o Gradle era bem-sucedido. Após o ajuste, confirmou-se saída 0 para o teste aprovado e saída 1 para uma tarefa inexistente.

**CI remoto:** execuções #41 e #42 concluídas com sucesso; a #42 validou o build Debug e a suíte unitária no commit `fa2712d`. [Ver execução #42 no GitHub Actions](https://github.com/mundoarkade1-arch/sky-watch-cg/actions/runs/41896892995).

## Testes instrumentados em aparelho físico — 2026-11-04

Em um aparelho físico Android 14, a primeira execução encontrou um fixture incompleto em `FlightDaoTest` (faltava `arrivalDate`), uma cadeia de migração de teste incompleta (registrava v1→v2 apesar do banco atual estar na v5) e uma divergência real: a migração criava um índice para `alert_sent` que não estava declarado na entidade Room.

Correções: preenchido `arrivalDate` no fixture; teste configurado para executar a cadeia completa v1→v5; declarado na entidade `AlertSent` o índice já criado pela migração v3→v4; acrescentada migração v4→v5 idempotente para criar o índice em bancos v4 sem ele. A variante Debug usa `com.skywatch.cg.debug`, mantendo o pacote Release `com.skywatch.cg` separado.

**Comando executado** (a partir de `android-app`):
```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```
**Resultado no aparelho Android 14 naquela execução:** `BUILD SUCCESSFUL`; 11 testes instrumentados, 0 falhas, 0 erros e 0 ignorados. Incluía migração v1→v5, migração v4→v5 preservando registro de alerta e os testes do DAO de voos.

**Reexecução no aparelho após os ajustes de alertas — 2026-11-04:** executado `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` com o telefone físico modelo 22101320G (Android 14 / API 34), identificado pelo Android SDK. Resultado: `BUILD SUCCESSFUL`; 11 testes, 0 falhas, 0 erros e 0 ignorados. Relatório XML em `android-app\app\build\outputs\androidTest-results\connected\debug\TEST-22101320G - 14-_app-.xml`.

Essa execução valida migrações Room e operações DAO no aparelho; não valida a entrega de notificações, permissões em uso, voo real na janela ou o `BootReceiver` após reinicialização. O telefone não foi reiniciado.

**Validação local complementar:** `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin` e `:app:processReleaseManifest` concluíram com sucesso. Suíte unitária: 53 testes contabilizados, 0 falhas, 0 erros e 1 ignorado por depender de CSV real.

**CI remoto:** CI #46 passou no commit `5c7bdf5`, incluindo a correção Room v5. O CI #47 para `c8d6761` e #48 para `9e82c84` também passaram. A correção de inicialização em `MainActivity` descrita abaixo ainda aguarda CI.

**Teste manual de notificações após reinicialização:** permanece pendente; o aparelho não foi reiniciado, a tela do app não foi configurada para alertas e não foi verificado voo SIROS real na janela exigida. Os 11 testes instrumentados não cobrem esse fluxo ponta a ponta.

## Validação local dos alertas — 2026-11-04

O `FlightAlertWorker` considera horários planejados SIROS entre 15 e 30 minutos antes do evento no fuso do aeroporto, deduplica pelo voo/operação/data local e usa `arrivalDate` como data UTC das chegadas. A permissão de notificações é verificada antes de processar os voos. O registro de deduplicação é salvo após a chamada de publicação, para não marcar como enviado antes de tentar notificar.

**Comandos executados** (a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
```
**Resultado:** `BUILD SUCCESSFUL`; 56 testes contabilizados, 0 falhas, 0 erros e 1 ignorado por depender de CSV real. APK Debug e compilação dos testes instrumentados concluídos.

O WorkManager pode atrasar execuções, então a janela pode passar sem notificação. Esse comportamento usa apenas o horário programado SIROS e não confirma o estado operacional do voo. O teste ponta a ponta após reinicialização continua pendente.

## Validação de inicialização e ViewModel — 2026-11-04

Removida a segunda execução de inicialização em `MainActivity.onCreate()`, que repetia a criação do canal, o pedido de permissão de notificações, o agendamento de trabalhos do WorkManager e a verificação de versão.

**Comando executado** (a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.skywatch.cg.ui.viewmodel.FlightListViewModelTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
```
**Resultado:** `BUILD SUCCESSFUL`; 11 testes do `FlightListViewModelTest`, 0 falhas, 0 erros e 0 ignorados; APK Debug e compilação dos testes instrumentados aprovados. CI #47 e #48 também concluíram com sucesso para commits anteriores; a correção de `MainActivity` ainda precisa de CI.

Após essa correção, `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` também passou no telefone físico Android 14: 11 testes, 0 falhas, 0 erros e 0 ignorados. Isso valida banco/DAO/migrações, mas não abre a tela principal nem verifica a notificação real.

## Migração Room v5→v6 — 2026-11-04

A versão 5 tinha uma diferença: migrações antigas criavam `sync_metadata` com `CHECK (id = 1)`, mas instalações novas usavam o schema gerado pelo Room sem esse `CHECK`. A migração v5→v6 reconstrói apenas essa tabela sem a restrição estrutural, preserva todas as colunas/linhas e adiciona gatilhos de inserção e atualização para rejeitar qualquer `id` diferente de 1. O callback idempotente também instala os gatilhos em bancos criados do zero e em aberturas futuras.

**Comandos executados** (a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
.\gradlew.bat :app:assembleDebug :app:processReleaseManifest --console=plain
```
**Resultados:** suíte unitária: 56 testes, 0 falhas, 0 erros e 1 ignorado por depender de CSV local. No telefone físico Android 14 (22101320G), 13 testes instrumentados, 0 falhas, 0 erros e 0 ignorados. APK Debug e manifest Release compilados com sucesso. Os testes cobrem instalações novas, migração até v6, remoção da antiga `CHECK`, preservação dos dados e rejeição de `id != 1` em inserção/atualização. Nenhum emulador foi aberto.

Schema Room exportado em `android-app/app/schemas/com.skywatch.cg.data.db.AppDatabase/6.json`. CI #49 passou para o commit anterior; o CI das alterações v6 só será iniciado após o envio.

## Roteiro manual — alertas após reinicialização (a executar no Android)

**Objetivo:** verificar permissões e reagendamento pelo `BootReceiver` em um aparelho real. Este roteiro não substitui a informação operacional da companhia aérea nem confirma a operação do voo.

**Comportamento atual a considerar:** o `FlightAlertWorker` só notifica quando o evento planejado está entre 15 e 30 minutos adiante. O trabalho periódico exige conexão de rede e tem intervalo mínimo configurado de 15 minutos; o WorkManager pode atrasar a execução e deixar a janela passar. A fonte SIROS contém planejamento, não confirmação operacional. Use apenas um registro real presente no aplicativo; se não houver voo real na janela de teste, registre “não foi possível testar” em vez de criar dados fictícios.

1. Instale o APK Debug produzido pelo build validado e abra o aplicativo pelo menos uma vez.
2. Quando solicitado, permita notificações. Confirme também nas configurações do Android que as notificações do SKY WATCH CG e o canal “Alertas de Voo” estão habilitados.
3. Conecte o aparelho à internet e atualize a lista SIROS. Confira que existe um voo real no cache com evento programado entre 15 e 30 minutos adiante. Registre o identificador/data do registro SIROS e o horário programado local, sem afirmar que o voo está confirmado.
4. Anote a hora local e UTC; reinicie o aparelho usando o menu normal de energia. Após desbloquear, mantenha a conexão de rede ativa.
5. Aguarde o WorkManager executar. A periodicidade de 15 minutos é um intervalo mínimo aproximado, não um prazo exato. Verifique se a notificação aparece e se mostra chegada/partida e horário local compatíveis com o registro planejado.
6. Observe se a mesma notificação reaparece ou é atualizada em execuções subsequentes. Registre repetições; após o primeiro envio registrado, o worker deve ignorar o mesmo evento.
7. Repita, se possível, com aparelho offline durante parte do período e depois reconectado; anote horários e se o trabalho retomou. Não interprete a ausência de notificação como prova de cancelamento do voo.

**Registrar no resultado:** modelo do aparelho, versão Android, versão/commit do APK, permissões, identificador SIROS e data do voo, horário programado local/UTC, horário do reinício, horário de cada notificação, estado de rede e resultado. Não publicar matrícula, posição ou status operacional sem fonte verificável.

| Campo | Resultado |
|---|---|
| Data/hora do teste (UTC e Campo Grande) | A preencher |
| Aparelho / Android | A preencher |
| APK (versão e commit) | A preencher |
| Permissão e canal de notificação | A preencher |
| Registro SIROS real / evento e horário programado | A preencher |
| Reinicialização e condição de rede | A preencher |
| Notificação recebida (horário) / repetida? | A preencher |
| Resultado / evidências (sem dados sensíveis) | A preencher |
