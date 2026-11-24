import { isJobId, json, list, readPublicJob, request } from "./api";
import { createDemoApi } from "./demo";
import type { RecruitmentApi } from "./RecruitmentApi";

const httpApi: RecruitmentApi = {
  jobs: (company) => list(company ? "/painel/vagas" : "/vagas"),
  publicJob: readPublicJob,
  shareable: isJobId,
  board: (id) => list("/painel/vagas/" + id + "/candidaturas"),
  async mine(jobId) {
    if (jobId && !isJobId(jobId)) return [];
    return list("/painel/minhas", jobId ? { vagaId: jobId } : undefined);
  },
  history: (id) => list("/candidaturas/" + id + "/historico"),
  async move(id, stage, note) {
    if (stage === "DESISTIU")
      await request("/candidaturas/" + id + "/desistencia", {
        method: "PATCH",
      });
    else
      await request(
        "/candidaturas/" + id + "/etapa",
        json("PATCH", { novaEtapa: stage, observacao: note }),
      );
  },
  async apply(job, file) {
    if (!file) throw new Error("Selecione um currículo em PDF.");
    const data = new FormData();
    data.append("curriculo", file);
    await request("/vagas/" + job.id + "/candidaturas", {
      method: "POST",
      body: data,
    });
  },
  async saveDraft(data, id, companyId) {
    if (id) await request("/vagas/" + id, json("PUT", data));
    else {
      if (!companyId)
        throw new Error("O perfil não está vinculado a uma empresa.");
      await request("/empresas/" + companyId + "/vagas", json("POST", data));
    }
  },
  async setJobStatus(id, status) {
    await request("/vagas/" + id + "/status", json("PATCH", { status }));
  },
};

export const demo = import.meta.env.MODE === "demonstracao";
export const recruitment = demo ? createDemoApi() : httpApi;
