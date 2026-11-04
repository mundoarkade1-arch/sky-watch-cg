# Estado do projeto

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
- [x] **Pesquisa inicial de fonte ADS-B (04/11/2026):** documentação oficial OpenSky descreve state vectors em tempo real e consultas por área; uso informado para pesquisa/não comercial. Termos completos não puderam ser consultados (HTTP 403) e a cobertura de CGR não foi verificada. Nenhuma integração foi feita.
- [x] **Radar ADS-B experimental (04/11/2026):** primeira versão usa a API ADSB.lol com raio de 50 NM, botão de consulta manual, apresentação de observações em escopo radial e lista com timestamp/idade, sem associação a voos SIROS. Atribuição ODbL exibida na tela. A fonte solicita contato antes de uso em produção; não foi feito esse contato.
- [x] **Testes do radar:** parser de payload, posições inválidas/ausentes, resposta vazia, timestamp obrigatório, integração HTTP bem-sucedida e erro HTTP cobertos por 6 testes unitários.
- [x] **Teste físico inicial do radar (04/11/2026):** instalada e aberta apenas a variante Debug no aparelho 22101320G, sem emulador; a tela carregou resposta sem posições e exibiu estado vazio e horário da fonte, sem crash. Isso não prova ausência de tráfego nem cobertura contínua.

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

## Próxima etapa — radar ADS-B
- Contatar ADSB.lol antes de distribuir/usar a integração em produção; confirmar condições, limites de frequência e atribuição adequados.
- Avaliar cobertura em diferentes dias/horários e documentar quando não houver observações; uma resposta isolada não representa cobertura contínua.
- Revalidar legibilidade/usabilidade no aparelho e amostrar cobertura em dias/horários distintos.
- Manter observações ADS-B separadas do planejamento SIROS; não associar a voo comercial sem correspondência verificável.
- A integração Aena permanece uma frente separada e depende de resposta escrita; a minuta não foi enviada.
