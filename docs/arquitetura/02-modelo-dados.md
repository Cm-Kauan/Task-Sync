# Modelo de Dados

## Diagrama entidade-relacionamento (estado atual)

```
┌─────────────────┐
│      users       │
├─────────────────┤
│ id (PK)          │
│ name             │
│ email (unique)   │
│ password (hash)  │
│ phone            │
│ verified         │
│ verification_code│
│ verification_code│
│   _expires_at    │
│ verification_    │
│   method         │
└─────────────────┘

        ⚠ SEM relacionamento com nenhuma tabela abaixo — ver nota.

┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│      task        │   │   transaction    │   │   investment     │
├─────────────────┤   ├─────────────────┤   ├─────────────────┤
│ id (PK)          │   │ id (PK)          │   │ id (PK)          │
│ title            │   │ description      │   │ name             │
│ description      │   │ amount           │   │ type             │
│ priority         │   │ type             │   │ applied          │
│ tags             │   │ payment          │   │ current          │
│ due_date         │   │ ref_month        │   │ ref_month        │
│ completed        │   │ category         │   └─────────────────┘
│ subtasks (JSON)  │   └─────────────────┘
│ created_at       │
│ type             │   ┌─────────────────┐   ┌─────────────────┐
│ goal_target      │   │     project      │   │   team_member    │
│ goal_current     │   ├─────────────────┤   ├─────────────────┤
└─────────────────┘   │ id (PK)          │   │ id (PK)          │
                       │ name             │   │ name             │
                       │ status           │   │ role             │
                       │ progress         │   └─────────────────┘
                       └─────────────────┘
```

### ⚠️ Estado atual: nenhuma tabela de dados pertence a um usuário

Esta é a lacuna mais importante registrada nesta documentação (ver também
`04-seguranca.md`): `task`, `transaction`, `investment`, `project` e
`team_member` **não têm uma foreign key para `users`**. Isso significa que,
hoje, o sistema de autenticação e o sistema de dados são, na prática, dois
sistemas paralelos — qualquer linha dessas tabelas é visível/editável por
qualquer requisição, autenticada ou não.

A correção planejada (ver `06-debito-tecnico-roadmap.md`, item "Fase 4.2") é
adicionar uma coluna `user_id` (FK para `users.id`) em cada uma dessas cinco
tabelas, e filtrar toda consulta pelo usuário autenticado na requisição.

## Detalhe de cada entidade

### `users`
Única entidade com dono de si mesma. Campos de suporte à verificação de
conta (`verification_code`, `verification_code_expires_at`,
`verification_method`) ficam `null` depois que a conta é verificada com
sucesso — não há histórico de códigos antigos, só o mais recente.

### `task`
Entidade dupla propositalmente: representa tanto uma **tarefa** quanto uma
**meta**, diferenciadas pelo campo `type`. Isso evita duplicar um modelo
quase idêntico — a troca é que metas usam `goal_target`/`goal_current` (ex.:
"economizar R$ 5.000", progresso atual R$ 3.200) enquanto tarefas usam
`priority`/`due_date`/`subtasks`. `tags` e `subtasks` são armazenados como
texto (JSON serializado em string), não como tabelas relacionadas — modelagem
simples proposital para o volume atual de dados; se subtarefas precisarem de
operações próprias (completar uma subtarefa sem reescrever o JSON inteiro),
vale normalizar para uma tabela `subtask` com FK para `task`.

### `transaction`
Representa um lançamento financeiro (entrada, saída ou cartão). A coluna no
banco se chama `description` e não `desc` porque `DESC` é palavra reservada
em SQL (usada em `ORDER BY ... DESC`) — usá-la como nome de coluna sem aspas
quebra a query no PostgreSQL. O código aceita o JSON de entrada com a chave
antiga `desc` via `@JsonAlias` só por compatibilidade com uma versão anterior
do frontend; não deve ser usado como padrão em chamadas novas. Esse foi um
bug real em produção, documentado no ADR correspondente.

### `investment`
Investimentos são tratados separados de `transaction` (não é um lançamento
negativo) porque têm semântica diferente: um valor "aplicado" e um valor
"atual", representando ganho/perda ao longo do tempo, não uma entrada/saída
pontual de caixa.

### `project` / `team_member`
Modelagem propositalmente simples — projeto e membro de equipe não têm
relação direta entre si no banco hoje (um projeto não lista seus membros via
FK; a associação, se existir, é só visual no frontend). Se o produto evoluir
para "quais membros estão em qual projeto" como uma necessidade real, isso
exigiria uma tabela de associação (`project_member`, N:N).

## Convenções de nomenclatura usadas

- Tabelas e colunas: `snake_case` no banco (padrão do Hibernate ao converter
  de `camelCase` do Java), exceto onde explicitamente renomeado por conflito
  com palavra reservada SQL (`ref_month` em vez de `month`, que é reservada
  no H2; `description` em vez de `desc`, reservada no PostgreSQL).
- JSON da API: também `snake_case` (configurado globalmente via
  `spring.jackson.property-naming-strategy=SNAKE_CASE`), para bater com o que
  o frontend espera sem precisar de mapeamento manual campo a campo.
