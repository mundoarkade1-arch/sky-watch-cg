# SKY WATCH CG

Aplicativo Android para acompanhar voos comerciais de chegada e partida do Aeroporto Internacional de Campo Grande (CGR / SBCG), com radar ADS-B e alertas.

## Objetivo inicial
Exibir voos planejados para Campo Grande com fontes documentadas, distinguindo planejamento, status operacional e observações ADS-B.

## Escopo aprovado
- Plataforma: Android
- Idioma: português brasileiro
- Aeroporto: Campo Grande (CGR / SBCG)
- Foco: voos comerciais
- Antecedência padrão dos alertas: 15 minutos
- Interface: tema escuro
- Prioridade: fontes gratuitas

## Estado atual
Projeto em desenvolvimento ativo. O MVP Android, a sincronização SIROS, a sincronização periódica e a base de notificações locais estão implementados. Alertas de planejamento são avaliados entre 15 e 30 minutos antes, com deduplicação local por voo/evento. A suíte unitária mais recente contabilizou 56 testes, com 0 falhas e 1 ignorado por depender de CSV real. Treze testes instrumentados passaram em aparelho físico Android 14. O APK Debug usa identificador separado (`.debug`), sem alterar o identificador Release. CI #47 a #49 passaram; o CI das alterações Room v6 será executado após o envio. A validação física e o teste manual de notificação estão documentados em `TESTING.md`. Status operacional Aena e radar ADS-B continuam planejados e dependem de fontes autorizadas e verificáveis.

## Estrutura
- `AGENTS.md`: regras para agentes de IA
- `PROJECT_STATE.md`: estado atual e decisões
- `ROADMAP.md`: etapas do projeto
- `CHANGELOG.md`: histórico
- `TESTING.md`: plano e resultados de testes
- `docs/API_SOURCES.md`: fontes e limitações
- `docs/ARCHITECTURE.md`: arquitetura proposta
- `android-app/`: aplicativo Android Jetpack Compose


## Aviso
Este aplicativo é um projeto de observação/entretenimento. Não é ferramenta de navegação, controle de tráfego aéreo ou segurança operacional. Horários e posições podem estar incompletos, atrasados ou indisponíveis.
