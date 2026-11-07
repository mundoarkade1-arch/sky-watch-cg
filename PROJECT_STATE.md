# Estado do projeto

## Seção 7 — radar, etapa 1: lógica (07/11/2026)
- Constantes documentadas de SBCG: elevação 1.834 ft/559 m e cabeceiras 06/24 nos rumos verdadeiros 042,26°/222,26°, conforme AISWEB/DECEA.
- Histórico curto por hex deriva tendência de distância e altitude; taxa explícita usa `baro_rate`, depois `geom_rate`, e somente na ausência de ambos usa amostras.
- Regras usam altitude AGL, ignoram sobrevoo alto, aeronave parada, posição inválida e posição velha.
- Categoria ADS-B `A7` é tratada conservadoramente como helicóptero e não gera alerta; categoria ausente não é inferida.
- Etapa restrita a `data/radar`; nenhuma UI foi alterada.
- Suíte completa: 123 testes, 0 falhas, 0 erros e 1 ignorado; `assembleDebug` aprovado.

## Seção 5 — exibição e fusos concluída (07/11/2026)
- Formatação de aeroportos e companhias foi normalizada com testes para Dallas/Fort Worth, Rio de Janeiro e casos desconhecidos.
- `nextMovements` tem testes explícitos para voo de hoje já passado, data anterior e dia futuro; a `DetailScreen` usa rolagem vertical e nomes formatados.
- O mapa cobre Cuiabá (`SBCY`), Manaus (`SBEG`) e Rio Branco (`SBRB`); ICAO desconhecido usa o fallback declarado no registro.
- A rolagem foi validada por teste JVM/Compose, mas ainda não foi verificada em aparelho físico.
- Suíte completa mais recente: 113 testes, 0 falhas, 0 erros e 1 ignorado; `assembleDebug` aprovado.

## Seção 6 — parser SIROS (07/11/2026)
- `SirosStreamParser` é agora o único parser, usado pelo `SirosRepository` e pelos testes; `SirosCsvParser` foi removido.
- Cabeçalho ausente produz `Formato do CSV mudou: cabeçalho SIROS não reconhecido`, antes de qualquer fallback para “Nenhum voo SBCG”.
- Logs por registro foram removidos; permanecem logs agregados e de erro.
- A fixture versionada comprova `Objeto Transporte = PASSAGEIROS` e `Tipo Serviço = REGULAR DE PASSAGEIROS`; o CSV oficial baixado mostrou os mesmos valores nas 929 linhas envolvendo SBCG.
- Suíte completa: 112 testes, 0 falhas, 0 erros e 1 ignorado; `assembleDebug` aprovado.

## Seção 4 — alertas locais concluída (07/11/2026)
- A tela Sobre informa a ausência de permissão por Toast/feedback e oferece `ACTION_APP_NOTIFICATION_SETTINGS`.
- `showTextAlert` publica com `contentIntent` para `MainActivity`; som e silêncio usam canais distintos.
- O sync cancela alarmes de voos removidos e mantém apenas os alarmes dos movimentos atuais.
- `FlightAlertWorker` e seus testes foram removidos; `AlarmManager`/receiver é a única via de alerta.
- O receiver só grava `alert_sent` após permissão e publicação confirmada; `deleteOldAlerts` roda no sync ativo.
- Suíte completa: 101 testes, 0 falhas, 0 erros e 1 ignorado; `assembleDebug` aprovado.

## Identidade SIROS e migração Room 6→7 (06/11/2026)
- `sirosStage` agora é propagado de `SirosRecord.numeroEtapa` para `NormalizedFlight` e `FlightEntity`.
- `flightId` usa registro SIROS, etapa, tipo de operação e data: `<siros>-E<etapa>-<ARRIVAL|DEPARTURE>-<yyyy-MM-dd>`.
- `MIGRATION_6_7` recria somente o cache `flights`, preservando `sync_metadata` e `alert_sent`; o schema foi exportado em `android-app/app/schemas/com.skywatch.cg.data.db.AppDatabase/7.json`.
- Fixture real reduzida e teste de identidade adicionados. Suíte final: 91 testes, 0 falhas, 0 erros e 1 ignorado; `assembleDebug` passou.
- Teste instrumentado de migração compilado, mas não executado por ausência de `adb` nesta sessão.
- Validação JVM complementar aprovada em Robolectric: `AppDatabaseMigrationJvmTest` executou a migração real e preservou cache, metadados e alertas.
- `FlightIdKeyTest` aprovou URI e chaves de `PendingIntent`; `SirosStageIdentityTest` é o teste que comprova a persistência conjunta de pouso e decolagem.

## Revisão da sincronização excessiva (06/11/2026)
- O ciclo de 60 segundos do `FlightListViewModel` não chama mais `syncFromRemote()`; ele atualiza apenas o relógio local e acompanha a virada do dia em `America/Campo_Grande`.
- A abertura do app sincroniza somente com cache vazio ou quando a última sincronização bem-sucedida tem seis horas ou mais.
- A combinação dos fluxos de chegadas e decolagens passou a ser feita por data dentro de `flatMapLatest`, evitando emitir a lista antiga com a nova data.
- Adicionado `FlightDao.hasAnyFlights()` e teste determinístico da política de sincronização.
- Validação local: checker de releases e política da seção 2 passaram; 5 testes, 0 falhas.

## Correção do verificador de atualizações (06/11/2026)
- O repositório GitHub foi tornado público com autorização explícita para que o app consulte `releases/latest` sem token embutido.
- O checker agora trata HTTP 404 com mensagem específica, mantém a comparação numérica de versões e o parser de asset `app-release.apk` coberto por testes.
- Corrigido o texto da tela Sobre para `Verificar atualização`.
- Removido um `!!` desnecessário na tela de detalhes; o campo opcional de aeronave agora usa smart cast seguro.
- Validação local: teste específico do checker, suíte unitária Debug e `assembleDebug` passaram; o teste de CSV real permaneceu ignorado.

## Correcao de alertas em segundo plano (06/11/2026)
- O agendamento de alertas SIROS passou a ser chamado na abertura do app, apos sincronizacao periodica e apos `BOOT_COMPLETED`; antes, o `FlightAlertScheduler` existia, mas nao era acionado pelo fluxo de producao.
- Alarmes sao criados para movimentos planejados futuros no cache local, com janela de 48 horas e antecedencia padrao de 15 minutos. Se o app for aberto dentro dessa antecedencia, o alarme e programado para execucao imediata.
- O instante do evento agora interpreta corretamente data/horario armazenados em UTC; a notificacao exibe o horario local calculado por aeroporto e identifica o movimento como programado.
- A entrega continua dependente de `POST_NOTIFICATIONS`, canal habilitado e politicas de bateria do fabricante. A entrega em aparelho com voo real elegivel ainda nao foi demonstrada.

## Release assinada e atualizacao do app (06/11/2026)
- O modulo `:app` continua em Kotlin + Jetpack Compose, com `applicationId` `com.skywatch.cg`.
- `build.gradle.kts` le assinatura Release de `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`; sem essas variaveis o build local permanece configuravel e nao exige segredo.
- `VERSION_CODE` e `VERSION_NAME` podem vir do ambiente e mantem fallback para `gradle.properties`.
- `.github/workflows/release.yml` publica o APK assinado como `app-release.apk` a partir da `main` ou por disparo manual. O numero e monotonicamente ajustado quando `github.run_number` colide com tags antigas.
- `GitHubReleaseUpdateChecker` consulta a API publica de releases, compara versoes numericamente e a tela Sobre oferece verificacao manual. O botao Atualizar abre o asset no navegador; instalacao e autorizacao do Android continuam sob controle do usuario.

## Alterações da rodada wip-fase3 (05/11/2026)
- O radar padrão usa adsb.lol, airplanes.live e adsb.fi em failover; HTTP 429 recebe cooldown e a UI não fixa erro vermelho.
- O ciclo de atualização de voos é vinculado a `Lifecycle.State.STARTED`; a sincronização SIROS continua separada.
- Alertas ADS-B e próximos movimentos têm funções puras e testes adicionados.
- Alertas de voos seguidos usam AlarmManager/receiver no caminho novo; a restauração completa de cada alarme no boot e a UI de configurações ainda precisam de CI e revisão.

## Identidade
- Nome: SKY WATCH CG
- Plataforma: Android
- Idioma: pt-BR
- Aeroporto: Campo Grande, MS
- IATA: CGR
- ICAO: SBCG

## Decisões confirmadas
- Foco enquadrado em aeronaves comerciais.
- Alertas padrão com 15 minutos de antecedência.
- Tema escuro.
- Prioridade para fontes gratuitas e públicas institucionais (SIROS/ANAC CSV e Aena).
- **Arquitetura conceitual aprovada:** Modelo normalizado separado por camadas (Planejamento SIROS com ID SIROS original e data específica do voo, Status em tempo real Aena, e Radar experimental AvioADSB). Fusos horários tratados por aeroporto (ex: America/Sao_Paulo, America/Campo_Grande).

## Estado técnico
- [x] PoC realizada no endpoint CSV de registros vigentes do SIROS (/registros/registros/registros.csv): HTTP 200, acesso público gratuito e sem autenticação.
- [x] Validação de fuso horário por aeroporto e conversão UTC para horário local correta.
- [x] Arquitetura conceitual e modelo de dados normalizado aprovados.
- [x] Criar projeto Android (Kotlin + Jetpack Compose) na pasta android-app com tema escuro e tela inicial.
- [x] Parser CSV, normalizador e modelos de dados do SIROS implementados (sem download remoto do CSV).
- [x] Testes unitários do módulo SIROS criados e passando.
- [x] **Implementar persistência local (Room) e distinção entre data de sincronização geral e última atualização de status.**
- [x] **Implementar telas do MVP (Chegadas e Partidas) e sincronização de testes do ViewModel (Etapa 5 concluída com sucesso — BUILD SUCCESSFUL).**
- [x] **Fase 6.1 (histórico, schema v2):** entidade `SyncMetadata`, `SyncMetadataDao` e migração Room v1→v2. A validação daquela fase usou uma "v1 provável"; o schema real v1 não foi exportado e não é recuperável do Git, portanto não prova migração de um aparelho legado real. O schema atual é v6.
- [x] **Fase 6.3 (Corrigida): Ingestão remota, transação atômica, última atualização na interface, WorkManager e APK de debug.**
- [x] **Etapa A concluída:** Correção do teste `testRefreshStateEmitsRefreshing` em `FlightListViewModelTest` utilizando fonte remota falsa (`SirosRemoteSource` com localhost fictício) e controle de corrotinas (`yield()` / `advanceUntilIdle()`). 36 testes unitários verdes (0 falhas). Código enviado para o repositório remoto via `git push origin main` (após `git commit --amend`).
- [x] **Estabilização da Fase 3 (04/11/2026):** investigada a falha do CI #38 (2 testes de atualização falhando). A causa era a dependência de rede real em testes do ViewModel; os testes agora usam fonte remota controlada. O estado `Refreshing` é mantido durante emissões do Room, atualizações repetidas são evitadas, falhas preservam dados em cache e a tela Sobre usa retorno acessível e conteúdo rolável. Validação local em 18:59 UTC / 14:59 Campo Grande: 53 testes contabilizados (52 executados, 0 falhas e 1 ignorado); `assembleDebug` bem-sucedido.
- [x] **CI #39 (04/11/2026):** commit `bccfd48` na branch `wip-fase3` concluído com sucesso no GitHub Actions.
- [x] **Revalidação de testes (04/11/2026):** suíte completa: 53 testes, 0 falhas, 0 erros, 1 ignorado; `assembleDebug` bem-sucedido. `FlightListViewModelTest` também passou isoladamente (11 testes). Corrigido `android-app/gradlew.bat`, que sempre retornava código 1 mesmo quando o Gradle concluía com sucesso; validado retorno 0 em build bem-sucedido e retorno 1 em tarefa inexistente.
- [x] **CI #41 e #42 (04/11/2026):** ambos concluídos com sucesso; #42 validou build Debug e suíte unitária para o commit `fa2712d`.
- [x] **CI #43 e #44 (04/11/2026):** concluídos com sucesso para os commits `8aff2ce` e `dcdaddb`.
- [x] **Documentação alinhada (04/11/2026):** `README.md`, `ROADMAP.md`, `PROJECT_STATE.md`, `TESTING.md` e `CHANGELOG.md` sincronizados com o estado comprovado do projeto.
- [x] **Alertas locais:** janela entre 15 e 30 minutos antes do horário programado local, deduplicação por voo/operação/data local e uso de `arrivalDate` para calcular chegadas que cruzam a meia-noite UTC. Validação local: 56 testes contabilizados, 0 falhas e 1 ignorado; build Debug e compilação dos testes instrumentados concluídos.
- [x] **Inicialização da tela principal:** removidas chamadas duplicadas de canal/permissão de notificações, agendamento WorkManager e consulta de atualização dentro de `MainActivity.onCreate()`. `FlightListViewModelTest`: 11 testes aprovados; `assembleDebug` e 11 testes instrumentados no aparelho físico aprovados.
- [x] **Pesquisa Aena Brasil (04/11/2026):** página e sitemap oficiais de CGR consultados; não foi localizada documentação pública de painel/API para CGR. Os termos consultados restringem reprodução de conteúdo sem autorização escrita e identificam ANB, enquanto CGR é administrado pela BOAB. Sem presumir que os termos resolvam o escopo de CGR, nenhuma coleta automatizada será implementada antes de confirmação escrita e documentação apropriada. Detalhes e fontes em `docs/API_SOURCES.md`.
- [x] **Minuta de pedido Aena/BOAB preparada (04/11/2026):** `docs/AENA_API_REQUEST.md` pede identificação da entidade responsável, documentação de API, condições de uso e autorização escrita. A minuta não foi enviada; confirmação do canal apropriado e envio pelo responsável pelo projeto continuam pendentes.
- [x] **Fixture instrumentado corrigido (04/11/2026):** preenchido `arrivalDate` em `FlightDaoTest`; `compileDebugAndroidTestKotlin` aprovado.
- [x] **Isolamento do pacote de teste Debug (04/11/2026):** adicionado sufixo `.debug` para evitar conflito com a instalação Release e preservar dados do app existente; o identificador Release permanece inalterado.
- [x] **Correção e validação de migrações Room (04/11/2026):** o índice `idx_alert_sent_flight_date` que a migração v3→v4 já criava foi declarado na entidade `AlertSent`; banco atualizado para versão 5 com migração v4→v5 idempotente. Na validação daquela versão em Android 14, 11 testes instrumentados passaram, incluindo migração v1→v5 e v4→v5 com preservação de registro.
- [x] **Schema Room v6:** a migração v5→v6 remove o `CHECK` que existia apenas em bancos migrados e preserva os registros. Gatilhos de inserção/atualização agora impõem `id = 1` tanto em bancos novos como migrados. Verificado no aparelho com testes de inserção, atualização e preservação de dados.
- [x] **Testes instrumentados no aparelho (04/11/2026):** após a alteração da janela de alertas, 11 testes passaram em telefone físico Android 14 (modelo 22101320G), sem emulador.
- [x] **CI #45 a #48 (04/11/2026):** concluídos com sucesso. #47 validou `c8d6761`; #48 validou a documentação do teste no aparelho no commit `9e82c84`.
- [x] **CI #49 (04/11/2026):** concluído com sucesso para `4b072c4`.
- [x] **CI #50 (04/11/2026):** concluído com sucesso para `9206269`, incluindo a migração Room v5→v6.
- [x] **Reinicialização do telefone (04/11/2026):** app Debug instalado antes do boot. Logcat do boot natural registrou `BOOT_COMPLETED recebido` e o reagendamento de sync e alertas às 17:06:21–17:06:21.258; `boot_count=69` e uptime de 864 s quando conferidos. Permissão concedida e dois jobs WorkManager visíveis. Sem voo SIROS na janela, não houve teste de entrega de alerta.
- [x] **Teste automatizado do `BootReceiver` (04/11/2026):** teste Robolectric envia a ação `BOOT_COMPLETED` ao receiver e verifica que os trabalhos únicos de sincronização e alertas ficam enfileirados. Não simula nem comprova entrega pelo sistema operacional no boot real.
- [x] **CI #51 (04/11/2026):** concluído com sucesso para o commit `ad4db53`.
- [x] **CI #52 e #53 (04/11/2026):** concluídos com sucesso para os commits `fe84e0b` e `6f0ba0e`.
- [x] **CI #54 (04/11/2026):** concluído com sucesso para o protótipo de radar no commit `e65bb63`.
- [x] **Pesquisa inicial de fonte ADS-B (04/11/2026):** documentação oficial OpenSky descreve state vectors em tempo real e consultas por área; uso informado para pesquisa/não comercial. Termos completos não puderam ser consultados (HTTP 403) e a cobertura de CGR não foi verificada. Nenhuma integração foi feita.
- [x] **Radar ADS-B experimental (04/11/2026):** primeira versão usa a API ADSB.lol com raio de 50 NM, botão de consulta manual, apresentação de observações em escopo radial e lista com timestamp/idade, sem associação a voos SIROS. Atribuição ODbL exibida na tela. A fonte solicita contato antes de uso em produção; não foi feito esse contato.
- [x] **Testes do radar:** parser de payload, posições inválidas/ausentes, resposta vazia, timestamp obrigatório, integração HTTP bem-sucedida e erro HTTP cobertos por 6 testes unitários.
- [x] **Teste físico inicial do radar (04/11/2026):** instalada e aberta apenas a variante Debug no aparelho 22101320G, sem emulador; a tela carregou resposta sem posições e exibiu estado vazio e horário da fonte, sem crash. Isso não prova ausência de tráfego nem cobertura contínua.
- [x] **Atualização manual do radar revalidada (04/11/2026):** no aparelho 22101320G, o botão “Atualizar observações ADS-B” manteve a tela aberta e atualizou o horário da consulta de 17:41:43 para 17:42:08; a resposta continuou vazia. A captura posterior não mostrou exceção fatal nem ANR no logcat; a primeira tentativa, que retornou ao launcher, não foi reproduzida.
- [x] **Detalhes de voo e versão de prévia (04/11/2026):** cartões e próximo voo abrem a tela de detalhes com rota e horário local; removido o botão "Seguir voo" sem implementação. A situação SIROS está identificada como cadastral e não como confirmação em tempo real. Comparação de versões numérica e versão Debug `1.0.17`/código 17.
- [x] **Regressão local final (04/11/2026):** `testDebugUnitTest` e `testReleaseUnitTest`: 68 testes por variante, 0 falhas, 0 erros e 1 ignorado por depender de CSV local. `assembleDebug`, `assembleRelease`, `lintVitalRelease` e `processReleaseManifest` concluídos em `BUILD SUCCESSFUL`.
- [x] **Verificação física da prévia (04/11/2026):** APK Debug de 11.091.182 bytes instalado no aparelho Android 14; pacote `com.skywatch.cg.debug`, versão 1.0.17/código 17. Abertura, cartão→detalhes e botão Voltar verificados sem exceção fatal no logcat. Não foi aberto emulador.
- [x] **Pré-lançamento GitHub (04/11/2026):** CI #56 passou para `3858738`; o workflow de Release #13 concluiu testes, build e upload do APK para a tag `v1.0.17`. [Release no GitHub](https://github.com/mundoarkade1-arch/sky-watch-cg/releases/tag/v1.0.17). É uma prévia Debug, não uma versão assinada para distribuição estável; a assinatura do runner pode exigir desinstalar a prévia anterior para atualizar.
- [x] **Agenda e horários locais (04/11/2026):** tela principal unificada em ordem cronológica, com filtros Todos/Pousos/Decolagens e horário local de CGR consistente em cartões, detalhes e notificações. A recomendação de 3 horas calcula pousos e decolagens juntos mesmo quando a lista está filtrada; a lista continua mostrando todos os voos de passageiros ativos encontrados no SIROS para a data, sem afirmar cobertura de todos os movimentos reais.
- [x] **Validação da agenda (04/11/2026):** 75 testes Debug e 75 Release, 0 falhas, 0 erros e 1 ignorado por variante; `assembleDebug`, `assembleRelease`, `lintVitalRelease`, `processReleaseManifest` e `compileDebugAndroidTestKotlin` concluídos com sucesso. APK Debug `1.0.18` instalado e aberto no Android 14 sem emulador. Em consulta visual, a janela para 05/11/2026 foi 09:05–12:05, com 10 movimentos programados (5 pousos e 5 decolagens); é uma estimativa baseada no SIROS, não confirmação operacional.
- [x] **Pré-lançamento GitHub 1.0.18 (04/11/2026):** CI #58 e workflow de Release #14 concluídos com sucesso para o commit `951ce68`; APK `SKY-WATCH-CG-v1.0.18-debug.apk` publicado em [GitHub Releases](https://github.com/mundoarkade1-arch/sky-watch-cg/releases/tag/v1.0.18). Continua sendo uma prévia Debug, sem assinatura estável/Play Store.

  - **6.3.1:** Download HTTP via OkHttp com parâmetros customizáveis no `SirosRemoteSource`, verificações de arquivo vazio (0 bytes) e download incompleto (Content-Length divergente).
  - **6.3.2:** `syncFromRemote()` com transação Room atômica (`withTransaction`), fechamento de streams, filtro streaming de SBCG, tratamento de erros e gravação de status `FAILURE` preservando a última sincronização bem-sucedida.
  - **6.3.3:** Interface com botão de atualização com estado de carregamento (spinner em progresso) e exibição da última atualização ou erro ocorrido.
  - **6.3.4:** `WorkManager` com `SirosSyncWorker` configurado com limite de 5 tentativas (`runAttemptCount >= 5 -> Result.failure()`).
  - **6.3.5:** Suíte completa de 33 testes unitários aprovados (`BUILD SUCCESSFUL` em 21s) e APK de debug gerado em `android-app\app\build\outputs\apk\debug\app-debug.apk` (**12.620.046 bytes**).
- [x] **AndroidManifest.xml:** permissão `INTERNET` adicionada.
- [x] **SirosNormalizer:** corrigido mapeamento dos dias da semana (seg=1 ... dom=7, 0 = não opera). Testes expandidos para 35 testes unitários (0 falhas). `BUILD SUCCESSFUL`.
- [x] Pasta local ligada ao repositório GitHub privado (mundoarkade1-arch/sky-watch-cg) em 01/11/2026: primeiro envio feito, com .gitignore para build, local.properties, chaves e arquivos de log. A partir daqui o trabalho é versionado com git add, git commit e git push.

## Bloqueios e riscos
- SIROS CSV: Requer download periódico e tratamento correto da codificação UTF-8 com BOM (detectado automaticamente) e delimitador ;.
- Aena: Sem API pública documentada confirmada; investigar integração autorizada antes de iniciar implementação.
- AvioADSB: não foi possível confirmar uma documentação/API oficial ou os termos a partir dos materiais disponíveis; cobertura de CGR não deve ser presumida.
- ADSB.lol: API ODbL 1.0 usada em protótipo experimental. A documentação pede contato antes de uso em produção; a cobertura foi apenas amostrada uma vez e não garante disponibilidade. Atribuição e separação SIROS/ADS-B estão implementadas.
- OpenSky: candidato alternativo; termos completos não foram acessados e não está integrado.
- **Notificação ponta a ponta pendente:** o `BootReceiver` foi confirmado pelo logcat após reinício natural e o teste automatizado cobre seu agendamento; sem voo SIROS entre 15 e 30 minutos, entrega e deduplicação do alerta não foram observadas.
- **Validação manual de alertas pendente:** o worker busca eventos dentro da janela de 15–30 minutos e usa rede/periodicidade gerenciadas pelo WorkManager. Atrasos podem fazer a janela passar; o WorkManager não garante execução exatamente a cada 15 minutos. Verificar em aparelho real permissões, recebimento, deduplicação e reagendamento após reinicialização.
- **Integração SIROS:** a tela usa os dados locais do Room; a atualização manual e a sincronização periódica atualizam esse cache com o CSV remoto.
- A regra `sync_metadata.id = 1` está agora aplicada uniformemente por gatilhos no schema v6; migração v5→v6 mantém os dados. O schema legado v1 real continua indisponível, então os testes de v1 não comprovam a atualização de uma instalação legada real.
- **Testes instrumentados nesta rodada:** a instalação do APK de teste foi bloqueada por `INSTALL_FAILED_VERIFICATION_FAILURE`; a proteção do Android não foi desativada. A instalação e abertura do APK Debug normal passaram.
- **Distribuição estável pendente:** não há assinatura persistente de produção/Play Store configurada. O APK publicado é uma prévia Debug; a instalação de atualização entre execuções do CI pode exigir remover a anterior e perder os dados locais.

## Próximas etapas — dependem de validação externa
- Contatar ADSB.lol antes de distribuir/usar a integração em produção; confirmar condições, limites de frequência e atribuição adequados.
- Avaliar cobertura em diferentes dias/horários e documentar quando não houver observações; uma resposta isolada não representa cobertura contínua.
- Concluir o teste de entrega/deduplicação de alertas quando houver um voo SIROS real elegível na janela de 15–30 minutos.
- Solicitar confirmação escrita à entidade responsável por CGR sobre acesso autorizado a status operacional Aena.
- Manter observações ADS-B separadas do planejamento SIROS; não associar a voo comercial sem correspondência verificável.
- A integração Aena permanece uma frente separada e depende de resposta escrita; a minuta não foi enviada.
### Seção 7 — etapa 2, bloco 1 (2026-11-07)

- [x] Vigilância radar ligada ao ciclo de vida da `MainActivity`, com `repeatOnLifecycle(STARTED)` e intervalo padrão de 10 segundos.
- [x] Switch de vigília persistido em `AppPreferences`, padrão ligado.
- [x] Limite de 1 requisição por segundo por endpoint, failover na ordem `adsb.lol`, `airplanes.live`, `adsb.fi`, cooldown de `429` com `Retry-After` e teto de backoff preservados.
- [x] Histórico curto por hex conectado ao caminho de avaliação do `RadarViewModel`.
- [x] Testes JVM com relógio/espera falsos para intervalo, pausa fora de `STARTED`/switch desligado e MockWebServer para limite, failover e cooldown.
- [ ] Harness Android/Robolectric completo para observar diretamente a entrada e saída de `STARTED` no `repeatOnLifecycle`.

Validação local: `testDebugUnitTest assembleDebug` — 127 testes, 0 falhas, 0 erros e 1 ignorado; build Debug aprovado.

### Revisão da pausa por ciclo de vida (2026-11-07)

- [x] `RadarSurveillance.runForLifecycle` usa `repeatOnLifecycle(STARTED)` diretamente.
- [x] `RadarSurveillanceTest.pausesPollingWhenLifecycleMovesFromStartedBackToCreated` usa `LifecycleRegistry`, conta consultas e confirma pausa em `CREATED`.
- [x] Cooldown de 20 minutos é deduplicação de alertas (`RadarAlertDeduplicator`), distinto do cooldown HTTP/backoff das fontes.
- [x] `RadarAlertRules` mantém constantes nomeadas para idade, velocidade, AGL, distância, rumo e taxa vertical.
- [x] `MainActivity` inicia a vigilância antes da composição, permitindo execução em qualquer tela.
- [x] Um snapshot bem-sucedido recria `RadarUiState` sem `errorMessage`.
- [x] ODbL 1.0, adsb.lol, atribuição e necessidade de contato antes de produção estão documentados em `docs/API_SOURCES.md`.

