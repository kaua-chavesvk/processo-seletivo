import { useEffect, useState } from "react";
import {
  ArrowUpRight,
  BriefcaseBusiness,
  CalendarDays,
  CheckCheck,
  ChevronDown,
  Columns3,
  Layers3,
  List,
  MoreHorizontal,
  Search,
  UsersRound,
} from "lucide-react";
import {
  active,
  closed,
  date,
  initials,
  labels,
  options,
  pretty,
  type Card,
  type Job,
  type Stage,
} from "./model";

export function Avatar({ name }: { name: string }) {
  const hue = [...name].reduce((acc, c) => acc + c.charCodeAt(0), 0) % 6;
  return (
    <span className={"avatar tone" + hue} aria-hidden="true">
      {initials(name)}
    </span>
  );
}
export function Board({
  jobs,
  jobId,
  choose,
  cards,
  open,
  move,
  pending,
}: {
  jobs: Job[];
  jobId: string;
  choose: (id: string) => void;
  cards: Card[];
  open: (c: Card) => void;
  move: (c: Card, s: Stage) => Promise<void>;
  pending: boolean;
}) {
  const [term, search] = useState(""),
    [archive, showArchive] = useState(false),
    [rows, showRows] = useState(() => matchMedia("(max-width: 650px)").matches),
    [stageFilter, filterStage] = useState<Stage | "">(""),
    [drag, setDrag] = useState<Card | null>(null),
    [over, setOver] = useState<Stage | null>(null),
    [error, setError] = useState("");
  const job = jobs.find((v) => v.id === jobId);
  const normalize = (s: string) =>
    s
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase();
  const visible = cards.filter((c) =>
    normalize(c.nome).includes(normalize(term)),
  );
  const stages = archive ? closed : active;
  const displayedStages = rows && stageFilter ? [stageFilter] : stages;
  useEffect(() => {
    const screen = matchMedia("(max-width: 650px)");
    const resize = (event: MediaQueryListEvent) => showRows(event.matches);
    screen.addEventListener("change", resize);
    return () => screen.removeEventListener("change", resize);
  }, []);
  function changeTab(nextArchive: boolean) {
    if (archive !== nextArchive) filterStage("");
    showArchive(nextArchive);
  }
  async function drop(s: Stage) {
    setOver(null);
    setDrag(null);
    if (drag && options(drag.etapaAtual).includes(s)) {
      try {
        setError("");
        await move(drag, s);
      } catch (ex) {
        setError((ex as Error).message);
      }
    }
  }
  return (
    <>
      <div className="title-row">
        <div>
          <p className="eyebrow">
            <span /> RECRUTAMENTO
          </p>
          <h1>Funil de seleção</h1>
          <p className="subtitle">
            Acompanhe cada candidatura, da triagem à contratação.
          </p>
        </div>
      </div>
      <div className="stats">
        {[
          {
            name: "Candidaturas",
            value: cards.length,
            icon: UsersRound,
            color: "violet",
          },
          {
            name: "Em andamento",
            value: cards.filter((c) => active.includes(c.etapaAtual)).length,
            icon: Layers3,
            color: "blue",
          },
          {
            name: "Em proposta",
            value: cards.filter((c) => c.etapaAtual === "PROPOSTA").length,
            icon: BriefcaseBusiness,
            color: "amber",
          },
          {
            name: "Contratações",
            value: cards.filter((c) => c.etapaAtual === "CONTRATADO").length,
            icon: CheckCheck,
            color: "green",
          },
        ].map(({ name, value, icon: Icon, color }) => (
          <div className="stat" key={name}>
            <span className={"stat-icon " + color}>
              <Icon size={18} />
            </span>
            <div>
              <span>{name}</span>
              <strong>{pending ? "—" : value}</strong>
            </div>
          </div>
        ))}
      </div>
      <section className="position">
        <div className="job-glyph">
          <BriefcaseBusiness size={23} />
        </div>
        <div className="job-select">
          <label htmlFor="job">Vaga selecionada</label>
          <div>
            <select
              id="job"
              value={jobId}
              onChange={(e) => choose(e.target.value)}
            >
              {jobs.map((j) => (
                <option value={j.id} key={j.id}>
                  {j.titulo}
                </option>
              ))}
            </select>
            <ChevronDown size={16} />
          </div>
        </div>
        {job && (
          <span className="job-meta">
            {pretty(job.modalidade)} <i /> {pretty(job.nivel)}
          </span>
        )}
      </section>
      <div className="tools">
        <div className="stage-tools">
          <div className="tabs" aria-label="Situação">
            <button
              className={!archive ? "on" : ""}
              aria-pressed={!archive}
              onClick={() => changeTab(false)}
            >
              Em andamento
            </button>
            <button
              className={archive ? "on" : ""}
              aria-pressed={archive}
              onClick={() => changeTab(true)}
            >
              Encerradas
            </button>
          </div>
          {rows && (
            <select
              className="stage-filter"
              aria-label="Filtrar por etapa"
              value={stageFilter}
              onChange={(e) => filterStage(e.target.value as Stage | "")}
            >
              <option value="">Todas as etapas</option>
              {stages.map((stage) => (
                <option value={stage} key={stage}>
                  {labels[stage]}
                </option>
              ))}
            </select>
          )}
        </div>
        <div className="tools-right">
          <label className="search">
            <Search size={16} />
            <input
              aria-label="Buscar candidato"
              placeholder="Buscar candidato"
              value={term}
              onChange={(e) => search(e.target.value)}
            />
          </label>
          <div className="switch">
            <button
              className={!rows ? "on" : ""}
              aria-label="Quadro"
              aria-pressed={!rows}
              onClick={() => showRows(false)}
            >
              <Columns3 size={16} />
            </button>
            <button
              className={rows ? "on" : ""}
              aria-label="Lista"
              aria-pressed={rows}
              onClick={() => showRows(true)}
            >
              <List size={17} />
            </button>
          </div>
        </div>
      </div>
      {error && <p role="alert">{error}</p>}
      {pending ? (
        <p role="status">Carregando candidaturas…</p>
      ) : (
        <div className={rows ? "board rows" : "board"}>
          {displayedStages.map((s) => {
            const group = visible.filter((c) => c.etapaAtual === s);
            return (
              <section
                key={s}
                className={"lane " + s + (over === s ? " over" : "")}
                aria-label={labels[s]}
                onDragOver={(e) => {
                  if (drag && options(drag.etapaAtual).includes(s)) {
                    e.preventDefault();
                    setOver(s);
                  }
                }}
                onDragLeave={() => setOver(null)}
                onDrop={(e) => {
                  e.preventDefault();
                  void drop(s);
                }}
              >
                <header>
                  <span className="dot" />
                  <h2>{labels[s]}</h2>
                  <small>{group.length}</small>
                </header>
                {group.map((c) => (
                  <button
                    key={c.id}
                    className="person"
                    draggable={options(c.etapaAtual).length > 0}
                    onDragStart={(e) => {
                      setDrag(c);
                      e.dataTransfer.setData("text/plain", c.id);
                      e.dataTransfer.effectAllowed = "move";
                    }}
                    onDragEnd={() => {
                      setDrag(null);
                      setOver(null);
                    }}
                    onClick={() => open(c)}
                  >
                    <span className="person-top">
                      <Avatar name={c.nome} />
                      <MoreHorizontal className="more" size={17} />
                    </span>
                    <strong>{c.nome}</strong>
                    <span className="person-date">
                      <CalendarDays size={13} />
                      {date(c.criadaEm)}
                      <ArrowUpRight className="open-icon" size={13} />
                    </span>
                  </button>
                ))}
                {!group.length && (
                  <p className="empty-lane">
                    {term ? "Sem resultados" : "Sem candidaturas"}
                  </p>
                )}
              </section>
            );
          })}
        </div>
      )}
    </>
  );
}
