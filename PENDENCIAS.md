# PENDENCIAS.md

## Atualização 2026-11-23

- A bússola foi implementada e testada matematicamente; falta validar o comportamento visual em aparelho físico.
- A contagem de lint legado será reavaliada após o `lintDebug` desta rodada. Problemas em arquivos tocados serão corrigidos; problemas antigos permanecerão documentados.
- Lint reavaliado: 180 erros legados foram congelados em `android-app/app/lint-baseline.xml`; `lintDebug` passou e não há erros nos arquivos novos do radar.
- A versão 1.0.25 ainda precisa de validação visual em aparelho físico e publicação pelo fluxo GitHub.

## Auditoria de radar - 2026-11-23

- A orientação pelo sensor de rotação do celular e a validação em aparelho físico não foram concluídas nesta rodada.
- O lintDebug existente falha com 200 erros e 81 avisos legados; o primeiro é `DayStrip.kt:30` usando `LocalDate.now` sem anotação de API. O `assembleDebug` e os 142 testes unitários passaram.
- Não houve dados ADS-B reais suficientes para comprovar cobertura contínua nem alerta comercial em campo.

Lista de pendências e itens futuros do projeto **SKY WATCH CG**:

1. **Fase 0 Concluída**:
   - Limpeza de logs antigos e ajuste do `.gitignore` para ignorar logs e arquivos temporários CSV.
   - Correção do normalizador de datas do SIROS (`SirosNormalizer`) para a faixa `[referenceDate - 1, referenceDate + 45]`.
   - Ajuste das asserções do teste `testSyncFromRemoteSuccess`.
   - Documentação revisada e atualizada.

2. **Fase 1 em Diante (Próximos Passos conforme solicitação do usuário)**:
   - Permissão `POST_NOTIFICATIONS` no AndroidManifest e solicitação moderna.
   - Tratamento de chegada no dia seguinte e fuso horário `America/Campo_Grande`.
   - Migração Room versão 3 com script manual.
   - MVP UI, Alertas, Aena e Radar ADS-B.
