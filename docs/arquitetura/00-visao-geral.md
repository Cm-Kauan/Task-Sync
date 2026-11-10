# Visão Geral da Arquitetura — TaskSync

> Documentação interna de engenharia. Não é um manual de usuário nem um guia
> de setup — isso já está no `README.md` da raiz. Aqui o objetivo é registrar
> **como o sistema é construído e por quê**, para servir de referência própria
> e para qualquer decisão futura de arquitetura.

## Índice desta pasta

| Documento | Conteúdo |
|---|---|
| `00-visao-geral.md` | Este arquivo — como ler o resto da documentação |
| `01-arquitetura-sistema.md` | Componentes, camadas, fluxo de dados, deploy |
| `02-modelo-dados.md` | Entidades, relacionamentos, diagrama ER |
| `03-api.md` | Todos os endpoints da API, request/response |
| `04-seguranca.md` | Modelo de ameaças, o que está protegido hoje, o que não está |
| `05-decisoes-tecnicas.md` | ADRs — por que cada escolha técnica foi feita |
| `06-debito-tecnico-roadmap.md` | O que falta, priorizado, com plano de evolução |

## Estilo arquitetural em uma frase

TaskSync é um **monólito modular em camadas** no backend (Spring Boot: 
controller → service implícito nos controllers → repository → banco) servindo
um **frontend estático desacoplado** (sem build step, consumindo a API via
`fetch`), com autenticação **stateless via JWT**. Não há microsserviços, fila
de mensagens, cache distribuído ou qualquer infraestrutura além do
necessário — é a escolha certa para o tamanho e o estágio atual do projeto,
não uma limitação.

## Como este documento deve ser mantido

Esta pasta descreve o sistema **como ele é**, não como ele deveria ser um dia.
Sempre que uma mudança estrutural for feita (nova entidade, nova camada de
segurança, troca de serviço externo), os arquivos aqui devem ser atualizados
no mesmo commit/PR da mudança — documentação desatualizada é pior do que
nenhuma documentação, porque engana.
