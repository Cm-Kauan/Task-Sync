# Débito Técnico e Roadmap de Evolução

Lista consolidada de melhorias identificadas em auditoria de segurança,
interface, acessibilidade e arquitetura, priorizadas por risco/impacto. O
plano de execução de curto prazo (5 dias, **concluído**) está detalhado ao
final, junto com o que ficou de fora dele.

## Segurança

| # | Item | Severidade | Status |
|---|---|---|---|
| 1 | `task`/`transaction`/`investment`/`project`/`team_member` sem dono (`user_id`), rotas em `permitAll` | 🔴 Crítico | ✅ Resolvido (Dia 1) |
| 2 | XSS armazenado via `innerHTML` sem escapar dados do usuário em `app.js` | 🔴 Crítico | ✅ Resolvido (Dia 2) |
| 3 | Sem rate limiting em `/login`, `/register`, `/send-verification-code` | 🟠 Importante | ✅ Resolvido (Dia 3) |
| 4 | JWT em `localStorage` (acessível via XSS) | 🟠 Importante | ⚠️ Mitigado (Dia 3 reduziu a janela de exposição via expiração mais curta) - a mudança estrutural (cookie `httpOnly`) segue no backlog |
| 5 | `JWT_EXPIRATION_MS` sem revisão de valor / sem revogação de token | 🟠 Importante | ✅ Expiração resolvida (Dia 3) - revogação de token segue no backlog |
| 6 | CORS libera `*.vercel.app` inteiro, não só a URL própria | 🟠 Importante | ✅ Resolvido (Dia 2) |
| 7 | Política de senha só exige tamanho mínimo (6) | 🟡 Menor | ✅ Resolvido (Dia 2) |
| 8 | `/register` permite enumeração de e-mails cadastrados | 🟡 Menor | ⏸️ Decisão consciente de não aplicar por ora (ver nota no Dia 2 abaixo) |
| 9 | Sem confirmação explícita de redirecionamento HTTP→HTTPS (Railway/Vercel já servem HTTPS por padrão) | 🟡 Menor | ⏸️ Backlog |

## Interface / Design

| # | Item | Status |
|---|---|---|
| 10 | Sem estados de carregamento visíveis em criar/editar/deletar | ✅ Resolvido (Dia 5) |
| 11 | Mensagens de erro cruas (JSON técnico do backend) expostas ao usuário final | ✅ Resolvido (Dia 3) |
| 12 | Dependência de CDN (Tailwind, Lucide) sem fallback caso o CDN falhe/seja bloqueado | ✅ Resolvido (Dia 5) |
| 13 | Sem dark mode | ⏸️ Backlog |
| 14 | Contraste de cores não auditado formalmente | ✅ Resolvido (Dia 4) |

## Acessibilidade

| # | Item | Status |
|---|---|---|
| 15 | Nenhum atributo `aria-*`/`role` no projeto | ✅ Resolvido (Dia 4) |
| 16 | Ícones/imagens informativas sem `alt`/`aria-label` | ✅ Resolvido (Dia 4) |
| 17 | Sem gerenciamento de foco em modais (abrir/fechar) | ✅ Resolvido (Dia 4) |
| 18 | Navegação por teclado não validada | ✅ Resolvido (Dia 4 - Esc fecha modais; foco gerenciado) |
| 19 | Contraste de cores não auditado (Lighthouse/axe) | ✅ Resolvido (Dia 4 - cálculo manual de contraste WCAG, Lighthouse real não disponível no ambiente de desenvolvimento) |
| 20 | Erros/status não anunciados a leitores de tela (`aria-live` ausente) | ✅ Resolvido (Dia 4) |

## Arquitetura / Qualidade de código

| # | Item | Status |
|---|---|---|
| 21 | Sem fallback claro se o envio de e-mail falhar após o cadastro já ter sido persistido | ⏸️ Backlog (usuário pode reenviar o código manualmente; não há retry automático) |
| 22 | Sem testes automatizados (unitários/integração) em backend ou frontend | ⏸️ Backlog |
| 23 | `permitAll` com `TODO` sem prazo formal | ✅ Resolvido (Dia 1 - rotas removidas do `permitAll`) |
| 24 | Sem paginação nas listagens | ⏸️ Backlog |
| 25 | Sem logging estruturado/monitoramento de erros em produção (tipo Sentry) | ⏸️ Backlog |

---

## Plano de execução — 5 dias (concluído)

### Dia 1 — Fechar o compartilhamento de dados entre usuários (Fase 4.2) ✅
- `user_id` (FK) adicionado em `Task`, `Transaction`, `Investment`,
  `Project`, `TeamMember`.
- Toda consulta/escrita filtrada pelo usuário autenticado
  (`CurrentUserService`); update/delete de recurso de outro usuário
  retorna 404.
- Rotas removidas do `permitAll` em `SecurityConfig.java`.
- Testado com dois usuários reais via curl: isolamento confirmado.

### Dia 2 — XSS, CORS e validação de entrada ✅
- Toda interpolação de dado do usuário em `innerHTML` passa por
  `escapeHtml()`.
- CORS restrito à URL exata de produção do frontend.
- Senha exige 8+ caracteres, letra e número.
- **Decisão consciente**: mensagem de "e-mail já cadastrado" mantida como
  está - sem rate limiting (que só chegou no Dia 3), ocultar a mensagem
  não bloquearia o ataque de verdade, só pioraria a UX de quem erra o
  e-mail no cadastro.

### Dia 3 — Rate limiting e hardening de autenticação ✅
- `RateLimiterService` (janela deslizante em memória) aplicado em login,
  verify-code (5 tentativas/15min por e-mail), send-verification-code
  (3/15min por e-mail) e register (5/hora por IP).
- `JWT_EXPIRATION_MS` reduzido de 24h para 2h.
- `api.js` traduz o JSON técnico padrão de erro do Spring Boot em
  mensagens amigáveis em português.

### Dia 4 — Acessibilidade ✅
- `aria-hidden="true"` em todos os ícones decorativos; `aria-label` em
  todo botão só-ícone.
- Gerenciamento de foco em todos os 7 modais (abrir/fechar/Esc), com um
  bug real corrigido no processo (input oculto capturando o foco).
- `aria-live="polite"` nas regiões de erro/toast.
- Contraste corrigido via cálculo manual WCAG (`text-slate-400` e
  `text-red-500` reprovavam, trocados por `text-slate-500`/`text-red-600`).

### Dia 5 — Polimento de UX e fechamento ✅
- Estados de carregamento (desabilitar botão) adicionados a todos os
  handlers que ainda não tinham.
- Dois bugs reais corrigidos: `handleSubmit` (tarefa) e `confirmDelete`
  fechavam o modal mesmo quando a chamada à API falhava, escondendo a
  falha do usuário.
- Aviso visível caso o CDN do Tailwind/Lucide falhe.
- Teste de regressão completo via Playwright: registro → verificação →
  dashboard → criar/excluir em cada módulo → logout → login. XSS
  reconfirmado bloqueado após todas as mudanças subsequentes.

## Backlog (fora do escopo dos 5 dias)

- #4/#5 (parcial): mover JWT para cookie `httpOnly` + `Secure` +
  `SameSite`, e implementar revogação/logout server-side.
- #8: reavaliar a decisão de enumeração de e-mail depois que outras
  camadas de proteção (rate limiting) amadurecerem.
- #9: confirmar redirecionamento HTTP→HTTPS explícito.
- #13: dark mode.
- #21: retry automático ou fila para envio de e-mail falho.
- #22: suíte de testes automatizados (unitários/integração).
- #24: paginação nas listagens.
- #25: logging estruturado/monitoramento de erros (ex.: Sentry).
- 2FA contínuo (a verificação atual só acontece uma vez, no cadastro).
