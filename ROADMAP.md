# Roadmap

## Status atual — 2026-11-04
- O MVP de chegadas e partidas, ingestão SIROS, sincronização periódica e infraestrutura de notificações locais estão implementados.
- Correção da tela Sobre e do estado de atualização concluída. Validação local: 53 testes contabilizados (52 executados sem falhas e 1 ignorado); APK Debug gerado.
- Revalidação local completa: 53 testes contabilizados (52 aprovados e 1 ignorado), `assembleDebug` aprovado; `FlightListViewModelTest` passou isoladamente (11 testes). Wrapper Gradle do Windows corrigido para propagar corretamente o código de saída.
- CI #43 a #53 na branch `wip-fase3`: concluídos com sucesso; #50 validou a migração Room v5→v6, #51 documentação do reinício, #52 teste de boot e #53 evidência de logcat. CI do protótipo radar ainda será disparada após o push.
- 13 testes instrumentados passaram em aparelho físico Android 14 (modelo 22101320G), incluindo migrações v1→v6, v4→v5 e v5→v6 com preservação de dados; execução feita sem emulador.
- A pesquisa inicial da Etapa 4 foi registrada; integração Aena depende de autorização/documentação apropriadas para CGR. Nenhuma posição ADS-B ou status operacional é apresentado como confirmado sem correspondência verificável.

## Etapa 1 — Validação das fontes e arquitetura (Concluída)
- [x] Investigar acesso ao painel Aena e portal SIROS/ANAC.
- [x] Realizar PoC do CSV de registros vigentes do SIROS.
- [x] Fazer avaliação inicial de AvioADSB e OpenSky como candidatos, sem validar integração, termos ou cobertura local.
- [x] Definir modelo de dados normalizado, identificador SIROS e fusos horários por aeroporto.
- [x] Definir estratégia de alertas locais com tratamento de reinicialização via `BOOT_COMPLETED`.

## Etapa 2 — MVP Android e ingestão SIROS (Concluída)
- [x] Criar projeto Kotlin + Jetpack Compose.
- [x] Implementar parser CSV e normalizador SIROS.
- [x] Configurar Room Database para persistência local e cache.

## Etapa 3 — Telas do MVP (Concluída)
- [x] Implementar telas de chegadas e partidas conectadas ao Room.
- [x] Exibir atualização e estados de carregamento, vazio e erro.
- [x] Implementar navegação para Sobre e retorno à tela de voos.

## Etapa 4 — Status operacional e radar ADS-B (Aena depende de autorização; avaliação ADS-B em andamento)
- [x] Conferir página/sitemap oficial Aena Brasil e identificar as condições de uso públicas consultadas; registrar os resultados em `docs/API_SOURCES.md`.
- [x] Preparar minuta para solicitar orientação e autorização à entidade responsável por CGR; envio e resposta ainda pendentes (`docs/AENA_API_REQUEST.md`).
- [ ] Solicitar confirmação escrita à entidade responsável por CGR (BOAB/Aena Brasil), usando a Central de Atendimento oficial indicada em `docs/API_SOURCES.md`, sobre termos aplicáveis e autorização/documentação para uma API de status.
- [ ] Exibir status operacional separado dos horários planejados SIROS, apenas quando houver dado verificável.
- [ ] Avaliar cobertura, termos e atribuição de fonte ADS-B para CGR antes de qualquer exibição.
- [ ] Associar observações ADS-B a voos somente com correspondência verificável; caso contrário, identificá-las apenas como observações experimentais.

## Etapa 6 — Integração SIROS, alertas locais e qualidade
- [x] **Fase 6.1:** `SyncMetadata`, DAO, migração Room e testes.
- [x] **Fase 6.2:** parser streaming e sincronização SIROS transacional no repositório.
- [x] **Fase 6.3:** download remoto, atualização manual, WorkManager periódico e apresentação do status de sincronização.
- [x] **Fase 6.4:** notificações locais, `FlightAlertWorker` e `BootReceiver`.
- [x] Corrigir atualização que podia permanecer carregando; remover rede real dos testes de ViewModel.
- [x] Validar suíte unitária e gerar APK Debug em 2026-11-04.
- [x] Definir janela de alerta SIROS de 15–30 minutos, deduplicação por voo/operação/data local e correção da data UTC de chegadas que cruzam a meia-noite; testes unitários e build local aprovados.
- [x] Remover a inicialização duplicada de notificações, trabalhos periódicos e consulta de atualizações em `MainActivity`; 11 testes do ViewModel, APK Debug e 11 testes instrumentados no telefone aprovados.
- [x] Uniformizar a regra `sync_metadata.id = 1` em bancos novos e migrados no schema v6; testes físicos cobrem inserção inválida, atualização inválida e migração preservando dados.
- [x] Reiniciar o telefone com o app Debug instalado e confirmar no logcat a execução natural do `BootReceiver`, permissão concedida e jobs WorkManager após o boot.
- [x] Testar o contrato do `BootReceiver` em Robolectric: `BOOT_COMPLETED` agenda os trabalhos únicos de sync e alertas; teste de código, não prova de entrega do broadcast pelo Android.
- [ ] Verificar a entrega/deduplicação de alerta com um voo SIROS real na janela de 15–30 minutos; não havia voo elegível nesta execução.

## Etapa 7 — Radar ADS-B (protótipo experimental implementado)
- [x] Avaliar documentação e licença da ADSB.lol: API pública, dados ODbL 1.0; contactar a fonte antes de uso em produção.
- [x] Fazer uma consulta pontual centrada em SBCG/CGR (50 NM): a resposta continha uma posição reportada a 45.472 NM às 21:22:07 UTC de 04/11/2026. A observação é apenas um recorte daquele instante e não prova cobertura contínua nem identifica voo comercial.
- [x] Implementar cliente HTTP no OkHttp existente, parser com validação de coordenadas e estado de erro explícito, sem adicionar dependências.
- [x] Adicionar atualização manual, indicador de carregamento/erro/vazio, escopo radial e lista com posição, altitude, velocidade, distância e idade da posição.
- [x] Exibir atribuição ADSB.lol/ODbL e avisos distinguindo observações ADS-B de planejamento/status operacional.
- [x] Abrir a tela no aparelho Android 14 sem emulador; a resposta atual sem posições exibiu o estado vazio e o horário da fonte, sem crash.
- [ ] Contactar ADSB.lol para confirmar condições/limites antes de uso em produção.
- [ ] Revalidar legibilidade/usabilidade no telefone físico e amostrar cobertura em dias/horários diferentes.
- [ ] Associar observações ADS-B a voo apenas com correspondência verificável; o protótipo não faz essa associação.

## Pendências independentes
- Enviar a minuta em `docs/AENA_API_REQUEST.md` pelo canal confirmado pela Aena e aguardar resposta escrita; não fazer scraping nem integrar dados Aena sem autorização.
- Quando houver voo SIROS real na janela de 15–30 minutos, concluir o teste de entrega/deduplicação de alerta descrito em `TESTING.md`.
