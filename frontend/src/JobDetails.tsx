import { useState, type FormEvent } from "react";
import { ArrowRight, FileText, Link2 } from "lucide-react";
import { ApiError } from "./api";
import { demo, recruitment } from "./recruitment";
import { labels, type Card, type Job } from "./model";
import { ResumoVaga } from "./ResumoVaga";

export function ApplicationStatus({
  application,
  checking,
  checkError,
  retry,
}: {
  application?: Card;
  checking: boolean;
  checkError?: string;
  retry: () => void;
}) {
  if (checking) return <p role="status">Consultando sua candidatura…</p>;
  if (checkError)
    return (
      <div className="stack">
        <p role="alert">Falha ao consultar sua candidatura. {checkError}</p>
        <button className="secondary" onClick={retry}>
          Tentar novamente
        </button>
      </div>
    );
  if (!application) return null;
  return (
    <section className="application-status" aria-label="Sua candidatura">
      <h2>Você já se candidatou</h2>
      <p>
        Etapa atual · <strong>{labels[application.etapaAtual]}</strong>
      </p>
      <a className="primary" href={"#minhas/" + application.id}>
        Acompanhar candidatura <ArrowRight size={16} aria-hidden="true" />
      </a>
    </section>
  );
}

export function JobDetails({
  job,
  limitePdf,
  erroConfig,
  recarregar,
  canApply,
  application,
  checkingApplication,
  applicationError,
  reloadApplications,
  guest,
  enter,
  done,
}: {
  job: Job;
  limitePdf?: number;
  erroConfig: boolean;
  recarregar: () => void;
  canApply: boolean;
  application?: Card;
  checkingApplication: boolean;
  applicationError?: string;
  reloadApplications: () => Promise<unknown>;
  guest: boolean;
  enter: () => void;
  done: () => void;
}) {
  const [file, setFile] = useState<File>(),
    [error, setError] = useState(""),
    [share, setShare] = useState<"copied" | "manual" | null>(null),
    [busy, setBusy] = useState(false);
  const publicUrl = location.origin + location.pathname + "#vagas/" + job.id;
  async function copyLink() {
    try {
      await navigator.clipboard.writeText(publicUrl);
      setShare("copied");
    } catch {
      setShare("manual");
    }
  }
  async function apply(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (
        !demo &&
        (!file ||
          file.size === 0 ||
          !limitePdf ||
          file.size > limitePdf ||
          file.type !== "application/pdf")
      )
        throw new Error("Selecione um PDF dentro do limite indicado.");
      await recruitment.apply(job, file);
      done();
    } catch (ex) {
      setError((ex as Error).message);
      if (ex instanceof ApiError && ex.status === 409) {
        await reloadApplications();
      }
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="stack">
      <ResumoVaga job={job} />
      {recruitment.shareable(job.id) ? (
        <button className="secondary" onClick={copyLink}>
          <Link2 size={16} /> Copiar link da vaga
        </button>
      ) : (
        <p className="note">
          Esta vaga de demonstração está disponível apenas nesta aba.
        </p>
      )}
      {share === "copied" && <p role="status">Link da vaga copiado.</p>}
      {share === "manual" && (
        <label>
          Copie o endereço da vaga
          <input
            readOnly
            value={publicUrl}
            onFocus={(e) => e.target.select()}
          />
        </label>
      )}
      {guest && (
        <button className="primary" onClick={enter}>
          Entrar para se candidatar
          <ArrowRight size={16} />
        </button>
      )}
      {canApply && (
        <ApplicationStatus
          application={application}
          checking={checkingApplication}
          checkError={applicationError}
          retry={() => void reloadApplications()}
        />
      )}
      {canApply &&
        !application &&
        !checkingApplication &&
        !applicationError && (
          <form className="stack" onSubmit={apply}>
            {demo ? (
              <p className="note">
                <FileText size={17} /> A simulação usa o perfil de Marina Costa
                e um currículo fictício.
              </p>
            ) : (
              <label>
                Currículo em PDF{" "}
                {limitePdf
                  ? "· até " +
                    (limitePdf / 1048576).toLocaleString("pt-BR") +
                    " MB"
                  : ""}
                <input
                  type="file"
                  accept="application/pdf,.pdf"
                  required
                  onChange={(e) => setFile(e.target.files?.[0])}
                />
              </label>
            )}
            {error && <p role="alert">{error}</p>}
            {erroConfig && (
              <p role="alert">
                Falha ao carregar o limite de envio.{" "}
                <button type="button" onClick={recarregar}>
                  Tentar novamente
                </button>
              </p>
            )}
            <button
              className="primary"
              disabled={busy || (!demo && !limitePdf)}
            >
              {demo ? "Simular candidatura" : "Enviar candidatura"}
              <ArrowRight size={16} />
            </button>
          </form>
        )}
    </div>
  );
}
