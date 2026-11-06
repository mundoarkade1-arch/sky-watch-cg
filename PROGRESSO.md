## Rodada de release assinada e atualizacao (06/11/2026)
- Analise concluida: modulo `:app`, Kotlin + Jetpack Compose, `applicationId` `com.skywatch.cg`, tela Sobre em `MainScreen.kt`, repositorio publico `mundoarkade1-arch/sky-watch-cg`.
- Implementado Gradle com assinatura Release opcional por ambiente, versao por ambiente com fallback, workflow `release.yml`, consulta publica de releases e dialogo de atualizacao.
- Escolha de instalacao: o botao abre o asset no navegador; nao foi adicionado token, FileProvider ou permissao de instalacao automatica.
- O workflow evita colisao com as tags `v1.0.1` a `v1.0.19` existentes usando o proximo numero livre quando o `github.run_number` da nova definicao ainda e menor.
- Proximo passo exato: publicar a alteracao na `main`, aguardar o CI e conferir a primeira release assinada no GitHub Actions.

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

## Bloco 5 — diagnóstico do alerta anterior (05/11/2026)
1. O alerta era agendado como `FlightAlertWorker` periódico do WorkManager, com intervalo mínimo de 15 minutos.
2. A checagem ocorria no worker, consultando o cache SIROS apenas quando o sistema executava o trabalho.
3. A janela de 15–30 minutos podia ser perdida porque o WorkManager não garante horário exato.
4. O trabalho também era enfileirado no `onCreate` e no boot, por isso parecia disparar ao entrar e sair do app.
5. O caminho anterior não usava `AlarmManager`, não verificava `canScheduleExactAlarms()` e não reagendava alarmes individuais após configurações.

## Bloco 5 — implementação em andamento (05/11/2026)
- Criados plano puro `eventAtMillis - minutos`, `AlarmManager` exato quando permitido, fallback `setAndAllowWhileIdle`, receiver e deduplicação em `alert_sent` antes de publicar.
- `POST_NOTIFICATIONS`, canal de importância alta e texto de otimização de bateria continuam documentados; a tela de configurações e o reagendamento automático ao mudar preferências ainda estão pendentes.
- O WorkManager periódico foi retirado do caminho de alertas e mantido somente para sincronização SIROS.
- Próximo passo exato: revisar imports Kotlin, atualizar documentação final e aguardar CI remoto; Gradle local permanece proibido.

## Correção do CI após execução vermelha (05/11/2026)
- A API/página pública do repositório retornou HTTP 404, portanto os logs da execução vermelha não puderam ser lidos sem autenticação.
- Corrigido `.github/workflows/ci.yml`: removidos escapes de crase incompatíveis com Bash no Summary e tornado condicional o upload do APK quando o build falha.
- Commit enviado: `c9a8f82`.
- Próximo passo exato: aguardar a nova execução automática e conferir o Summary/log do runner; nenhum Gradle foi executado localmente.

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

## Registro final da correção do CI
- Commit da correção do código: `bb473af`; CI #68 verde.
- Commit do diagnóstico de totais: `182dfa8`; CI #69 vermelho por erro de heredoc no resumo.
- Commit da correção final do diagnóstico: `4ca880e`; CI #70 verde (run `42136555472`, 6m22s).
- Totais reais publicados no Summary do CI #70: 84 testes, 1 pulado, 0 falhas.
- O APK debug foi gerado e o relatório JUnit foi publicado como artefato; nenhum Gradle foi executado localmente.
- Pendente funcional: validação manual no aparelho e conclusão dos itens de alerta visual/sonoro, correspondência SIROS por callsign e rolagem automática descritos nos blocos 3–5.
- Próximo passo exato: testar o APK `app-debug` no aparelho, verificando refresh em primeiro plano, failover ADS-B, alerta e AlarmManager.

### Bloco 1 — três linhas solicitadas
1. Fonte padrão atual: `https://api.adsb.lol/v2/point/-20.4686/-54.6724/50`, com reserva em `api.airplanes.live` e `opendata.adsb.fi`.
2. O radar chama ao abrir e depois a cada 60 s enquanto o app está STARTED; há no máximo uma chamada por segundo por servidor.
3. HTTP 429 entra em cooldown por fonte, respeitando `Retry-After` ou usando 60 s com backoff até 5 min; a rodada tenta a próxima fonte.

### Bloco 5 — cinco linhas solicitadas
1. Antes, o alerta periódico era checado pelo WorkManager ao abrir/reativar o app; por isso parecia disparar ao entrar e sair.
2. Agora há um alarme individual por voo seguido, no horário programado menos os minutos escolhidos.
3. O AlarmManager usa alarme exato quando permitido e `setAndAllowWhileIdle` como fallback.
4. O receiver mostra a notificação e grava `alert_sent` para impedir duplicidade; o reagendamento no boot está registrado na implementação.
5. O app verifica POST_NOTIFICATIONS, canal de alta importância e informa que alguns celulares exigem Bateria > Sem restrições; WorkManager periódico não é usado para isso.
