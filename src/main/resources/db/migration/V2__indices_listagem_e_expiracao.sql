create index vaga_publicada_criada on vaga(status, criada_em desc, id);
create index refresh_expira on refresh_token(expira_em);
