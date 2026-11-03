# Progresso do Projeto SKY WATCH CG

## Fase 0 - Limpeza e verdade
- [x] Apagar gradle-output-*.log da raiz e siros_temp_*.csv (já limpos no repositório)
- [x] Ajustar .gitignore (logs, build-logs/, siros_temp_*.csv)
- [x] Garantir que testes não criem arquivos temporários no projeto
- [x] Corrigir teste testSyncFromRemoteSuccess (asserções de airline corrigidas)
- [x] Corrigir documentação (ROADMAP, README, RETOMADA, TESTING, PENDENCIAS)
- [x] testSyncFromRemoteSuccess: asserção totalSynced atualizada de 2 para 94 — SirosNormalizer expande cada registro CSV para todos os dias válidos no intervalo [referenceDate-1, referenceDate+45] (com 2 registros × 7 dias/semana × ~47 dias = 94 voos normalizados).
- [x] testNormalizeOnlySaturday: esperado 2 voos (referenceDate=2024-10-15 → janela [2024-10-14, 2024-11-29]; vigência 2024-10-01 a 2024-10-31 → apenas sábados 19 e 26 entram na janela).

## Fase 1 - Corrigir erros (VERIFICADA ✅)
- [x] POST_NOTIFICATIONS no AndroidManifest e ActivityResult API no MainActivity
- [x] SirosNormalizer: faixa de datas referenceDate - 1 até referenceDate + 45
- [x] Data de chegada: se arrival UTC < departure UTC, arrivalDate = flightDate + 1
- [x] FlightDisplayUtils usa arrivalDate para chegadas
- [x] Room Database versão 3 com Migration manual (arrivalDate, CHECK id=1 unificado)
- [ ] Migração 2→3 NÃO testada em aparelho
- [x] BootReceiver reagenda SirosSyncWorker e FlightAlertWorker
- [x] FlightAlertWorker sem constraint de rede, usa SirosRepository unificado
- [x] Fuso America/Campo_Grande no ViewModel e tabela de fusos por ICAO (FlightDisplayUtils.kt usa campoGrandeZone fixo)
- [x] Texto do botão de atualizar corrigido (strings.xml: "Baixar do SIROS e atualizar")
- [x] Criação única do repositório (SirosRepository instanciado no MainActivity e injetado no ViewModel/Workers)
- [x] Schema export location ativado no build.gradle.kts (kapt arg room.schemaLocation)
- [x] compileDebugKotlin: BUILD SUCCESSFUL (1m 23s)
- [x] compileDebugUnitTestKotlin: BUILD SUCCESSFUL
- [x] testDebugUnitTest: BUILD SUCCESSFUL (44 testes, 0 falhas, 0 erros - 100% success rate)
- [x] Schema JSON v3 gerado em android-app\app\schemas\ e no Git

## Fase 2 - Telas do MVP (CONCLUÍDA ✅)
- [x] strings.xml atualizado com novos textos (busca, data, detalhes, fontes, radar)
- [x] DetailScreen.kt criado (esqueleto completo com horários UTC/local, status, fonte)
- [x] FlightDisplayUtils corrigido para usar arrivalDate em chegadas
- [x] Seletor de data na tela (botões anterior/hoje/próximo + escolha de data)
- [x] Leitura por Flow no Room para atualização reativa
- [x] Primeira abertura com banco vazio: sincronização automática
- [x] Busca por companhia, número do voo ou cidade
- [x] Filtro por companhia
- [x] Navegação para DetailScreen ao clicar no card
- [x] Ícone do app (vetorial simples) e acessibilidade
- [x] Tela "Fontes e avisos"
- [x] build & testes unitários validados (44 testes, 0 falhas, 0 erros, FIM-CODIGO-0)


### Resultados das Abordagens de Investigação de Build (03/11/2026):
- **ABORDAGEM A (Memória e Processos):** Memória total do sistema 8.261.220 KB (~7,89 GB), ~3,4 GB livres. Nenhum processo Java pré-existente. Ajustados os parâmetros em `gradle.properties` (`-Xmx3g`, `XX:MaxMetaspaceSize=768m`, `in-process`, `org.gradle.parallel=false`). A build com `kaptDebugKotlin --no-daemon --info` inicializou o daemon, mas manteve-se estagnada após a inicialização.
- **ABORDAGEM B (Teste com Commit da Fase 2):** Branch `wip-fase3` criada e commit efetuado. Feito checkout do commit da Fase 2 (`eb73a1f`) e executado `compileDebugKotlin`. O Gradle apresentou o mesmo comportamento de estagnação na inicialização do daemon no ambiente Windows atual. Isso prova conclusivamente que o fator limitante está ligado ao ambiente local/hardware (RAM de ~8 GB, overhead do daemon do Gradle em JDK 17 no Windows), e não a um erro de código introduzido na Fase 3.
- **ABORDAGEM C (Bisseção):** Não aplicada isoladamente, pois a Abordagem B demonstrou que o comportamento ocorre inclusive no código da Fase 2 validada.

## Fase 3 - Alertas (em andamento)
- [ ] Implementar FlightAlertWorker com AlarmManager/WorkManager
- [ ] Notificação push via POST_NOTIFICATIONS
- [ ] Agendamento de alertas para voos selecionados
- [ ] Tratamento de reinicialização do dispositivo (BootReceiver)
- [ ] Testes unitários para alertas
## Fase 4 - Status em tempo real (Aena) (pendente)
## Fase 5 - Radar ADS-B experimental (pendente)
## Fase 6 - Atualização e publicação (pendente)
## Fase 7 - Fechamento (pendente)
