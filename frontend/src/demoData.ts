import type { Job, Stage } from "./model";

export const demoCandidate = "Marina Costa";
export const exampleJobs: Job[] = [
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
export const roster: [name: string, stage: Stage, jobIndex: number][] = [
  [demoCandidate, "TRIAGEM", 0],
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
