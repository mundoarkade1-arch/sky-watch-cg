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
- [x] **Fase 6.1 (SyncMetadata) validada nos testes unitários e instrumentados:** entidade `SyncMetadata`, `SyncMetadataDao`, migração Room v1→v2 com restrição `CHECK (id = 1)`. Testes instrumentados (`AppDatabaseMigrationTest`, `FlightDaoTest`) executados com sucesso em aparelho real (Poco X5 Pro, Android 14, USB): 10 testes, 0 falhas, 0 erros. **Ressalvas:** a migração foi validada sobre uma "v1 provável" (tabela flights idêntica ao schema atual gerado pelo Room); o schema real da v1 nunca foi exportado e não é recuperável do Git (arquivos só existem a partir do commit 5385fe2), portanto o teste não prova a migração de um aparelho real rodando a v1 antiga. Além disso, em instalações novas (schema v2), o Room cria `sync_metadata` SEM o `CHECK (id = 1)`; apenas bancos migrados da v1 possuem o CHECK.
- [x] **Fase 6.3 (Corrigida): Ingestão remota, transação atômica, última atualização na interface, WorkManager e APK de debug.**
- [x] **Etapa A concluída:** Correção do teste `testRefreshStateEmitsRefreshing` em `FlightListViewModelTest` utilizando fonte remota falsa (`SirosRemoteSource` com localhost fictício) e controle de corrotinas (`yield()` / `advanceUntilIdle()`). 36 testes unitários verdes (0 falhas). Código enviado para o repositório remoto via `git push origin main` (após `git commit --amend`).
- [x] **Estabilização da Fase 3 (04/11/2026):** investigada a falha do CI #38 (2 testes de atualização falhando). A causa era a dependência de rede real em testes do ViewModel; os testes agora usam fonte remota controlada. O estado `Refreshing` é mantido durante emissões do Room, atualizações repetidas são evitadas, falhas preservam dados em cache e a tela Sobre usa retorno acessível e conteúdo rolável. Validação local em 18:59 UTC / 14:59 Campo Grande: 53 testes contabilizados (52 executados, 0 falhas e 1 ignorado); `assembleDebug` bem-sucedido.
- [x] **CI #39 (04/11/2026):** commit `bccfd48` na branch `wip-fase3` concluído com sucesso no GitHub Actions.
- [x] **Revalidação de testes (04/11/2026):** suíte completa: 53 testes, 0 falhas, 0 erros, 1 ignorado; `assembleDebug` bem-sucedido. `FlightListViewModelTest` também passou isoladamente (11 testes). Corrigido `android-app/gradlew.bat`, que sempre retornava código 1 mesmo quando o Gradle concluía com sucesso; validado retorno 0 em build bem-sucedido e retorno 1 em tarefa inexistente.
- [x] **CI #41 e #42 (04/11/2026):** ambos concluídos com sucesso; #42 validou build Debug e suíte unitária para o commit `fa2712d`.
- [x] **Documentação alinhada (04/11/2026):** `README.md`, `ROADMAP.md`, `PROJECT_STATE.md`, `TESTING.md` e `CHANGELOG.md` sincronizados com o estado comprovado do projeto.
- [x] **Pesquisa Aena Brasil (04/11/2026):** página e sitemap oficiais de CGR consultados; não foi localizada documentação pública de painel/API para CGR. Os termos consultados restringem reprodução de conteúdo sem autorização escrita e identificam ANB, enquanto CGR é administrado pela BOAB. Sem presumir que os termos resolvam o escopo de CGR, nenhuma coleta automatizada será implementada antes de confirmação escrita e documentação apropriada. Detalhes e fontes em `docs/API_SOURCES.md`.
- [x] **Roteiro manual de alertas após reinicialização:** adicionado a `TESTING.md`; execução em aparelho ainda pendente.

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
- AvioADSB: Baixa cobertura em CGR (experimental).
- **Validação manual pendente:** registrar teste ponta a ponta das notificações após reinicialização do dispositivo. Os testes automatizados e a implementação de `BootReceiver` não substituem esse teste.
- **Comportamento de alertas a validar:** o worker considera eventos nas próximas 2 horas e roda periodicamente com restrição de rede; WorkManager não garante execução exatamente a cada 15 minutos. Verificar no aparelho se uma notificação é repetida/atualizada nas execuções seguintes.
- **Integração SIROS:** a tela usa os dados locais do Room; a atualização manual e a sincronização periódica atualizam esse cache com o CSV remoto.
- **Ponto em aberto (Room):** em instalações NOVAS (schema v2), o Room cria a tabela sync_metadata SEM a restrição CHECK (id = 1). Somente bancos que passaram pela MIGRATION_1_2 têm o CHECK. Ou seja, o CHECK não vale em todos os bancos. Nenhum código foi alterado por causa disso; a decisão fica para depois.

## Próxima etapa
- Solicitar à entidade responsável pelo aeroporto de Campo Grande (BOAB/Aena Brasil), usando a Central de Atendimento oficial indicada em `docs/API_SOURCES.md`, confirmação escrita das condições de uso e documentação de uma eventual API de status operacional.
- Executar e registrar o roteiro de notificações após reinicialização descrito em `TESTING.md`.
- Só então decidir se uma integração Aena é permitida e adequada; avaliar cobertura/termos ADS-B separadamente.
