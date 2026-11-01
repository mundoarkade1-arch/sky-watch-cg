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
- [x] **Fase 6.1 (SyncMetadata) validada nos testes unitários:** entidade `SyncMetadata`, `SyncMetadataDao`, migração Room v1→v2 com restrição `CHECK (id = 1)` e testes unitários do DAO (5 testes passando). O teste instrumentado da migração real v1→v2 continua pendente (sem dispositivo ou emulador) (o CHECK existe apenas em bancos migrados da v1; instalações novas criadas pelo Room não o têm).
- [ ] **Fase 6.2 (Em andamento / Não concluída):** Parser streaming validado com CSV real do SIROS (UTF-8 com BOM). Correção em `SirosStreamParser.openReader` detecta BOM UTF-8 e lê em UTF-8; sem BOM mantém ISO-8859-1. Novo teste `SirosStreamParserBomTest` (3 testes). Testes instrumentados (`FlightDaoTest`, `AppDatabaseMigrationTest`) compilam com `runBlocking` (antes não compilavam: `FlightDao` tem métodos `suspend`; o `AppDatabaseMigrationTest` foi reescrito para não depender de `MigrationTestHelper`/`room-testing`, que não estão no projeto). **O teste instrumentado da migração v1→v2 NÃO foi executado** (sem aparelho/emulador viável). Pendência: ingestão remota (`syncFromRemote`, `WorkManager`). Fase 6.3 não autorizada.
- [ ] Implementar sistema de alertas locais com AlarmManager/WorkManager e tratamento de reinicialização do dispositivo.
- [x] Pasta local ligada ao repositório GitHub privado (mundoarkade1-arch/sky-watch-cg) em 01/11/2026: primeiro envio feito, com .gitignore para build, local.properties, chaves e arquivos de log. A partir daqui o trabalho é versionado com git add, git commit e git push.

## Bloqueios e riscos
- SIROS CSV: Requer download periódico e tratamento correto da codificação UTF-8 com BOM (detectado automaticamente) e delimitador ;.
- Aena: Sem API pública documentada, sujeito a tratamento de frontend.
- AvioADSB: Baixa cobertura em CGR (experimental).
- Alertas locais: Exigem receptor de boot (BOOT_COMPLETED) para reagendar alarmes após reiniciar o aparelho.
- **Limitação atual:** A ingestão remota automatizada do CSV SIROS/ANAC ainda não está totalmente integrada no fluxo de tela (as telas consultam diretamente o banco local Room).
- **Ponto em aberto (Room):** em instalações NOVAS (schema v2), o Room cria a tabela sync_metadata SEM a restrição CHECK (id = 1). Somente bancos que passaram pela MIGRATION_1_2 têm o CHECK. Ou seja, o CHECK não vale em todos os bancos. Nenhum código foi alterado por causa disso; a decisão fica para depois.

## Próxima etapa
- Executar `connectedDebugAndroidTest` em celular Android real via depuração USB para validar migração instrumentada e DAO.
- Concluir validações pendentes da Fase 6.2 (teste instrumentado de migração Room).
