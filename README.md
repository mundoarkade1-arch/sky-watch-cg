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
Projeto em desenvolvimento ativo. O MVP Android, a sincronização SIROS, a sincronização periódica e a infraestrutura de notificações locais estão implementados. Alertas usam horários planejados SIROS (não confirmação operacional), com janela de 15–30 minutos e deduplicação por voo/evento. A suíte unitária contabiliza 57 testes, 0 falhas e 1 ignorado por depender de CSV real; 13 testes instrumentados passaram em aparelho Android 14. O APK Debug tem identificador separado (`.debug`). CI #47 a #51 passaram. O reagendamento após `BOOT_COMPLETED` tem teste automatizado; o teste real de notificação segue pendente porque não havia voo SIROS elegível. A avaliação inicial do radar encontrou a API OpenSky como candidata, mas uso, termos completos e cobertura de CGR ainda precisam ser confirmados. Detalhes em `TESTING.md` e `docs/API_SOURCES.md`.

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
