# Fontes candidatas de dados

## 1. Aena Brasil — painel oficial
Página: https://www.aenabrasil.com.br/pt/aeroportos/aeroporto-de-campo-grande/informacoes-de-voos.html

Uso pretendido:
- Voos de chegada e partida.
- Número do voo, origem/destino, horário estimado e estado.

Status:
- A página oficial foi localizada e apresenta campos de programação/status.
- Não foi confirmada uma API pública documentada.
- Antes de automatizar a coleta, investigar termos, mecanismo oficial e estabilidade. Não assumir que scraping é permitido.

## 2. AvioADSB — posições ADS-B
Documentação: https://avioadsb.org/docs/api
Swagger: https://avioadsb.org/v1/docs

Uso pretendido:
- Posição ao vivo, callsign, altitude, velocidade, direção e identificação ICAO quando disponíveis.
- Consulta geográfica por ponto e raio.

Limites publicados:
- Anônimo: 1 requisição a cada 10 segundos e 100 por dia por rede.
- Raio máximo anônimo: 100 NM.
- Contribuidor: até 10.000 requisições/mês e 1 requisição/segundo enquanto o receptor estiver ativo conforme as regras do serviço.
- Licença: CC BY-NC 4.0; uso pessoal/pesquisa. Uso comercial exige contato.
- Atribuição visível: “Data: AvioADSB (CC BY-NC 4.0)” com link para https://avioadsb.org.
- Não é fonte certificada para segurança operacional.

## 3. OpenSky Network
Documentação: https://openskynetwork.github.io/opensky-api/
REST: https://github.com/openskynetwork/opensky-api/blob/master/docs/free/rest.rst

Uso pretendido:
- Posições ADS-B/Mode S e estado de aeronaves.
- Alternativa para rastreamento e comparação.

Limitações:
- Não fornece programação comercial de aeroportos nem atrasos.
- Endpoints de chegadas/partidas históricas são atualizados em lote, não são previsão em tempo real.
- Há limites de créditos e autenticação OAuth2 para uso autenticado.
- Verificar termos de uso e política atual antes da implementação.

## Estratégia
Aena é candidata para programação; ADS-B é candidato para posição real. O backend deve manter as duas informações separadas e correlacionar apenas quando houver evidência suficiente.
