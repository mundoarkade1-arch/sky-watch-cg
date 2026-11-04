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
Projeto em desenvolvimento ativo. O MVP inclui grade SIROS, sincronização periódica, alertas locais e um radar ADS-B experimental com consulta manual à API ADSB.lol (raio de 50 NM), atribuição ODbL e observações mantidas separadas dos voos planejados. Alertas usam horários planejados SIROS, não confirmação operacional, com janela de 15–30 minutos e deduplicação. A regressão local mais recente executou 68 testes por variante (Debug e Release), sem falhas e com 1 teste ignorado por depender de CSV real; lint vital e builds Debug/Release passaram. O APK Debug `1.0.17` (`com.skywatch.cg.debug`) foi instalado no aparelho Android 14; abertura, navegação cartão→detalhes e retorno foram verificadas sem exceção fatal. A instalação dos testes instrumentados foi bloqueada pela verificação do Android, sem desativar proteções. O workflow de tags prepara um pré-lançamento Debug, não uma versão estável. O logcat e um teste automatizado confirmaram o reagendamento após `BOOT_COMPLETED`; a entrega de alerta real segue pendente por falta de voo SIROS elegível durante o teste. Cobertura ADS-B contínua não foi demonstrada e ADSB.lol exige contato antes de uso em produção. Detalhes em `TESTING.md` e `docs/API_SOURCES.md`.

## APK de prévia

[Baixar SKY WATCH CG 1.0.17 — prévia Debug](https://github.com/mundoarkade1-arch/sky-watch-cg/releases/tag/v1.0.17). O pacote `com.skywatch.cg.debug` é instalável em paralelo à versão Release. Esta prévia não é assinada para distribuição estável ou Play Store; devido à chave Debug do CI, instalar futuras prévias pode exigir desinstalar a anterior e apagar os dados locais.

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
