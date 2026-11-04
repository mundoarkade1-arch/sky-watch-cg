# Fontes candidatas de dados

## 1. SIROS / ANAC - Registros Vigentes CSV
Endpoint: https://siros.anac.gov.br/siros/registros/registros/registros.csv
- Gratuito, sem autenticação.
- Formato CSV (UTF-8 COM BOM `EF BB BF`, delimitador `;`). Nota: versões anteriores e documentação oficial referiam-se a Latin1/ISO-8859-1, mas o arquivo real baixado em 01/11/2026 possui BOM UTF-8.
- Contém voos planejados (HTA) em UTC.
- Endpoint REST via sas.anac.gov.br retorna CAPTCHA.

## 2. Aena Brasil — status operacional de voos (pesquisa em 04/11/2026)

Fontes oficiais consultadas:
- [Página do Aeroporto de Campo Grande (CGR)](https://www.aenabrasil.com.br/pt/aeroportos/aeroporto-de-campo-grande/index.html)
- [Página institucional da Aena Brasil](https://www.aenabrasil.com.br/pt/corporativo/sobre-nos.html)
- [Termos e Condições de Uso](https://www.aenabrasil.com.br/pt/corporativo/termocondicaouso.html)
- [Central de Atendimento ao Usuário](https://www.aenabrasil.com.br/pt/corporativo/centralatendimento.html) e [contatos dos aeroportos](https://www.aenabrasil.com.br/pt/corporativo/ContatoAena.html)
- [robots.txt](https://www.aenabrasil.com.br/robots.txt)
- [Índice do sitemap](https://www.aenabrasil.com.br/sites/sitemaps/sitemapsBrasil/sitemap_index.xml) e [sitemap Brasil](https://www.aenabrasil.com.br/sites/sitemaps/sitemapsBrasil/sitemap.xml)

Constatações verificadas:
- Aena Brasil identifica CGR como um dos aeroportos administrados pela BOAB; a mesma página distingue BOAB de ANB.
- A página pública de CGR consultada não expõe um painel de voos nem documentação de API. No sitemap oficial, páginas com o nome `Informacao-voos.html` aparecem para Campina Grande e Juazeiro do Norte, mas não para CGR. Isso não prova que inexista API privada ou outro canal de integração.
- Os Termos e Condições descrevem a informação de voos como baseada em dados operacionais confirmados e estimativas das companhias aéreas, mas declaram que a reprodução, transmissão, adaptação ou outra exploração do conteúdo do site depende de autorização prévia e escrita.
- Os mesmos termos identificam a pessoa jurídica ANB, enquanto a página institucional informa que Campo Grande pertence à BOAB. Portanto, não se presume que esses termos esclareçam todos os direitos/condições aplicáveis aos dados de CGR.
- O `robots.txt` desautoriza rastreamento de `/aena_cdp/*`. Não acessar caminhos internos, endpoints não documentados ou tentar contornar controles.

Conclusão e decisão:
- **Integração automatizada não autorizada/validada.** Não fazer scraping nem reutilizar dados da interface até obter confirmação escrita da entidade responsável por CGR, esclarecer a aplicabilidade dos termos e receber documentação, licença e limites de uso de uma API apropriada.
- Se não houver autorização/API, manter SIROS como fonte de planejamento e encaminhar o passageiro à companhia aérea/Aena para confirmação operacional. Não rotular horário planejado como status real.
- Canal oficial para solicitar orientação: Central de Atendimento ao Usuário; a página lista o contato do Aeroporto Internacional de Campo Grande como `(34) 2512-1803`. A central também publica `institucional@aenabrasil.com.br` para comunicações administrativas. Confirmar com a própria Aena o canal adequado para pedido técnico/jurídico e solicitar resposta escrita.
- Uma minuta pronta para iniciar esse pedido está em [`AENA_API_REQUEST.md`](./AENA_API_REQUEST.md); ela ainda precisa ser enviada pelo responsável pelo projeto e não constitui autorização.

## 3. Aena Espanha — nota de escopo

A página [Info vuelos](https://www.aena.es/es/infovuelos.html) diz que os horários são locais e baseados em dados operacionais confirmados e estimativas fornecidas pelas companhias aéreas, mas trata da operação espanhola. Ela não é uma fonte para voos do Aeroporto de Campo Grande e não autoriza reutilização dos dados da Aena Brasil.

## 4. OpenSky Network — candidata para observações ADS-B (pesquisa em 04/11/2026)

Documentação oficial consultada:
- [Documentação da API OpenSky](https://openskynetwork.github.io/opensky-api/)
- [REST API e consulta por bounding box](https://openskynetwork.github.io/opensky-api/rest.html)
- [Termos de uso referenciados pela documentação](https://opensky-network.org/about/terms-of-use) — a página respondeu HTTP 403 nesta consulta; os termos completos não foram verificados.

Constatações:
- A API documentada fornece state vectors em tempo real com campos ADS-B como endereço ICAO, callsign, posição e timestamp; posições podem estar ausentes/obsoletas quando não há atualização recente.
- A REST API documenta consulta de estado por bounding box WGS84 e menciona limites de taxa.
- A própria documentação descreve o uso da API como voltado a pesquisa e não comercial; não fornece horários de aeroporto, atraso ou status operacional que não seja derivável dos dados ADS-B.
- A documentação recomenda citar o trabalho OpenSky em publicações e materiais públicos que usem os dados. As exigências detalhadas de atribuição/licença não foram confirmadas por causa do HTTP 403.
- Não foi feita consulta de cobertura ao redor de CGR nesta etapa. Logo, não se afirma que haja receptores ou aeronaves visíveis em SBCG.

Decisão provisória:
- OpenSky é apenas uma candidata técnica, não uma fonte aprovada para integração. Antes de incluí-la no app ou distribuir a funcionalidade, confirmar os termos completos para o uso pretendido, as exigências de atribuição, os limites da API e a cobertura local.
- A arquitetura menciona AvioADSB, mas os materiais do projeto não identificam documentação oficial, endpoint ou termos. Não é possível confirmar o serviço nem sua cobertura sem uma referência canônica.
- Se a cobertura for nula ou intermitente, o radar deve mostrar “sem observações disponíveis”, não preencher posições fictícias. Observações ADS-B permanecem separadas de voos planejados SIROS e não serão associadas a um voo sem correspondência verificável.
