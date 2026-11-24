# Trilha

Como um candidato sabe o que mudou depois de enviar o currículo? No Trilha, o recrutador move uma candidatura no quadro da vaga e o candidato vê a etapa e a orientação no seu histórico. A decisão central foi gravar a etapa e o histórico na mesma transação.

**[Experimentar sem cadastro](https://trilha-processo-seletivo.vercel.app)** · [Aplicação com login](https://trilha-selecao-app.vercel.app) · [English](README.en.md)

![Na demo, a recrutadora move Marina de Triagem para Entrevista. Marina abre seu histórico e lê a orientação para a conversa.](docs/demonstracao.gif)

## Minhas decisões

| O que escolhi | O que ganhei e qual foi o custo |
| --- | --- |
| Bloqueio a candidatura durante a mudança de etapa. | Se dois recrutadores agirem juntos, o segundo espera e a API valida a decisão sobre a etapa já atualizada. A espera se limita àquela candidatura. |
| Guardo os tokens na memória da aba. | O navegador deixa de persistir as credenciais. Recarregar exige novo login, e duas consultas que recebem 401 precisam compartilhar uma renovação. |
| Ofereço a demo com dados fictícios em memória. | Quem avalia pode acompanhar os dois lados do processo sem enviar um PDF. A demo usa as mesmas telas e perde as alterações ao recarregar. |

Ao revisar o funil, aprendi que um teste pode percorrer todas as combinações e ainda repetir o erro da implementação. Por isso escrevi os resultados esperados como uma tabela independente. Com mais tempo, eu observaria recrutadores usando o quadro no celular para decidir se a lista por etapa reduz o tempo de triagem. [Regra do funil](src/main/java/br/dev/selecao/candidatura/Funil.java) · [Teste de domínio](src/test/java/br/dev/selecao/DominioTest.java)

## Rodar localmente

Na raiz do repositório, com Docker Compose instalado, execute o comando abaixo.

```sh
docker compose up --build -d
```

Abra a [interface local](http://localhost:8080). O banco começa vazio. O guia explica como cadastrar o primeiro recrutador e publicar uma vaga. [Guia de operação](docs/operacao.md)

## Autoria e ferramentas

O histórico registra as contribuições ao projeto sob **kaua-chavesvk**, da API Spring Boot à interface React. O Maven Wrapper vem do Apache Maven, e o MapStruct gera as implementações dos mapeadores. Nesta revisão, o Codex escreveu refatorações, testes e documentação e produziu o GIF a partir da demo. Os commits anteriores registram as mudanças, sem detalhar o uso de assistentes em cada arquivo. [Histórico de commits](https://github.com/kaua-chavesvk/processo-seletivo/commits/main/) · [Verificações da CI](https://github.com/kaua-chavesvk/processo-seletivo/actions/workflows/ci.yml) · [Relatório de segurança](docs/SEGURANCA.md)
