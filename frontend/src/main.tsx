import { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import {
  ArrowRight,
  ArrowUpRight,
  BriefcaseBusiness,
  Building2,
  ChevronRight,
  Columns3,
  FileText,
  FlaskConical,
  GitFork,
  LogIn,
  LogOut,
  MapPin,
  Menu,
  FolderOpen,
  PanelLeft,
  X,
} from "lucide-react";
import "@fontsource-variable/dm-sans/wght.css";
import "./styles.css";
import {
  active,
  date,
  demo,
  labels,
  pretty,
  type Card,
  type Job,
  type Profile,
  type Stage,
} from "./model";
import { sample } from "./demo";
import {
  isJobId,
  json,
  list,
  logout,
  read,
  readPublicJob,
  request,
} from "./api";
import { Operacao } from "./Operacao";
import { MinhasVagas } from "./MinhasVagas";
import { Avatar, Board } from "./Board";
import {
  ApplicationStatus,
  Auth,
  Details,
  Dialog,
  JobDetails,
} from "./Dialogs";

const client = new QueryClient({
  defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
});
const views = {
  funil: { title: "Funil de seleção", icon: Columns3 },
  "minhas-vagas": { title: "Minhas vagas", icon: FolderOpen },
  vagas: { title: "Vagas abertas", icon: BriefcaseBusiness },
  minhas: { title: "Minhas candidaturas", icon: FileText },
};
type View = keyof typeof views;
type Open = { type: "auth" } | { type: "card"; card: Card };
const routes = () => location.hash.slice(1);
function App() {
  const [profile, setProfile] = useState<Profile | null>(null),
    [route, setRoute] = useState(routes),
    [chosen, choose] = useState(""),
    [opened, setOpen] = useState<Open | null>(null),
    [mobile, setMobile] = useState(false),
    [notice, setNotice] = useState(""),
    [movedDemoId, setMovedDemoId] = useState(""),
    [revision, revise] = useState(0);
  const [timelineBack, setTimelineBack] = useState({
    href: "#minhas",
    text: "Voltar às minhas candidaturas",
  });
  const [gerenciar, setGerenciar] = useState(false);
  const detailHeading = useRef<HTMLHeadingElement>(null);
  const recruiter = demo || profile?.papel === "RECRUTADOR",
    candidate = demo || profile?.papel === "CANDIDATO";
  const links: View[] = [
    ...(recruiter ? (["funil", "minhas-vagas"] as const) : []),
    "vagas",
    ...(candidate ? ["minhas" as View] : []),
  ];
  const jobRouteId = route.startsWith("vagas/")
    ? route.slice("vagas/".length)
    : "";
  const timelineId = route.startsWith("minhas/")
    ? route.slice("minhas/".length)
    : "";
  const section = jobRouteId ? "vagas" : timelineId ? "minhas" : route;
  const view = Object.hasOwn(views, section) ? (section as View) : links[0];
  const allowed = links.includes(view);
  const companyJobs = view === "funil" || view === "minhas-vagas";
  const user = demo
    ? {
        nome:
          view === "minhas" || view === "vagas" ? "Marina Costa" : "Lia Mendes",
        papel:
          view === "minhas" || view === "vagas" ? "Candidata" : "Recrutadora",
      }
    : profile;
  useEffect(() => {
    const change = (event: HashChangeEvent) => {
      if (routes().startsWith("minhas/")) {
        const from = new URL(event.oldURL).hash;
        if (from.startsWith("#vagas/"))
          setTimelineBack({ href: from, text: "Voltar à vaga" });
        else if (demo && (!from || from === "#funil"))
          setTimelineBack({
            href: "#funil",
            text: "Voltar ao funil de seleção",
          });
        else
          setTimelineBack({
            href: "#minhas",
            text: "Voltar às minhas candidaturas",
          });
      }
      setRoute(routes());
      setMobile(false);
      setOpen(null);
      setNotice("");
      setMovedDemoId("");
    };
    const expired = () => {
      setGerenciar(false);
      setProfile(null);
      setOpen({ type: "auth" });
      client.clear();
      setNotice("Sua sessão expirou. Entre novamente.");
    };
    window.addEventListener("hashchange", change);
    window.addEventListener("session-expired", expired);
    return () => {
      window.removeEventListener("hashchange", change);
      window.removeEventListener("session-expired", expired);
    };
  }, []);
  const recursos = useQuery({
    queryKey: ["recursos"],
    enabled: !demo,
    staleTime: Infinity,
    queryFn: () =>
      read<{ limitePdf: number; swagger: boolean }>("/interface/config"),
  });
  const jobs = useQuery({
    queryKey: ["jobs", companyJobs, profile?.id],
    enabled: allowed && !jobRouteId && view !== "minhas",
    queryFn: () =>
      demo
        ? Promise.resolve(sample.jobs(companyJobs))
        : list<Job>(companyJobs ? "/painel/vagas" : "/vagas"),
  });
  const publicJob = useQuery({
    queryKey: ["public-job", jobRouteId],
    enabled: !!jobRouteId,
    queryFn: () =>
      demo
        ? Promise.resolve(sample.publicJob(jobRouteId))
        : readPublicJob(jobRouteId),
  });
  const jobId = jobs.data?.some((j) => j.id === chosen)
    ? chosen
    : (jobs.data?.[0]?.id ?? "");
  const board = useQuery({
    queryKey: ["board", jobId, profile?.id],
    enabled: recruiter && view === "funil" && !!jobId,
    queryFn: () =>
      demo
        ? Promise.resolve(sample.board(jobId))
        : list<Card>("/painel/vagas/" + jobId + "/candidaturas"),
  });
  const mine = useQuery({
    queryKey: ["mine", profile?.id, jobRouteId],
    enabled: candidate && (view === "minhas" || !!jobRouteId),
    refetchOnWindowFocus: true,
    queryFn: () => {
      if (demo) {
        const cards = sample.mine();
        return Promise.resolve(
          jobRouteId ? cards.filter((c) => c.vagaId === jobRouteId) : cards,
        );
      }
      if (jobRouteId && !isJobId(jobRouteId)) return Promise.resolve([]);
      return list<Card>(
        "/painel/minhas",
        jobRouteId ? { vagaId: jobRouteId } : undefined,
      );
    },
  });
  const sharedJob = publicJob.data;
  const ownApplication =
    candidate && timelineId
      ? mine.data?.find((c) => c.id === timelineId)
      : undefined;
  const existingApplication =
    candidate && jobRouteId
      ? mine.data?.find((c) => c.vagaId === jobRouteId)
      : undefined;
  const checkingApplication =
    mine.isPending || (!existingApplication && mine.isFetching);
  useEffect(() => {
    detailHeading.current?.focus();
  }, [route, sharedJob?.id, ownApplication?.id]);
  const timelineLink = movedDemoId && (
    <a href={"#minhas/" + movedDemoId}>
      Ver como Marina acompanha essa mudança
    </a>
  );
  async function refresh() {
    revise((x) => x + 1);
    await Promise.all(
      ["board", "mine"].map((k) => client.invalidateQueries({ queryKey: [k] })),
    );
  }
  async function move(card: Card, stage: Stage, note?: string) {
    if (demo) sample.move(card.id, stage, note);
    else if (stage === "DESISTIU")
      await request("/candidaturas/" + card.id + "/desistencia", {
        method: "PATCH",
      });
    else
      await request(
        "/candidaturas/" + card.id + "/etapa",
        json("PATCH", { novaEtapa: stage, observacao: note }),
      );
    await refresh();
    setOpen((current) =>
      current?.type === "card" && current.card.id === card.id
        ? { type: "card", card: { ...card, etapaAtual: stage } }
        : current,
    );
    setNotice("Etapa atualizada para " + labels[stage] + ".");
    setMovedDemoId(
      demo && view === "funil" && sample.isMarina(card) ? card.id : "",
    );
  }
  async function signOut() {
    let message = "Sessão encerrada.";
    try {
      await logout();
    } catch {
      message =
        "Sessão local encerrada. A revogação no servidor não foi confirmada.";
    } finally {
      client.clear();
      setGerenciar(false);
      setProfile(null);
      setOpen(null);
      choose("");
      setNotice(message);
    }
  }
  function signedIn(p: Profile) {
    client.clear();
    setProfile(p);
    choose("");
    setNotice("Você entrou como " + p.nome + ".");
    setOpen(null);
    const resumeCandidate =
      p.papel === "CANDIDATO" && (jobRouteId || view === "minhas");
    const resumeRecruiter = p.papel === "RECRUTADOR" && companyJobs;
    if (!resumeCandidate && !resumeRecruiter) {
      location.hash = p.papel === "RECRUTADOR" ? "funil" : "vagas";
    }
  }
  const current = jobRouteId ? publicJob : view === "minhas" ? mine : jobs;
  const failure = current.error || (view === "funil" ? board.error : null);
  return (
    <div className="app">
      {mobile && (
        <button
          className="backdrop"
          aria-label="Fechar navegação"
          onClick={() => setMobile(false)}
        />
      )}
      <aside className={mobile ? "sidebar shown" : "sidebar"}>
        <div className="brand">
          <span className="brand-icon">
            <GitFork size={24} />
          </span>
          Trilha<span className="brand-dot">.</span>
        </div>
        <div className="workspace">
          <span className="work-icon">
            <Building2 size={18} />
          </span>
          <div>
            <strong>{demo ? "Estúdio Aurora" : "Processo seletivo"}</strong>
            <small>Espaço de trabalho</small>
          </div>
        </div>
        <div className="nav-label">NAVEGAÇÃO</div>
        <nav>
          {links.map((key) => {
            const Icon = views[key].icon;
            return (
              <a
                key={key}
                className={key === view ? "selected" : ""}
                href={"#" + key}
                aria-current={key === view ? "page" : undefined}
                onClick={() => setMobile(false)}
              >
                <Icon size={18} />
                {views[key].title}
              </a>
            );
          })}
        </nav>
        <div className="side-bottom">
          {demo && (
            <div className="demo-note">
              <FlaskConical size={17} />
              <div>
                <b>Modo demonstração</b>
                <small>Dados fictícios nesta sessão</small>
                <a
                  href="https://github.com/kaua-chavesvk/processo-seletivo"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Código no GitHub <ArrowUpRight size={13} aria-hidden="true" />
                </a>
              </div>
            </div>
          )}
          <div className="identity">
            {user ? (
              <>
                <Avatar name={user.nome} />
                <div>
                  <b>{user.nome}</b>
                  <small>
                    {(
                      {
                        CANDIDATO: "Candidato",
                        RECRUTADOR: "Recrutador",
                        ADMIN: "Administrador",
                      } as Record<string, string>
                    )[user.papel] ?? user.papel}
                  </small>
                </div>
                {!demo && (
                  <button className="icon" onClick={signOut} aria-label="Sair">
                    <LogOut size={17} />
                  </button>
                )}
              </>
            ) : (
              <button
                className="secondary"
                onClick={() => setOpen({ type: "auth" })}
              >
                <LogIn size={16} /> Entrar
              </button>
            )}
          </div>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <button
            className="mobile-menu icon"
            onClick={() => setMobile(!mobile)}
            aria-label="Abrir navegação"
          >
            {mobile ? <X /> : <Menu />}
          </button>
          <PanelLeft className="desktop-icon" size={17} />
          <span className="divider" />
          <span>Workspace</span>
          <ChevronRight size={13} />
          <b>{views[view].title}</b>
          {demo && (
            <span className="demo-chip">
              <i />
              Demonstração
            </span>
          )}
        </header>
        <main>
          {profile?.papel === "ADMIN" && allowed && (
            <div className="acoes-painel">
              <button className="secondary" onClick={() => setGerenciar(true)}>
                Gerenciar empresas e recrutadores
              </button>
            </div>
          )}
          {notice && (
            <div className="notice" role="status">
              {notice}
              {opened?.type !== "card" && timelineLink}
              <button
                className="icon"
                aria-label="Dispensar aviso"
                onClick={() => setNotice("")}
              >
                <X size={15} />
              </button>
            </div>
          )}
          {!allowed ? (
            <section className="job-detail">
              <h1>{views[view].title}</h1>
              <p>
                {view === "minhas"
                  ? "Esta área é exclusiva do candidato."
                  : "Esta área é exclusiva do recrutador."}
              </p>
              {profile ? (
                <a href={"#" + links[0]}>Voltar a {views[links[0]].title}</a>
              ) : (
                <button
                  className="primary"
                  onClick={() => setOpen({ type: "auth" })}
                >
                  Entrar na minha conta
                </button>
              )}
            </section>
          ) : failure ? (
            <section>
              <h1>{views[view].title}</h1>
              <p role="alert">{failure.message}</p>
              <button
                className="secondary"
                onClick={() => {
                  void current.refetch();
                  if (view === "funil" && jobId) void board.refetch();
                }}
              >
                Tentar novamente
              </button>
            </section>
          ) : jobRouteId ? (
            <section className="job-detail">
              <a href="#vagas">Voltar às vagas abertas</a>
              {publicJob.isPending ? (
                <p role="status">Carregando vaga…</p>
              ) : sharedJob ? (
                <>
                  <h1 ref={detailHeading} tabIndex={-1}>
                    {sharedJob.titulo}
                  </h1>
                  <JobDetails
                    key={sharedJob.id}
                    job={sharedJob}
                    limitePdf={recursos.data?.limitePdf}
                    erroConfig={recursos.isError}
                    recarregar={() => void recursos.refetch()}
                    canApply={candidate}
                    application={existingApplication}
                    checkingApplication={checkingApplication}
                    applicationError={mine.error?.message}
                    reloadApplications={() => mine.refetch()}
                    guest={!demo && !profile}
                    enter={() => setOpen({ type: "auth" })}
                    done={() => {
                      void refresh();
                      location.hash = "minhas";
                    }}
                  />
                </>
              ) : (
                <>
                  <h1>Vaga indisponível</h1>
                  {candidate && (
                    <ApplicationStatus
                      application={existingApplication}
                      checking={checkingApplication}
                      checkError={mine.error?.message}
                      retry={() => void mine.refetch()}
                    />
                  )}
                  {!profile && !demo && (
                    <button
                      className="secondary"
                      onClick={() => setOpen({ type: "auth" })}
                    >
                      Já me candidatei · Entrar para acompanhar
                    </button>
                  )}
                </>
              )}
            </section>
          ) : timelineId ? (
            <section className="job-detail">
              <a href={timelineBack.href}>{timelineBack.text}</a>
              {mine.isPending ? (
                <p role="status">Carregando candidatura…</p>
              ) : ownApplication ? (
                <>
                  <div>
                    <p className="eyebrow">
                      <span />{" "}
                      {demo ? "VISÃO DA CANDIDATA" : "MINHA CANDIDATURA"}
                    </p>
                    <h1 ref={detailHeading} tabIndex={-1}>
                      {ownApplication.nome}
                    </h1>
                  </div>
                  <Details
                    key={ownApplication.id}
                    card={ownApplication}
                    own
                    move={move}
                    changed={revision}
                  />
                </>
              ) : (
                <h1>Candidatura indisponível nesta conta</h1>
              )}
            </section>
          ) : view === "minhas-vagas" ? (
            jobs.isPending ? (
              <p role="status">Carregando vagas…</p>
            ) : (
              <MinhasVagas
                empresaId={profile?.empresaId}
                vagas={jobs.data ?? []}
                atualizar={async () => {
                  await Promise.all([
                    client.invalidateQueries({ queryKey: ["jobs"] }),
                    client.invalidateQueries({ queryKey: ["public-job"] }),
                  ]);
                }}
              />
            )
          ) : view === "funil" && (jobId || jobs.isPending) ? (
            <Board
              jobs={jobs.data ?? []}
              jobId={jobId}
              choose={choose}
              cards={board.data ?? []}
              pending={board.isPending}
              open={(card) => setOpen({ type: "card", card })}
              move={move}
            />
          ) : (
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
              {current.isPending ? (
                <p role="status">Carregando…</p>
              ) : view === "vagas" ? (
                <div className="job-grid">
                  {jobs.data?.map((j) => (
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
                  {!jobs.data?.length && (
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
                  {mine.data?.map((c) => (
                    <button
                      key={c.id}
                      onClick={() => setOpen({ type: "card", card: c })}
                    >
                      <FileText size={20} />
                      <div>
                        <strong>{c.titulo}</strong>
                        <small>Inscrição em {date(c.criadaEm)}</small>
                      </div>
                      <span
                        className={
                          "pill " +
                          (active.includes(c.etapaAtual) ? "ongoing" : "")
                        }
                      >
                        {labels[c.etapaAtual]}
                      </span>
                      <ArrowRight size={16} />
                    </button>
                  ))}
                  {!mine.data?.length && (
                    <p>
                      Você ainda não se candidatou.{" "}
                      <a href="#vagas">Explorar vagas</a>
                    </p>
                  )}
                </div>
              ) : (
                <p>
                  Nenhuma vaga cadastrada.{" "}
                  <a href="#minhas-vagas">Criar a primeira vaga</a>
                </p>
              )}
              {profile?.papel === "ADMIN" &&
                recursos.data?.swagger &&
                view === "vagas" && (
                  <a href="/swagger-ui.html">
                    Abrir operações administrativas na API
                  </a>
                )}
            </>
          )}
        </main>
      </div>
      {gerenciar && profile?.papel === "ADMIN" && (
        <Dialog
          title="Empresas e recrutadores"
          close={() => setGerenciar(false)}
        >
          <Operacao perfil={profile} />
        </Dialog>
      )}
      {opened && (
        <Dialog
          title={
            opened.type === "auth" ? "Acessar a plataforma" : opened.card.nome
          }
          close={() => {
            setOpen(null);
          }}
        >
          {opened.type === "auth" ? (
            <Auth done={signedIn} />
          ) : (
            <Details
              card={opened.card}
              own={view === "minhas"}
              move={move}
              changed={revision}
              timelineLink={
                movedDemoId === opened.card.id ? timelineLink : undefined
              }
            />
          )}
        </Dialog>
      )}
    </div>
  );
}
createRoot(document.getElementById("root")!).render(
  <QueryClientProvider client={client}>
    <App />
  </QueryClientProvider>,
);
