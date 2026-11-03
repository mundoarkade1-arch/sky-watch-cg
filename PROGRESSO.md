# Progresso do Projeto SKY WATCH CG

## Fase 0 - Limpeza e verdade
- [x] Apagar gradle-output-*.log da raiz e siros_temp_*.csv (já limpos no repositório)
- [x] Ajustar .gitignore (logs, build-logs/, siros_temp_*.csv)
- [x] Garantir que testes não criem arquivos temporários no projeto
- [x] Corrigir teste testSyncFromRemoteSuccess (asserções de airline corrigidas)
- [x] Corrigir documentação (ROADMAP, README, RETOMADA, TESTING, PENDENCIAS)
- [x] testSyncFromRemoteSuccess: asserção totalSynced atualizada de 2 para 94 — SirosNormalizer expande cada registro CSV para todos os dias válidos no intervalo [referenceDate-1, referenceDate+45] (com 2 registros × 7 dias/semana × ~47 dias = 94 voos normalizados).

## Fase 1 - Corrigir erros (VERIFICADA ✅)
- [x] POST_NOTIFICATIONS no AndroidManifest e ActivityResult API no MainActivity
- [x] SirosNormalizer: faixa de datas referenceDate - 1 até referenceDate + 45
- [x] Data de chegada: se arrival UTC < departure UTC, arrivalDate = flightDate + 1
- [x] FlightDisplayUtils usa arrivalDate para chegadas
- [x] Room Database versão 3 com Migration manual (arrivalDate, CHECK id=1 unificado)
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

## Fase 2 - Telas do MVP (pendente Fase 1 verificada)
- [x] strings.xml atualizado com novos textos (busca, data, detalhes, fontes, radar)
- [x] DetailScreen.kt criado (esqueleto completo com horários UTC/local, status, fonte)
- [x] FlightDisplayUtils corrigido para usar arrivalDate em chegadas
- [ ] Seletor de data na tela (botões anterior/hoje/próximo + escolha de data)
- [ ] Leitura por Flow no Room para atualização reativa
- [ ] Primeira abertura com banco vazio: sincronização automática
- [ ] Busca por companhia, número do voo ou cidade
- [ ] Filtro por companhia
- [ ] Navegação para DetailScreen ao clicar no card
- [ ] Ícone do app (vetorial simples) e acessibilidade
- [ ] Tela "Fontes e avisos"

## Fase 3 - Alertas (pendente)
## Fase 4 - Status em tempo real (Aena) (pendente)
## Fase 5 - Radar ADS-B experimental (pendente)
## Fase 6 - Atualização e publicação (pendente)
## Fase 7 - Fechamento (pendente)
