# Arquitetura proposta

## Visão geral
Fontes externas -> backend de integração -> modelo normalizado -> aplicativo Android -> notificações.

## Modelo conceitual de voo
- flight_number: número do voo, se fornecido por fonte confiável
- airline: companhia, se disponível
- origin / destination
- scheduled_time
- estimated_time
- status_source
- callsign
- icao24
- aircraft_type
- registration
- latitude / longitude
- altitude
- ground_speed
- track
- observed_at
- data_source
- confidence / match_method

Campos ausentes devem permanecer nulos. Não preencher com suposições.

## Regras importantes
- Programação não é posição real.
- Posição ADS-B não garante identificação comercial.
- ETA calculada deve ser rotulada como estimativa própria.
- Dados antigos devem ser marcados como desatualizados.
- Segredos ficam somente no backend.
- Aplicativo deve funcionar com cache e informar quando não houver conexão.

## Tecnologias candidatas
- Android: Kotlin + Jetpack Compose + Room.
- Backend: Cloudflare Workers (a confirmar após validar necessidades e limites).
- Push: Firebase Cloud Messaging (a confirmar).
- Mapa: avaliar OpenStreetMap e biblioteca compatível com licença/termos.
