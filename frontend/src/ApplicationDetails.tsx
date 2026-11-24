import { useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Clock3, Download } from "lucide-react";
import { download } from "./api";
import { demo, recruitment } from "./recruitment";
import { date, labels, options, type Card, type Stage } from "./model";

export function ApplicationDetails({
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
    queryFn: () => recruitment.history(card.id),
  });
  const available = options(card.etapaAtual, own ? "CANDIDATO" : "RECRUTADOR");
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
