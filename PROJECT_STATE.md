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
- [x] **Fase 6.2 (Concluída com ressalvas):** Parser streaming validado com CSV real do SIROS (UTF-8 com BOM). Correção em `SirosStreamParser.openReader` detecta BOM UTF-8 e lê em UTF-8; sem BOM mantém ISO-8859-1. Novos testes `SirosStreamParserBomTest` (3 testes) e `SirosStreamParserRealFileTest` (1 teste com CSV de ~25 MB). Testes instrumentados executados em aparelho real (Poco X5 Pro) sem erros. Pendência restante: ingestão remota (`syncFromRemote`, `WorkManager`). Fase 6.3 não autorizada.
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
- Iniciar a implementação do sistema de alertas locais com AlarmManager/WorkManager.
