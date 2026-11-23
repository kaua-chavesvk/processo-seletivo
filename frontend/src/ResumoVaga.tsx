import { MapPin } from "lucide-react";
import { pretty, type Job } from "./model";

export function ResumoVaga({ job }: { job: Job }) {
  return (
    <>
      <div className="meta">
        <MapPin size={15} />
        {job.localizacao} · {pretty(job.modalidade)} · {pretty(job.nivel)}
      </div>
      <p>{job.descricao}</p>
      <div className="tags">
        {job.tecnologias.map((technology) => (
          <span key={technology}>{technology}</span>
        ))}
      </div>
    </>
  );
}
