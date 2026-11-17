# Changelog

Registro cronológico dos marcos relevantes do TaskSync. Formato livre,
inspirado em [Keep a Changelog](https://keepachangelog.com/pt-BR/) — cada
entrada nova vai no topo, abaixo de "Não lançado".

## Não lançado

- Espaço para o que estiver em andamento — atualizar conforme o plano de
  evolução em `docs/arquitetura/06-debito-tecnico-roadmap.md` avançar.

## 2026-11-17 — Plano de evolução de 5 dias (segurança, acessibilidade, UX)

- **Dia 1**: associa `Task`, `Transaction`, `Investment`, `Project` e
  `TeamMember` ao usuário logado (`user_id`); remove essas rotas do
  `permitAll`. Antes dessa mudança, qualquer pessoa com a URL via/editava
  dados de qualquer usuário.
- **Dia 2**: corrige XSS armazenado (dado do usuário era inserido via
  `innerHTML` sem escapar); restringe CORS à URL exata de produção;
  exige senha mais forte (8+ caracteres, letra e número).
- **Dia 3**: adiciona rate limiting em login, verificação de código,
  reenvio de código e cadastro; reduz expiração do JWT de 24h para 2h;
  traduz erros técnicos do backend em mensagens amigáveis no frontend.
- **Dia 4**: acessibilidade - `aria-label`/`aria-hidden` em toda a
  interface, gerenciamento de foco em todos os modais (com Esc para
  fechar), `aria-live` nas regiões de erro/toast, contraste de cores
  corrigido para atender WCAG AA.
- **Dia 5**: estados de carregamento em todas as ações que chamam a API;
  corrige dois bugs onde o modal fechava mesmo quando a ação falhava
  (escondendo o erro do usuário); aviso visível caso o CDN do
  Tailwind/Lucide fique indisponível; teste de regressão completo
  ponta a ponta.

## 2026-08-22

- Troca o envio de e-mail de SMTP para a API HTTP da Brevo — o Railway
  bloqueia portas SMTP por padrão, o que impedia o envio em produção.

## 2026-08-21

- Adiciona sistema de login com cadastro (nome, e-mail, senha, telefone) e
  verificação de conta por código de 6 dígitos enviado por e-mail. SMS fica
  desenhado na interface, mas inativo (depende de serviço pago).
- Aceita `desc` como alias de `description` no JSON de entrada, por
  compatibilidade com clientes desatualizados.

## 2026-08-19

- Corrige a causa raiz do erro 403 ao criar lançamentos financeiros: a
  coluna `desc` é palavra reservada no PostgreSQL. Renomeada para
  `description`.
- Adiciona cache-busting nos scripts do frontend para evitar JS
  desatualizado em cache do navegador.
- Renomeia a rota de transações para `/api/finance/lancamentos` durante a
  investigação do bug acima.

## 2026-08-09

- Corrige valores decimais em formato BRL (vírgula como separador) que
  bloqueavam silenciosamente o envio de formulários.
- Remove o conceito de workspaces Pessoal/Profissional — a aba
  "Profissional" já cumpre esse papel.
- Adiciona gestão real de Equipe e Projetos na aba Profissional.

## 2026-08-07

- Corrige o botão de excluir tarefa, que não atualizava a tela após a
  exclusão.
- Adiciona exclusão de lançamentos financeiros e investimentos.

## 2026-08-06

- Corrige CORS intermitente e remove erro 404 de favicon.
- Aponta o frontend para o backend publicado no Railway em produção.

## 2026-08-05

- Implementa o banco de dados e os CRUDs de Investimentos, Tarefas e
  Lançamentos (model, repository, controller).
- Implementa autenticação com Spring Security + JWT (Fase 1).
- Mescla o dashboard de tarefas/finanças com a autenticação JWT.

## 2026-08-04

- Fase 0: fundação do projeto — esqueleto do backend (Spring Boot) e do
  frontend (HTML/CSS/JS), conversando via API.
