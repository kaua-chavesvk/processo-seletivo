import type { Profile } from "./model";
type Session = { accessToken: string; refreshToken: string };
let session: Session | null = null;
let generation = 0;
let renewing: Promise<void> | undefined;
export const json = (method: string, data: unknown): RequestInit => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(data),
});
async function error(response: Response) {
  const problem = await response.json().catch(() => ({}));
  const fallback: Record<number, string> = {
    401: "Confira seu e-mail e sua senha.",
    403: "Seu perfil não tem acesso a esta ação.",
    409: "Este registro já existe.",
    413: "O arquivo excede o limite permitido.",
    429: "Muitas tentativas. Aguarde um minuto.",
  };
  return new Error(
    problem.detail ??
      fallback[response.status] ??
      "A solicitação falhou (HTTP " + response.status + ").",
  );
}
async function send(path: string, init: RequestInit = {}, token?: string) {
  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", "Bearer " + token);
  try {
    return await fetch(path, { ...init, headers, credentials: "omit" });
  } catch {
    throw new Error("Falha de conexão. Confira sua rede e tente novamente.");
  }
}
async function renew(previous: Session, version: number) {
  const res = await send(
    "/auth/refresh",
    json("POST", { refreshToken: previous.refreshToken }),
  );
  if (!res.ok) {
    if (generation === version && (res.status === 401 || res.status === 403)) {
      session = null;
      generation++;
      window.dispatchEvent(new Event("session-expired"));
    }
    throw await error(res);
  }
  const next: Session = await res.json();
  if (generation === version) session = next;
  else
    await send(
      "/auth/logout",
      json("POST", { refreshToken: next.refreshToken }),
    );
}
export async function request(path: string, init: RequestInit = {}) {
  const previous = session,
    version = generation;
  let res = await send(path, init, previous?.accessToken);
  if (res.status === 401 && previous && generation === version) {
    if (session === previous) {
      if (!renewing)
        renewing = renew(previous, version).finally(() => {
          renewing = undefined;
        });
      await renewing;
    }
    if (session && generation === version)
      res = await send(path, init, session.accessToken);
  }
  if (generation !== version)
    throw new Error("A sessão mudou. Entre novamente.");
  if (!res.ok) throw await error(res);
  return res;
}
export async function read<T>(path: string, init?: RequestInit): Promise<T> {
  return (await request(path, init)).json();
}
export async function list<T>(path: string): Promise<T[]> {
  const result: T[] = [];
  for (let page = 0; ; page++) {
    const data = await read<{
      content: T[];
      totalPages?: number;
      page?: { totalPages: number };
    }>(path + "?size=100&page=" + page);
    result.push(...data.content);
    if (page + 1 >= (data.totalPages ?? data.page?.totalPages ?? 1))
      return result;
  }
}
export async function login(email: string, senha: string): Promise<Profile> {
  const res = await send("/auth/login", json("POST", { email, senha }));
  if (!res.ok) throw await error(res);
  generation++;
  session = await res.json();
  try {
    return await read<Profile>("/painel/perfil");
  } catch (ex) {
    await logout();
    throw ex;
  }
}
export async function logout() {
  const previous = session;
  generation++;
  session = null;
  if (previous) {
    const res = await send(
      "/auth/logout",
      json("POST", { refreshToken: previous.refreshToken }),
    );
    if (!res.ok && res.status !== 401) throw await error(res);
  }
}
export async function download(id: string) {
  const response = await request("/candidaturas/" + id + "/curriculo");
  const url = URL.createObjectURL(await response.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = "curriculo.pdf";
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
