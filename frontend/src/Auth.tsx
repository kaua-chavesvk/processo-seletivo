import { useState, type FormEvent } from "react";
import { ArrowRight } from "lucide-react";
import { json, login, request } from "./api";
import type { Profile } from "./model";

export function Auth({ done }: { done: (p: Profile) => void }) {
  const [signup, setSignup] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget),
      email = String(form.get("email")),
      senha = String(form.get("senha"));
    setBusy(true);
    setError("");
    try {
      if (signup)
        await request(
          "/auth/cadastro",
          json("POST", { email, senha, nome: String(form.get("nome")) }),
        );
      done(await login(email, senha));
    } catch (ex) {
      setError((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit} className="stack">
      {signup && (
        <label>
          Nome completo
          <input name="nome" autoComplete="name" maxLength={255} required />
        </label>
      )}
      <label>
        E-mail
        <input
          type="email"
          name="email"
          autoComplete="username"
          maxLength={255}
          required
        />
      </label>
      <label>
        Senha
        <input
          type="password"
          name="senha"
          autoComplete={signup ? "new-password" : "current-password"}
          maxLength={72}
          minLength={signup ? 8 : 1}
          required
        />
      </label>
      {error && <p role="alert">{error}</p>}
      <button disabled={busy} className="primary">
        {signup ? "Criar conta de candidato" : "Entrar"}
        <ArrowRight size={16} />
      </button>
      <button
        type="button"
        disabled={busy}
        className="link"
        onClick={() => {
          setSignup(!signup);
          setError("");
        }}
      >
        {signup ? "Já tenho uma conta" : "Criar conta de candidato"}
      </button>
    </form>
  );
}
