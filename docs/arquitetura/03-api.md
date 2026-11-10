# API — Referência de Endpoints

Base URL: `http://localhost:8080` (dev) ou a URL do serviço no Railway
(produção). Todas as rotas abaixo começam com esse prefixo.

Formato de dados: JSON, `snake_case` em todas as chaves (configuração global
do Jackson). Autenticação: `Authorization: Bearer <token>` quando marcado
como protegido.

## `/api/auth` — Autenticação (pública)

| Método | Rota | Corpo | Resposta | Descrição |
|---|---|---|---|---|
| POST | `/api/auth/register` | `{name, email, password, phone}` | `{message, email}` | Cria a conta (não verificada, sem login automático) |
| POST | `/api/auth/send-verification-code` | `{email, method}` (`method`: `"email"` ou `"sms"`) | `{message}` | Gera e envia um código de 6 dígitos, válido por 15 min. `method: "sms"` retorna **501 Not Implemented** (recurso desenhado mas inativo) |
| POST | `/api/auth/verify-code` | `{email, code}` | `{token, name, email}` | Valida o código; se correto, marca a conta como verificada e já retorna um JWT |
| POST | `/api/auth/login` | `{email, password}` | `{token, name, email}` | Autentica; retorna **403** se a conta ainda não foi verificada |

## `/api/me` — Usuário logado (protegida)

| Método | Rota | Resposta | Descrição |
|---|---|---|---|
| GET | `/api/me` | e-mail do usuário autenticado pelo token | Usado para validar sessão/token no frontend |

## `/api/tasks` — Tarefas e metas

> ⚠️ Rota hoje **pública** (`permitAll` em `SecurityConfig`) — ver
> `04-seguranca.md`. A tabela abaixo descreve o contrato da API,
> independente do estado de proteção.

| Método | Rota | Corpo | Descrição |
|---|---|---|---|
| GET | `/api/tasks` | — | Lista todas as tarefas/metas |
| POST | `/api/tasks` | `Task` (ver `02-modelo-dados.md`) | Cria uma tarefa ou meta |
| PUT | `/api/tasks/{id}` | `Task` | Atualiza por completo |
| DELETE | `/api/tasks/{id}` | — | Remove |

## `/api/finance/lancamentos` — Lançamentos financeiros

> ⚠️ Mesma ressalva de proteção acima.

| Método | Rota | Corpo | Descrição |
|---|---|---|---|
| GET | `/api/finance/lancamentos` | — | Lista lançamentos |
| POST | `/api/finance/lancamentos` | `Transaction` | Cria um lançamento |
| DELETE | `/api/finance/lancamentos/{id}` | — | Remove |

Rota renomeada de `/api/finance/transactions` para `/api/finance/lancamentos`
durante a investigação de um bug de produção (ver ADR em
`05-decisoes-tecnicas.md`) — o nome em português não tem relação com a causa
raiz do bug, mas o nome ficou assim depois da tentativa.

## `/api/finance/investments` — Investimentos

> ⚠️ Mesma ressalva de proteção.

| Método | Rota | Corpo | Descrição |
|---|---|---|---|
| GET | `/api/finance/investments` | — | Lista investimentos |
| POST | `/api/finance/investments` | `Investment` | Cria um investimento |
| DELETE | `/api/finance/investments/{id}` | — | Remove |

## `/api/projects` — Projetos (aba Profissional)

> ⚠️ Mesma ressalva de proteção.

| Método | Rota | Corpo | Descrição |
|---|---|---|---|
| GET | `/api/projects` | — | Lista projetos |
| POST | `/api/projects` | `Project` | Cria um projeto |
| PUT | `/api/projects/{id}` | `Project` | Atualiza (ex.: status, % de progresso) |
| DELETE | `/api/projects/{id}` | — | Remove |

## `/api/team` — Membros de equipe (aba Profissional)

> ⚠️ Mesma ressalva de proteção.

| Método | Rota | Corpo | Descrição |
|---|---|---|---|
| GET | `/api/team` | — | Lista membros |
| POST | `/api/team` | `TeamMember` | Adiciona um membro |
| DELETE | `/api/team/{id}` | — | Remove |

## `/api/health` — Health check (pública)

| Método | Rota | Resposta |
|---|---|---|
| GET | `/api/health` | `{"status":"ok","service":"tasksync-backend"}` |

## Códigos de erro relevantes

| Status | Quando acontece |
|---|---|
| 400 | Validação de entrada falhou, ou regra de negócio simples violada (ex.: e-mail já cadastrado, código já verificado) |
| 401 | Credenciais inválidas no login, ou código de verificação errado/expirado |
| 403 | Login bem-sucedido mas conta ainda não verificada |
| 501 | Tentativa de verificação por SMS (canal desenhado mas não implementado) |
| 500 | Erro não tratado explicitamente (ex.: falha ao chamar a API da Brevo) — hoje o corpo da resposta nesses casos é o JSON padrão do Spring Boot (`timestamp`, `status`, `error`, `path`), não uma mensagem amigável (ver item de melhoria em `06-debito-tecnico-roadmap.md`) |
