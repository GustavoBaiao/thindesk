# Thindesk — Dashboard Administrativo

Dashboard integrado ao sistema de chamados Thindesk, desenvolvido para acompanhar
a operação por meio de indicadores, gráficos e rankings. A interface consome
uma API própria, implementada com Spring Boot e integrada ao MongoDB Atlas.

## Funcionalidades

- Total de chamados e contagem por status: aberto, em andamento, concluído e cancelado.
- Gráfico de atendimentos por dia, semana ou mês.
- Distribuição dos chamados por status, com quantidades e percentuais.
- Ranking dos cinco tipos de chamados mais atendidos.
- Filtros por período, tipo e status dos indicadores.
- Atualização manual, limpeza dos filtros e horário da última atualização.
- Tratamento de carregamento, ausência de dados, falhas de consulta e sessão expirada.
- Acesso autenticado com Spring Security.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 23 |
| Backend | Spring Boot 3.4.0, Spring Web e Spring Data MongoDB |
| Segurança | Spring Security, login por formulário e sessão |
| Banco de dados | MongoDB Atlas |
| Interface | Thymeleaf, HTML, CSS, JavaScript e Bootstrap |
| Gráficos | HTML e CSS, sem biblioteca externa de gráficos |
| Build e testes | Maven, JUnit e Spring Test / MockMvc |

## Executar localmente

### Pré-requisitos

- JDK 23, com `JAVA_HOME` e `PATH` configurados.
- Acesso à internet para baixar as dependências na primeira execução.
- Cluster MongoDB Atlas disponível, usuário de banco com as permissões necessárias
  e endereço IP autorizado no Network Access do Atlas.

O projeto inclui Maven Wrapper; não é necessário instalar o Maven separadamente.

### Configuração

A aplicação lê a conexão pela variável `MONGODB_URI`. O banco configurado é
`thindesk`. Não coloque credenciais reais em arquivos versionados.

Exemplo para PowerShell, substituindo os valores da URI pela conexão do seu Atlas:

```powershell
$env:MONGODB_URI = 'mongodb+srv://USUARIO:SENHA@HOST_DO_CLUSTER/?retryWrites=true&w=majority'
```

O arquivo `application.properties` contém credenciais de desenvolvimento para
o login. Para usar outras credenciais sem editar o arquivo, defina:

```powershell
$env:SPRING_SECURITY_USER_NAME = 'seu_usuario'
$env:SPRING_SECURITY_USER_PASSWORD = 'sua_senha'
```

No IntelliJ, informe essas variáveis em **Run → Edit Configurations → Environment
variables**, selecione o JDK 23 e execute `com.pfc.ThindeskApplication`.
Um arquivo `.env` não é carregado automaticamente pela aplicação.

### Inicialização pelo terminal

Na raiz do projeto, em Windows/PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS, com as variáveis equivalentes configuradas:

```bash
bash mvnw spring-boot:run
```

Abra [http://localhost:8080/dashboard](http://localhost:8080/dashboard) e faça login.
A página inicial `/` também apresenta o dashboard.

O MongoDB precisa estar acessível: a inicialização consulta e cria as coleções
necessárias. Uma falha de conexão pode impedir que a aplicação permaneça em execução.

## Regras dos indicadores

| Recurso | Critério |
|---|---|
| Indicadores gerais e situação | Período pela `dataAbertura` |
| Atendimentos e ranking | Status atual `CONCLUIDO`, com `dataConclusao` preenchida |
| Período dos atendimentos | Pela `dataConclusao` |
| Agrupamento semanal | Semana iniciada na segunda-feira |
| Ranking | Até cinco tipos, quantidade decrescente e desempate por nome |
| Datas dos filtros | Incluem os dias inicial e final completos |

O filtro de status afeta somente os indicadores e a distribuição por status.
O gráfico de atendimentos e o ranking sempre consideram apenas concluídos.
Sem período informado, considera-se todo o histórico. Intervalos sem atendimentos
são omitidos no gráfico.

Na criação, o chamado recebe status `ABERTO` e data de abertura. A conclusão
preenche `dataConclusao`; editar um chamado que continua concluído preserva essa
data. Sair de `CONCLUIDO` limpa a conclusão, preservando a abertura.

## API do dashboard

Todos os endpoints abaixo usam GET e exigem uma sessão autenticada.

| Endpoint | Parâmetros opcionais |
|---|---|
| `/api/dashboard/indicadores` | `dataInicio`, `dataFim`, `tipo`, `status` |
| `/api/dashboard/atendidos` | `dataInicio`, `dataFim`, `tipo`, `agrupamento` |
| `/api/dashboard/tipos-mais-atendidos` | `dataInicio`, `dataFim`, `tipo` |
| `/api/dashboard/tipos` | Nenhum |

Datas: `YYYY-MM-DD`. Agrupamento: `DIA` (padrão), `SEMANA` ou `MES`.
Status: `ABERTO`, `EM_ANDAMENTO`, `CONCLUIDO` ou `CANCELADO`.

Exemplo:

```http
GET /api/dashboard/atendidos?dataInicio=2026-09-01&dataFim=2026-11-30&agrupamento=MES
```

No Postman, execute primeiro **POST `/login`**, com `username` e `password`
em **x-www-form-urlencoded**, e mantenha o cookie de sessão nas próximas chamadas.
O login retorna um redirecionamento; confira o destino para distinguir sucesso
de falha. Sem sessão, a API pode redirecionar para a página de login.

Parâmetros inválidos retornam `400`. Consultas sem resultados retornam contagens
zeradas ou listas vazias. Consulte a [documentação da API](docs/dashboard-api.md)
para exemplos de respostas, regras e limitações.

## Dados de demonstração

O arquivo [chamados-demo.json](docs/dados/chamados-demo.json) contém 40 registros
fictícios em MongoDB Extended JSON, com datas de setembro a novembro de 2026.
Esses dados não são inseridos automaticamente ao iniciar a aplicação.

| Status | Quantidade |
|---|---:|
| Aberto | 8 |
| Em andamento | 8 |
| Concluído | 20 |
| Cancelado | 4 |
| **Total** | **40** |

Ranking esperado sem filtros: Hardware (7), Rede (5), Software (4), Impressora (2)
e Acesso (1). Telefonia possui um concluído e fica fora do top 5 pelo desempate.

Veja as [instruções de inserção no Atlas](docs/dados/README.md). Preserve os campos
de data como BSON Date; não importe datas como texto simples. Como os IDs são
fixos, inserir novamente os mesmos documentos causa conflito de chave.

## Testes

Em Windows/PowerShell:

```powershell
.\mvnw.cmd test
```

Em Linux/macOS:

```bash
bash mvnw test
```

A última execução local passou com **11 testes, sem falhas ou erros**:

- Contagens, respostas vazias e critérios das consultas.
- Limites de período, agrupamentos por dia/semana/mês e virada de ano.
- Limite e desempate do ranking e normalização dos tipos.
- Rotas, parâmetros e respostas `400` para filtros inválidos.
- Seleção e renderização da página Thymeleaf, sidebar e URLs dos recursos.

Os testes usam MongoOperations simulado e MockMvc; não exigem conexão com o Atlas
nem validam o login. A API também foi exercitada manualmente no Postman e a tela
foi conferida em desktop com os 40 registros fictícios. Isso não representa
cobertura completa de todos os filtros, erros de interface ou dispositivos.
Os relatórios locais ficam em `target/surefire-reports/`.

## Estrutura principal

```text
src/main/java/com/pfc/
  SecurityConfig.java
  MongoInitConfig.java
  thindesk/
    controller/       # Rotas de páginas e APIs
    entity/           # Modelo do chamado e enum de status
    repository/       # Acesso às coleções MongoDB
    service/          # Regras do chamado e consultas do dashboard
src/main/resources/
  application.properties
  templates/dashboard.html
  static/css/dashboard.css
  static/js/dashboard.js
src/test/java/         # Testes da API e da view
docs/
  dashboard-api.md
  dados/              # Dados fictícios e instruções
```

## Organização da entrega

Branches combinadas: `main`, `develop` e `feature/dashboard-administrativo`.
Esta funcionalidade é desenvolvida na branch `feature/dashboard-administrativo`.

O dashboard e a API estão implementados. A documentação detalhada de requisitos,
a consolidação das evidências e o vídeo de até três minutos serão preparados
na próxima etapa. Documento e vídeo devem estar no GitHub; no Teams será enviado
apenas o link do repositório.

### Limitações atuais

- Consultas filtram e projetam os campos no MongoDB, mas agrupam os resultados
  em memória no Java. Para grandes volumes, avaliar agregações no banco e índices.
- As rotas exigem autenticação, mas ainda não distinguem um perfil administrativo.
- O login e as configurações atuais são voltados ao ambiente de desenvolvimento.
- As datas existentes usam `LocalDateTime`; mantenha o fuso da aplicação consistente
  com o utilizado na gravação dos dados (America/Sao_Paulo).
