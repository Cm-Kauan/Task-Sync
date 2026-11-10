# Decisões Técnicas (ADRs)

Registro das decisões de arquitetura mais relevantes, no formato "contexto →
decisão → consequência". O objetivo é que, daqui a meses, dê pra entender
*por que* o código está do jeito que está, sem precisar reconstruir o
raciocínio do zero.

---

## ADR-01 — Banco H2 em dev, PostgreSQL em produção

**Contexto**: o projeto é de aprendizado, e exigir que qualquer pessoa
instale e configure PostgreSQL localmente só para rodar o projeto é
fricção desnecessária para testar uma mudança pequena.

**Decisão**: usar `@Profile` para trocar o banco inteiro — H2 em memória no
perfil `dev`, PostgreSQL via variáveis de ambiente no perfil padrão
(produção).

**Consequência**: desenvolvimento local instantâneo, sem setup. Trade-off
aceito: comportamento de SQL pode divergir entre H2 e Postgres (como de fato
aconteceu — ver ADR-03), então bugs específicos de Postgres só aparecem em
produção ou testando com um Postgres local de propósito.

---

## ADR-02 — Frontend sem framework (ainda)

**Contexto**: o projeto é de aprendizado de fundamentos antes de abstrações.

**Decisão**: HTML/CSS/JS puro, Tailwind via CDN, sem bundler/build step.
React fica para uma fase futura, deliberadamente.

**Consequência**: manipulação direta do DOM em `app.js` (sem
componentização, sem reatividade automática) — todo re-render é manual.
Funciona bem no tamanho atual do projeto; tende a ficar mais difícil de
manter conforme mais telas/estados forem adicionados, o que é o sinal
esperado de quando fará sentido migrar.

---

## ADR-03 — Coluna `description` em vez de `desc` na tabela `transaction`

**Contexto**: bug real de produção, o mais difícil de diagnosticar do
projeto até hoje. Um 403 aparecia ao criar lançamentos financeiros, mas a
causa raiz era um 500: `desc` é palavra reservada no PostgreSQL (usada em
`ORDER BY ... DESC`), e usá-la como nome de coluna sem aspas quebra a query
com erro de sintaxe SQL. Esse 500 interno acionava um forward do Spring Boot
para `/error`, que por sua vez estava bloqueado pelo Spring Security (rota
não liberada em `permitAll`), fazendo o cliente receber um 403 sem relação
nenhuma com a causa real.

**Decisão**: renomear a coluna/campo para `description`; manter
`@JsonAlias("desc")` na entrada para aceitar o nome antigo via JSON, caso
algum cliente desatualizado ainda envie assim; liberar `/error` no
`permitAll` do `SecurityConfig` para que erros internos nunca mais fiquem
mascarados por um 403 enganoso.

**Consequência**: lição arquitetural registrada — **nomes de coluna que
colidem com palavras reservadas de SQL devem ser evitados desde o design da
entidade**, e qualquer rota interna do framework (`/error` incluso) precisa
estar explicitamente liberada para não distorcer o diagnóstico de erros
futuros.

---

## ADR-04 — Envio de e-mail via API HTTP da Brevo, não via SMTP

**Contexto**: a implementação inicial usava `JavaMailSender` (SMTP,
`smtp-relay.brevo.com:587`). Funcionava localmente, mas falhava em produção
com "Connection timed out".

**Decisão**: trocar para a API HTTP da Brevo (`POST
https://api.brevo.com/v3/smtp/email`, porta 443), usando uma API Key em vez
de usuário/senha SMTP.

**Consequência**: a causa raiz era que o Railway (como a maioria dos PaaS)
bloqueia portas de SMTP (25/465/587) por padrão, política anti-spam da
plataforma — não tinha relação com as credenciais, que estavam corretas. A
lição: **qualquer integração de envio de e-mail em uma plataforma de
hospedagem compartilhada deve ser feita via API HTTP, não SMTP direto**, a
menos que a plataforma declare explicitamente suporte a portas SMTP
de saída.

---

## ADR-05 — Autenticação por código (OTP) em vez de link clicável

**Contexto**: era preciso decidir o mecanismo de verificação de conta, com o
requisito de que o usuário pudesse escolher entre e-mail e SMS no momento da
verificação.

**Decisão**: usar um código numérico de 6 dígitos, válido por 15 minutos,
em vez de um link de confirmação com token na URL.

**Consequência**: um código funciona de forma idêntica para e-mail e SMS
(um link clicável não faz sentido como mensagem de texto SMS); o usuário
digita o mesmo tipo de informação independente do canal escolhido. Trade-off
aceito: UX exige um passo extra de digitação, em troca de um mecanismo de
verificação uniforme entre os dois canais.

---

## ADR-06 — SMS desenhado na interface, mas não implementado

**Contexto**: o pedido original incluía verificação por e-mail *e* SMS, com
escolha do usuário. SMS transacional não tem camada gratuita permanente
(serviços como Twilio cobram por mensagem).

**Decisão**: implementar a interface e o contrato de API já preparados para
os dois canais (`SendVerificationCodeRequest.method` aceita `"sms"`), mas o
backend responde `501 Not Implemented` para esse canal, e o botão
correspondente no frontend fica desabilitado com um aviso.

**Consequência**: a arquitetura já suporta adicionar SMS no futuro sem
redesenho — bastaria implementar um `SmsService` análogo ao `EmailService` e
trocar a resposta 501 pela chamada real — sem exigir custo nenhum até essa
decisão ser tomada deliberadamente.

---

## ADR-07 — Troca de classe `hidden`/`flex` do Tailwind por `style.display` direto

**Contexto**: ao implementar a UI de login/cadastro, a troca entre a tela de
autenticação (`#auth-screen`) e o dashboard (`#app-shell`) usava as classes
utilitárias `hidden` e `flex` do Tailwind simultaneamente no mesmo elemento.
Isso é ambíguo: como as duas classes têm a mesma especificidade CSS (seletor
de classe única), qual delas "vence" depende da **ordem em que as regras
aparecem no CSS final gerado**, não da ordem das classes no HTML — um
comportamento não garantido e dependente da versão/build do Tailwind.

**Decisão**: para esses dois elementos especificamente, controlar a
visibilidade via `element.style.display = 'flex' | 'none'` diretamente em
JavaScript, em vez de alternar classes utilitárias.

**Consequência**: visibilidade determinística, independente de qualquer
ordem de regras no CSS gerado pelo Tailwind. Esse padrão (`style.display`
direto) deve ser preferido sobre alternância de classes `hidden`/`flex`
sempre que dois estados mutuamente exclusivos de exibição precisarem ser
garantidos com certeza.
