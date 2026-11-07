# Plano de testes

## Seção 7 — radar, etapa 1: lógica (2026-11-07)

| Item | Situação | Commit | Teste por item |
|---|---|---|---|
| Elevação e cabeceiras de SBCG | Feito | `f7087b7`, `f1946f9` | `sbcgUsesPublishedElevationAndRunwayHeadings` em [RadarAirportConfigTest.kt](android-app/app/src/test/java/com/skywatch/cg/data/radar/RadarAirportConfigTest.kt) |
| Histórico curto por hex e tendência de distância/altitude | Feito | `f7087b7`, `bdd4028` | `historyIsShortAndKeyedByHex`, `missingRatesUseAltitudeSamplesToDeriveDescent` em [RadarTrajectoryRulesTest.kt](android-app/app/src/test/java/com/skywatch/cg/data/radar/RadarTrajectoryRulesTest.kt) |
| `baro_rate`/`geom_rate` somente quando presentes | Feito | `f7087b7`, `bdd4028` | `missingBarometricRateUsesGeometricRateWhenPresent`, `missingRatesUseAltitudeSamplesToDeriveDescent` |
| Regras de pouso/decolagem em AGL | Feito | `f7087b7`, `bdd4028` | `typicalLandingUsesAltitudeAboveAirport`, `typicalTakeoffUsesAltitudeAboveAirport` |
| Ignorar sobrevoo, solo, dado velho e posição inválida | Feito | `f7087b7`, `f1946f9` | `highOverflightIsIgnoredUsingAgl`, `stationaryAircraftIsIgnored`, `stalePositionIsIgnored`, `invalidPositionIsIgnored` |
| Helicóptero | Feito — decisão conservadora | `f7087b7`, `bdd4028` | `helicopterIsIgnoredConservativelyWhenCategoryIsA7`; categoria `A7` é ignorada, categoria ausente não é inventada |
| Deduplicação por hex/tipo com cooldown | Feito | existente, revisado na etapa | `deduplicatesByAircraftAndTypeForTwentyMinutes` em [RadarAlertRulesTest.kt](android-app/app/src/test/java/com/skywatch/cg/data/radar/RadarAlertRulesTest.kt) |

Fonte aeronáutica: [AISWEB/DECEA — SBCG AD 2](https://aisweb.decea.mil.br/eaip/A%2020-2026_2026_10_29/eAIP/AD%202%20SBCG-pt-BR.html), que publica 559 m/1834 ft e as cabeceiras 06/24 com rumos verdadeiros 042,26°/222,26°. A categoria `A7` como rotorcraft foi conferida na [referência do ADS-B emitter category](https://mode-s.org/1090mhz/content/ads-b/2-identification.html).

Suíte completa: `testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`; 123 testes, 0 falhas, 0 erros e 1 ignorado. `assembleDebug` passou; nenhuma alteração de UI foi feita.

## Seção 5 — exibição e fusos (2026-11-07)

| Item | Situação | Commit | Teste por item |
|---|---|---|---|
| `formatAirportDisplay` | Feito | `2144770`, `ea63f45` | `formatAirportDisplayInternationalUsesCityInsteadOfCountry`, `formatAirportDisplayKeepsPortuguesePrepositionLowercase` e `formatAirportDisplayKeepsUnknownPatternWithoutInventingCity` em [FlightDisplayUtilsTest.kt](android-app/app/src/test/java/com/skywatch/cg/util/FlightDisplayUtilsTest.kt) |
| `formatAirlineName` | Feito | `4045032` | `formatAirlineNameRemovesPortugueseAndCorporateSuffixes` em [FlightDisplayUtilsTest.kt](android-app/app/src/test/java/com/skywatch/cg/util/FlightDisplayUtilsTest.kt) |
| `nextMovements` | Feito | `0a99b21`, `369c266` | `todayMovementAlreadyPastIsExcluded`, `futureDateMovementIsIncluded` e `ignoresMovementsFromEarlierDates` em [NextMovementsTest.kt](android-app/app/src/test/java/com/skywatch/cg/util/NextMovementsTest.kt) |
| `DetailScreen` rolável e `formatAirportDisplay` | Feito | `6678e38`, `4f2a682` | `detailDisplayUsesFormattedAirportNames` em [DetailScreenDisplayTest.kt](android-app/app/src/test/java/com/skywatch/cg/ui/screens/DetailScreenDisplayTest.kt); rolagem não verificada em aparelho físico |
| Fusos por ICAO | Feito | `b5afc50`, `ea63f45` | `brazilianCapitalTimezoneMapCoversCuiabaManausAndRioBranco` e `knownIcaoTimezoneOverridesAnIncorrectDeclaredTimezone` em [FlightDisplayUtilsTest.kt](android-app/app/src/test/java/com/skywatch/cg/util/FlightDisplayUtilsTest.kt); ICAO desconhecido usa o fallback declarado |

Suíte completa: `testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`; 113 testes, 0 falhas, 0 erros e 1 ignorado. `assembleDebug` também passou.

## Seção 6 — parser SIROS (2026-11-07, revisada)

| Item | Situação | Commit | Teste por item |
|---|---|---|---|
| Parser único em produção e testes | Feito | `007d33b` | `SirosRepository` usa `SirosStreamParser`; `SirosCsvParser` e seus testes foram removidos |
| Cabeçalho descritivo e cabeçalho de 28 colunas | Feito | `178dce6` | `testParseStreamSkipsDescriptiveLine`, `testParseStreamSkipsHeader` em [SirosStreamParserTest.kt](android-app/app/src/test/java/com/skywatch/cg/data/siros/SirosStreamParserTest.kt) |
| Cabeçalho ausente gera erro claro | Feito | `007d33b` | `testParseWithoutRecognizableHeaderReportsFormatChange` e `testSyncWithoutRecognizableHeaderReportsFormatChange` |
| Codificação UTF-8 com BOM | Feito | `178dce6` | `testParseStreamWithUtf8Bom`, `testParseStreamWithCallbackUtf8Bom` em [SirosStreamParserBomTest.kt](android-app/app/src/test/java/com/skywatch/cg/data/siros/SirosStreamParserBomTest.kt) |
| Codificação ISO-8859-1 sem BOM e acentos | Feito | `178dce6` | `testParseStreamLatin1Encoding`, `testParseStreamWithoutBomIso88591` |
| Campos vazios, linha curta e erro por callback | Feito | `79f9cd5` | `testParseStreamEmptyFields`, `testParseStreamMalformedLine`, `testParseStreamWithCallbackReportsShortLine` |
| Delimitador `;`, campo citado e aspas escapadas | Feito | `79f9cd5` | `testParseStreamPreservesQuotedDelimiterAndEscapedQuote` |
| Avaliação streaming/lazy e callback | Feito | `178dce6` | `testParseStreamLazyEvaluation`, `testParseStreamWithCallback` |
| Filtro de passageiros por `objetoTransporte` e `tipoServico` | Feito | `dd4e0ef` | `fixtureUsesPassengerObjectAndServiceAndRejectsCargoVariant` em [SirosFixturePassengerFilterTest.kt](android-app/app/src/test/java/com/skywatch/cg/data/siros/SirosFixturePassengerFilterTest.kt) |
| Logs por registro | Feito | `007d33b` | Revisão de [SirosRepository.kt](android-app/app/src/main/java/com/skywatch/cg/data/siros/SirosRepository.kt): permanecem apenas logs agregados/erro |
| Fixture CSV completo e memória | Pendente localmente | `178dce6` | `parseRealCsvStreamingCountsRecordsAndKeepsMemoryFlat`; ignorado porque o arquivo não está no repositório |

Fixture versionada: 2 registros de `siros_cgr_real_sample.csv`, ambos `Tipo Serviço = REGULAR DE PASSAGEIROS` e `Objeto Transporte = PASSAGEIROS`. No CSV oficial baixado (26.650.737 bytes), 929 registros envolviam `SBCG`; todos tinham a mesma combinação. O arquivo completo permaneceu apenas em diretório temporário e não foi versionado.

Suíte completa: `testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`; 112 testes, 0 falhas, 0 erros e 1 ignorado. O APK Debug foi gerado.

## Seção 4 — checklist de alertas locais (2026-11-07)

| Item | Situação | Commit | Teste que prova o comportamento |
|---|---|---|---|
| (a) Snackbar/Toast sem permissão e botão `ACTION_APP_NOTIFICATION_SETTINGS` | Feito | `dbd324c` | `NotificationHelperTest.notificationSettingsButtonTargetsThisApp` e `FlightAlertScheduleLogicTest.notificationTestExplainsWhenPermissionIsMissing` |
| (b) `contentIntent` de `showTextAlert` abre `MainActivity` | Feito | `aeaa5d4` | `NotificationHelperTest.showTextAlertNotificationContentIntentOpensMainActivity` |
| (c) Voo removido no sync cancela alarme e os atuais são reagendados | Feito | `b1005fb` | `FlightAlertScheduleLogicTest.syncRemovesMissingFlightFromScheduledAlarmState` |
| (d) Switch Som e vibração usa canais separados | Feito | `dbd324c` | `NotificationHelperTest.createsSeparateSoundAndSilentChannels` |
| (e) `FlightAlertWorker` e testes removidos; AlarmManager é a única via | Feito | `2f6cf5f` | Suíte completa sem referências ao worker; agendamento ativo passa por `FlightAlertScheduler` |
| (f) Sem permissão, alerta não é consumido em `alert_sent` | Feito | `02b28b8` | `NotificationHelperTest.notificationPermissionDeniedCannotConsumeAlertSent` |

Suíte completa: `testDebugUnitTest assembleDebug` — `BUILD SUCCESSFUL`; 101 testes, 0 falhas, 0 erros e 1 ignorado. `assembleDebug` também passou.

## 2026-11-06 - Verificador de atualizações

Foi confirmado que o repositório privado retornava HTTP 404 para o app, que não possui token por segurança. Após autorização, o repositório foi tornado público; a API pública passou a retornar `v1.0.22` e o asset `app-release.apk`.

Comandos executados:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.skywatch.cg.data.update.GitHubReleaseUpdateCheckerTest --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon
```

Resultados: teste específico aprovado; suíte Debug completa e `assembleDebug` concluídos com `BUILD SUCCESSFUL`. Um teste de CSV real foi ignorado por depender de arquivo externo.

## 2026-11-06 - Alertas em segundo plano

Alteracoes verificadas: o agendamento agora e acionado na abertura, apos sincronizacao SIROS e apos `BOOT_COMPLETED`; o calculo do instante usa UTC; o receiver cria o canal antes de publicar a notificacao.

Comando executado a partir de `android-app`:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain --no-daemon
```

Resultado: `BUILD SUCCESSFUL`. A suite unitária Debug concluiu sem falhas; o teste `SirosStreamParserRealFileTest` foi ignorado por depender do CSV real. O APK Debug foi gerado por `assembleDebug`.

Limitacao: ainda nao ha evidencia de entrega de um alerta com voo SIROS real no aparelho; essa validacao requer um movimento programado dentro da janela de 15-30 minutos, com permissao de notificacoes concedida e sem restricao de bateria.

## Release assinada e atualizacao (06/11/2026)

Implementacao registrada sem executar Gradle localmente. O CI deve validar `assembleRelease` com os secrets ja configurados no repositorio. A API de releases e publica; nao ha token embutido no APK.

Testes unitarios adicionados em `GitHubReleaseUpdateCheckerTest` cobrem release mais nova com asset estavel, versao ja atualizada e resposta HTTP malsucedida. `AppVersionComparatorTest` cobre comparacao numerica como `1.0.10 > 1.0.9`.

Pendente de validacao manual: baixar `app-release.apk` pelo dialogo, confirmar a tela de instalacao do Android e verificar a permissao do navegador para instalar aplicativos desconhecidos, se solicitada.

## Agenda local e recomendação de observação — 2026-11-04

A tela inicial combina pousos e decolagens em ordem cronológica e apresenta o horário local de Campo Grande. Os filtros Todos/Pousos/Decolagens alteram a lista, não a base da recomendação: a faixa de 3 horas sempre considera os dois tipos de movimento. A lista representa voos de passageiros ativos encontrados no SIROS para CGR, não todos os movimentos reais; a faixa é calculada a partir do planejamento e não garante operação.

**Regressão final local** (executada a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease :app:lintVitalRelease :app:processReleaseManifest :app:compileDebugAndroidTestKotlin --console=plain
```
**Resultado:** `BUILD SUCCESSFUL`; 75 testes por variante (Debug e Release), 0 falhas, 0 erros e 1 ignorado por depender de CSV real. Builds Debug/Release, lint vital, processamento do manifest Release e compilação dos testes instrumentados aprovados. APK Debug `1.0.18` gerado.

**Telefone físico:** APK Debug `1.0.18` instalado e aberto em aparelho Android 14, sem emulador. A interface apresentou, para 05/11/2026, a janela 09:05–12:05 com 10 movimentos programados (5 pousos e 5 decolagens). Esse resultado reflete os dados SIROS em cache naquela consulta, não uma confirmação de operação.

**Publicação:** CI #58 e workflow de Release #14 concluídos com sucesso para `951ce68`. O APK `SKY-WATCH-CG-v1.0.18-debug.apk` foi enviado para a [prévia v1.0.18 no GitHub Releases](https://github.com/mundoarkade1-arch/sky-watch-cg/releases/tag/v1.0.18).

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

**Validação local complementar (antes do radar):** `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin` e `:app:processReleaseManifest` concluíram com sucesso. Suíte unitária: 53 testes contabilizados, 0 falhas, 0 erros e 1 ignorado por depender de CSV real.

**CI remoto:** CI #46 passou no commit `5c7bdf5`, CI #47 para `c8d6761`, #48 para `9e82c84`, #49 para `4b072c4`, #50 para `9206269`, #51 para `ad4db53`, #52 para `fe84e0b` e #53 para `6f0ba0e` também passaram.

**Teste real de notificação após reinicialização:** pendente porque não havia voo SIROS elegível na janela de 15–30 minutos. O reinício está registrado abaixo e o teste de código do `BootReceiver` está na seção final; nenhum dos dois comprova a entrega de uma notificação real.

## Validação local dos alertas — 2026-11-04

O fluxo ativo usa alarmes individuais do `FlightAlertScheduler` entre 15 e 30 minutos antes do evento planejado, com deduplicação no receiver e uso de `arrivalDate` como data UTC das chegadas. A permissão de notificações é verificada antes de processar o alerta. O registro de deduplicação é salvo após a chamada de publicação, para não marcar como enviado antes de tentar notificar.

**Comandos executados** (a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
```
**Resultado naquela etapa:** `BUILD SUCCESSFUL`; 56 testes contabilizados, 0 falhas, 0 erros e 1 ignorado por depender de CSV real. APK Debug e compilação dos testes instrumentados concluídos.

O WorkManager pode atrasar execuções, então a janela pode passar sem notificação. Esse comportamento usa apenas o horário programado SIROS e não confirma o estado operacional do voo. A validação de entrega ponta a ponta continua pendente.

## Validação de inicialização e ViewModel — 2026-11-04

Removida a segunda execução de inicialização em `MainActivity.onCreate()`, que repetia a criação do canal, o pedido de permissão de notificações, o agendamento de trabalhos do WorkManager e a verificação de versão.

**Comando executado** (a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.skywatch.cg.ui.viewmodel.FlightListViewModelTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
```
**Resultado:** `BUILD SUCCESSFUL`; 11 testes do `FlightListViewModelTest`, 0 falhas, 0 erros e 0 ignorados; APK Debug e compilação dos testes instrumentados aprovados. CI #47 a #51 também passaram.

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

Schema Room exportado em `android-app/app/schemas/com.skywatch.cg.data.db.AppDatabase/6.json`. CI #50 passou para o commit `9206269`.

## Radar ADS-B experimental — 2026-11-04

O protótipo usa a API ADSB.lol documentada, chamada apenas ao abrir a tela ou por atualização manual. A consulta está centrada nas coordenadas configuradas para CGR e pede observações em um raio de 50 milhas náuticas. O `RadarSnapshotParser` exige timestamp, valida latitude/longitude e ignora registros sem posição; chamadas HTTP malsucedidas geram erro visível, preservando o último resultado em memória. A interface mostra escopo radial e lista, tempo da consulta, idade da posição, atribuição ADSB.lol/ODbL e distinção explícita entre posição observada, planejamento SIROS e status operacional. Não há associação automática a voos.

**Verificação pontual da API (web, sem inserir dados no app):** em 04/11/2026 às 21:22:07 UTC, o endpoint de 50 NM retornou `HTTP 200`, `total=1`, com uma posição reportada a 45.472 NM do centro consultado. Os identificadores foram omitidos. É apenas uma amostra daquele instante, não comprova cobertura contínua, presença de tráfego próximo ao aeroporto nem identidade de voo.

**Testes automatizados:**
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.skywatch.cg.data.radar.RadarSnapshotParserTest --tests com.skywatch.cg.data.radar.RadarRemoteSourceTest :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:processReleaseManifest
```
**Resultados:** testes focados do parser/cliente HTTP e build Debug aprovados. Regressão completa `BUILD SUCCESSFUL`: 63 testes contabilizados, 0 falhas, 0 erros e 1 ignorado por depender de CSV real; `assembleDebug` e `processReleaseManifest` aprovados. A consulta de app não é feita pelos testes (HTTP é simulado com MockWebServer). Nenhum emulador foi aberto.

**Condições de uso:** ADSB.lol declara ODbL 1.0 e solicita contato antes de uso em produção. O protótipo exibe atribuição, mas a integração não deve ser distribuída como funcionalidade de produção até confirmar com o mantenedor as exigências aplicáveis. Consulte `docs/API_SOURCES.md`.

**Teste no telefone físico:** instalado por `adb install -r` apenas o pacote `com.skywatch.cg.debug` no aparelho 22101320G (Android 14/API 34); o pacote Release não foi alterado e nenhum emulador foi aberto. A tela Radar abriu, mostrou o escopo, timestamp de consulta da fonte e estado “nenhuma observação ADS-B com posição” às 17:35:14, horário local. Não foi observado crash. A resposta vazia é válida para aquele instante; não prova ausência de aeronaves nem falha/continuidade de cobertura.

**Revalidação do botão Atualizar (04/11/2026):** aberta a tela Radar no mesmo aparelho e acionado o botão “Atualizar observações ADS-B” pela área identificada na hierarquia da interface. A tela permaneceu aberta; o horário da consulta passou de 17:41:43 para 17:42:08 e a API retornou novamente o estado vazio. A captura de logcat após a ação não mostrou exceção fatal nem ANR. Uma tentativa anterior havia terminado no launcher, mas o comportamento não foi reproduzido após reabrir o app e repetir a ação; a causa daquela transição não foi determinada. A consulta vazia não comprova ausência de aeronaves nem cobertura contínua.

**CI:** GitHub Actions CI #54 concluiu com sucesso para o commit `e65bb63` (protótipo experimental do radar).

## Roteiro manual — alertas após reinicialização (execução parcial no Android)

**Objetivo:** verificar permissões e reagendamento pelo `BootReceiver` em um aparelho real. Este roteiro não substitui a informação operacional da companhia aérea nem confirma a operação do voo.

**Comportamento atual a considerar:** o `FlightAlertScheduler` agenda alarmes individuais para eventos planejados futuros. O WorkManager atualiza o cache e os alarmes após o sync; o sistema pode atrasar a entrega. A fonte SIROS contém planejamento, não confirmação operacional. Use apenas um registro real presente no aplicativo; se não houver voo real na janela de teste, registre “não foi possível testar” em vez de criar dados fictícios.

1. Instale o APK Debug produzido pelo build validado e abra o aplicativo pelo menos uma vez.
2. Quando solicitado, permita notificações. Confirme também nas configurações do Android que as notificações do SKY WATCH CG e o canal “Alertas de Voo” estão habilitados.
3. Conecte o aparelho à internet e atualize a lista SIROS. Confira que existe um voo real no cache com evento programado entre 15 e 30 minutos adiante. Registre o identificador/data do registro SIROS e o horário programado local, sem afirmar que o voo está confirmado.
4. Anote a hora local e UTC; reinicie o aparelho usando o menu normal de energia. Após desbloquear, mantenha a conexão de rede ativa.
5. Aguarde o WorkManager executar. A periodicidade de 15 minutos é um intervalo mínimo aproximado, não um prazo exato. Verifique se a notificação aparece e se mostra chegada/partida e horário local compatíveis com o registro planejado.
6. Observe se a mesma notificação reaparece ou é atualizada em execuções subsequentes. Registre repetições; após o primeiro envio registrado, o worker deve ignorar o mesmo evento.
7. Repita, se possível, com aparelho offline durante parte do período e depois reconectado; anote horários e se o trabalho retomou. Não interprete a ausência de notificação como prova de cancelamento do voo.

### Execução observada — 2026-11-04

O telefone modelo 22101320G (Android 14/API 34) foi reiniciado manualmente depois da instalação e primeira abertura do APK Debug `com.skywatch.cg.debug`. O ADB voltou a conectar; `POST_NOTIFICATIONS` estava concedida, `RECEIVE_BOOT_COMPLETED` estava concedida e dois jobs do `SystemJobService` associados ao pacote foram encontrados no `dumpsys jobscheduler`.

**Evidência direta do receiver:** sem enviar broadcast manual, o logcat após o boot registrou `BOOT_COMPLETED recebido - re-agendando workers periódicos` às 17:06:21, `sincronização periódica re-agendada` às 17:06:21.257 e `verificação de alertas re-agendada` às 17:06:21.258. Na conferência, o aparelho indicava `boot_count=69`, uptime de 864 s e fuso `America/Manaus` (UTC−4), compatíveis com o boot e os horários dos logs.

**Limite restante:** o usuário informou que não havia voo SIROS real programado na janela de 15–30 minutos; assim, não foi possível verificar entrega nem deduplicação de notificação. Nenhum dado de voo foi inventado.

| Verificação | Resultado |
|---|---|
| Telefone reiniciou e ADB reconectou | Sim; modelo 22101320G, Android 14 |
| App Debug instalado antes do boot | Sim |
| Permissão de notificações | Concedida |
| Jobs WorkManager visíveis após o boot | 2 jobs associados ao pacote |
| Log direto de execução do `BootReceiver` | Sim; logcat após boot natural às 17:06:21 |
| Voo SIROS real na janela de alerta | Não havia |
| Notificação real recebida | Não avaliada; sem voo elegível |

Os detalhes disponíveis do reinício estão registrados na tabela acima. O voo SIROS elegível, o horário de notificação e a entrega/deduplicação não se aplicam a esta execução e só devem ser registrados em novo teste com voo real. Não publicar matrícula, posição ou status operacional sem fonte verificável.

## Teste automatizado do BootReceiver — 2026-11-04

Adicionado `BootReceiverTest`, que inicializa o WorkManager de teste, envia `ACTION_BOOT_COMPLETED` ao receiver e verifica que os trabalhos únicos `siros_periodic_sync` e `flight_alert_check` são enfileirados como `ENQUEUED`, com os respectivos workers.

**Comando executado** (a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.skywatch.cg.BootReceiverTest --console=plain
```
**Resultado focado:** `BUILD SUCCESSFUL`; 1 teste, 0 falhas, 0 erros e 0 ignorados.

**Regressão completa:** `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` concluiu com `BUILD SUCCESSFUL`; 57 testes contabilizados, 0 falhas, 0 erros e 1 ignorado por depender de CSV real. APK Debug atualizado. Não foi aberto emulador.

O teste verifica a lógica do receiver com uma ação enviada pelo teste. Não comprova que o Android entregou `BOOT_COMPLETED` no reinício físico nem substitui o teste de notificação com voo SIROS real.

## Detalhes de voo e prévia Debug 1.0.17 — 2026-11-04

**Regressão reproduzível** (a partir de `android-app`):
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease :app:lintVitalRelease :app:processReleaseManifest --console=plain
```
**Resultado:** `BUILD SUCCESSFUL` em 5 min 4 s. Os relatórios XML registram 68 testes em cada variante (Debug e Release), 0 falhas, 0 erros e 1 ignorado por depender de CSV real. `assembleDebug`, `assembleRelease`, `lintVitalRelease` e `processReleaseManifest` passaram. O teste ignorado é `SirosStreamParserRealFileTest`, que requer o CSV local real.

**APK Debug:** `android-app\app\build\outputs\apk\debug\app-debug.apk`, 11.091.182 bytes, SHA-256 `369ED4F04295679273D3576D84696EE4E3276FF75BF3D6BC12DAD0743E93C649`. Assinatura verificada como Android Debug. No Poco 22101320G (Android 14/API 34), a instalação foi concluída; o pacote reportou `com.skywatch.cg.debug`, versão 1.0.17/código 17. O app abriu, um cartão abriu os detalhes e Voltar retornou à lista; a situação SIROS apareceu como cadastral e o logcat não registrou exceção fatal. Nenhum emulador foi aberto.

Durante essa verificação, a tela de detalhes foi ajustada para identificar `sirosStatus` como situação cadastral e esclarecer que ela não confirma a operação do voo. O teste manual do fluxo foi repetido depois do ajuste.

**Limite dos testes instrumentados:** `connectedDebugAndroidTest` não conseguiu instalar o APK instrumentado no aparelho devido a `INSTALL_FAILED_VERIFICATION_FAILURE`. A verificação de segurança do Android não foi desativada nem contornada. O APK Debug normal foi instalado com sucesso, mas isso não substitui os testes instrumentados.

O workflow acionado por tags `v*` publica somente uma prévia Debug, não um APK assinado para distribuição estável/Play Store. Como o runner do CI pode usar uma chave Debug diferente em cada execução, instalar uma prévia atualizada talvez exija desinstalar a anterior, o que apaga os dados locais. O teste de entrega/deduplicação de alerta real continua pendente até haver voo SIROS elegível.

**Publicação remota:** CI #56 passou para o commit `3858738`. O workflow "Build and Release Android APK" #13 para a tag `v1.0.17` concluiu com sucesso, incluindo a etapa `Upload APK to Release`. O APK está no [Release v1.0.17](https://github.com/mundoarkade1-arch/sky-watch-cg/releases/tag/v1.0.17) com o nome `SKY-WATCH-CG-v1.0.17-debug.apk`.
## Revisão da sincronização excessiva — 2026-11-06

Alterado o `FlightListViewModel`: o ciclo de 60 segundos agora atualiza somente o relógio local; a sincronização SIROS na abertura ocorre apenas quando o cache está vazio ou a última sincronização bem-sucedida tem pelo menos seis horas. A troca automática para a data atual usa `America/Campo_Grande`, e os fluxos de chegadas/decolagens são combinados dentro da troca de data para evitar estado transitório.

Comando executado em `android-app`:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.skywatch.cg.data.update.GitHubReleaseUpdateCheckerTest --tests com.skywatch.cg.ui.viewmodel.FlightListViewModelTest.syncPolicyRefreshesOnlyWhenCacheIsEmptyOrOlderThanSixHours --offline
```

Resultado real: `BUILD SUCCESSFUL`; 5 testes executados, 0 falhas. O teste do checker de releases permaneceu verde. O build usou o SDK Android local e não acessou a rede durante a execução (`--offline`).
## Seção 3 — identidade SIROS e migração Room 6→7 (2026-11-06)

`NormalizedFlight` e `FlightEntity` passaram a preservar `sirosStage`. O identificador normalizado agora usa `sirosRecordId-E<etapa>-<ARRIVAL|DEPARTURE>-<yyyy-MM-dd>`, evitando que uma chegada e uma decolagem do mesmo registro SIROS se sobrescrevam.

Foi adicionada `MIGRATION_6_7`, que recria somente a tabela de cache `flights` com a coluna `sirosStage`, preservando `sync_metadata` e `alert_sent`. O schema Room 7 foi exportado em `android-app/app/schemas/com.skywatch.cg.data.db.AppDatabase/7.json`.

Validações realizadas:

- `testDebugUnitTest assembleDebug`: `BUILD SUCCESSFUL`; 91 testes, 0 falhas, 0 erros e 1 ignorado (`SirosStreamParserRealFileTest`).
- `SirosStageIdentityTest`: fixture derivada de linhas reais do CSV SIROS; chegada e decolagem com mesma identidade de registro e etapas distintas permanecem como dois voos.
- `compileDebugAndroidTestKotlin`: aprovado, incluindo `AppDatabaseMigrationTest` para 6→7.
- O teste instrumentado não foi executado porque `adb` não está disponível nesta sessão.

### Validação complementar antes da seção 4

- `AppDatabaseMigrationJvmTest`: executou em Robolectric a criação de um banco v6, a abertura com `MIGRATION_6_7` e a verificação de `sirosStage`, `sync_metadata` e `alert_sent` preservados.
- `FlightIdKeyTest`: validou `Uri.encode`/`Uri.decode`, IDs de alerta distintos e `PendingIntent` distintos usando o hash completo do `flightId`.
- Resultado do comando focado: `BUILD SUCCESSFUL`; 3 testes, 0 falhas.
- O teste que prova que pouso e decolagem persistem juntos é `SirosStageIdentityTest.sameSirosRecordWithDifferentStagesKeepsArrivalAndDeparture`: ele normaliza dois registros com a mesma identidade SIROS e etapas diferentes, persiste ambos e verifica uma chegada e uma decolagem no banco.
## Seção 7, etapa 2, bloco 1 — vigilância radar (2026-11-07, revisão)

Commit: `3303aa1` (`feat: adicionar vigilancia radar no ciclo de vida`).

| Item | Evidência | Teste | Resultado |
|---|---|---|---|
| Vigilância em qualquer tela, somente em `STARTED`, intervalo de ~10 s e switch persistido ligado por padrão | `MainActivity` inicia a vigilância; `repeatOnLifecycle(STARTED)` controla o ciclo; `AppPreferences.radarWatchEnabled` persiste a opção | `RadarSurveillanceTest.pollsEveryTenSecondsWithFakeClock`, `RadarSurveillanceTest.doesNotPollWhileOutsideStartedOrWhenDisabled`, `AppPreferencesTest.radarWatchIsEnabledByDefaultAndPersistsChanges` | Passou |
| Pausa real fora de `STARTED` | `RadarSurveillance.runForLifecycle` usa `repeatOnLifecycle`; o teste conduz `LifecycleRegistry` de `CREATED → STARTED → CREATED` | `RadarSurveillanceTest.pausesPollingWhenLifecycleMovesFromStartedBackToCreated` | Passou |
| Limite de 1 requisição/s por endpoint | `RadarRemoteSource.MIN_REQUEST_INTERVAL_MILLIS` | `RadarRemoteSourceTest.enforcesOneSecondMinimumBetweenRequestsToSameEndpoint` | Passou |
| Failover `adsb.lol → airplanes.live → adsb.fi` | Ordem em `DEFAULT_ENDPOINTS` | `RadarRemoteSourceTest.failsOverAfterRateLimit` | Passou |
| `429`, `Retry-After` e backoff limitado | Estado de cooldown por endpoint e teto de 5 minutos | `RadarRemoteSourceTest.respectsRetryAfterCooldownUsingFakeClock` | Passou |
| Histórico por hex integrado ao alerta do ViewModel | `RadarViewModel.applySnapshot` observa `RadarAircraftHistory` antes de avaliar a regra | `RadarTrajectoryRulesTest` cobre histórico/tendência; integração de ciclo de vida ainda não possui harness Android completo | Código integrado; harness de ViewModel pendente |

Suíte completa será revalidada após este ajuste de ciclo de vida. O teste focado passou: 3 testes, 0 falhas.

