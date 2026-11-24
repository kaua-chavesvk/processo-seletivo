import type { Card, Event, Job, Stage } from "./model";

export interface RecruitmentApi {
  jobs(company: boolean): Promise<Job[]>;
  publicJob(id: string): Promise<Job | null>;
  shareable(id: string): boolean;
  board(jobId: string): Promise<Card[]>;
  mine(jobId?: string): Promise<Card[]>;
  history(id: string): Promise<Event[]>;
  move(id: string, stage: Stage, note?: string): Promise<void>;
  apply(job: Job, file?: File): Promise<void>;
  saveDraft(
    data: Omit<Job, "id" | "status">,
    id?: string,
    companyId?: string | null,
  ): Promise<void>;
  setJobStatus(id: string, status: "PUBLICADA" | "ENCERRADA"): Promise<void>;
}
