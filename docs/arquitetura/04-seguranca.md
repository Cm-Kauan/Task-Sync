# Segurança — Modelo de Ameaças e Estado Atual

Este documento registra, com honestidade, o que está protegido hoje, o que
não está, e por quê. É o ponto de partida de qualquer auditoria futura — e a
razão de existir do plano de 5 dias descrito em `06-debito-tecnico-roadmap.md`.

## O que está implementado corretamente

- **Senhas**: hasheadas com BCrypt (`PasswordEncoder` do Spring Security) —
  nunca armazenadas em texto puro.
- **Autenticação stateless via JWT**: sem sessão guardada no servidor
  (`SessionCreationPolicy.STATELESS`), token assinado com segredo próprio
  (`JWT_SECRET`, variável de ambiente, nunca commitada).
- **Verificação de conta obrigatória antes do login**: conta criada fica
  `verified = false`; login é bloqueado (403) até a verificação por e-mail.
- **Código de verificação com expiração**: 15 minutos, de uso único (campo
  zerado após sucesso).
- **CORS restrito por padrão de origem** (não é `*` aberto): só
  `localhost`, `127.0.0.1` e subdomínios `*.vercel.app` podem chamar a API.
- **CSRF desabilitado conscientemente**: correto para uma API stateless
  consumida só via JSON/fetch com Bearer token (CSRF é um risco de
  cookies/sessão, que não se aplica aqui).
- **`/error` liberado explicitamente no `permitAll`**: evita que o Spring
  Security mascare um erro 500 real como um 403 enganoso quando o Spring Boot
  faz o forward interno para a rota de erro (bug real já enfrentado e
  documentado no ADR correspondente).

## O que NÃO está protegido hoje (lacunas conhecidas)

### 🔴 Crítico — dados sem dono
`task`, `transaction`, `investment`, `project` e `team_member` estão
liberados em `permitAll` no `SecurityConfig.java` e não têm coluna
`user_id`. **Qualquer requisição, autenticada ou não, lê/cria/edita/apaga
qualquer registro dessas tabelas.** O login hoje autentica a *pessoa*, mas
não isola os *dados* dela. Este é o item de maior prioridade no roadmap
(Fase 4.2).

### 🔴 Crítico — XSS armazenado no frontend
`js/app.js` insere valores vindos da API (título de tarefa, descrição de
lançamento, nome de projeto/membro) diretamente via `innerHTML`, sem
escapar. Um valor malicioso salvo uma vez (ex. um título de tarefa contendo
`<img src=x onerror="...">`) executa para qualquer sessão que renderizar
aquela lista — incluindo roubo do token JWT do `localStorage` de quem
visualizar.

### 🟠 Importante — sem rate limiting
`/api/auth/login`, `/register` e `/send-verification-code` aceitam
tentativas ilimitadas. Isso permite força bruta de senha por e-mail e abuso
do limite de envio de e-mails da conta Brevo (negação de serviço indireta
via esgotamento de cota).

### 🟠 Importante — JWT em `localStorage`
Acessível por qualquer script no mesmo documento (inclusive via o XSS
acima), diferente de um cookie `httpOnly`. Risco amplificado pela
combinação com o item anterior.

### 🟠 Importante — CORS com wildcard de subdomínio
`https://*.vercel.app` libera **qualquer** projeto hospedado na Vercel, de
qualquer conta, não só o frontend deste projeto. Deveria ser restrito à URL
exata de produção.

### 🟡 Menor — enumeração de contas
`/api/auth/register` responde de forma explícita que um e-mail já está
cadastrado, permitindo a um atacante descobrir quais e-mails têm conta no
sistema por tentativa.

### 🟡 Menor — política de senha mínima
Só exige 6 caracteres de tamanho (`RegisterRequest`), sem checar
força/composição.

## Fluxo de autenticação (como funciona hoje, passo a passo)

1. **Cadastro** (`POST /register`): conta criada com `verified=false`, senha
   já hasheada. Nenhum token é emitido aqui.
2. **Escolha do canal** (`POST /send-verification-code`): gera um código de
   6 dígitos (`SecureRandom`), salva com expiração de 15 min, dispara e-mail
   via Brevo (API HTTP, não SMTP — ver ADR).
3. **Verificação** (`POST /verify-code`): compara o código enviado com o
   salvo e sua validade. Se correto: marca `verified=true`, zera os campos
   de código, e **já emite o JWT** nesse momento (não precisa logar de novo).
4. **Login** (`POST /login`): autentica usuário/senha via
   `AuthenticationManager`; se a conta não estiver verificada, retorna 403
   mesmo com a senha certa — força o usuário a completar a verificação antes
   de entrar.
5. **Requisições autenticadas**: o frontend guarda o token em
   `localStorage` e o `js/api.js` o anexa automaticamente em todo `fetch`
   como `Authorization: Bearer <token>`. O `JwtAuthFilter` roda em toda
   requisição e popula o contexto de segurança do Spring quando o token é
   válido.

## Checklist de hardening futuro (fora do escopo dos 5 dias)

- Mover o JWT para um cookie `httpOnly` + `Secure` + `SameSite=Strict`
  (exige repensar CORS, já que cookies cross-origin têm regras próprias).
- Rotação/invalidação de token (hoje não existe "logout" server-side — o
  token continua válido até expirar, mesmo que o usuário clique em "Sair").
- Auditoria/log de tentativas de login falhas.
- 2FA opcional além da verificação de cadastro (que só acontece uma vez).
