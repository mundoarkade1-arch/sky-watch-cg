# Fontes candidatas de dados

## 1. SIROS / ANAC - Registros Vigentes CSV
Endpoint: https://siros.anac.gov.br/siros/registros/registros/registros.csv
- Gratuito, sem autenticação.
- Formato CSV (UTF-8 COM BOM `EF BB BF`, delimitador `;`). Nota: versões anteriores e documentação oficial referiam-se a Latin1/ISO-8859-1, mas o arquivo real baixado em 01/11/2026 possui BOM UTF-8.
- Contém voos planejados (HTA) em UTC.
- Endpoint REST via sas.anac.gov.br retorna CAPTCHA.
