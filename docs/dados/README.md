# Dados fictícios do dashboard

`chamados-demo.json` contém 40 documentos MongoDB Extended JSON. Os IDs são
fixos para impedir uma segunda inserção acidental dos mesmos registros.
As datas são BSON Date (`$date`), compatíveis com LocalDateTime da aplicação.
Os horários foram preparados no fuso America/Sao_Paulo.

## Substituição manual no Atlas

1. No Data Explorer, abra exatamente `thindesk.chamados`.
2. Com filtro `{}`, confirme que são os dois chamados de teste existentes.
   Exclua somente esses dois documentos. Preserve a coleção e os outros bancos.
3. Abra `chamados-demo.json` e copie todo o conteúdo, incluindo os colchetes.
4. Use Add Data → Insert Document → JSON View. Substitua o conteúdo do editor
   pelo array e clique em Insert. O Atlas aceita vários documentos nesse formato.
5. Com filtro `{}`, confira 40 documentos. No dashboard, clique Limpar para
   remover filtros antigos e atualizar.

Não use POST `/api/chamados` para importar este arquivo: a criação na API
força ABERTO e a data atual, impedindo a preservação do histórico fictício.

## Resultado esperado sem filtros

| Indicador | Quantidade |
|---|---:|
| Total | 40 |
| ABERTO | 8 |
| EM_ANDAMENTO | 8 |
| CONCLUIDO | 20 |
| CANCELADO | 4 |

Ranking: Hardware 7; Rede 5; Software 4; Impressora 2; Acesso 1.
Telefonia tem 1 concluído, mas fica fora do top 5 pelo desempate por nome.
Os atendimentos somam 20 e estão distribuídos entre setembro, outubro e
novembro de 2026. Períodos filtrados podem apresentar totais diferentes.

Preparação e validação estrutural realizadas localmente. A exclusão dos
documentos e a inserção no Atlas ainda dependem da execução manual desses passos.
