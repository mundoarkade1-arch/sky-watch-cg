# Roadmap

## Status atual — 2026-11-04
- O MVP de chegadas e partidas, ingestão SIROS, sincronização periódica e infraestrutura de notificações locais estão implementados.
- Correção da tela Sobre e do estado de atualização concluída. Validação local: 53 testes contabilizados (52 executados sem falhas e 1 ignorado); APK Debug gerado.
- CI #39 para o commit `bccfd48` na branch `wip-fase3`: concluído com sucesso.
- A Etapa 4 (status operacional Aena e radar ADS-B) continua planejada; nenhuma posição ADS-B ou status operacional é apresentado como confirmado sem correspondência verificável.

## Etapa 1 — Validação das fontes e arquitetura (Concluída)
- [x] Investigar acesso ao painel Aena e portal SIROS/ANAC.
- [x] Realizar PoC do CSV de registros vigentes do SIROS.
- [x] Avaliar AvioADSB e OpenSky Network.
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

## Etapa 4 — Status operacional e radar ADS-B (Planejada; depende de fonte autorizada)
- [ ] Investigar e confirmar uma API ou integração autorizada para status operacional Aena. Não fazer scraping contrário aos termos da fonte.
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
- [ ] Registrar teste manual ponta a ponta de alertas e reinicialização em dispositivo Android. Testes instrumentados anteriores cobriram Room/migração; não comprovam, por si só, esse fluxo completo de notificações.

## Próxima etapa recomendada
Confirmar a fonte autorizada para status Aena e documentar suas limitações antes de implementar a Etapa 4. Não adicionar dependências nem iniciar integrações sem essa validação.
