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
  await page.getByRole("button", { name: /Analista de Dados/ }).click();
  await page.getByRole("button", { name: "Desistir da candidatura" }).click();
  await page.getByRole("button", { name: "Confirmar desistência" }).click();
  await expect(page.getByRole("listitem").last()).toContainText("Desistiu");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
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
