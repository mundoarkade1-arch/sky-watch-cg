# Arquitetura proposta

## Visão geral
Fontes externas (SIROS CSV / Aena / AvioADSB) -> Normalização local -> Armazenamento em Cache (Room Database) -> Aplicativo Android (Jetpack Compose) -> Alertas Locais (AlarmManager / WorkManager).

## Modelo conceitual de voo (Normalizado e Separado por Fontes)
- light_id (Chave única composta: ID SIROS original + Data específica do voo)
- siros_record_id (Identificador original do registro SIROS para deduplicação)
- light_number, irline
- origin, origin_timezone (ex: America/Sao_Paulo)
- destination, destination_timezone (ex: America/Campo_Grande)
- scheduled_departure_utc, scheduled_arrival_utc
- scheduled_departure_local, scheduled_arrival_local
- operational_status: UNKNOWN (padrão para dados puramente SIROS), CONFIRMED, DELAYED, CANCELED (via Aena)
- delay_minutes: 
ull quando não houver informação operacional confirmada
- last_sync_at: Data/hora da última sincronização geral da grade de voos
- status_updated_at: Data/hora da última atualização do status operacional
- 
adar_data: Posição ADS-B (AvioADSB experimental), altitude, velocidade, timestamp

## Regras arquiteturais confirmadas
1. **Fusos horários:** Cada aeroporto é convertido com base em sua respectiva timezone geográfica (sem fusos fixos globais).
2. **Status:** SIROS representa apenas planejamento (UNKNOWN por padrão); confirmação operacional requer fonte em tempo real.
3. **Alertas locais:** Gerenciados via AlarmManager/WorkManager no próprio dispositivo, com tratamento de reinicialização (BOOT_COMPLETED) para reagendamento automático.
4. **Segurança:** Sem chaves ou segredos no aplicativo.
