create table usuario (
    id uuid primary key,
    nome varchar(255) not null,
    email varchar(255) not null,
    senha_hash varchar(255) not null,
    papel varchar(32) not null check (papel in ('ADMIN', 'RECRUTADOR', 'CANDIDATO')),
    ativo boolean not null,
    criado_em timestamptz not null
);

create table empresa (
    id uuid primary key,
    nome varchar(255) not null,
    cnpj varchar(255) not null unique,
    site varchar(255),
    criada_em timestamptz not null
);

create table vaga (
    id uuid primary key,
    empresa_id uuid not null references empresa(id),
    titulo varchar(255) not null,
    descricao text not null,
    modalidade varchar(32) not null check (modalidade in ('PRESENCIAL', 'HIBRIDO', 'REMOTO')),
    nivel varchar(32) not null check (nivel in ('ESTAGIO', 'JUNIOR', 'PLENO', 'SENIOR')),
    localizacao varchar(255) not null,
    status varchar(32) not null check (status in ('RASCUNHO', 'PUBLICADA', 'ENCERRADA')),
    criada_em timestamptz not null,
    atualizada_em timestamptz not null
);

create table candidatura (
    id uuid primary key,
    vaga_id uuid not null references vaga(id),
    candidato_id uuid not null references usuario(id),
    curriculo_caminho varchar(255) not null,
    etapa_atual varchar(32) not null check (etapa_atual in ('TRIAGEM', 'ENTREVISTA', 'TESTE_TECNICO', 'PROPOSTA', 'CONTRATADO', 'REPROVADO', 'DESISTIU')),
    criada_em timestamptz not null,
    atualizada_em timestamptz not null,
    unique (vaga_id, candidato_id)
);

create table historico_etapa (
    id uuid primary key,
    candidatura_id uuid not null references candidatura(id),
    etapa_anterior varchar(32) check (etapa_anterior in ('TRIAGEM', 'ENTREVISTA', 'TESTE_TECNICO', 'PROPOSTA', 'CONTRATADO', 'REPROVADO', 'DESISTIU')),
    etapa_nova varchar(32) not null check (etapa_nova in ('TRIAGEM', 'ENTREVISTA', 'TESTE_TECNICO', 'PROPOSTA', 'CONTRATADO', 'REPROVADO', 'DESISTIU')),
    alterado_por uuid not null references usuario(id),
    observacao text,
    alterado_em timestamptz not null
);

create table refresh_token (
    id uuid primary key,
    usuario_id uuid not null references usuario(id),
    hash_token varchar(255) not null unique,
    expira_em timestamptz not null,
    revogado boolean not null
);
create unique index usuario_email_ci on usuario(lower(email));
create table recrutador_empresa (
    usuario_id uuid references usuario(id),
    empresa_id uuid references empresa(id),
    primary key (usuario_id, empresa_id),
    unique (usuario_id)
);
create table vaga_tecnologia (
    vaga_id uuid references vaga(id),
    tecnologia varchar(255),
    primary key (vaga_id, tecnologia)
);
create index vaga_tec on vaga_tecnologia(tecnologia);
create index vaga_empresa_status on vaga(empresa_id,status);
create index vaga_modalidade_nivel on vaga(modalidade,nivel);
create index candidatura_vaga_etapa on candidatura(vaga_id,etapa_atual);
create index historico_ordem on historico_etapa(candidatura_id,alterado_em);
create index candidatura_candidato on candidatura(candidato_id);
create index refresh_usuario on refresh_token(usuario_id);
