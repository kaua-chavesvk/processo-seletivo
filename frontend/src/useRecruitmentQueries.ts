import { useQuery } from "@tanstack/react-query";
import { read } from "./api";
import { demo, recruitment } from "./recruitment";
import type { Profile } from "./model";
import type { View } from "./Workspace";

export function useRecruitmentQueries({
  view,
  allowed,
  profile,
  chosen,
  jobRouteId,
  companyJobs,
  candidate,
  recruiter,
}: {
  view: View;
  allowed: boolean;
  profile: Profile | null;
  chosen: string;
  jobRouteId: string;
  companyJobs: boolean;
  candidate: boolean;
  recruiter: boolean;
}) {
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
    queryFn: () => recruitment.jobs(companyJobs),
  });
  const publicJob = useQuery({
    queryKey: ["public-job", jobRouteId],
    enabled: !!jobRouteId,
    queryFn: () => recruitment.publicJob(jobRouteId),
  });
  const jobId = jobs.data?.some((j) => j.id === chosen)
    ? chosen
    : (jobs.data?.[0]?.id ?? "");
  const board = useQuery({
    queryKey: ["board", jobId, profile?.id],
    enabled: recruiter && view === "funil" && !!jobId,
    queryFn: () => recruitment.board(jobId),
  });
  const mine = useQuery({
    queryKey: ["mine", profile?.id, jobRouteId],
    enabled: candidate && (view === "minhas" || !!jobRouteId),
    refetchOnWindowFocus: true,
    queryFn: () => recruitment.mine(jobRouteId),
  });
  return { recursos, jobs, publicJob, jobId, board, mine };
}
