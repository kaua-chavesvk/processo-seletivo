import {
  active,
  options,
  type Card,
  type Event,
  type Job,
  type Stage,
} from "./model";

const jobs: Job[] = [
  {
    id: "produto",
    titulo: "Product Designer",
    descricao:
      "Desenhe experiências digitais de ponta a ponta, da pesquisa com usuários à entrega com o time de produto.",
    modalidade: "REMOTO",
    localizacao: "Brasil",
    nivel: "PLENO",
    tecnologias: ["Figma", "Pesquisa", "Design systems"],
    status: "PUBLICADA",
  },
  {
    id: "frontend",
    titulo: "Desenvolvedor Front-end",
    descricao:
      "Construa interfaces acessíveis para produtos digitais com React e TypeScript.",
    modalidade: "HIBRIDO",
    localizacao: "São Paulo",
    nivel: "PLENO",
    tecnologias: ["React", "TypeScript"],
    status: "PUBLICADA",
  },
  {
    id: "dados",
    titulo: "Analista de Dados",
    descricao:
      "Transforme dados de produto em análises que orientem decisões do time.",
    modalidade: "REMOTO",
    localizacao: "Brasil",
    nivel: "JUNIOR",
    tecnologias: ["SQL", "Python"],
    status: "PUBLICADA",
  },
];
const exampleJobIds = new Set(jobs.map((job) => job.id));
const roster: [string, Stage, number][] = [
  ["Marina Costa", "TRIAGEM", 0],
  ["Rafael Almeida", "TRIAGEM", 0],
  ["Júlia Martins", "TRIAGEM", 0],
  ["Pedro Rocha", "TRIAGEM", 0],
  ["Camila Ribeiro", "ENTREVISTA", 0],
  ["Lucas Ferreira", "ENTREVISTA", 0],
  ["Bruna Castro", "ENTREVISTA", 0],
  ["Gabriel Santos", "TESTE_TECNICO", 0],
  ["Isabela Lima", "TESTE_TECNICO", 0],
  ["Beatriz Souza", "PROPOSTA", 0],
  ["Ana Dias", "CONTRATADO", 0],
  ["Felipe Alves", "TRIAGEM", 1],
  ["Carolina Nunes", "ENTREVISTA", 2],
];
const cards: Card[] = [];
const events = new Map<string, Event[]>();
const demoName = "Marina Costa";
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
export const sample = {
  isExampleJob: (id: string) => exampleJobIds.has(id),
  jobs: (includeUnpublished: boolean) =>
    jobs
      .filter((job) => includeUnpublished || job.status === "PUBLICADA")
      .map((job) => ({ ...job, tecnologias: [...job.tecnologias] })),
  publicJob: (id: string) => {
    const job = jobs.find((v) => v.id === id && v.status === "PUBLICADA");
    return job ? { ...job, tecnologias: [...job.tecnologias] } : null;
  },
  saveDraft(data: Omit<Job, "id" | "status">, id?: string) {
    if (!id) {
      jobs.unshift({ ...data, id: crypto.randomUUID(), status: "RASCUNHO" });
      return;
    }
    const job = jobs.find((v) => v.id === id);
    if (!job || job.status !== "RASCUNHO")
      throw new Error("A edição exige uma vaga em rascunho.");
    Object.assign(job, data);
  },
  setJobStatus(id: string, status: "PUBLICADA" | "ENCERRADA") {
    const job = jobs.find((v) => v.id === id);
    if (
      !job ||
      (status === "PUBLICADA" && job.status !== "RASCUNHO") ||
      (status === "ENCERRADA" && job.status !== "PUBLICADA")
    )
      throw new Error(
        "A situação da vaga mudou. Confira a lista e tente novamente.",
      );
    job.status = status;
  },
  board: (id: string) =>
    cards.filter((c) => c.vagaId === id).map((c) => ({ ...c })),
  mine: () => cards.filter((c) => c.nome === demoName).map((c) => ({ ...c })),
  history: (id: string) => [...(events.get(id) ?? [])],
  isMarina: (card: Card) => card.nome === demoName,
  move(id: string, stage: Stage, observacao?: string) {
    const card = cards.find((c) => c.id === id);
    if (
      !card ||
      !(
        options(card.etapaAtual).includes(stage) ||
        (stage === "DESISTIU" &&
          card.nome === demoName &&
          active.includes(card.etapaAtual))
      )
    )
      throw new Error("Mudança de etapa indisponível.");
    append(card, stage, new Date().toISOString(), observacao);
  },
  apply(job: Job) {
    if (!jobs.some((v) => v.id === job.id && v.status === "PUBLICADA"))
      throw new Error("Esta vaga não está recebendo candidaturas.");
    if (cards.some((c) => c.nome === demoName && c.vagaId === job.id))
      throw new Error("Você já se candidatou a esta vaga.");
    const card: Card = {
      id: crypto.randomUUID(),
      nome: demoName,
      vagaId: job.id,
      titulo: job.titulo,
      etapaAtual: "TRIAGEM",
      criadaEm: new Date().toISOString(),
    };
    cards.push(card);
    append(card, "TRIAGEM", card.criadaEm);
  },
};
