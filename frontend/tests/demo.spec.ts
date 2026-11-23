import { test, expect } from "@playwright/test";

test("recrutamento: mudança de etapa, histórico e busca", async ({ page }) => {
  await page.goto("/");
  const from = page.getByRole("region", { name: "Triagem", exact: true }),
    to = page.getByRole("region", { name: "Entrevista", exact: true });
  await expect(
    from.getByRole("button", { name: /Marina Costa/ }),
  ).toBeVisible();
  await from.getByRole("button", { name: /Marina Costa/ }).dragTo(to);
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
    .getByRole("button", { name: "Ver vaga" })
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
