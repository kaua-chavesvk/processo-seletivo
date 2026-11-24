export const labels = {
  TRIAGEM: "Triagem",
  ENTREVISTA: "Entrevista",
  TESTE_TECNICO: "Teste técnico",
  PROPOSTA: "Proposta",
  CONTRATADO: "Contratado",
  REPROVADO: "Reprovado",
  DESISTIU: "Desistiu",
} as const;
export type Stage = keyof typeof labels;
export const active: Stage[] = [
  "TRIAGEM",
  "ENTREVISTA",
  "TESTE_TECNICO",
  "PROPOSTA",
];
export const closed: Stage[] = ["CONTRATADO", "REPROVADO", "DESISTIU"];
export function options(
  stage: Stage,
  role: Profile["papel"] = "RECRUTADOR",
): Stage[] {
  const index = active.indexOf(stage);
  if (index < 0 || role === "ADMIN") return [];
  if (role === "CANDIDATO") return ["DESISTIU"];
  return [active[index + 1] ?? "CONTRATADO", "REPROVADO"];
}
export type Job = {
  id: string;
  titulo: string;
  descricao: string;
  modalidade: string;
  localizacao: string;
  nivel: string;
  tecnologias: string[];
  status: string;
};
export function nextJobStatus(job: Job) {
  if (job.status === "RASCUNHO") return "PUBLICADA";
  if (job.status === "PUBLICADA") return "ENCERRADA";
  return null;
}
export type Card = {
  id: string;
  vagaId: string;
  nome: string;
  titulo: string;
  etapaAtual: Stage;
  criadaEm: string;
};
export type Event = {
  id: string;
  etapaNova: Stage;
  alteradoEm: string;
  observacao?: string;
};
export type Profile = {
  empresaId?: string | null;
  id: string;
  nome: string;
  papel: "RECRUTADOR" | "CANDIDATO" | "ADMIN";
};
export const initials = (name: string) => {
  const words = name.trim().split(/\s+/);
  if (!words[0]) return "";
  return (
    words[0][0] + (words.length > 1 ? words.at(-1)![0] : "")
  ).toUpperCase();
};
export const date = (value: string) =>
  new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" })
    .format(new Date(value))
    .replace(" de ", " ");
export const pretty = (value: string) =>
  ({
    REMOTO: "Remoto",
    HIBRIDO: "Híbrido",
    PRESENCIAL: "Presencial",
    PLENO: "Pleno",
    SENIOR: "Sênior",
    JUNIOR: "Júnior",
    ESTAGIO: "Estágio",
    PUBLICADA: "Publicada",
    ENCERRADA: "Encerrada",
    RASCUNHO: "Rascunho",
  })[value] ?? value;
