# Arquitetura do Sistema

## Diagrama de componentes (alto nível)

```
┌───────────────────────┐         HTTPS/JSON          ┌────────────────────────────┐
│   Navegador (cliente) │ ───────────────────────────▶ │   Frontend estático        │
│                       │ ◀─────────────────────────── │   (Vercel)                 │
└───────────────────────┘                              │  index.html + js/app.js    │
                                                         │  + js/api.js               │
                                                         └──────────────┬─────────────┘
                                                                        │ fetch() com
                                                                        │ Authorization: Bearer <JWT>
                                                                        ▼
┌───────────────────────┐        SMTP API (HTTPS)      ┌────────────────────────────┐
│   Brevo (e-mail        │ ◀───────────────────────────│   Backend Spring Boot      │
│   transacional)        │                              │   (Railway)                │
└───────────────────────┘                              │                            │
                                                         │  Controller → Repository   │
                                                         │  Spring Security + JWT     │
                                                         └──────────────┬─────────────┘
                                                                        │ JDBC
                                                                        ▼
                                                         ┌────────────────────────────┐
                                                         │  PostgreSQL (Railway)      │
                                                         └────────────────────────────┘
```

## Backend — estrutura de pacotes

```
com.tasksync.backend
├── TaskSyncApplication      # entry point Spring Boot
├── config/
│   └── CorsConfig           # política de CORS (quem pode chamar a API)
├── controller/              # camada HTTP — um controller por recurso
│   ├── AuthController        # /api/auth/** (register, login, verificação)
│   ├── HealthController      # /api/health (health check)
│   ├── MeController           # /api/me (dados do usuário logado)
│   ├── TaskController         # /api/tasks (tarefas e metas, mesma entidade)
│   ├── TransactionController  # /api/finance/lancamentos
│   ├── InvestmentController   # /api/finance/investments
│   ├── ProjectController      # /api/projects
│   └── TeamMemberController   # /api/team
├── dto/                      # objetos de transferência (request/response), não as entidades
├── model/                    # entidades JPA (mapeiam 1:1 para tabelas)
├── repository/                # interfaces Spring Data JPA (uma por entidade)
├── security/
│   ├── SecurityConfig          # cadeia de filtros, rotas públicas vs. protegidas
│   ├── JwtService               # gera/valida/extrai dados do token JWT
│   ├── JwtAuthFilter             # intercepta toda request, autentica se houver Bearer válido
│   └── CustomUserDetailsService  # ponte entre User (entidade) e UserDetails (Spring Security)
└── email/
    ├── EmailService             # interface (abstração de "enviar código")
    ├── ConsoleEmailService      # implementação @Profile("dev") — só loga no console
    └── BrevoApiEmailService     # implementação @Profile("!dev") — chama a API HTTP da Brevo
```

Não existe uma camada "Service" separada explícita (padrão
Controller-Service-Repository completo) — os controllers chamam os
repositories diretamente. Para o tamanho atual do projeto (CRUDs simples,
sem regra de negócio complexa) isso é proporcional; se a lógica de negócio
crescer (ex. cálculos financeiros mais elaborados, regras de permissão por
papel dentro de um projeto), vale extrair uma camada de serviço nesse
momento — não antes.

## Dois perfis de execução (`dev` vs. produção)

O projeto usa `@Profile` do Spring para alternar implementações inteiras
dependendo do ambiente, sem exigir nenhuma configuração manual do
desenvolvedor:

| Aspecto | Perfil `dev` (local) | Perfil padrão (produção / Railway) |
|---|---|---|
| Banco de dados | H2 em memória | PostgreSQL |
| Envio de e-mail | `ConsoleEmailService` (loga o código no console) | `BrevoApiEmailService` (envia de verdade via API da Brevo) |
| Como ativar | `-Dspring-boot.run.profiles=dev` | Nenhuma flag — é o padrão quando a variável `PORT` do Railway está presente |

Essa escolha existe para que qualquer pessoa consiga rodar e testar o projeto
localmente sem precisar de credenciais de Postgres nem de uma conta na
Brevo — só `mvn spring-boot:run -Dspring-boot.run.profiles=dev` e pronto.

## Frontend — arquitetura

O frontend é **intencionalmente** HTML/CSS/JS puro, sem build step, sem
bundler, sem framework:

- `index.html`: toda a estrutura das telas (login/cadastro/verificação +
  dashboard) vive num único arquivo, usando Tailwind CSS via CDN para
  estilização utilitária.
- `js/api.js`: camada única de acesso à API. Centraliza a `API_BASE_URL`
  (detecta automaticamente se está em `localhost` ou em produção), injeta o
  header `Authorization` em toda chamada quando há um token salvo, e expõe um
  objeto `api` com um método por endpoint.
- `js/app.js`: toda a lógica de tela — renderização das listas, manipulação
  do DOM, handlers de formulário, autenticação (login/registro/verificação),
  troca entre `#auth-screen` e `#app-shell` via `style.display` (ver ADR na
  seção de decisões técnicas sobre por que não se usa a classe `hidden` do
  Tailwind para isso).

Não há roteamento client-side (SPA router) — é uma página só, com seções
mostradas/escondidas via JS. Essa é a razão de ainda não ter sido migrado
para React: os fundamentos de manipulação direta do DOM e de uma arquitetura
simples de "telas como estados" são a base proposital antes de introduzir
um framework.

## Deploy e CI/CD

Não há pipeline de CI/CD configurado (sem testes automatizados rodando em
PR, sem build gate antes do deploy). O fluxo é:

1. `git push` para a branch de trabalho no GitHub.
2. **Railway** detecta o push, builda o backend com Nixpacks (detecta o
   `pom.xml`, roda `mvn package`) e sobe o `.jar` gerado.
3. **Vercel** detecta o push, serve a pasta `frontend/` como site estático
   (sem build step, já que não há framework).

Ambos os deploys são automáticos e independentes — não há coordenação entre
eles (ex.: não existe um "deploy conjunto" com rollback atômico). Um deploy
de backend que quebra a API não impede o deploy do frontend de subir
igualmente, e vice-versa.

## Dependências externas de produção

| Serviço | Função | Observação |
|---|---|---|
| Railway (Postgres) | Banco de dados relacional | Plano gratuito/hobby |
| Railway (web service) | Hospedagem do backend | — |
| Vercel | Hospedagem do frontend estático | — |
| Brevo | Envio de e-mail transacional (código de verificação) | Via API HTTP (porta 443) — **não** via SMTP, porque o Railway bloqueia as portas SMTP padrão (25/465/587) por política anti-abuso |
| cdnjs/jsdelivr (Tailwind CDN, Lucide icons) | CSS utilitário e ícones | Carregado em runtime pelo navegador do usuário final, não empacotado |
