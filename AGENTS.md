# Instruções para agentes de IA

1. Leia README.md, PROJECT_STATE.md, ROADMAP.md e TESTING.md antes de alterar o projeto.
2. Não invente voos, horários, aeronaves, matrículas, posições ou resultados de testes.
3. Diferencie sempre:
   - horário programado/estimado (fonte aeroportuária);
   - posição ADS-B observada;
   - previsão calculada pelo aplicativo.
4. Não apresente uma aeronave como voo comercial identificado sem correspondência verificável.
5. Respeite limites, licenças, atribuição e termos de cada fonte de dados.
6. Nunca coloque tokens, chaves ou segredos no aplicativo Android, no Git ou em arquivos públicos.
7. Não faça scraping de páginas se isso contrariar os termos da fonte. Prefira API/documentação ou integração autorizada.
8. Antes de adicionar dependências, explique sua finalidade e confirme compatibilidade Android.
9. Faça alterações pequenas, teste-as e registre resultados em TESTING.md.
10. Atualize PROJECT_STATE.md, ROADMAP.md e CHANGELOG.md ao concluir uma tarefa.
11. O usuário é iniciante: forneça instruções simples, numeradas e sem presumir conhecimento de programação.
12. Nunca declare uma etapa concluída sem evidências reproduzíveis.
13. Arquitetura conceitual aprovada: Separação estrita entre dados planejados (SIROS/ANAC via CSV utilizando ID SIROS original e data específica do voo), status operacional em tempo real (Aena) e radar ADS-B experimental (AvioADSB). Fusos horários tratados por aeroporto (America/Sao_Paulo, America/Campo_Grande, etc.). Alertas locais via AlarmManager/WorkManager com tratamento de reinicialização do dispositivo.

## Regra permanente de autonomia, tentativas e comunicação

1. **Autonomia em primeiro lugar:** Trabalhe de forma autônoma (investigue erros, analise arquivos, aplique correções, execute testes e builds) sem depender do usuário para cada pequeno problema.
2. **Limite de três tentativas:** Tente resolver um erro em até 3 tentativas fundamentadas e com abordagens distintas. Se falhar, PARE e comunique o usuário.
3. **Comunicação obrigatória:** Avise o usuário em caso de bloqueio após 3 tentativas, indisponibilidade de serviços, necessidade de credenciais ou autorizações, risco de perda de alterações ou falhas no ambiente. Forneça relatório completo (o que aconteceu, investigações, tentativas, resultados).
4. **Não finja trabalhar:** É proibido afirmar que está executando tarefas em segundo plano sem processos ativos. Seja transparente sobre o estado real.
5. **Relatórios de andamento:** Ao concluir tarefas importantes, apresente resumo objetivo (arquivos modificados, implementação, testes, build, solução de problemas e próxima etapa).
6. **Proteção e continuidade:** Nunca apague implementações anteriores sem necessidade/autorização. Preserve alterações, atualize documentações e **nunca avance para uma nova etapa sem autorização explícita do usuário**.
