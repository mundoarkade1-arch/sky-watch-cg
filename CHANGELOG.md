# Changelog
## 2026-11-04 — Validação focada e correção do wrapper Windows
- Reexecutada a suíte completa: 53 testes contabilizados, 0 falhas, 0 erros e 1 ignorado; `assembleDebug` e os 11 testes isolados de `FlightListViewModelTest` passaram. CI #41 e #42 concluídos com sucesso.
- Corrigido `android-app/gradlew.bat` para preservar o código de saída real do Gradle, em vez de retornar falha após builds aprovados.
- O teste físico de notificações após reinicialização continua pendente; não havia `adb` nem aparelho detectado no ambiente.
- Preparada em `docs/AENA_API_REQUEST.md` uma minuta de pedido de orientação e autorização; ela ainda não foi enviada.
- Corrigido o fixture `FlightDaoTest` para incluir `arrivalDate`; compilação instrumentada passou. A execução no aparelho Android 14 foi bloqueada por conflito de assinatura com o app já instalado; nenhuma instalação/dado foi removido.
- Isolado o identificador do build Debug como `com.skywatch.cg.debug`, preservando o identificador Release. A tentativa no aparelho foi recusada pelo Android (`INSTALL_FAILED_USER_RESTRICTED`); os CI #43 e #44 passaram.

## 2026-11-04 — Pesquisa de fonte Aena e validação manual de alertas
- Documentadas as páginas, sitemap, `robots.txt` e termos oficiais consultados para Aena Brasil. Não foi encontrada documentação pública de uma API de status para CGR; os termos consultados restringem reutilização sem autorização escrita e a entidade indicada nos termos não é a mesma identificada para a concessão de CGR.
- Sem scraping nem integração: solicitar esclarecimento/autorização escrita e documentação apropriada à entidade responsável por CGR.
- Incluídos na documentação os canais oficiais de atendimento da Aena para iniciar o pedido de orientação e autorização por escrito.
- Adicionado roteiro para teste real de notificações após reinicialização. O roteiro explicita a janela atual de até 2 horas, a restrição de rede e a natureza não exata da periodicidade do WorkManager.

## 2026-11-04 — Alinhamento da documentação do projeto
- Atualizados README e roadmap para refletir o estado implementado e validado: sincronização SIROS, infraestrutura de notificações locais, suíte unitária e CI #39 aprovado.
- Removidas as indicações contraditórias de que as Fases 6.2–6.4 e o refresh remoto ainda não estavam implementados.
- Registrados como próximos itens a investigação de uma fonte autorizada para Aena e a validação manual de alertas após reinicialização do dispositivo.
- Esclarecida a contagem da suíte local: 53 testes contabilizados, 52 executados sem falhas e 1 ignorado por depender de CSV real.

## 2026-11-04 — Estabilização da atualização e da tela Sobre
- O `FlightListViewModel` mantém o estado de atualização enquanto os fluxos do Room emitem mudanças, evita atualizações remotas simultâneas e encerra o estado de carregamento em sucesso ou falha. A falha preserva os voos em cache; sem cache, apresenta erro.
- Testes de atualização passaram a usar uma fonte remota falsa controlada pelo teste, sem depender de rede ou do endpoint SIROS.
- A tela Sobre passou a usar o ícone acessível de voltar, as cores do tema e conteúdo rolável em telas menores.
- Validação local em 18:59 UTC / 14:59 Campo Grande: `testDebugUnitTest` (53 testes, 0 falhas, 0 erros, 1 ignorado) e `assembleDebug` concluídos com sucesso.

## 2026-11-03 — Redesenho Visual Completo (Partes 1-4)
### Parte 1 - Tema e Fontes
- Novo tema escuro "painel de aeroporto" (`SkyWatchTheme`) com cores: fundo `#0A0F1C`, cartão `#121A2B`, borda `#1E2A42`, texto `#F1F5F9`, secundário `#8A97B1`, azul-céu `#38BDF8` (chegadas), âmbar `#FBBF24` (partidas).
- Fontes Google Fonts via `androidx.compose.ui:ui-text-google-fonts:1.6.0`: **Manrope** para textos, **Space Grotesk** para horários/números.
- Fallback para fonte do sistema se provedor indisponível.
- `MainActivity` atualizado para usar `SkyWatchTheme` e status bar transparente (edge-to-edge).

### Parte 2 - Tela Principal
- Cabeçalho: "CAMPO GRANDE · SBCG" (maiúsculas, espaçado), título dinâmico "Voos de hoje" ou dia/semana/data, botão de refresh quadrado 44dp.
- Faixa de datas deslizante (chips) de ontem a +14 dias; selecionado em azul-céu com texto escuro.
- Segmentos "Chegadas | Partidas" substituindo abas; fundo `#121A2B`, selecionado em `#1C2842`.
- Cartão "PRÓXIMO VOO" (só hoje): horário 38sp, "Cia nº · Cidade (código)", contagem regressiva "35 min / para chegar".
- Lista em duas seções: "MAIS TARDE" e "JÁ PASSARAM" (opacidade 50%).
- Cards: horário local 24sp Space Grotesk, "CIA 1234" negrito, "Cidade (CÓD)" secundário, etiqueta "Programado".
- Rodapé SIROS discreto, sem aviso vermelho no topo.
- Barra inferior só para telas existentes: "Voos" e "Sobre".
- Estados: carregando (esqueletos animados 200ms), vazio (ícone + texto), erro (botão "Tentar de novo").

### Parte 3 - Tela de Detalhe
- Horário gigante 48sp, rota "GRU → CGR" com nomes de cidades.
- Linhas de informação: horário local, UTC, aeronave, status SIROS, fonte.
- Botão "Seguir voo" (só se existir); aviso "Sem confirmação em tempo real".

### Parte 4 - Ícone do App
- Drawables: `ic_launcher_background.xml` (fundo `#0A0F1C`), `ic_launcher_foreground.xml` (anel radar azul-céu suave + avião `#38BDF8` + ponto âmbar `#FBBF24`).
- Mipmaps adaptativos `ic_launcher.xml` / `ic_launcher_round.xml` para anydpi-v26 e anydpi-v24.
- `AndroidManifest.xml`: adicionados `android:icon` e `android:roundIcon`.

### Dependências
- Adicionada `androidx.compose.ui:ui-text-google-fonts:1.6.0` (autorizada única dependência nova).

## 2026-11-02 — Correção e Conclusão da Etapa A (36 testes verdes)
- **Correção em `FlightListViewModelTest` (`testRefreshStateEmitsRefreshing`):** O teste falhava inicialmente por tentar ler o estado logo após `viewModel.refresh()` sem ceder a execução à corrotina, além de tentar acessar a rede real. A solução pontual consistiu em injetar uma fonte remota simulada (`SirosRemoteSource` apontando para `http://localhost:0/dummy.csv`) via o construtor do `SirosRepository`, permitindo que o teste valide a emissão do estado `Refreshing` e a conclusão bem-sucedida sem dependência de rede externa.
- **Resultado Gradle (`testDebugUnitTest`):** 36 testes executados, 0 falhas, 0 erros (`BUILD SUCCESSFUL`). Timestamp do XML atualizado e confirmado.
- **Git Amend:** Commit local `ea18f99` atualizado via `git commit --amend` e enviado com sucesso (`git push origin main`). Nenhum `push --force` foi necessário.

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