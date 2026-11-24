import assert from "node:assert/strict";
import { test } from "node:test";

const tokens = (name) => ({
  accessToken: name,
  refreshToken: "refresh-" + name,
});
const unauthorized = () => new Response(null, { status: 401 });

async function client(t, handler) {
  // A fresh module models a newly opened browser tab.
  const api = await import("../src/api.ts?tab=" + encodeURIComponent(t.name));
  const events = new EventTarget();
  globalThis.window = events;
  t.after(() => {
    delete globalThis.window;
  });
  const calls = [];
  t.mock.method(globalThis, "fetch", async (path, init) => {
    const token = init.headers.get("Authorization");
    const body = init.body ? JSON.parse(init.body) : {};
    calls.push({ path, token, body });
    if (path === "/auth/login") return Response.json(tokens(body.email));
    if (path === "/painel/perfil")
      return Response.json({ id: token, nome: "Teste", papel: "CANDIDATO" });
    if (path === "/auth/logout") return new Response(null, { status: 204 });
    return handler(path, token, body);
  });
  await api.login("A", "senha");
  return { api, events, calls };
}

test("dois pedidos com 401 renovam uma vez e repetem com o mesmo token", async (t) => {
  const pending = Promise.withResolvers();
  const started = Promise.withResolvers();
  const { api, calls } = await client(t, (path, token) => {
    if (path === "/auth/refresh") {
      started.resolve();
      return pending.promise;
    }
    return token === "Bearer A" ? unauthorized() : Response.json(path);
  });
  const both = Promise.all([api.read("/vagas"), api.read("/minhas")]);
  await started.promise;
  pending.resolve(Response.json(tokens("A2")));
  assert.deepEqual(await both, ["/vagas", "/minhas"]);
  assert.deepEqual(
    calls
      .filter((c) => c.path === "/auth/refresh")
      .map((c) => c.body.refreshToken),
    ["refresh-A"],
  );
  for (const path of ["/vagas", "/minhas"])
    assert.deepEqual(
      calls.filter((c) => c.path === path).map((c) => c.token),
      ["Bearer A", "Bearer A2"],
    );
});

test("um 401 atrasado reutiliza o token que a outra consulta já renovou", async (t) => {
  const late = Promise.withResolvers();
  const { api, calls } = await client(t, (path, token) => {
    if (path === "/auth/refresh") return Response.json(tokens("A2"));
    if (token === "Bearer A")
      return path === "/lenta" ? late.promise : unauthorized();
    return Response.json(token);
  });
  const slow = api.read("/lenta");
  assert.equal(await api.read("/rapida"), "Bearer A2");
  late.resolve(unauthorized());
  assert.equal(await slow, "Bearer A2");
  assert.equal(calls.filter((c) => c.path === "/auth/refresh").length, 1);
});

test("refresh recusado expira a sessão uma vez para todas as consultas", async (t) => {
  const { api, events, calls } = await client(t, unauthorized);
  let expired = 0;
  events.addEventListener("session-expired", () => expired++);
  const results = await Promise.allSettled([
    api.read("/vagas"),
    api.read("/minhas"),
  ]);
  assert.ok(
    results.every((r) => r.status === "rejected" && r.reason.status === 401),
  );
  assert.equal(expired, 1);
  await assert.rejects(api.read("/minhas"), { status: 401 });
  assert.equal(calls.at(-1).token, null);
  assert.equal(calls.filter((c) => c.path === "/auth/refresh").length, 1);
});

test("uma falha temporária de renovação permite uma nova tentativa", async (t) => {
  let attempts = 0;
  const { api } = await client(t, (path, token) => {
    if (path === "/auth/refresh")
      return ++attempts === 1
        ? new Response(null, { status: 503 })
        : Response.json(tokens("A2"));
    return token === "Bearer A2" ? Response.json(token) : unauthorized();
  });
  await assert.rejects(api.read("/minhas"), { status: 503 });
  assert.equal(await api.read("/minhas"), "Bearer A2");
});

test("a nova conta renova enquanto a resposta da conta anterior está pendente", async (t) => {
  const stale = Promise.withResolvers();
  const started = Promise.withResolvers();
  const { api, calls } = await client(t, (path, token, body) => {
    if (path === "/auth/refresh") {
      if (body.refreshToken === "refresh-A") {
        started.resolve();
        return stale.promise;
      }
      return Response.json(tokens("B2"));
    }
    return token === "Bearer B2" ? Response.json(token) : unauthorized();
  });
  const old = assert.rejects(api.read("/antiga"), /sessão mudou/);
  await started.promise;
  await api.logout();
  await api.login("B", "senha");
  assert.equal(await api.read("/nova"), "Bearer B2");
  stale.resolve(Response.json(tokens("A2")));
  await old;
  assert.deepEqual(
    calls
      .filter((c) => c.path === "/auth/logout")
      .map((c) => c.body.refreshToken),
    ["refresh-A", "refresh-A2"],
  );
  assert.equal(await api.read("/nova"), "Bearer B2");
});
