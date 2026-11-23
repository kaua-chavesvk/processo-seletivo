import { useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { json, list, request } from "./api";
import type { Profile } from "./model";

type Empresa = { id: string; nome: string; cnpj: string };
export function Operacao({ perfil }: { perfil: Profile }) {
  const [aba, setAba] = useState("empresa");
  const [aviso, setAviso] = useState(""),
    [erro, setErro] = useState(""),
    [ocupado, setOcupado] = useState(false);
  const empresas = useQuery({
    queryKey: ["empresas", perfil.id],
    queryFn: () => list<Empresa>("/empresas"),
    enabled: perfil.papel === "ADMIN",
  });
  async function salvar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (ocupado) return;
    const form = event.currentTarget,
      campos = new FormData(form),
      campo = (k: string) => String(campos.get(k) ?? "").trim();
    setOcupado(true);
    setErro("");
    setAviso("");
    try {
      if (aba === "empresa") {
        await request(
          "/empresas",
          json("POST", { nome: campo("nome"), cnpj: campo("cnpj") }),
        );
        await empresas.refetch();
        setAviso("Empresa criada. Você já pode vincular um recrutador.");
      } else {
        await request(
          "/usuarios",
          json("POST", {
            papel: "RECRUTADOR",
            empresaId: campo("empresaId"),
            dados: {
              nome: campo("nome"),
              email: campo("email"),
              senha: String(campos.get("senha")),
            },
          }),
        );
        setAviso("Recrutador cadastrado e vinculado à empresa.");
      }
      form.reset();
    } catch (ex) {
      setErro((ex as Error).message);
    } finally {
      setOcupado(false);
    }
  }
  const abas = [
    ["empresa", "Empresa"],
    ["recrutador", "Recrutador"],
  ];
  return (
    <div className="stack">
      <div className="tabs">
        {abas.map(([id, titulo]) => (
          <button
            key={id}
            type="button"
            className={aba === id ? "on" : ""}
            disabled={ocupado}
            onClick={() => {
              setAba(id);
              setAviso("");
              setErro("");
            }}
            aria-pressed={aba === id}
          >
            {titulo}
          </button>
        ))}
      </div>
      <form key={aba} className="stack" onSubmit={salvar}>
        {aba === "empresa" && (
          <>
            <label>
              Nome da empresa
              <input name="nome" maxLength={255} required />
            </label>
            <label>
              CNPJ
              <input name="cnpj" maxLength={18} required />
            </label>
          </>
        )}
        {aba === "recrutador" && (
          <>
            <label>
              Empresa
              <select name="empresaId" aria-label="Empresa" required>
                <option value="">Selecione a empresa</option>
                {empresas.data?.map((e) => (
                  <option key={e.id} value={e.id}>
                    {e.nome} · {e.cnpj}
                  </option>
                ))}
              </select>
            </label>
            {empresas.isError && (
              <p role="alert">
                {empresas.error.message}{" "}
                <button
                  className="link"
                  type="button"
                  onClick={() => empresas.refetch()}
                >
                  Tentar novamente
                </button>
              </p>
            )}
            <label>
              Nome do recrutador
              <input name="nome" maxLength={255} required />
            </label>
            <label>
              E-mail do recrutador
              <input name="email" type="email" maxLength={255} required />
            </label>
            <label>
              Senha do recrutador
              <input
                name="senha"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                required
              />
            </label>
          </>
        )}
        {erro && <p role="alert">{erro}</p>}
        {aviso && <p role="status">{aviso}</p>}
        <button
          className="primary"
          disabled={ocupado || (aba === "recrutador" && !empresas.data?.length)}
        >
          {aba === "empresa" ? "Criar empresa" : "Cadastrar recrutador"}
        </button>
      </form>
    </div>
  );
}
