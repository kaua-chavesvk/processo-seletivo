# Trilha · Recruitment platform

[![Platform checks](https://github.com/kaua-chavesvk/processo-seletivo/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/kaua-chavesvk/processo-seletivo/actions/workflows/ci.yml) · [Português](README.md)

Trilha follows a job application from the first PDF submission through the hiring decision. Recruiters work on a board for each opening. Candidates see the stage history of their own applications. Moving a card to **Entrevista** updates both views of the same record.

**[Try the board without signing up](https://trilha-processo-seletivo.vercel.app)** · **[Open the live application](https://trilha-selecao-app.vercel.app)** · [Watch the walkthrough](docs/demonstracao.webm)

![The Trilha recruiter board with four active hiring stages](docs/funil.png)

## Try it in a minute

1. Open **Marina Costa** in **Triagem**, select **Entrevista**, add an optional message for the candidate and click **Salvar etapa**. On a phone, the funnel opens as a list with a stage filter. Desktop opens in board view.
2. Click **Ver como Marina acompanha essa mudança** to see the interview and your message in Marina’s own timeline.
3. Open the [sample data analyst vacancy](https://trilha-processo-seletivo.vercel.app/#vagas/dados). **Copiar link da vaga** copies the address of that opening. **Simular candidatura** lets you try applying.

The demo is in Portuguese, uses fictional records in browser memory and resets on reload. The live application uses authentication, persistent database records and private file storage. Its openings page reflects what recruiters have published.

## Engineering decisions

The live application and the demo have separate Vercel projects. Both track this repository’s `main` branch for production deployments.

- **Concurrent reviews.** The recruitment service checks company membership, locks the application row and validates the transition using a plain Java state machine. Stage and history changes commit together. [State machine](src/main/java/br/dev/selecao/candidatura/Funil.java) · [Application service](src/main/java/br/dev/selecao/candidatura/CandidaturasService.java)
- **Sessions and authorization.** The React client keeps both tokens in tab memory and requires a new login on reload. Spring authorizes each request. Candidates read their own history, recruiters work within their company and administrators onboard companies and recruiters.
- **Private résumés.** In the live deployment, Spring validates PDFs up to 4 MiB, stores them in a private Vercel Blob store and authorizes downloads. Docker uses a local volume by default. An S3-compatible adapter is also available. [Candidate history from a production smoke test](docs/historico.png)
- **Deployment.** A Vercel container serves the built React and TypeScript frontend and the Java 21 Spring Boot API on one origin. Neon hosts PostgreSQL. Flyway applies migrations with a separate database identity. [Operations guide in Portuguese](docs/operacao.md)

## Run and verify

Start `docker compose up --build -d` and open the [local application](http://localhost:8080). Run `python scripts/demonstrar.py` for an isolated end-to-end HTTP workflow. A new database starts with an empty job board.

CI exercises the state machine, tenant isolation and authentication with JUnit and PostgreSQL via Testcontainers. Playwright covers the board, the mobile candidate experience and a candidate-to-recruiter handoff across two browser sessions. The security report records the limits of the hardening work and two tracked Spring MVC advisories. [CI runs](https://github.com/kaua-chavesvk/processo-seletivo/actions/workflows/ci.yml) · [Browser workflow](frontend/tests/integrado.spec.ts) · [Security report](docs/SEGURANCA.md)
