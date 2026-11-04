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
Projeto em desenvolvimento ativo. O MVP inclui grade SIROS, sincronização periódica, alertas locais e um radar ADS-B experimental com consulta manual à API ADSB.lol (raio de 50 NM), atribuição ODbL e observações mantidas separadas dos voos planejados. Alertas usam horários planejados SIROS, não confirmação operacional, com janela de 15–30 minutos e deduplicação. A suíte unitária contabiliza 63 testes, 0 falhas e 1 ignorado por depender de CSV real; 13 testes instrumentados passaram em aparelho Android 14. O APK Debug tem identificador separado (`.debug`). CI #47 a #53 passaram; a CI do protótipo radar está pendente. O logcat e um teste automatizado confirmaram o reagendamento após `BOOT_COMPLETED`; a entrega de alerta real segue pendente por falta de voo SIROS elegível durante o teste. A tela Radar abriu no aparelho físico e exibiu o estado vazio sem crash; cobertura contínua não foi demonstrada. ADSB.lol exige contato antes de uso em produção. Detalhes em `TESTING.md` e `docs/API_SOURCES.md`.

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
