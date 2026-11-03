# Progresso do Projeto SKY WATCH CG

## Fase 0 - Limpeza e verdade
- [x] Apagar gradle-output-*.log da raiz e siros_temp_*.csv (já limpos no repositório)
- [x] Ajustar .gitignore (logs, build-logs/, siros_temp_*.csv)
- [x] Garantir que testes não criem arquivos temporários no projeto
- [x] Rodar testes atuais e anotar resultado real
- [x] Corrigir teste testSyncFromRemoteSuccess
- [x] Corrigir documentação (ROADMAP, README, RETOMADA, TESTING, PENDENCIAS)

## Fase 1 - Corrigir erros
- [x] POST_NOTIFICATIONS no AndroidManifest e ActivityResult API no MainActivity
- [x] SirosNormalizer: faixa de datas referenceDate - 1 até referenceDate + 45
- [x] Data de chegada: se arrival UTC < departure UTC, arrivalDate = flightDate + 1
- [x] FlightDisplayUtils usa arrivalDate para chegadas
- [x] Room Database versão 3 com Migration manual (arrivalDate, CHECK id=1 unificado)
- [x] BootReceiver reagenda SirosSyncWorker e FlightAlertWorker
- [x] FlightAlertWorker sem constraint de rede, usa SirosRepository unificado

## Fase 2 - Telas do MVP (próxima)
