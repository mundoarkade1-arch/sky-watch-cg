## Falha na Fase 3 - Bloqueio do Gradle/Kapt
- Último trecho do log:
```
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.5/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build
```
- Tentativas realizadas (3 abordagens distintas):
  1. kaptDebugKotlin com `--no-daemon --stacktrace --info`
  2. testDebugUnitTest com `--no-daemon`
  3. assembleDebug com `--no-daemon --stacktrace`
- Resultado: Travamento persistente na inicialização do daemon/processo do Gradle (nenhum avanço após 10 minutos em cada tentativa, processos Java limpos repetidamente).

# Progresso do Projeto SKY WATCH CG

## Bloco 1 — estado da fonte ADS-B antes da alteração (05/11/2026)
1. URL/servidor atual: `https://api.adsb.lol/v2/lat/-20.4686/lon/-54.6724/dist/50`, servidor `api.adsb.lol`.
2. Frequência atual: somente ao abrir a tela Radar ou ao tocar manualmente em Atualizar; não havia laço periódico.
3. HTTP 429 atual: não havia cooldown, nova tentativa automática nem mensagem específica; qualquer HTTP não bem-sucedido virava erro fixo.

## Bloco 0 — estado e diagnóstico do CI (05/11/2026)
- Branch confirmada: `wip-fase3`; `git status` contém apenas o arquivo não rastreado preexistente `old_MainScreen.kt`, preservado.
- `git log --oneline -5` inicia em `a6f1d38`; o último CI não pôde ser consultado porque o executável `gh` não está instalado neste ambiente.
- Ajustado `.github/workflows/ci.yml` para manter build e testes em passos separados, capturar logs e escrever no `GITHUB_STEP_SUMMARY` as linhas de compilação iniciadas por `e:` e os nomes/indicadores de testes falhos, com falha final agregada.
- Próximo passo exato: implementar o Bloco 1 (failover, cooldown e limite por servidor do radar ADS-B), registrar as 3 linhas exigidas antes da alteração, testar no CI e atualizar este arquivo.

## Bloco 2 — atualização em primeiro plano (05/11/2026)
- `FlightListViewModel.startAutoRefresh()` usa um único `repeatOnLifecycle(STARTED)`, busca ao entrar e repete em 60 segundos; o ciclo é cancelado ao sair para segundo plano.
- O relógio local de Campo Grande passou a ser exposto pelo ViewModel e consumido pela lista; a composição não mantém laço próprio.
- Radar ganhou a chave `Manter tela ligada`, persistida com padrão ligado e aplicada por `FLAG_KEEP_SCREEN_ON`.
- Botões existentes continuam protegidos pelo `refreshJob`, evitando pedidos simultâneos; o tratamento de erro já encerra o estado girando.
- Próximo passo exato: implementar e testar as regras do Bloco 3 (chegada, saída, voo alto e deduplicação de 20 minutos).

## Bloco 3 — regras de alerta ADS-B (05/11/2026)
- Criado `RadarAlertRules` com limiares centralizados para chegada, saída e exclusão de tráfego acima de 20.000 pés.
- Criada deduplicação por `hex + tipo` com cooldown de 20 minutos e estimativa calculada apenas quando há velocidade válida.
- A faixa global passa a exibir a detecção enquanto o estado do radar a mantém; a associação a voo SIROS ainda não é feita e nenhum callsign é tratado como companhia.
- Testes adicionados para chegada, saída, voo alto, cooldown e tipos independentes; a execução depende do CI porque Gradle local é proibido.
- Próximo passo exato: concluir o Bloco 4 com agrupamento visual da lista, seção recolhida de voos passados e testes de troca de data.

## Bloco 4 — próximo voo e lista diária (05/11/2026)
- A função pura `nextMovements(voos, agoraLocal)` fornece o próximo pouso e a próxima decolagem respeitando a hora local do aeroporto.
- A tela de voos mostra os dois cartões fixos no topo, usa o relógio do Bloco 2 para contagem regressiva e separa voos passados em `JÁ PASSARAM (N)` recolhível.
- Nomes de companhia removem o marcador cadastral `(EX- VRG )`; quando não há movimento restante a tela preserva o estado vazio sem inventar dados.
- Teste unitário cobre próximo pouso/decolagem e nenhum movimento restante. O rolar automático e o botão “Ir para o próximo” ainda ficaram pendentes para uma iteração visual posterior.
- Próximo passo exato: reestruturar notificações do Bloco 5 para AlarmManager exato quando permitido, receiver, boot e reagendamento por configurações.

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

- [x] Implementar FlightAlertWorker com AlarmManager/WorkManager
- [x] Notificação push via POST_NOTIFICATIONS
- [x] Agendamento de alertas para voos selecionados
- [x] Tratamento de reinicialização do dispositivo (BootReceiver)
- [x] Testes unitários para alertas (FlightAlertWorkerTest)

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
