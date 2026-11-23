import { test, expect } from "@playwright/test";
import { resolve } from "node:path";
import { randomUUID } from "node:crypto";

test("candidato e recrutador percorrem o mesmo processo na API Java", async ({
  page,
  browser,
}) => {
  test.skip(
    !process.env.TEST_RECRUTADOR_EMAIL,
    "Requer a instalação descartável de scripts/demonstrar.py",
  );
  const name = "Pessoa " + randomUUID().slice(0, 8);
  const email = randomUUID() + "@example.invalid";
  const senha = randomUUID();
  await page.goto("/");
  await page.getByRole("link", { name: "Ver vaga" }).first().click();
  const jobUrl = page.url();
  await page.reload();
  await page.getByRole("button", { name: "Entrar para se candidatar" }).click();
  await page.getByRole("button", { name: "Criar conta de candidato" }).click();
  await page.getByLabel("Nome completo").fill(name);
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(senha);
  await page.getByRole("button", { name: "Criar conta de candidato" }).click();
  await page
    .getByLabel("Currículo em PDF")
    .setInputFiles(resolve("../scripts/fixtures/curriculo-demo.pdf"));
  await expect(page).toHaveURL(jobUrl);
  await page.getByRole("button", { name: "Enviar candidatura" }).click();
  await expect(
    page.getByRole("button", { name: /Desenvolvedor Java/ }),
  ).toContainText("Triagem");

  const recruiter = await browser.newContext({
    baseURL: process.env.TEST_BASE_URL,
  });
  try {
    const panel = await recruiter.newPage();
    await panel.goto("/");
    await panel.getByRole("button", { name: "Entrar", exact: true }).click();
    await panel
      .getByLabel("E-mail", { exact: true })
      .fill(process.env.TEST_RECRUTADOR_EMAIL!);
    await panel
      .getByLabel("Senha", { exact: true })
      .fill(process.env.TEST_RECRUTADOR_SENHA!);
    await panel
      .getByRole("dialog")
      .getByRole("button", { name: "Entrar", exact: true })
      .click();
    await panel.getByRole("button", { name: new RegExp(name) }).click();
    const file = panel.waitForEvent("download");
    await panel.getByRole("button", { name: "Baixar currículo" }).click();
    expect((await file).suggestedFilename()).toBe("curriculo.pdf");
    await panel.getByLabel("Alterar etapa").selectOption("ENTREVISTA");
    const note = "Escolha um projeto para apresentar na entrevista.";
    await panel.getByLabel(/Orientação visível/).fill(note);
    await panel.getByRole("button", { name: "Salvar etapa" }).click();
    await expect(panel.getByRole("listitem").last()).toContainText(
      "Entrevista",
    );
    await page.getByRole("link", { name: "Vagas abertas" }).click();
    await page
      .getByRole("article")
      .filter({ hasText: "Desenvolvedor Java" })
      .getByRole("link", { name: "Ver vaga" })
      .click();
    await expect(page).toHaveURL(jobUrl);
    const inscricao = page.getByRole("region", { name: "Sua candidatura" });
    await expect(inscricao).toContainText("Entrevista");
    await expect(page.getByLabel("Currículo em PDF")).toHaveCount(0);
    await inscricao
      .getByRole("link", { name: "Acompanhar candidatura" })
      .click();
    await expect(page.getByRole("listitem").last()).toContainText("Entrevista");
    await expect(page.getByRole("listitem").last()).toContainText(note);
    await expect(
      panel.getByRole("link", { name: /Ver como Marina/ }),
    ).toHaveCount(0);

    const historico = page.url();
    expect(new URL(historico).hash).toMatch(/^#minhas\/.+/);
    await page.reload();
    await expect(
      page.getByRole("button", { name: "Entrar na minha conta" }),
    ).toBeVisible();
    await expect(page.getByText(note, { exact: true })).toHaveCount(0);
    await expect(page.getByRole("heading", { name })).toHaveCount(0);
    await page.getByRole("button", { name: "Entrar na minha conta" }).click();
    await page.getByLabel("E-mail", { exact: true }).fill(email);
    await page.getByLabel("Senha", { exact: true }).fill(senha);
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Entrar", exact: true })
      .click();
    await expect(page).toHaveURL(historico);
    await expect(page.getByRole("heading", { name })).toBeVisible();
    await expect(page.getByRole("listitem").last()).toContainText(note);

    await panel.goto(historico);
    await expect(
      panel.getByRole("link", { name: /Voltar a Funil de seleção/ }),
    ).toBeVisible();
    await expect(panel.getByText(note, { exact: true })).toHaveCount(0);
    await expect(panel.getByRole("heading", { name })).toHaveCount(0);
  } finally {
    await recruiter.close();
  }
});

test("administrador cria a equipe e o recrutador edita, publica e encerra a vaga", async ({
  page,
  browser,
}) => {
  test.skip(
    !process.env.TEST_ADMIN_EMAIL,
    "Requer a instalação descartável de scripts/demonstrar.py",
  );
  const email = randomUUID() + "@example.invalid",
    senha = randomUUID(),
    titulo = "Vaga de teste " + randomUUID().slice(0, 8);
  async function entrar(aba: typeof page, email: string, senha: string) {
    await aba.goto("/");
    await aba.getByRole("button", { name: "Entrar", exact: true }).click();
    await aba.getByLabel("E-mail", { exact: true }).fill(email);
    await aba.getByLabel("Senha", { exact: true }).fill(senha);
    await aba
      .getByRole("dialog")
      .getByRole("button", { name: "Entrar", exact: true })
      .click();
  }
  await entrar(
    page,
    process.env.TEST_ADMIN_EMAIL!,
    process.env.TEST_ADMIN_SENHA!,
  );
  await page
    .getByRole("button", { name: "Gerenciar empresas e recrutadores" })
    .click();
  await page
    .getByLabel("Nome da empresa")
    .fill("Empresa do teste de interface");
  await page.getByLabel("CNPJ").fill("04.252.011/0001-10");
  await page.getByRole("button", { name: "Criar empresa" }).click();
  await expect(page.getByRole("dialog").getByRole("status")).toContainText(
    "Empresa criada",
  );
  await page.getByRole("button", { name: "Recrutador", exact: true }).click();
  const empresa = page.getByRole("option", {
    name: /Empresa do teste de interface/,
  });
  await page
    .getByLabel("Empresa", { exact: true })
    .selectOption((await empresa.getAttribute("value")) ?? "");
  await page.getByLabel("Nome do recrutador").fill("Recrutador da interface");
  await page.getByLabel("E-mail do recrutador").fill(email);
  await page.getByLabel("Senha do recrutador").fill(senha);
  await page.getByRole("button", { name: "Cadastrar recrutador" }).click();
  await expect(page.getByRole("dialog").getByRole("status")).toContainText(
    "Recrutador cadastrado",
  );
  await page.getByRole("button", { name: "Fechar", exact: true }).click();
  await page.getByRole("button", { name: "Sair", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "Entrar", exact: true }),
  ).toBeVisible();
  const contexto = await browser.newContext({
    baseURL: process.env.TEST_BASE_URL,
  });
  try {
    const painel = await contexto.newPage();
    await entrar(painel, email, senha);
    await painel.getByRole("link", { name: "Minhas vagas" }).click();
    await painel.getByRole("button", { name: "Nova vaga" }).click();
    await painel.getByLabel("Título da vaga").fill(titulo);
    await painel
      .getByLabel("Descrição")
      .fill("Oportunidade fictícia do teste automático.");
    await painel.getByLabel("Localização").fill("Brasil");
    await painel.getByRole("button", { name: "Salvar rascunho" }).click();
    const vaga = painel.getByRole("article").filter({ hasText: titulo });
    await vaga.getByRole("button", { name: "Editar rascunho" }).click();
    const descricaoFinal =
      "Realizar entrevistas com o time e acompanhar a entrega.";
    await painel.getByLabel("Descrição").fill(descricaoFinal);
    await painel.getByRole("button", { name: "Salvar rascunho" }).click();
    await vaga.getByRole("button", { name: "Ver prévia" }).click();
    await expect(
      painel.getByText(descricaoFinal, { exact: true }),
    ).toBeVisible();
    await painel
      .getByRole("button", { name: "Voltar para minhas vagas" })
      .click();
    await vaga.getByRole("button", { name: "Publicar vaga" }).click();
    await expect(vaga).toContainText("Publicada");
    await page.reload();
    const anuncio = page.getByRole("article").filter({ hasText: titulo });
    await anuncio.getByRole("link", { name: "Ver vaga" }).click();
    await expect(page.getByText(descricaoFinal, { exact: true })).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Entrar para se candidatar" }),
    ).toBeVisible();
    await vaga.getByRole("button", { name: "Encerrar vaga" }).click();
    await painel
      .getByRole("button", { name: "Confirmar encerramento" })
      .click();
    await expect(vaga).toContainText("Encerrada");
    await page.reload();
    await expect(
      page.getByRole("heading", { name: "Vaga indisponível" }),
    ).toBeVisible();
    await page.getByRole("link", { name: "Voltar às vagas abertas" }).click();
    await expect(
      page.getByRole("heading", { name: /Desenvolvedor Java/ }),
    ).toBeVisible();
    await expect(anuncio).toHaveCount(0);
  } finally {
    await contexto.close();
  }
});
