import { useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { json, list, request } from "./api";
import type { Job, Profile } from "./model";

type Empresa = { id: string; nome: string; cnpj: string };
export function Operacao({
  perfil,
  atualizar,
}: {
  perfil: Profile;
  atualizar: () => Promise<void>;
}) {
  const admin = perfil.papel === "ADMIN";
  const [aba, setAba] = useState(admin ? "empresa" : "vaga");
  const [aviso, setAviso] = useState(""),
    [erro, setErro] = useState(""),
    [ocupado, setOcupado] = useState(false);
  const empresas = useQuery({
    queryKey: ["empresas", perfil.id],
    queryFn: () => list<Empresa>("/empresas"),
    enabled: admin,
  });
  const vagas = useQuery({
    queryKey: ["jobs", true, perfil.id],
    queryFn: () => list<Job>("/painel/vagas"),
    enabled: !admin,
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
      } else if (aba === "recrutador") {
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
      } else if (aba === "vaga") {
        if (!perfil.empresaId)
          throw new Error("O perfil não está vinculado a uma empresa.");
        await request(
          "/empresas/" + perfil.empresaId + "/vagas",
          json("POST", {
            titulo: campo("titulo"),
            descricao: campo("descricao"),
            modalidade: campo("modalidade"),
            nivel: campo("nivel"),
            localizacao: campo("localizacao"),
            tecnologias: campo("tecnologias")
              .split(",")
              .map((t) => t.trim())
              .filter(Boolean),
          }),
        );
        await vagas.refetch();
        setAviso("Rascunho salvo. Abra Publicar para disponibilizar a vaga.");
      } else {
        await request(
          "/vagas/" + campo("vagaId") + "/status",
          json("PATCH", { status: "PUBLICADA" }),
        );
        await vagas.refetch();
        setAviso("Vaga publicada e disponível para candidaturas.");
      }
      form.reset();
      await atualizar();
    } catch (ex) {
      setErro((ex as Error).message);
    } finally {
      setOcupado(false);
    }
  }
  const abas = admin
    ? [
        ["empresa", "Empresa"],
        ["recrutador", "Recrutador"],
      ]
    : [
        ["vaga", "Nova vaga"],
        ["publicar", "Publicar"],
      ];
  const rascunhos = vagas.data?.filter((v) => v.status === "RASCUNHO") ?? [];
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
              <select name="empresaId" required>
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
        {aba === "vaga" && (
          <>
            <label>
              Título da vaga
              <input name="titulo" maxLength={255} required />
            </label>
            <label>
              Descrição
              <textarea name="descricao" maxLength={16000} rows={4} required />
            </label>
            <label>
              Modalidade
              <select name="modalidade">
                <option value="REMOTO">Remoto</option>
                <option value="HIBRIDO">Híbrido</option>
                <option value="PRESENCIAL">Presencial</option>
              </select>
            </label>
            <label>
              Nível
              <select name="nivel">
                <option value="JUNIOR">Júnior</option>
                <option value="PLENO">Pleno</option>
                <option value="SENIOR">Sênior</option>
                <option value="ESTAGIO">Estágio</option>
              </select>
            </label>
            <label>
              Localização
              <input name="localizacao" maxLength={255} required />
            </label>
            <label>
              Tecnologias, separadas por vírgula
              <input name="tecnologias" maxLength={2000} />
            </label>
          </>
        )}
        {aba === "publicar" && (
          <label>
            Vaga em rascunho
            <select name="vagaId" required>
              <option value="">Selecione a vaga</option>
              {rascunhos.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.titulo}
                </option>
              ))}
            </select>
          </label>
        )}
        {erro && <p role="alert">{erro}</p>}
        {aviso && <p role="status">{aviso}</p>}
        <button
          className="primary"
          disabled={
            ocupado ||
            (aba === "publicar" && !rascunhos.length) ||
            (aba === "recrutador" && !empresas.data?.length)
          }
        >
          {
            {
              empresa: "Criar empresa",
              recrutador: "Cadastrar recrutador",
              vaga: "Salvar rascunho",
              publicar: "Publicar vaga",
            }[aba]
          }
        </button>
      </form>
    </div>
  );
}
