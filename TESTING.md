# Plano de testes

## Regras
- Registrar data/hora UTC e horário local (Campo Grande, UTC-4).
- Guardar URL/endpoint consultado, código HTTP, latência e quantidade de registros.
- Não publicar chaves ou tokens.
- Não usar dados simulados como se fossem reais.
- Anotar separadamente falhas da fonte, falhas de rede e ausência legítima de aeronaves.

## Testes pendentes
| ID | Teste | Resultado |
|---|---|---|
| T-001 | Consultar painel oficial Aena para CGR | Pendente |
| T-002 | Consultar ADS-B em torno de SBCG | Pendente |
| T-003 | Verificar cobertura em horários diferentes | Pendente |
| T-004 | Correlacionar callsign e voo programado | Pendente |
| T-005 | Testar limites e cabeçalhos de quota | Pendente |

## Critério mínimo de aprovação da Etapa 1
- Fonte de programação identificada e acesso permitido documentado.
- Pelo menos uma fonte ADS-B respondendo com dados ou ausência de cobertura comprovada.
- Campos e frequência de atualização conhecidos.
- Limites/licenças documentados.
- Limitações visíveis ao usuário final.
