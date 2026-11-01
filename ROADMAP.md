# Roadmap

## Status atual
- **Fase 6.1 (SyncMetadata e Migração Room v1→v2) concluída com sucesso.** Pronto para autorização da Fase 6.2 (Parser Streaming e Sincronização no SirosRepository).

## Etapa 1 — Validação das fontes e Arquitetura (Concluído)
- [x] Investigar acesso ao painel Aena e portal SIROS/ANAC.
- [x] Realizar Prova de Conceito (PoC) do CSV de registros vigentes do SIROS.
- [x] Avaliar AvioADSB e OpenSky Network.
- [x] Definir modelo de dados normalizado (identificador SIROS original, data específica do voo, fusos horários por aeroporto, delay_minutes como null quando sem confirmação, separação entre última sincronização geral e última atualização de status operacional).
- [x] Definir estratégia de alertas locais (AlarmManager / WorkManager com tratamento de reinicialização via BOOT_COMPLETED).

## Etapa 2 — MVP Android e Ingestão SIROS (Concluído)
- [x] Criar projeto Kotlin + Jetpack Compose.
- [x] Implementar módulo de leitura e normalização do CSV do SIROS.
- [x] **Configurar Room Database para persistência local e cache (FlightEntity, FlightDao, AppDatabase, SirosRepository).**

## Etapa 3 — Telas do MVP (Chegadas e Partidas) — **CONCLUÍDA**
- [x] Tela de chegadas consumindo Room.
- [x] Tela de partidas consumindo Room.
- [x] Indicadores de última sincronização e status desatualizado.

## Etapa 4 — Radar e Status em Tempo Real (Concluído)
- [x] Integração complementar com painel Aena para status operacional.
- [x] Integração experimental com AvioADSB (com aviso claro de ausência de cobertura).

## Etapa 5 — Telas do MVP e Testes Unitários do ViewModel — **CONCLUÍDA**
- [x] Implementação de corrotinas e controle de concorrência (`loadJob?.cancel()`).
- [x] Resolução dos testes unitários do ViewModel com sincronização de emissão de estado (`viewModel.uiState.first { it !is FlightUiState.Loading }`).

## Etapa 6 — Integração Real SIROS, Alertas Locais e Qualidade
- [x] **Fase 6.1:** Entidade `SyncMetadata`, `SyncMetadataDao`, migração Room v1→v2 e testes de migração e DAO.
- [ ] **Fase 6.2:** Parser streaming e sincronização no SirosRepository.
- [ ] **Fase 6.3:** Sincronização periódica em background (WorkManager).
- [ ] **Fase 6.4:** Agendamento de notificações e tratamento de reinicialização (BOOT_COMPLETED).
- [ ] Testes em dispositivo Android real e geração de APK de teste.
