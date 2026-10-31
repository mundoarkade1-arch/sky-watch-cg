# Roadmap

## Etapa 1 — Validação das fontes
- [ ] Investigar acesso autorizado ao painel Aena.
- [ ] Consultar posições ADS-B para a área do aeroporto.
- [ ] Medir disponibilidade e cobertura em diferentes horários.
- [ ] Avaliar OpenSky como fallback.
- [ ] Relacionar callsign/horário/rota sem inferências frágeis.
- [ ] Documentar limites e licenças.

## Etapa 2 — MVP Android
- [ ] Criar projeto Kotlin + Jetpack Compose.
- [ ] Tela de chegadas.
- [ ] Tela de partidas.
- [ ] Estado do voo e horário estimado.
- [ ] Indicador de última atualização e indisponibilidade.

## Etapa 3 — Radar
- [ ] Mapa com aeronaves ADS-B.
- [ ] Detalhes de altitude, velocidade, direção e callsign.
- [ ] Identificação do tipo/matrícula quando disponível.
- [ ] Filtros e tratamento de dados ausentes.

## Etapa 4 — Alertas
- [ ] Backend para monitoramento e envio push.
- [ ] Alertas configuráveis, padrão 15 minutos.
- [ ] Deduplicação e cancelamento de alertas obsoletos.
- [ ] Mensagens claras quando previsão for incerta.

## Etapa 5 — Qualidade e distribuição
- [ ] Testes em Android real.
- [ ] Testes de bateria, rede e notificações.
- [ ] Política de privacidade e atribuições.
- [ ] Gerar APK de teste.
