import assert from "node:assert/strict";
import { test } from "node:test";
import {
  date,
  initials,
  nextJobStatus,
  options,
  pretty,
} from "../src/model.ts";

test("o recrutador pode avançar uma etapa ou reprovar", () => {
  assert.deepEqual(options("TRIAGEM"), ["ENTREVISTA", "REPROVADO"]);
  assert.deepEqual(options("ENTREVISTA"), ["TESTE_TECNICO", "REPROVADO"]);
  assert.deepEqual(options("TESTE_TECNICO"), ["PROPOSTA", "REPROVADO"]);
  assert.deepEqual(options("PROPOSTA"), ["CONTRATADO", "REPROVADO"]);
});

test("a pessoa candidata pode desistir e o administrador não altera etapas", () => {
  for (const stage of ["TRIAGEM", "ENTREVISTA", "TESTE_TECNICO", "PROPOSTA"]) {
    assert.deepEqual(options(stage, "CANDIDATO"), ["DESISTIU"]);
    assert.deepEqual(options(stage, "ADMIN"), []);
  }
  for (const stage of ["REPROVADO", "DESISTIU", "CONTRATADO"]) {
    for (const role of ["RECRUTADOR", "CANDIDATO", "ADMIN"])
      assert.deepEqual(options(stage, role), []);
  }
});

test("as ações de publicação seguem a situação atual da vaga", () => {
  assert.equal(nextJobStatus({ status: "RASCUNHO" }), "PUBLICADA");
  assert.equal(nextJobStatus({ status: "PUBLICADA" }), "ENCERRADA");
  assert.equal(nextJobStatus({ status: "ENCERRADA" }), null);
});

test("iniciais aceitam acentos, espaços e nomes de uma palavra", () => {
  assert.equal(initials("  álvaro  da  silva  "), "ÁS");
  assert.equal(initials("João"), "J");
  assert.equal(initials("  "), "");
});

test("a interface formata datas locais e preserva rótulos desconhecidos", () => {
  assert.match(date(new Date(2026, 10, 9, 12).toISOString()), /^09 nov\.?$/);
  assert.equal(pretty("HIBRIDO"), "Híbrido");
  assert.equal(pretty("SENIOR"), "Sênior");
  assert.equal(pretty("Outro nível"), "Outro nível");
});
