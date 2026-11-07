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
Projeto em desenvolvimento ativo. O MVP inclui grade SIROS, sincronização periódica, alertas locais e radar ADS-B com consulta manual à API ADSB.lol (raio de 50 NM), atribuição ODbL e observações separadas do planejamento. A tela principal reúne pousos e decolagens numa agenda cronológica, oferece filtros e exibe horários locais de Campo Grande; uma faixa de 3 horas destaca a maior concentração prevista de movimentos. A lista inclui voos de passageiros ativos encontrados no SIROS para CGR e não representa necessariamente todos os movimentos reais do aeroporto. Os horários e a faixa recomendada são planejados, não confirmação operacional. Alertas SIROS usam a mesma conversão de horário local, com janela de 15–30 minutos e deduplicação.

O repositório GitHub agora é público para permitir que o app consulte releases sem token. O verificador automático e o botão "Verificar atualização" consultam a release mais recente e abrem o APK publicado quando houver uma versão superior.

A validação local mais recente executou a suíte Debug sem falhas, com 1 teste ignorado por depender de CSV real; `assembleDebug` também passou. A release estável mais recente é `v1.0.22`. No aparelho Android 14, o app Debug foi instalado e aberto sem emulador. A entrega ponta a ponta de alerta real segue pendente por falta de voo SIROS elegível durante o teste. Cobertura ADS-B contínua não foi demonstrada e ADSB.lol exige contato antes de uso em produção. Detalhes em `TESTING.md` e `docs/API_SOURCES.md`.

## APK de prévia

[Baixar SKY WATCH CG 1.0.18 — prévia Debug](https://github.com/mundoarkade1-arch/sky-watch-cg/releases/tag/v1.0.18). O pacote `com.skywatch.cg.debug` é instalável em paralelo à versão Release. Esta prévia não é assinada para distribuição estável ou Play Store; devido à chave Debug do CI, instalar futuras prévias pode exigir desinstalar a anterior e apagar os dados locais.

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
