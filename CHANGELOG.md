# Changelog

## [1.0.0] - 2026-11-01
### Adicionado
- Implementação completa da persistência local com Room Database (`AppDatabase`, `FlightEntity`, `FlightDao`, `SirosRepository`).
- Testes unitários avançados utilizando `Room.inMemoryDatabaseBuilder` e Robolectric para validação de persistência, consultas por data e prevenção de duplicidades.
- Suporte a chave composta de identificação de voo (`sirosRecordId` + data específica), garantindo deduplicação correta sem apagar voos em datas diferentes.
- Build de debug gerado com sucesso (`app-debug.apk`).

### Telas do MVP e Testes Unitários do ViewModel (Etapa 5) - 2026-11-01
- Interface Android moderna em tema escuro (Material 3) e português brasileiro.
- Cabeçalho com nome **SKY WATCH CG** e identificação do aeroporto **Campo Grande (CGR / SBCG)**.
- Navegação por abas entre **Chegadas** e **Partidas** (Navigation Compose).
- Listas conectadas ao Room via `SirosRepository` com `FlightListViewModel`.
- Cards de voo exibindo: companhia aérea, número do voo, origem/destino com nomes, horário previsto (chegada ou partida conforme aba), modelo de aeronave quando disponível.
- Estados de carregamento (spinner), erro (mensagem vermelha) e lista vazia (texto explicativo).
- Botão de atualização manual (ícone refresh na TopAppBar) que recarrega os dados locais do Room. Ainda não existe sincronização remota do CSV SIROS.
- Aviso obrigatório e permanente: *"Horários programados (SIROS/ANAC). Não constituem confirmação de operação em tempo real."*
- Fuso horário local do aeroporto respeitado (UTC armazenado, exibido como UTC no card com indicação de data do voo).
- Sem voos fictícios: se o banco estiver vazio, exibe mensagem explicativa ao usuário.
- Testes unitários do ViewModel (`FlightListViewModelTest`) cobrindo estados iniciais, banco vazio, dados populados e refresh.
- **Corrigida injeção de dependência no `FlightListViewModel`** com construtor secundário para testes, permitindo injetar `SirosRepository` conectado ao banco em memória. Construtor de produção preservado com banco real. Todos os 14 testes passam.
- **Corrigida condição de corrida de corrotinas** no `FlightListViewModel`: adicionado controle `loadJob?.cancel()` antes de iniciar nova consulta, garantindo que apenas a consulta mais recente atualize o estado da interface. Preservado carregamento automático no `init` e funcionamento do botão refresh.
- **Sincronização dos Testes Unitários (Etapa 5 resolvida em 01/11/2026):** Os 3 testes que falhavam em `FlightListViewModelTest` passaram com sucesso. A causa era que os testes liam o estado logo depois de `advanceUntilIdle()`, mas a consulta do Room roda em uma thread que o relógio de teste não controla. A correção foi adicionar `viewModel.uiState.first { it !is FlightUiState.Loading }` nos três testes, sem alterar o código de produção.
### Fase 6.1 (Revisada): Restrição de Registro Único em `sync_metadata` (2026-11-01)
- **Correção da restrição de banco de dados:** Adicionada a restrição `CHECK (id = 1)` na instrução SQL da migração `MIGRATION_1_2` em `AppDatabase.kt`, garantindo no próprio nível do SQLite que a tabela `sync_metadata` rejeite qualquer tentativa de inserir registros com IDs diferentes de 1 (o CHECK existe apenas em bancos migrados da v1; instalações novas criadas pelo Room não o têm).
- **Novos testes de rejeição:** Adicionados os testes unitários `testSyncMetadataRejectId2` e `testSyncMetadataRejectId3` em `SyncMetadataDaoTest.kt`, que verificam e comprovam com sucesso que inserções com `id = 2` ou `id = 3` lançam `SQLiteConstraintException` por violação da constraint.
- **Resultado:** 19 unitários executados e aprovados (`BUILD SUCCESSFUL`).

### Fase 6.2 (Concluída com Ressalvas) - 2026-11-01
- **Criação de `SirosStreamParser.kt`:** Parser streaming implementado com `Sequence` e com callback.
- **Correções de compilação aplicadas:**
  1. `yield` dentro de `forEachLine` foi corrigido (trocado por loop `for` com `lineSequence()` dentro de `use`).
  2. Removido um caractere `}` sobrando em `SirosStreamParserTest.kt`.
  3. Removidos dois `return@Test` inválidos em `SyncMetadataDaoTest.kt`.
- **Correção de codificação (01/11/2026):** O CSV real do SIROS (24.7 MB, baixado em 01/11/2026) está em UTF-8 COM BOM (`EF BB BF`), não em ISO-8859-1. Causa do bug: o parser lia como ISO-8859-1, nunca reconhecia o cabeçalho "Cód. Empresa" e devolvia 0 registros. Correção: função `openReader` em `SirosStreamParser.kt` detecta o BOM UTF-8 e lê em UTF-8; sem BOM mantém ISO-8859-1.
- **Novos testes:** `SirosStreamParserBomTest` (3 testes: UTF-8 com BOM via `parseStream`, UTF-8 com BOM via `parseStreamWithCallback`, ISO-8859-1 sem BOM). `SirosStreamParserRealFileTest` (1 teste com CSV real de ~25 MB).
- **Ajustes nos Testes Instrumentados (01/11/2026):** `FlightDaoTest.kt` e `AppDatabaseMigrationTest.kt` ajustados para usar `runBlocking` (métodos do `FlightDao` são `suspend`). `AppDatabaseMigrationTest.kt` reescrito sem `MigrationTestHelper` / `room-testing` (ausentes no projeto), simulando uma v1 provável.
- **Execução de Testes Instrumentados (01/11/2026):** `connectedDebugAndroidTest` em Poco X5 Pro (modelo 22101320G, Android 14, USB): 10 testes, 0 falhas, 0 erros, 0 pulados (`AppDatabaseMigrationTest` 2, `FlightDaoTest` 8). Comando: `cmd /c "cd android-app && gradlew.bat connectedDebugAndroidTest --console=plain"` (exige aparelho com depuração USB; emulador não viável com 7,9 GB RAM).
- **Ressalvas:**
  1. A migração foi validada sobre uma "v1 provável" (tabela `flights` idêntica ao schema atual gerado pelo Room). O schema real da v1 nunca foi exportado e não é recuperável do Git (arquivos só existem a partir do commit 5385fe2), portanto o teste não prova a migração de um aparelho real rodando a v1 antiga.
  2. Em instalações novas (schema v2), o Room cria `sync_metadata` SEM `CHECK (id = 1)`; apenas bancos migrados da v1 possuem o CHECK. Decisão em aberto.
- **Resultados dos testes unitários em 01/11/2026 (`testDebugUnitTest`):** `BUILD SUCCESSFUL`, 32 testes, 0 falhas, 0 erros (distribuídos em `SyncMetadataDaoTest` 5, `SirosCsvParserTest` 4, `SirosNormalizerTest` 4, `SirosRepositoryTest` 2, `SirosStreamParserBomTest` 3, `SirosStreamParserRealFileTest` 1, `SirosStreamParserTest` 9, `FlightListViewModelTest` 4).
- **Medição real com CSV completo (`SirosStreamParserRealFileTest`):** total_lines=65477, records=65475, sbcg=867, baseline_mb=10, peak_live_mb=10, growth_mb=0, elapsed_ms=966. A memória é amostrada a cada 50.000 registros, então é indicação, não pico exato.
- **Pendências da Fase 6.2 (Itens NÃO validados):**
  - Ingestão remota (`download`, `syncFromRemote`, `WorkManager`) ainda não implementada (Fase 6.3 não autorizada).
### 2026-11-02 — Correções: AndroidManifest, SirosNormalizer, MainScreen (35 testes verdes)
- **AndroidManifest.xml:** Adicionada permissão `android.permission.INTERNET` (obrigatória para download do CSV SIROS).
- **SirosNormalizer (`parseDaysOfWeek`):** Corrigido mapeamento dos dias da semana para o formato real do CSV SIROS — seg=1, ter=2, qua=3, qui=4, sex=5, sab=6, dom=7 (0 = não opera). Antes comparava tudo com "1", funcionando apenas para segunda-feira.
- **Testes expandidos em `SirosNormalizerTest`:** +2 testes: (a) `testNormalizeOnlySaturday` — registro com apenas sab="6" gera voos apenas nos 4 sábados de outubro 2024; (b) `testNormalizeAllDaysOfWeek` — registro com "1".."7" gera 7 voos (um por dia) na semana 1–7/10/2024. Fixtures atualizadas para usar valores reais do CSV.
- **MainScreen:** Removida duplicata "Última atualização" dentro do Row do aviso (disclaimer), que quebrava linha e cortava espaço da lista de voos. Mantido o Text "Última atualização" acima do Row.
- **Resultado:** 35 testes unitários aprovados, 0 falhas (`BUILD SUCCESSFUL` em 15s). APK inalterado (12.6 MB).

## 2026-10-31 — Módulo SIROS e Testes Unitários Implementados
- Implementado `SirosCsvParser` com suporte a `ISO-8859-1` e remoção correta de cabeçalhos.
- Implementado `SirosNormalizer` para filtrar voos comerciais de passageiros de/para SBCG, validar vigências, dias da semana e fusos horários.
- Criados testes unitários (`SirosCsvParserTest` e `SirosNormalizerTest`) validados com sucesso.
- Compilação e `assembleDebug` executados com sucesso.

## 2026-10-31 — Etapa 2: Projeto Android Criado
- Projeto Android Kotlin + Jetpack Compose criado em `android-app/`.
- Tema escuro configurado como padrão (Material 3).
- Tela inicial simples com nome do aplicativo SKY WATCH CG.
- Gradle Wrapper 8.5 configurado.

## 2026-10-31 — Planejamento inicial
- Definido nome SKY WATCH CG.
- Definido foco em voos comerciais do aeroporto CGR/SBCG.
- Definido alerta padrão de 15 minutos.
- Identificadas Aena, AvioADSB e OpenSky como fontes candidatas.
- Registradas limitações iniciais e necessidade de validação de cobertura.