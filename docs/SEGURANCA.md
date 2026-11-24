# Revisão de segurança do Trilha

**19 de novembro de 2026.** Escopo: código-fonte, configuração, dependências Maven de execução e testes locais da API com Java 21 e PostgreSQL 16. O ambiente publicado, o provedor de identidade do operador e a infraestrutura de backup ficaram fora da verificação.

## Achados e correções

| Área | Achado | Resultado e limite |
| --- | --- | --- |
| 1. Autenticação | BCrypt existente e tokens rotativos. Havia geração de uma conta em memória desnecessária pelo Spring. | Desabilitada a autoconfiguração dessa conta. Senhas novas usam Argon2id com 19 MiB, duas iterações, paralelismo 1 e salt individual. Um login bem-sucedido migra o hash BCrypt existente. Access token dura 15 minutos. Refresh token tem 256 bits aleatórios, hash no banco, rotação de uso único e revogação na desativação. |
| 2. Banco e consultas | A conta que inicializa o PostgreSQL no Compose tem privilégios de administração. | Compose destinado ao desenvolvimento. Em produção, Flyway fica separado da API. A inicialização verifica privilégios, propriedade da tabela de usuários, permissão de criar no schema e TLS da conexão. Revisadas as consultas JPA e SQL com parâmetros vinculados, critérios tipados e ordenação por lista permitida. As permissões efetivas de um banco publicado exigem conferência no provedor. |
| 3. Transporte | O serviço de desenvolvimento aceita HTTP. | Execução local vinculada a loopback. Perfil prod configura conector HTTPS na porta 8443 com TLS 1.2/1.3 e HSTS. O banco usa verificação de certificado e hostname. Certificados e portas do domínio público dependem da implantação. |
| 4. Formulários | Alguns campos aceitavam entradas maiores que o necessário. | Validação no servidor para dados de cadastro, CNPJ, login, enums e paginação. Descrição de vaga limitada a 16.000 caracteres e tecnologias a 64 itens por vaga. |
| 5 e 10. XSS e cabeçalhos | Faltava uma política explícita para conteúdo web. | CSP restringe scripts à mesma origem, bloqueia objetos e enquadramento externo. Estilos em linha continuam permitidos para a interface local. Respostas usam nosniff e política de referência no-referrer. Textos retornam em JSON. A interface React exibe esses dados como texto. |
| 6 e 9. Abuso | Faltava limitação de frequência. | Por par TCP e instância, 180 requisições/minuto no total, das quais até 20 para autenticação. Excesso recebe 429 e Retry-After. O mapa tem tamanho máximo. O monitor de logs detecta séries de 401, 403 e 429 e emite alertas JSON. Ainda requer execução contínua e destino de notificações no ambiente publicado. |
| 7. Administração | Autorização concentrada nos métodos da aplicação. | Acrescentada verificação de papel no filtro para gestão de usuários e de empresas. O perfil do candidato e as rotas aninhadas de recrutadores preservam suas regras de acesso. |
| 8. Segredos | Defaults de desenvolvimento inadequados para um serviço publicado. | Perfil prod requer configuração externa de banco, JWT e TLS. Rejeita chave JWT curta, senhas de banco padrão e debug/trace. A chave da API e certificados devem ser injetados pelo ambiente. |
| 11 e 12. Sessão, CSRF e CORS | A API autentica por Bearer no cabeçalho e recebe refresh token explícito no corpo. | API stateless, cache de requisição e logout de formulário desabilitados. O teste verifica ausência de Set-Cookie. Perfil prod configura Secure, HttpOnly e SameSite=Strict como defesa adicional. O filtro rejeita origens CORS diferentes e pedidos marcados cross-site. O token anti-CSRF do Spring permanece desabilitado para esse modelo. Uma mudança para autenticação por cookie exige redesenhar o controle CSRF. |
| 13. Redefinição | Não existe rota de recuperação de senha nem emissão de links. | Não há link ativo para expirar. Em uma futura implementação, exigir validade curta, hash do token no banco e consumo único. |
| 14. Currículos | Entrada de binários por multipart. | Conferidos limite de 5 MiB, MIME PDF, assinatura %PDF, nome UUID e acesso autenticado. O download usa Content-Disposition attachment. A assinatura identifica o formato; o conteúdo interno de um PDF ainda pode ser malicioso. Não há antivírus integrado. |
| 15. Debug | Documentação interativa disponível no desenvolvimento. | Perfil prod desativa Swagger, OpenAPI, debug, trace, log de parâmetros SQL e detalhes de exceções na resposta. |
| 16. Dependências | A primeira consulta após atualizar o parent ainda encontrou avisos em sete coordenadas. | Atualizados Boot, Tomcat, Jackson, PostgreSQL JDBC, Log4j, Commons Lang, springdoc e JJWT. Consulta final ao OSV cobriu 86 coordenadas de runtime e encontrou dois avisos em Spring Web MVC. Dependabot configurado para atualizações semanais quando o projeto estiver hospedado no GitHub. As imagens de contêiner e as ferramentas de build não entram nessa contagem. |
| 17–19. Arquivos e chaves | A cópia examinada em 19 de novembro estava sem a pasta `.git`. | Inspeção local por nomes de arquivos sensíveis e assinaturas comuns de chaves privadas, AWS, GitHub e API não apontou credencial comprometida nos fontes. Ampliadas exclusões para certificados, chaves, logs e variantes de .env. Testados pedidos anônimos a /.env e /.git/config. Sem histórico Git disponível para varrer. Nenhuma chave externa foi revogada ou substituída. |
| 20–21. Verificação | Cobertura anterior focada no fluxo funcional. | Acrescentados testes de autorização administrativa, origem, cabeçalhos, sessão, limites de frequência, compatibilidade de hashes e validações do perfil prod. Os testes do perfil usam JDBC simulado; o fluxo de candidatura usa PostgreSQL real. |

## Dois avisos abertos

Na revisão de 19 de novembro, o ciclo Maven verify passou com 11 testes, zero falhas e zero testes ignorados. Essa contagem pertence à execução histórica. A suíte cresceu desde então. O resumo Surefire do passo Maven em cada [execução da CI](https://github.com/kaua-chavesvk/processo-seletivo/actions/workflows/ci.yml) traz a contagem do commit testado. A sintaxe do Compose passou em docker compose config --quiet. O teste de HSTS usa uma requisição marcada como segura em MockMvc. A negociação TLS pelo domínio público, a cadeia de certificados e a entrega de alertas ao operador ainda dependem de validação na implantação.

A árvore Maven contém Spring Web MVC 6.2.19. Inspecionamos os endpoints e não encontramos streaming SSE de fragmentos de view nem uso de XsltView, pré-condições descritas pelo fornecedor. Esta é uma avaliação do código atual. Manter os avisos abertos e reavaliá-los antes de adicionar renderização de views. Para aplicar a correção da linha 6.2 preservando Spring Boot 3, o fornecedor lista 6.2.20 via suporte empresarial. Uma migração para a linha pública corrigida muda a stack exigida pelo projeto. [Aviso Spring sobre SSE](https://spring.io/security/cve-2026-47890/) · [Aviso Spring sobre XsltView](https://spring.io/security/cve-2026-47884/)

## Implantação do perfil de produção

1. Use uma conta de migração separada para aplicar os scripts em src/main/resources/db/migration. Crie uma conta de execução sem superusuário, criação de roles/bancos, propriedade de tabelas, criação no schema ou privilégios de migração. Conceda CONNECT no banco, USAGE no schema e apenas leitura/escrita nas tabelas necessárias. Não conceda escrita no histórico do Flyway. Mantenha o PostgreSQL em rede privada, com TLS e backups cujo restauro tenha sido testado.
2. Configure SPRING_PROFILES_ACTIVE=prod, DATABASE_URL, POSTGRES_USER, POSTGRES_PASSWORD e JWT_SECRET. Gere a chave JWT com pelo menos 32 bytes aleatórios e codifique em Base64. A URL JDBC deve identificar o hostname do certificado do banco; configure a CA confiável do PostgreSQL e use sslmode=verify-full. Evite incluir senhas na URL.
3. Monte o keystore PKCS12 do domínio fora da pasta de fontes. Configure TLS_KEYSTORE com sua localização e TLS_KEYSTORE_PASSWORD via gerenciador de segredos. Se necessário, provisione o primeiro administrador com ADMIN_EMAIL e ADMIN_PASSWORD e retire essas credenciais de bootstrap após o uso.
4. Execute o JAR com essas variáveis e exponha apenas a porta HTTPS. Restrinja volumes de currículos e backups ao serviço e ao operador autorizado. O Compose padrão publica a API somente em localhost para desenvolvimento.
5. Atrás de proxy ou com várias réplicas, aplique também limites e tamanho máximo de corpo no gateway. O padrão usa o endereço TCP observado. Para um proxy fixo, configure APP_PROXIES_CONFIAVEIS com seus IPs literais separados por vírgula e configure o proxy para substituir X-Real-IP pelo IP observado na conexão de entrada. A API só aceita um IP literal nesse cabeçalho quando o par TCP está na lista. Cada réplica mantém seus próprios contadores.

## Monitoramento e repetição das verificações

Para acompanhar os eventos ao vivo em um ambiente Compose, rode:

```sh
docker compose logs --follow --tail=0 api | python scripts/monitorar_acesso.py
```

O monitor agrega eventos recebidos em 60 segundos e emite um alerta a partir de dez respostas 401, dez respostas 403 ou cinco respostas 429, por categoria. Inclui um correlation ID para investigação. O fluxo foi conferido com dez eventos de exemplo. Direcione a saída ao coletor de logs da implantação para operar alertas fora do terminal.

Para verificar os testes e os avisos de bibliotecas Maven:

```sh
./mvnw verify
./mvnw dependency:tree -Dscope=runtime -DoutputFile=target/runtime-tree.txt
python scripts/auditar_dependencias.py target/runtime-tree.txt
```

O script requer Python 3.10+ e consulta o OSV enviando coordenadas de pacote e versão. Retorna código 1 quando há avisos. Uma futura descoberta de segredo no histórico exige revogação no emissor, atualização do gerenciador de segredos e verificação de uso indevido. Para comprometimento da chave JWT, a resposta também inclui trocar a chave em todas as instâncias e revogar os refresh tokens ativos.

## Comportamento do logout e dos limites

O logout consome o refresh token apresentado. Um access token já emitido continua válido até sua expiração (padrão de 15 minutos). A desativação da conta também impede o uso dos seus access tokens nas próximas requisições.

O limitador usa dois token buckets por endereço TCP com reposição contínua. As capacidades iniciais são 180 chamadas no total e 20 de autenticação. Ao atingir 16.384 endereços em memória, a API responde 429 e Retry-After: 60 a um endereço novo até que a entrada menos recentemente usada complete um minuto de inatividade. A cota dos clientes já registrados é preservada. Essa escolha limita a memória e impede que diferentes clientes consumam uma reserva comum; o gateway deve controlar pressão distribuída de novos endereços. Múltiplas réplicas continuam exigindo limitação no gateway.

A CI consulta o OSV e falha para avisos novos. As duas exceções documentadas acima se aplicam somente às coordenadas exatas de spring-webmvc 6.2.19. A execução manual sem `--ci` continua sinalizando todos os avisos.

As exceções de versão e aviso ficam em [scripts/excecoes-auditoria.json](../scripts/excecoes-auditoria.json). O responsável deve reavaliá-las antes de `expira_em`. À meia-noite UTC dessa data a auditoria falha, inclusive se o aviso tiver desaparecido do OSV, até que a lista seja revisada.

## Revisão da interface e do armazenamento em 23 de novembro

- O painel do recrutador confere a empresa da vaga e a área do candidato busca apenas registros do usuário autenticado. Os testes verificam essas restrições e o fluxo entre dois navegadores com sessões independentes.
- A publicação na Vercel usa dados fictícios em memória. A política de conteúdo restringe recursos à própria origem. As credenciais da aplicação integrada ficam na memória da aba. Nenhuma credencial do banco ou do bucket integra a configuração pública do Vite.
- No histórico Git disponível até o [commit `8d5c6c0`](https://github.com/kaua-chavesvk/processo-seletivo/commit/8d5c6c0eb4178755648a85f14a3ef0a68432506b), a busca por assinaturas de chaves privadas e tokens de AWS, GitHub, OpenAI e Vercel examinou 208 blobs de texto e retornou zero ocorrências. O resultado cobre os formatos pesquisados.
- A integração S3 usa o cliente HTTP da JDK. A primeira resolução do SDK também incluía o transporte Apache opcional. O OSV identificou três avisos nas dependências desse transporte, que foi excluído do runtime. As exceções já documentadas de Spring MVC continuam com a mesma expiração.

## Verificação da instalação publicada em 23 de novembro

A aplicação integrada foi verificada no endereço público da Vercel com dados exclusivamente fictícios. O teste criou dois perfis, publicou uma vaga temporária, enviou um PDF e comparou os bytes do download autenticado. Depois da mudança para entrevista, a conta candidata consultou o histórico em uma sessão independente do Chromium. A mesma conta recebeu HTTP 403 ao tentar baixar o currículo. Uma leitura direta do objeto privado sem credencial também recebeu HTTP 403. O PDF e os registros criados para esse teste foram removidos.

O Neon aceitou a conexão JDBC com verificação completa do certificado e do hostname. A conta de execução não tem CREATE no schema nem UPDATE no histórico Flyway. Duas migrações foram aplicadas com a conta separada. O banco gerenciado informou PostgreSQL 18.7 e a versão do Flyway usada nessa migração indicou cobertura testada até PostgreSQL 17. A CI do projeto continua verificando PostgreSQL 16, a versão do Compose. Restauração de backup e entrega de alertas ao operador continuam fora desta verificação.
