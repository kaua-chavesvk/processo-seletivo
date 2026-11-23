import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowRight,
  Clock3,
  Download,
  FileText,
  Link2,
  MapPin,
  X,
} from "lucide-react";
import {
  date,
  demo,
  labels,
  options,
  pretty,
  type Card,
  type Event as Change,
  type Job,
  type Profile,
  type Stage,
} from "./model";
import { download, json, login, read, request } from "./api";
import { sample } from "./demo";

export function Dialog({
  title,
  close,
  children,
}: {
  title: string;
  close: () => void;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const el = ref.current!;
    el.showModal();
    return () => el.close();
  }, []);
  return (
    <dialog
      ref={ref}
      onCancel={close}
      onClick={(e) => {
        if (e.target === ref.current) {
          const r = ref.current!.getBoundingClientRect();
          if (
            e.clientX < r.left ||
            e.clientX > r.right ||
            e.clientY < r.top ||
            e.clientY > r.bottom
          )
            close();
        }
      }}
      aria-labelledby="dialog-title"
    >
      <header>
        <h2 id="dialog-title">{title}</h2>
        <button className="icon" aria-label="Fechar" onClick={close}>
          <X />
        </button>
      </header>
      {children}
    </dialog>
  );
}

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
export function JobDetails({
  job,
  limitePdf,
  erroConfig,
  recarregar,
  canApply,
  guest,
  enter,
  done,
}: {
  job: Job;
  limitePdf?: number;
  erroConfig: boolean;
  recarregar: () => void;
  canApply: boolean;
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
      if (demo) sample.apply(job);
      else {
        if (
          !file ||
          file.size === 0 ||
          !limitePdf ||
          file.size > limitePdf ||
          file.type !== "application/pdf"
        )
          throw new Error("Selecione um PDF dentro do limite indicado.");
        const data = new FormData();
        data.append("curriculo", file);
        await request("/vagas/" + job.id + "/candidaturas", {
          method: "POST",
          body: data,
        });
      }
      done();
    } catch (ex) {
      setError((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="stack">
      <div className="meta">
        <MapPin size={15} />
        {job.localizacao} · {pretty(job.modalidade)} · {pretty(job.nivel)}
      </div>
      <p>{job.descricao}</p>
      <div className="tags">
        {job.tecnologias.map((t) => (
          <span key={t}>{t}</span>
        ))}
      </div>
      <button className="secondary" onClick={copyLink}>
        <Link2 size={16} /> Copiar link da vaga
      </button>
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
        <form className="stack" onSubmit={apply}>
          {demo ? (
            <p className="note">
              <FileText size={17} /> A simulação usa o perfil de Marina Costa e
              um currículo fictício.
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
          <button className="primary" disabled={busy || (!demo && !limitePdf)}>
            {demo ? "Simular candidatura" : "Enviar candidatura"}
            <ArrowRight size={16} />
          </button>
        </form>
      )}
    </div>
  );
}
export function Details({
  card,
  own,
  move,
  changed,
  timelineLink,
}: {
  card: Card;
  own: boolean;
  move: (c: Card, s: Stage, note?: string) => Promise<void>;
  changed: number;
  timelineLink?: ReactNode;
}) {
  const history = useQuery({
    queryKey: ["history", card.id, changed],
    queryFn: () =>
      demo
        ? Promise.resolve(sample.history(card.id))
        : read<Change[]>("/candidaturas/" + card.id + "/historico"),
  });
  const available = options(card.etapaAtual);
  const [stage, setStage] = useState<Stage | "">(""),
    [note, setNote] = useState(""),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [confirm, setConfirm] = useState(false);
  async function action(fn: () => Promise<void>) {
    setError("");
    setBusy(true);
    try {
      await fn();
      setStage("");
      setNote("");
      setConfirm(false);
    } catch (ex) {
      setError((ex as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="stack">
      <p className="meta">
        {card.titulo} <span>·</span> {labels[card.etapaAtual]}
      </p>
      <h3>
        <Clock3 size={16} /> Histórico da candidatura
      </h3>
      {history.isPending ? (
        <p>Carregando histórico…</p>
      ) : history.isError ? (
        <p role="alert">
          {history.error.message}{" "}
          <button className="link" onClick={() => history.refetch()}>
            Tentar novamente
          </button>
        </p>
      ) : (
        <ol className="timeline">
          {history.data.map((h) => (
            <li key={h.id}>
              <b>{labels[h.etapaNova]}</b>
              <time>{date(h.alteradoEm)}</time>
              {h.observacao && <p>{h.observacao}</p>}
            </li>
          ))}
        </ol>
      )}
      {timelineLink}
      {!own && !demo && (
        <button
          className="secondary"
          disabled={busy}
          onClick={() => action(() => download(card.id))}
        >
          <Download size={16} />
          Baixar currículo
        </button>
      )}
      {!own && available.length > 0 && (
        <form
          className="stack"
          onSubmit={(e) => {
            e.preventDefault();
            if (stage)
              void action(() => move(card, stage, note.trim() || undefined));
          }}
        >
          <label>
            Alterar etapa
            <select
              required
              value={stage}
              onChange={(e) => setStage(e.target.value as Stage)}
            >
              <option value="">Selecione a etapa</option>
              {available.map((s) => (
                <option value={s} key={s}>
                  {labels[s]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Orientação visível ao candidato (opcional)
            <textarea
              value={note}
              rows={2}
              maxLength={4000}
              onChange={(e) => setNote(e.target.value)}
            />
          </label>
          <button className="primary" disabled={!stage || busy}>
            Salvar etapa
          </button>
        </form>
      )}
      {own &&
        available.length > 0 &&
        (confirm ? (
          <div className="stack">
            <p>A desistência encerra esta candidatura. Deseja continuar?</p>
            <button
              className="danger"
              disabled={busy}
              onClick={() => action(() => move(card, "DESISTIU"))}
            >
              Confirmar desistência
            </button>
            <button className="link" onClick={() => setConfirm(false)}>
              Cancelar
            </button>
          </div>
        ) : (
          <button className="secondary" onClick={() => setConfirm(true)}>
            Desistir da candidatura
          </button>
        ))}
      {error && <p role="alert">{error}</p>}
    </div>
  );
}
