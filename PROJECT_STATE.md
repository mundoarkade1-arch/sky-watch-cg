# Estado do projeto

## Identidade
- Nome: SKY WATCH CG
- Plataforma: Android
- Idioma: pt-BR
- Aeroporto: Campo Grande, MS
- IATA: CGR
- ICAO: SBCG

## Decisões confirmadas
- Foco em aeronaves comerciais.
- Alertas padrão com 15 minutos de antecedência.
- Tema escuro.
- Prioridade para fontes gratuitas.
- Objetivo principal: ajudar o usuário a decidir quando ir observar pousos e decolagens.

## Estado técnico
- [x] Identificada página oficial de informações de voos da Aena para Campo Grande.
- [x] Identificada API pública AvioADSB para posições ADS-B.
- [x] Identificada API OpenSky como fonte alternativa de posições.
- [ ] Testar cobertura ADS-B efetiva na região de Campo Grande.
- [ ] Verificar método permitido e estável de obtenção dos dados Aena.
- [ ] Testar correspondência entre programação e aeronave detectada.
- [ ] Definir estratégia de atualização e cache.
- [ ] Criar repositório remoto GitHub.

## Bloqueios e riscos
- A página Aena não equivale a uma API pública documentada; a integração precisa ser validada e respeitar os termos.
- AvioADSB anônimo: 1 consulta a cada 10 segundos, 100 consultas/dia por rede, raio máximo 100 NM; licença CC BY-NC 4.0 para uso pessoal/pesquisa.
- OpenSky não fornece programação comercial nem atrasos; posições podem ter cobertura incompleta.
- Ainda não há teste de campo ou validação de dados reais para CGR.

## Próxima tarefa
Executar uma prova de conceito de leitura das fontes e registrar respostas, cobertura, timestamps e limitações sem construir ainda as telas finais.
