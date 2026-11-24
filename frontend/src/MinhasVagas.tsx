import { useEffect, useRef, useState, type FormEvent } from "react";
import { recruitment } from "./recruitment";
import { nextJobStatus, pretty, type Job } from "./model";
import { ResumoVaga } from "./ResumoVaga";

type Tela =
  | { tipo: "lista" | "criar" }
  | { tipo: "editar" | "previa" | "encerrar"; id: string };

const situacoes = [
  ["TODOS", "Todas"],
  ["RASCUNHO", "Rascunhos"],
  ["PUBLICADA", "Publicadas"],
  ["ENCERRADA", "Encerradas"],
] as const;

function CamposVaga({ vaga }: { vaga?: Job }) {
  return (
    <>
      <label>
        Título da vaga
        <input
          name="titulo"
          maxLength={255}
          defaultValue={vaga?.titulo}
          required
        />
      </label>
      <label>
        Descrição
        <textarea
          name="descricao"
          maxLength={16000}
          rows={4}
          defaultValue={vaga?.descricao}
          required
        />
      </label>
      <label>
        Modalidade
        <select name="modalidade" defaultValue={vaga?.modalidade ?? "REMOTO"}>
          <option value="REMOTO">Remoto</option>
          <option value="HIBRIDO">Híbrido</option>
          <option value="PRESENCIAL">Presencial</option>
        </select>
      </label>
      <label>
        Nível
        <select name="nivel" defaultValue={vaga?.nivel ?? "JUNIOR"}>
          <option value="JUNIOR">Júnior</option>
          <option value="PLENO">Pleno</option>
          <option value="SENIOR">Sênior</option>
          <option value="ESTAGIO">Estágio</option>
        </select>
      </label>
      <label>
        Localização
        <input
          name="localizacao"
          maxLength={255}
          defaultValue={vaga?.localizacao}
          required
        />
      </label>
      <label>
        Tecnologias, separadas por vírgula
        <input name="tecnologias" defaultValue={vaga?.tecnologias.join(", ")} />
      </label>
    </>
  );
}

export function MinhasVagas({
  empresaId,
  vagas,
  atualizar,
}: {
  empresaId?: string | null;
  vagas: Job[];
  atualizar: () => Promise<void>;
}) {
  const [tela, setTela] = useState<Tela>({ tipo: "lista" });
  const [filtro, setFiltro] = useState("TODOS");
  const [aviso, setAviso] = useState("");
  const [erro, setErro] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const tituloInterno = useRef<HTMLHeadingElement>(null);
  const vaga = "id" in tela ? vagas.find((v) => v.id === tela.id) : undefined;
  const visiveis = vagas.filter(
    (v) => filtro === "TODOS" || v.status === filtro,
  );
  useEffect(() => {
    if (tela.tipo !== "lista") tituloInterno.current?.focus();
  }, [tela]);

  function abrir(proxima: Tela) {
    setTela(proxima);
    setErro("");
    setAviso("");
  }

  async function executar(
    operacao: () => Promise<void>,
    status: string,
    mensagem: string,
  ) {
    if (ocupado) return;
    setOcupado(true);
    setAviso("");
    setErro("");
    try {
      await operacao();
      await atualizar();
      if (filtro !== "TODOS") setFiltro(status);
      setTela({ tipo: "lista" });
      setAviso(mensagem);
    } catch (ex) {
      setErro((ex as Error).message);
    } finally {
      setOcupado(false);
    }
  }

  function salvar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const campos = new FormData(event.currentTarget);
    const campo = (nome: string) => String(campos.get(nome) ?? "").trim();
    const dados = {
      titulo: campo("titulo"),
      descricao: campo("descricao"),
      modalidade: campo("modalidade"),
      nivel: campo("nivel"),
      localizacao: campo("localizacao"),
      tecnologias: [
        ...new Set(
          campo("tecnologias")
            .split(",")
            .map((t) => t.trim().toLowerCase())
            .filter(Boolean),
        ),
      ],
    };
    void executar(
      async () => {
        if (!dados.titulo || !dados.descricao || !dados.localizacao)
          throw new Error(
            "Preencha o título, a descrição e a localização da vaga.",
          );
        if (
          dados.tecnologias.length > 64 ||
          dados.tecnologias.some((t) => t.length > 255)
        )
          throw new Error(
            "Use até 64 tecnologias, com no máximo 255 caracteres cada.",
          );
        await recruitment.saveDraft(dados, vaga?.id, empresaId);
      },
      "RASCUNHO",
      "Rascunho salvo.",
    );
  }

  function mudarSituacao(v: Job, status: "PUBLICADA" | "ENCERRADA") {
    void executar(
      () => recruitment.setJobStatus(v.id, status),
      status,
      status === "PUBLICADA" ? "Vaga publicada." : "Vaga encerrada.",
    );
  }

  return (
    <section className="stack">
      <h1>Minhas vagas</h1>
      {aviso && (
        <p role="status" className="notice">
          {aviso}
        </p>
      )}
      {erro && <p role="alert">{erro}</p>}
      {tela.tipo === "criar" ||
      (tela.tipo === "editar" && vaga?.status === "RASCUNHO") ? (
        <form
          className="stack job-editor"
          key={vaga?.id ?? "nova"}
          onSubmit={salvar}
        >
          <h2 ref={tituloInterno} tabIndex={-1}>
            {vaga ? "Editar rascunho" : "Nova vaga"}
          </h2>
          <CamposVaga vaga={vaga} />
          <div className="job-actions">
            <button className="primary" disabled={ocupado}>
              Salvar rascunho
            </button>
            <button
              className="secondary"
              type="button"
              disabled={ocupado}
              onClick={() => abrir({ tipo: "lista" })}
            >
              Cancelar
            </button>
          </div>
        </form>
      ) : tela.tipo === "previa" && vaga ? (
        <section className="stack job-preview">
          <p className="eyebrow">PRÉVIA DA VAGA</p>
          <h2 ref={tituloInterno} tabIndex={-1}>
            {vaga.titulo}
          </h2>
          <ResumoVaga job={vaga} />
          <div className="job-actions">
            {nextJobStatus(vaga) === "PUBLICADA" && (
              <>
                <button
                  className="primary"
                  disabled={ocupado}
                  onClick={() => mudarSituacao(vaga, "PUBLICADA")}
                >
                  Publicar vaga
                </button>
                <button
                  className="secondary"
                  disabled={ocupado}
                  onClick={() => abrir({ tipo: "editar", id: vaga.id })}
                >
                  Editar rascunho
                </button>
              </>
            )}
            <button
              className="link"
              disabled={ocupado}
              onClick={() => abrir({ tipo: "lista" })}
            >
              Voltar para minhas vagas
            </button>
          </div>
        </section>
      ) : (
        <>
          <div className="jobs-toolbar">
            <label>
              Filtrar por situação
              <select
                value={filtro}
                disabled={ocupado}
                onChange={(e) => {
                  setFiltro(e.target.value);
                  abrir({ tipo: "lista" });
                }}
              >
                {situacoes.map(([valor, nome]) => (
                  <option key={valor} value={valor}>
                    {nome}
                  </option>
                ))}
              </select>
            </label>
            <button
              className="primary"
              disabled={ocupado}
              onClick={() => abrir({ tipo: "criar" })}
            >
              Nova vaga
            </button>
          </div>
          <div className="managed-jobs">
            {visiveis.map((v) => (
              <article
                key={v.id}
                className="managed-job"
                aria-labelledby={"vaga-" + v.id}
              >
                <span className={"status status-" + v.status.toLowerCase()}>
                  {pretty(v.status)}
                </span>
                <h2 id={"vaga-" + v.id}>{v.titulo}</h2>
                <p className="meta">
                  {v.localizacao} · {pretty(v.modalidade)} · {pretty(v.nivel)}
                </p>
                {tela.tipo === "encerrar" && tela.id === v.id ? (
                  <div className="close-confirm">
                    <h3 tabIndex={-1} ref={tituloInterno}>
                      Encerrar esta vaga?
                    </h3>
                    <p>
                      O anúncio sairá das vagas abertas. As candidaturas já
                      recebidas continuarão disponíveis no funil.
                    </p>
                    <div className="job-actions">
                      <button
                        className="danger"
                        disabled={ocupado}
                        onClick={() => mudarSituacao(v, "ENCERRADA")}
                      >
                        Confirmar encerramento
                      </button>
                      <button
                        className="secondary"
                        disabled={ocupado}
                        onClick={() => abrir({ tipo: "lista" })}
                      >
                        Cancelar
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="job-actions">
                    <button
                      className="secondary"
                      disabled={ocupado}
                      onClick={() => abrir({ tipo: "previa", id: v.id })}
                    >
                      Ver prévia
                    </button>
                    {nextJobStatus(v) === "PUBLICADA" && (
                      <>
                        <button
                          className="secondary"
                          disabled={ocupado}
                          onClick={() => abrir({ tipo: "editar", id: v.id })}
                        >
                          Editar rascunho
                        </button>
                        <button
                          className="primary"
                          disabled={ocupado}
                          onClick={() => mudarSituacao(v, "PUBLICADA")}
                        >
                          Publicar vaga
                        </button>
                      </>
                    )}
                    {nextJobStatus(v) === "ENCERRADA" && (
                      <button
                        className="link"
                        disabled={ocupado}
                        onClick={() => abrir({ tipo: "encerrar", id: v.id })}
                      >
                        Encerrar vaga
                      </button>
                    )}
                  </div>
                )}
              </article>
            ))}
            {!visiveis.length && (
              <p>
                {filtro === "TODOS"
                  ? "Sua empresa ainda não criou vagas."
                  : "Nenhuma vaga nesta situação."}
              </p>
            )}
          </div>
        </>
      )}
    </section>
  );
}
