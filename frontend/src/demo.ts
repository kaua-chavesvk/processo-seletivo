import { ApiError } from "./api";
import {
  active,
  nextJobStatus,
  options,
  type Card,
  type Event,
  type Stage,
} from "./model";
import { demoCandidate, exampleJobs, roster } from "./demoData";
import type { RecruitmentApi } from "./RecruitmentApi";

export function createDemoApi(): RecruitmentApi {
  const jobs = structuredClone(exampleJobs);
  const cards: Card[] = [];
  const events = new Map<string, Event[]>();
  function append(card: Card, stage: Stage, when: string, observacao?: string) {
    const log = events.get(card.id) ?? [];
    log.push({
      id: crypto.randomUUID(),
      etapaNova: stage,
      alteradoEm: when,
      observacao,
    });
    events.set(card.id, log);
    card.etapaAtual = stage;
  }
  roster.forEach(([nome, etapaAtual, job], i) => {
    const created = Date.UTC(2026, 10, 19 - i);
    const card = {
      id: "demo-" + i,
      nome,
      etapaAtual,
      vagaId: jobs[job].id,
      titulo: jobs[job].titulo,
      criadaEm: new Date(created).toISOString(),
    };
    cards.push(card);
    for (const [i, stage] of [...active, "CONTRATADO" as Stage].entries()) {
      append(card, stage, new Date(created + i * 86400000).toISOString());
      if (stage === etapaAtual) break;
    }
  });
  return {
    shareable: (id) => exampleJobs.some((job) => job.id === id),
    jobs: async (includeUnpublished) =>
      jobs
        .filter((job) => includeUnpublished || job.status === "PUBLICADA")
        .map((job) => ({ ...job, tecnologias: [...job.tecnologias] })),
    publicJob: async (id) => {
      const job = jobs.find((v) => v.id === id && v.status === "PUBLICADA");
      return job ? { ...job, tecnologias: [...job.tecnologias] } : null;
    },
    async saveDraft(data, id) {
      if (!id) {
        jobs.unshift({ ...data, id: crypto.randomUUID(), status: "RASCUNHO" });
        return;
      }
      const job = jobs.find((v) => v.id === id);
      if (!job || job.status !== "RASCUNHO")
        throw new Error("A edição exige uma vaga em rascunho.");
      Object.assign(job, data);
    },
    async setJobStatus(id, status) {
      const job = jobs.find((v) => v.id === id);
      if (!job || nextJobStatus(job) !== status)
        throw new Error(
          "A situação da vaga mudou. Confira a lista e tente novamente.",
        );
      job.status = status;
    },
    board: async (id) =>
      cards.filter((c) => c.vagaId === id).map((c) => ({ ...c })),
    mine: async (jobId) =>
      cards
        .filter(
          (c) => c.nome === demoCandidate && (!jobId || c.vagaId === jobId),
        )
        .map((c) => ({ ...c })),
    history: async (id) => [...(events.get(id) ?? [])],
    async move(id, stage, observacao) {
      const card = cards.find((c) => c.id === id);
      const role = stage === "DESISTIU" ? "CANDIDATO" : "RECRUTADOR";
      if (
        !card ||
        (role === "CANDIDATO" && card.nome !== demoCandidate) ||
        !options(card.etapaAtual, role).includes(stage)
      )
        throw new Error("Mudança de etapa indisponível.");
      append(card, stage, new Date().toISOString(), observacao);
    },
    async apply(job) {
      if (!jobs.some((v) => v.id === job.id && v.status === "PUBLICADA"))
        throw new Error("Esta vaga não está recebendo candidaturas.");
      if (cards.some((c) => c.nome === demoCandidate && c.vagaId === job.id))
        throw new ApiError(409, "Você já se candidatou a esta vaga.");
      const card: Card = {
        id: crypto.randomUUID(),
        nome: demoCandidate,
        vagaId: job.id,
        titulo: job.titulo,
        etapaAtual: "TRIAGEM",
        criadaEm: new Date().toISOString(),
      };
      cards.push(card);
      append(card, "TRIAGEM", card.criadaEm);
    },
  };
}
