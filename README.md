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
Projeto em desenvolvimento ativo. O MVP Android, a sincronização SIROS, a sincronização periódica e a base de notificações locais estão implementados. A validação local mais recente executou 53 testes: 52 sem falhas e 1 ignorado por depender de CSV real; a compilação dos testes instrumentados também passou. O APK Debug foi gerado. Os CI #43 e #44 concluíram com sucesso. O pacote Debug é separado (`.debug`) para permitir testes sem substituir a instalação Release. A execução instrumentada e o teste manual de notificações no aparelho aguardam autorização de instalação no dispositivo. Status operacional Aena e radar ADS-B continuam planejados e dependem de fontes autorizadas e verificáveis.

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
