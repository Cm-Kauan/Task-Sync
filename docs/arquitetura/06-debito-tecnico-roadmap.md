# Débito Técnico e Roadmap de Evolução

Lista consolidada de melhorias identificadas em auditoria de segurança,
interface, acessibilidade e arquitetura, priorizadas por risco/impacto. O
plano de execução de curto prazo (5 dias) está detalhado ao final.

## Segurança

| # | Item | Severidade |
|---|---|---|
| 1 | `task`/`transaction`/`investment`/`project`/`team_member` sem dono (`user_id`), rotas em `permitAll` | 🔴 Crítico |
| 2 | XSS armazenado via `innerHTML` sem escapar dados do usuário em `app.js` | 🔴 Crítico |
| 3 | Sem rate limiting em `/login`, `/register`, `/send-verification-code` | 🟠 Importante |
| 4 | JWT em `localStorage` (acessível via XSS) | 🟠 Importante |
| 5 | `JWT_EXPIRATION_MS` sem revisão de valor / sem revogação de token | 🟠 Importante |
| 6 | CORS libera `*.vercel.app` inteiro, não só a URL própria | 🟠 Importante |
| 7 | Política de senha só exige tamanho mínimo (6) | 🟡 Menor |
| 8 | `/register` permite enumeração de e-mails cadastrados | 🟡 Menor |
| 9 | Sem confirmação explícita de redirecionamento HTTP→HTTPS (Railway/Vercel já servem HTTPS por padrão) | 🟡 Menor |

## Interface / Design

| # | Item |
|---|---|
| 10 | Sem estados de carregamento visíveis em criar/editar/deletar |
| 11 | Mensagens de erro cruas (JSON técnico do backend) expostas ao usuário final |
| 12 | Dependência de CDN (Tailwind, Lucide) sem fallback caso o CDN falhe/seja bloqueado |
| 13 | Sem dark mode |
| 14 | Contraste de cores não auditado formalmente |

## Acessibilidade

| # | Item |
|---|---|
| 15 | Nenhum atributo `aria-*`/`role` no projeto |
| 16 | Ícones/imagens informativas sem `alt`/`aria-label` |
| 17 | Sem gerenciamento de foco em modais (abrir/fechar) |
| 18 | Navegação por teclado não validada |
| 19 | Contraste de cores não auditado (Lighthouse/axe) |
| 20 | Erros/status não anunciados a leitores de tela (`aria-live` ausente) |

## Arquitetura / Qualidade de código

| # | Item |
|---|---|
| 21 | Sem fallback claro se o envio de e-mail falhar após o cadastro já ter sido persistido |
| 22 | Sem testes automatizados (unitários/integração) em backend ou frontend |
| 23 | `permitAll` com `TODO` sem prazo formal (ver plano abaixo) |
| 24 | Sem paginação nas listagens |
| 25 | Sem logging estruturado/monitoramento de erros em produção (tipo Sentry) |

---

## Plano de execução — próximos 5 dias

### Dia 1 — Fechar o compartilhamento de dados entre usuários (Fase 4.2)
Resolve: #1.
- Adicionar `user_id` (FK) em `Task`, `Transaction`, `Investment`,
  `Project`, `TeamMember`.
- Filtrar toda consulta/escrita pelo usuário autenticado na requisição.
- Remover essas rotas do `permitAll` em `SecurityConfig.java`.
- Testar com dois usuários diferentes: isolamento de dados confirmado.

### Dia 2 — XSS, CORS e validação de entrada
Resolve: #2, #6, #7, #8.
- Escapar toda interpolação de dado do usuário antes de inserir via
  `innerHTML` (ou trocar por `textContent` onde possível).
- Restringir CORS à URL exata de produção do frontend.
- Adicionar validação de força de senha além do tamanho.
- Tornar a resposta de `/register` para e-mail já cadastrado menos
  específica (avaliar o trade-off de UX antes de aplicar).

### Dia 3 — Rate limiting e hardening de autenticação
Resolve: #3, #4 (mitigação), #5, #11.
- Rate limiting em `/login`, `/register`, `/send-verification-code`.
- Revisar `JWT_EXPIRATION_MS` para um valor mais curto.
- Camada no frontend que traduz erros técnicos em mensagens amigáveis.

### Dia 4 — Acessibilidade
Resolve: #15, #16, #17, #18, #19, #20.
- `aria-label` em botões só-ícone, `aria-hidden` em ícones decorativos.
- `aria-live="polite"` na região de toast/erro.
- Gerenciamento de foco em modais (abrir/fechar/Esc).
- Auditoria Lighthouse e correção de contraste.

### Dia 5 — Polimento de UX e fechamento
Resolve: #10, #12, #21 (parcial).
- Estados de carregamento nas ações principais.
- Fallback visual caso o CDN falhe.
- Revisão do fluxo de reenvio de código de verificação.
- Teste de regressão ponta a ponta de tudo que foi alterado nos 5 dias.

### Fora do escopo dos 5 dias (backlog futuro)
#9, #13, #14 (parcial — resolvido via Lighthouse no Dia 4), #22, #23 (torna-se
resolvido ao concluir o Dia 1), #24, #25, e o checklist de hardening avançado
listado no final de `04-seguranca.md` (JWT em cookie `httpOnly`, logout
server-side, 2FA contínuo, auditoria de login).
