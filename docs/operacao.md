# Operar a plataforma de processo seletivo

API REST em Java 21 e Spring Boot 3.5 para publicar vagas, receber currículos em PDF e acompanhar candidaturas. PostgreSQL 16 mantém o funil e seu histórico. Migrações Flyway criam as tabelas e o Hibernate valida o schema ao iniciar.

## Demonstração automática

Na raiz do projeto, com Python 3.10+ e Docker Compose com suporte a `up --wait`, execute:

```sh
python scripts/demonstrar.py
```

Em sistemas que disponibilizam o interpretador como `python3`, use `python3 scripts/demonstrar.py`. A primeira execução baixa as imagens e compila a API. O roteiro cria uma empresa e um recrutador, publica uma vaga, inscreve um candidato com um PDF fictício e avança a candidatura para entrevista. Pelo HTTP, confere também a recusa da inscrição duplicada, o conteúdo do PDF baixado e o histórico das etapas.

A demonstração usa credenciais temporárias, volumes próprios e uma porta local livre. Ao terminar, remove os recursos desse projeto temporário e informa o resultado no terminal. Use `python scripts/demonstrar.py --manter-imagem` para preservar a imagem da API identificada ao final. O mesmo roteiro, com remoção da imagem, roda no GitHub Actions. Para explorar a API interativamente com dados persistentes, siga a seção abaixo.

## Executar localmente

O Compose deste repositório foi preparado para desenvolvimento em loopback. Para configurar TLS, a conta restrita do banco e os segredos de produção, consulte o [relatório de segurança](SEGURANCA.md).

Na pasta que contém o arquivo compose.yaml, com Docker Compose instalado, execute:

```sh
docker compose up --build
```

Abra a [interface React](http://localhost:8080) ou o [Swagger UI](http://localhost:8080/swagger-ui.html) ou consulte a [especificação OpenAPI](http://localhost:8080/v3/api-docs). A API escuta na interface local. O Compose mantém o banco e os currículos em volumes. Use `docker compose down` para parar.

Para experimentar o fluxo administrativo, defina `ADMIN_EMAIL` e `ADMIN_PASSWORD` em um arquivo local `.env` antes da inicialização. Escolha uma senha com pelo menos oito caracteres. No primeiro início com essas variáveis, a aplicação cria a conta se o email estiver disponível. O cadastro público sempre cria um candidato.

## Percorrer o primeiro processo

No Swagger UI, execute o login, clique em **Authorize** e cole apenas o valor de `accessToken`. A interface acrescenta o prefixo Bearer. Ao trocar de perfil, substitua o token nessa mesma janela.

1. Faça `POST /auth/login` com `email` e `senha`. Envie o access token no cabeçalho `Authorization: Bearer <accessToken>` nas rotas protegidas.
2. Como administrador, crie uma empresa em `POST /empresas` com `nome`, `cnpj` e `site` opcional. Para criar seu recrutador, envie `{"dados":{"nome":"Recrutador","email":"recrutador@example.org","senha":"troque-esta-senha"},"papel":"RECRUTADOR","empresaId":"<uuid>"}` para `POST /usuarios`.
3. Faça login como recrutador. Crie a vaga em `POST /empresas/{empresaId}/vagas` e publique com `PATCH /vagas/{id}/status` e `{"status":"PUBLICADA"}`.
4. Cadastre o candidato em `POST /auth/cadastro`, faça login e envie um PDF em `POST /vagas/{id}/candidaturas`, no campo multipart `curriculo`. A inscrição inicia em TRIAGEM.
5. Como recrutador, envie `{"novaEtapa":"ENTREVISTA","observacao":"Conversa agendada"}` para `PATCH /candidaturas/{id}/etapa`. Consulte as mudanças em `GET /candidaturas/{id}/historico`. O candidato pode desistir enquanto o processo estiver ativo.

A busca pública aceita modalidade, nível, tecnologia e localização. A página inicial é zero, o tamanho padrão é 20 e o limite é 100. Em vagas, use por exemplo `sort=titulo,asc` ou `sort=criada_em,desc`. Em candidaturas, use `criada_em`. As respostas de erro incluem um identificador também disponível em `X-Correlation-ID`.

## Configuração

| Variável | Uso |
| --- | --- |
| JWT_SECRET | Chave Base64 de pelo menos 32 bytes. Se omitida, a API gera uma chave temporária e os access tokens expiram ao reiniciar. |
| POSTGRES_PASSWORD | Senha do banco. O Compose fornece um valor local padrão. |
| API_PORT | Porta de loopback publicada pelo Compose. O padrão é 8080. O script de demonstração seleciona uma porta livre no host e confirma a porta publicada. |
| APP_ACCESS_TTL | Duração do access token, padrão `15m`. Corresponde a `app.access-ttl` e aceita durações como `30m`. |
| DATABASE_URL / POSTGRES_USER | Conexão JDBC e usuário ao executar fora do Compose. |
| UPLOADS_DIR | Diretório de PDFs, por padrão ./uploads. |
| ADMIN_EMAIL / ADMIN_PASSWORD | Criação opcional do primeiro administrador. |

O access token dura 15 minutos por padrão. O refresh token dura 30 dias, é armazenado como hash e pode ser usado uma única vez para renovação. Logout recebe `{"refreshToken":"<valor>"}`. A desativação em `PATCH /usuarios/{id}/atividade` revoga os refresh tokens e impede o uso de access tokens do usuário.

## Testar

Com Java 21 e Docker em execução, use `./mvnw verify` (Windows: `.\mvnw.cmd verify`). Por padrão, os testes iniciam um PostgreSQL 16 descartável via Testcontainers. Para usar um banco de teste externo, defina `TEST_DATABASE_URL` com uma URL JDBC de um banco vazio e exclusivo para a execução, além de `POSTGRES_USER` e `POSTGRES_PASSWORD`.

Os testes exercitam a matriz de transições, os dígitos verificadores do CNPJ, o limite de upload, o fluxo HTTP de candidatura, a separação entre empresas e candidatos e o ciclo de tokens. O relatório de cobertura fica em `target/site/jacoco/index.html`. O GitHub Actions executa o mesmo ciclo Maven a cada push e pull request.

## Organização

Os pacotes usuario, auth, empresa, vaga, candidatura e curriculo agrupam código por domínio. O pacote comum reúne paginação, identificação do ator, persistência compartilhada e tratamento de erros. A classe Funil valida as transições sem dependência do Spring. O acesso concorrente à inscrição e à alteração de etapa utiliza bloqueios transacionais; a etapa e seu histórico são gravados juntos. A interface ArmazenamentoCurriculo permite substituir o diretório local por outro provedor.

## Regras de edição e operação

O recrutador edita os campos da vaga enquanto ela está em rascunho. Publicar congela o conteúdo e a vaga ainda pode ser encerrada. No funil, o recrutador pode avançar uma etapa ou reprovar o candidato. A desistência e a atualização do próprio perfil são operações do candidato. O endpoint de empresas retorna uma página com os registros em `content` e aceita `sort=nome,asc` ou `sort=criada_em,desc`.

O Compose utiliza `GET /actuator/health` para verificar a disponibilidade. O endpoint retorna o status agregado. O exemplo de ambiente está em `.env.example`.

### Migrar o banco de produção

Fixe a versão do Flyway CLI no processo de implantação e valide-a em homologação. Os testes deste repositório executam o Flyway integrado ao Spring Boot. No ambiente com o CLI e suporte a PostgreSQL, injete `FLYWAY_URL`, `FLYWAY_USER` e `FLYWAY_PASSWORD` da conta de migração. Configure a URL JDBC com `sslmode=verify-full` e `sslrootcert` apontando para a CA montada. Na raiz deste projeto, execute antes da implantação:

```sh
flyway -locations=filesystem:src/main/resources/db/migration migrate
```

O Flyway valida as migrações já aplicadas antes de aplicar as novas. A aplicação de produção usa a conta de execução e recebe o caminho da CA em `POSTGRES_SSL_ROOT_CERT`. Essa conta precisa de DELETE em refresh_token para a limpeza diária às 03h17 UTC.
