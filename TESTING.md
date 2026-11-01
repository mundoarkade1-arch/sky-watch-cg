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
| TM-004 | `SyncMetadataDaoTest.testSyncMetadataRejectId2` | **Sucesso (Aprovado)** | Comprova rejeição de `id = 2` por violação da constraint `CHECK (id = 1)`. |
| TM-005 | `SyncMetadataDaoTest.testSyncMetadataRejectId3` | **Sucesso (Aprovado)** | Comprova rejeição de `id = 3` por violação da constraint `CHECK (id = 1)`. |
| TM-006 | `AppDatabaseMigrationTest.migrate1To2PreservesFlights` | **Pendente (sem dispositivo/emulador)** | Teste de migração Room v1→v2 usando `MigrationTestHelper`: insere voos na v1, migra para v2, verifica preservação total dos dados na tabela `flights`. Requer dispositivo/emulador Android conectado. |

> **Nota:** Os testes unitários (TM-001 a TM-005) foram executados e aprovados via `testDebugUnitTest` em 2026-11-01. O teste instrumentado TM-006 permanece pendente por falta de dispositivo/emulador Android disponível.

## Resultados do Parser Streaming SIROS (Fase 6.2, em andamento) - 2026-11-01

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

**Pendências NÃO validados:**
- teste instrumentado da migração v1 para v2 (TM-006, sem dispositivo ou emulador);
- ingestão remota (download, syncFromRemote, WorkManager) ainda não implementada, a Fase 6.3 não foi autorizada.


## Resumo da Execução Unitária (testDebugUnitTest) - 01/11/2026

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
| **TOTAL** | **32** | **0** | **0** | **✅ TODOS APROVADOS** |

**Comando executado para garantir execução limpa (evitando cache do Gradle):** 
```powershell
cmd /c "cd android-app && gradlew.bat cleanTestDebugUnitTest testDebugUnitTest --console=plain"
```
**Resultado:** BUILD SUCCESSFUL, 32 testes, 0 falhas, 0 erros.
Atenção: a Fase 6.2 ainda NÃO está totalmente concluída. O parser foi validado com o CSV real e testes unitários passando, mas o teste instrumentado da migração Room v1→v2 (TM-006) segue pendente por falta de dispositivo ou emulador conectado.
