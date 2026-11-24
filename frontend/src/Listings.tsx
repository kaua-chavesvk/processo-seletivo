import {
  ArrowRight,
  ArrowUpRight,
  BriefcaseBusiness,
  FileText,
  MapPin,
} from "lucide-react";
import { active, date, labels, pretty, type Job, type Card } from "./model";
import { views, type View } from "./Workspace";

export function Listings({
  view,
  pending,
  jobs,
  mine,
  open,
  swagger,
}: {
  view: View;
  pending: boolean;
  jobs?: Job[];
  mine?: Card[];
  open: (card: Card) => void;
  swagger?: boolean;
}) {
  return (
    <>
      <p className="eyebrow">
        <span />
        {view === "minhas" ? "ÁREA DO CANDIDATO" : "OPORTUNIDADES"}
      </p>
      <h1>{views[view].title}</h1>
      <p className="subtitle">
        {view === "vagas"
          ? "Encontre uma oportunidade e acompanhe sua candidatura."
          : view === "minhas"
            ? "Consulte as etapas e o histórico de cada candidatura."
            : "As vagas da sua empresa aparecem aqui."}
      </p>
      {pending ? (
        <p role="status">Carregando…</p>
      ) : view === "vagas" ? (
        <div className="job-grid">
          {jobs?.map((j) => (
            <article className="listing" key={j.id}>
              <div className="listing-top">
                <span className="job-glyph">
                  <BriefcaseBusiness size={21} />
                </span>
                <span className="status">
                  <i />
                  Publicada
                </span>
              </div>
              <h2>{j.titulo}</h2>
              <p className="meta">
                <MapPin size={14} />
                {j.localizacao} · {pretty(j.modalidade)}
              </p>
              <p>{j.descricao}</p>
              <div className="tags">
                {j.tecnologias.map((t) => (
                  <span key={t}>{t}</span>
                ))}
              </div>
              <a className="link" href={"#vagas/" + j.id}>
                Ver vaga <ArrowUpRight size={16} />
              </a>
            </article>
          ))}
          {!jobs?.length && (
            <p>
              Nenhuma vaga publicada no momento.{" "}
              <a href="https://trilha-processo-seletivo.vercel.app">
                Explorar o funil com dados fictícios
              </a>
            </p>
          )}
        </div>
      ) : view === "minhas" ? (
        <div className="applications">
          {mine?.map((c) => (
            <button key={c.id} onClick={() => open(c)}>
              <FileText size={20} />
              <div>
                <strong>{c.titulo}</strong>
                <small>Inscrição em {date(c.criadaEm)}</small>
              </div>
              <span
                className={
                  "pill " + (active.includes(c.etapaAtual) ? "ongoing" : "")
                }
              >
                {labels[c.etapaAtual]}
              </span>
              <ArrowRight size={16} />
            </button>
          ))}
          {!mine?.length && (
            <p>
              Você ainda não se candidatou. <a href="#vagas">Explorar vagas</a>
            </p>
          )}
        </div>
      ) : (
        <p>
          Nenhuma vaga cadastrada.{" "}
          <a href="#minhas-vagas">Criar a primeira vaga</a>
        </p>
      )}
      {swagger && view === "vagas" && (
        <a href="/swagger-ui.html">Abrir operações administrativas na API</a>
      )}
    </>
  );
}
