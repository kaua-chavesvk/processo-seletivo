import { test, expect } from "@playwright/test";

test("recrutamento: mudança de etapa, histórico e busca", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("button", { name: "Quadro" })).toHaveAttribute(
    "aria-pressed",
    "true",
  );
  const from = page.getByRole("region", { name: "Triagem", exact: true }),
    to = page.getByRole("region", { name: "Entrevista", exact: true });
  await expect(
    from.getByRole("button", { name: /Marina Costa/ }),
  ).toBeVisible();
  await from.getByRole("button", { name: /Marina Costa/ }).dragTo(to);
  await expect(
    page.getByRole("link", { name: /Ver como Marina/ }),
  ).toBeVisible();
  await to.getByRole("button", { name: /Marina Costa/ }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByRole("listitem")).toHaveCount(2);
  await dialog.getByLabel("Alterar etapa").selectOption("TESTE_TECNICO");
  await dialog.getByRole("button", { name: "Salvar etapa" }).click();
  await expect(dialog.getByRole("listitem")).toHaveCount(3);
  await dialog.getByRole("button", { name: "Fechar" }).click();
  await page.getByLabel("Buscar candidato").fill("marina");
  await expect(page.locator(".person")).toHaveCount(1);
  await expect(
    page
      .getByRole("region", { name: "Teste técnico" })
      .getByRole("button", { name: /Marina Costa/ }),
  ).toBeVisible();
});

test("vaga já disputada mostra a etapa atual e abre o histórico da inscrição", async ({
  page,
}) => {
  await page.goto("/#vagas/produto");
  const inscricao = page.getByRole("region", { name: "Sua candidatura" });
  await expect(inscricao).toContainText("Você já se candidatou");
  await expect(inscricao).toContainText("Triagem");
  await expect(
    page.getByRole("button", { name: "Simular candidatura" }),
  ).toHaveCount(0);
  const acompanhar = inscricao.getByRole("link", {
    name: "Acompanhar candidatura",
  });
  const destino = await acompanhar.getAttribute("href");
  expect(destino).toMatch(/^#minhas\/.+/);

  await page.getByRole("link", { name: "Funil de seleção" }).click();
  await page.getByRole("button", { name: /Marina Costa/ }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Alterar etapa").selectOption("ENTREVISTA");
  await dialog.getByRole("button", { name: "Salvar etapa" }).click();
  await expect(dialog.getByRole("listitem").last()).toContainText("Entrevista");
  await dialog.getByRole("button", { name: "Fechar" }).click();

  await page.getByRole("link", { name: "Vagas abertas" }).click();
  await page
    .getByRole("article")
    .filter({ hasText: "Product Designer" })
    .getByRole("link", { name: "Ver vaga" })
    .click();
  await expect(inscricao).toContainText("Entrevista");
  await expect(acompanhar).toHaveAttribute("href", destino!);
  await acompanhar.click();
  await expect(
    page.getByRole("heading", { name: "Marina Costa" }),
  ).toBeVisible();
  await expect(page.getByRole("listitem").last()).toContainText("Entrevista");
});

test("candidatura fictícia e desistência em tela pequena", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await page.getByRole("button", { name: "Abrir navegação" }).click();
  await page.getByRole("link", { name: "Vagas abertas" }).click();
  await page
    .getByRole("article")
    .filter({ hasText: "Analista de Dados" })
    .getByRole("link", { name: "Ver vaga" })
    .click();
  await page.getByRole("button", { name: "Simular candidatura" }).click();
  await expect(
    page.getByRole("button", { name: /Analista de Dados/ }),
  ).toContainText("Triagem");
  await page.getByRole("button", { name: "Abrir navegação" }).click();
  await page.getByRole("link", { name: "Vagas abertas" }).click();
  await page
    .getByRole("article")
    .filter({ hasText: "Analista de Dados" })
    .getByRole("link", { name: "Ver vaga" })
    .click();
  const inscricao = page.getByRole("region", { name: "Sua candidatura" });
  await expect(inscricao).toContainText("Triagem");
  await inscricao.getByRole("link", { name: "Acompanhar candidatura" }).click();
  await expect(page.getByText(/Analista de Dados/)).toBeVisible();
  await page.getByRole("button", { name: "Desistir da candidatura" }).click();
  await page.getByRole("button", { name: "Confirmar desistência" }).click();
  await expect(page.getByRole("listitem").last()).toContainText("Desistiu");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
});

test("recrutadora edita o rascunho, confere a prévia e controla a publicação", async ({
  page,
}) => {
  const novoTitulo = "Designer de pesquisa";
  const descricao = "Planejar entrevistas e testar protótipos com usuários.";
  await page.goto("/");
  const minhasVagas = page.getByRole("link", {
    name: "Minhas vagas",
    exact: true,
  });
  const vagasAbertas = page.getByRole("link", {
    name: "Vagas abertas",
    exact: true,
  });
  await minhasVagas.click();
  await page.getByRole("button", { name: "Nova vaga" }).click();
  await page.getByLabel("Título da vaga").fill("Rascunho de pesquisa");
  await page.getByLabel("Descrição").fill(descricao);
  await page.getByLabel("Localização").fill("Recife");
  await page.getByRole("button", { name: "Salvar rascunho" }).click();
  const rascunho = page
    .getByRole("article")
    .filter({ hasText: "Rascunho de pesquisa" });
  await rascunho.getByRole("button", { name: "Editar rascunho" }).click();
  await expect(page.getByLabel("Descrição")).toHaveValue(descricao);
  await page.getByLabel("Título da vaga").fill(novoTitulo);
  await page.getByRole("button", { name: "Salvar rascunho" }).click();
  const vaga = page.getByRole("article").filter({ hasText: novoTitulo });
  await vaga.getByRole("button", { name: "Ver prévia" }).click();
  await expect(
    page.getByRole("heading", { name: novoTitulo, exact: true }),
  ).toBeVisible();
  await expect(page.getByText(descricao, { exact: true })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Simular candidatura" }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "Voltar para minhas vagas" }).click();
  await vagasAbertas.click();
  await expect(
    page.getByRole("heading", { name: "Product Designer" }),
  ).toBeVisible();
  await expect(vaga).toHaveCount(0);

  await minhasVagas.click();
  await vaga.getByRole("button", { name: "Publicar vaga" }).click();
  await expect(vaga).toContainText("Publicada");
  await vagasAbertas.click();
  await vaga.getByRole("link", { name: "Ver vaga" }).click();
  const endereco = page.url();
  await page.getByRole("button", { name: "Simular candidatura" }).click();
  await expect(
    page.getByRole("button", { name: new RegExp(novoTitulo) }),
  ).toContainText("Triagem");

  await minhasVagas.click();
  await vaga.getByRole("button", { name: "Encerrar vaga" }).click();
  await page.getByRole("button", { name: "Cancelar", exact: true }).click();
  await expect(vaga).toContainText("Publicada");
  await vaga.getByRole("button", { name: "Encerrar vaga" }).click();
  await page.getByRole("button", { name: "Confirmar encerramento" }).click();
  await page.getByLabel("Filtrar por situação").selectOption("ENCERRADA");
  await expect(vaga).toContainText("Encerrada");
  await vagasAbertas.click();
  await expect(
    page.getByRole("heading", { name: "Product Designer" }),
  ).toBeVisible();
  await expect(vaga).toHaveCount(0);
  await page.goto(endereco);
  await expect(
    page.getByRole("heading", { name: "Vaga indisponível" }),
  ).toBeVisible();
  const inscricao = page.getByRole("region", { name: "Sua candidatura" });
  await expect(inscricao).toContainText("Triagem");
  await expect(
    page.getByRole("button", { name: "Simular candidatura" }),
  ).toHaveCount(0);
  await inscricao.getByRole("link", { name: "Acompanhar candidatura" }).click();
  await expect(page.getByRole("listitem").last()).toContainText("Triagem");
  await expect(page.getByText(new RegExp(novoTitulo))).toBeVisible();
});

test("lista móvel filtra etapas e leva a recrutadora ao histórico da Marina", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await expect(page.getByRole("button", { name: "Lista" })).toHaveAttribute(
    "aria-pressed",
    "true",
  );
  const filter = page.getByLabel("Filtrar por etapa");
  await filter.selectOption("ENTREVISTA");
  await expect(page.getByRole("region")).toHaveCount(1);
  await expect(
    page.getByRole("button", { name: /Camila Ribeiro/ }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Encerradas", exact: true }).click();
  await expect(filter).toHaveValue("");
  await filter.selectOption("CONTRATADO");
  await expect(page.getByRole("button", { name: /Ana Dias/ })).toBeVisible();
  await page.getByRole("button", { name: "Em andamento", exact: true }).click();
  await filter.selectOption("TRIAGEM");
  await page.getByRole("button", { name: /Marina Costa/ }).click();
  const dialog = page.getByRole("dialog");
  const note = "Leve um projeto para a entrevista.";
  await dialog.getByLabel("Alterar etapa").selectOption("ENTREVISTA");
  await dialog.getByLabel(/Orientação visível/).fill(note);
  await dialog.getByRole("button", { name: "Salvar etapa" }).click();
  await dialog.getByRole("link", { name: /Ver como Marina/ }).click();
  await expect(
    page.getByRole("heading", { name: "Marina Costa" }),
  ).toBeFocused();
  await expect(page.getByRole("listitem").last()).toContainText("Entrevista");
  await expect(page.getByRole("listitem").last()).toContainText(note);
  await expect(page.getByLabel("Alterar etapa")).toHaveCount(0);
  await page.getByRole("link", { name: "Voltar ao funil de seleção" }).click();
  await expect(
    page
      .getByRole("region", { name: "Entrevista" })
      .getByRole("button", { name: /Marina Costa/ }),
  ).toBeVisible();
});

test("visitante copia a vaga e abre o endereço diretamente em outra aba", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  await page.goto("/?origem=portfolio#vagas");
  await page
    .getByRole("article")
    .filter({ hasText: "Analista de Dados" })
    .getByRole("link", { name: "Ver vaga" })
    .click();
  await expect(page).toHaveURL(/#vagas\/dados$/);
  await page.getByRole("button", { name: "Copiar link da vaga" }).click();
  await expect(page.getByRole("status")).toHaveText("Link da vaga copiado.");
  const address = await page.evaluate(() => navigator.clipboard.readText());
  const canonical = new URL(page.url());
  canonical.search = "";
  expect(address).toBe(canonical.href);
  const tab = await context.newPage();
  await tab.goto(address);
  await expect(
    tab.getByRole("heading", { name: "Analista de Dados" }),
  ).toBeVisible();
  await expect(
    tab.getByRole("button", { name: "Simular candidatura" }),
  ).toBeVisible();
  await page.goBack();
  await expect(
    page.getByRole("heading", { name: "Vagas abertas" }),
  ).toBeVisible();
  await tab.goto("/#vagas/indisponivel");
  await expect(
    tab.getByRole("heading", { name: "Vaga indisponível" }),
  ).toBeVisible();
});

test("filtra vagas do quadro e exibe as candidaturas encerradas", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Encerradas", exact: true }).click();
  await expect(page.getByRole("button", { name: /Ana Dias/ })).toBeVisible();
  await page.getByLabel("Vaga selecionada").selectOption("frontend");
  await page.getByRole("button", { name: "Em andamento", exact: true }).click();
  await expect(
    page.getByRole("button", { name: /Felipe Alves/ }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: /Marina Costa/ })).toHaveCount(
    0,
  );
});
