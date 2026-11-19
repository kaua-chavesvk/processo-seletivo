"""Executa o fluxo de seleção por HTTP em um projeto Compose descartável."""
import json
import os
import secrets
import subprocess
import sys
import uuid
from http.client import HTTPException
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import ProxyHandler, Request, build_opener


RAIZ = Path(__file__).resolve().parents[1]
PDF = RAIZ / "scripts" / "fixtures" / "curriculo-demo.pdf"


class Api:
    def __init__(self, porta):
        self.base = f"http://127.0.0.1:{porta}"
        self.http = build_opener(ProxyHandler({}))

    def chamar(self, metodo, caminho, *, token=None, dados=None, pdf=None, status=200):
        corpo = None
        headers = {}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        if dados is not None:
            headers["Content-Type"] = "application/json"
            corpo = json.dumps(dados).encode()
        if pdf is not None:
            borda = uuid.uuid4().hex
            headers["Content-Type"] = f"multipart/form-data; boundary={borda}"
            corpo = (f'--{borda}\r\nContent-Disposition: form-data; name="curriculo"; '
                     'filename="curriculo-demo.pdf"\r\nContent-Type: application/pdf\r\n\r\n'
                     ).encode() + pdf + f"\r\n--{borda}--\r\n".encode()
        pedido = Request(self.base + caminho, corpo, headers, method=metodo)
        try:
            resposta = self.http.open(pedido, timeout=30)
        except HTTPError as erro:
            resposta = erro
        with resposta:
            if resposta.status != status:
                identificador = resposta.headers.get("X-Correlation-ID", "indisponível")
                raise RuntimeError(f"{metodo} {caminho}: HTTP {resposta.status}, "
                                   f"esperado {status} (correlationId={identificador})")
            conteudo = resposta.read()
            if resposta.headers.get_content_type() == "application/pdf":
                return conteudo
            return json.loads(conteudo) if conteudo else None

    def login(self, email, senha):
        sessao = self.chamar("POST", "/auth/login", dados={"email": email, "senha": senha})
        return sessao["accessToken"]


def demonstrar(api, email_admin, senha_admin):
    saude = api.chamar("GET", "/actuator/health")
    if saude["status"] != "UP":
        raise RuntimeError("A API ainda não está saudável")
    admin = api.login(email_admin, senha_admin)
    empresa = api.chamar("POST", "/empresas", token=admin, status=201,
                         dados={"nome": "Empresa de demonstração", "cnpj": "12.ABC.345/01DE-35"})
    recrutador = {"nome": "Recrutador fictício", "email": "recrutador@example.invalid",
                  "senha": secrets.token_urlsafe(32)}
    api.chamar("POST", "/usuarios", token=admin, status=201,
               dados={"papel": "RECRUTADOR", "empresaId": empresa["id"], "dados": recrutador})
    recrutador_token = api.login(recrutador["email"], recrutador["senha"])

    vaga = api.chamar("POST", f"/empresas/{empresa['id']}/vagas", token=recrutador_token,
                      status=201, dados={"titulo": "Desenvolvedor Java Júnior", "descricao": "Vaga fictícia",
                                        "modalidade": "REMOTO", "nivel": "JUNIOR", "localizacao": "Brasil",
                                        "tecnologias": ["Java", "PostgreSQL"]})
    caminho_vaga = f"/vagas/{vaga['id']}"
    api.chamar("PATCH", caminho_vaga + "/status", token=recrutador_token, dados={"status": "PUBLICADA"})
    if api.chamar("GET", caminho_vaga)["status"] != "PUBLICADA":
        raise RuntimeError("A vaga publicada não apareceu na consulta pública")
    print("Vaga publicada e consultada sem autenticação.", flush=True)

    email = "candidato@example.invalid"
    senha = secrets.token_urlsafe(32)
    candidato = api.chamar("POST", "/auth/cadastro", status=201,
                           dados={"nome": "Candidato fictício", "email": email, "senha": senha})
    token = api.login(email, senha)
    arquivo = PDF.read_bytes()
    inscricoes = caminho_vaga + "/candidaturas"
    candidatura = api.chamar("POST", inscricoes, token=token, pdf=arquivo, status=201)
    api.chamar("POST", inscricoes, token=token, pdf=arquivo, status=409)
    caminho = f"/candidaturas/{candidatura['id']}"
    recebido = api.chamar("GET", caminho + "/curriculo", token=recrutador_token)
    if recebido != arquivo:
        raise RuntimeError("O PDF baixado difere do enviado")
    print("Candidatura criada; duplicata recusada com HTTP 409; PDF conferido.", flush=True)

    api.chamar("PATCH", caminho + "/etapa", token=recrutador_token, dados={"novaEtapa": "ENTREVISTA"})
    historico = api.chamar("GET", caminho + "/historico", token=token)
    etapas = [item["etapaNova"] for item in historico]
    if etapas != ["TRIAGEM", "ENTREVISTA"] or historico[0]["alteradoPor"] != candidato["id"]:
        raise RuntimeError("O histórico recebido diverge das etapas executadas")
    print("Histórico consultado pelo candidato: " + " -> ".join(etapas), flush=True)


def executar():
    projeto = "selecao-demo-" + uuid.uuid4().hex
    email = "admin@example.invalid"
    senha = secrets.token_urlsafe(32)
    ambiente = {**os.environ, "API_PORT": "0", "ADMIN_EMAIL": email, "ADMIN_PASSWORD": senha,
                "POSTGRES_PASSWORD": secrets.token_urlsafe(32), "JWT_SECRET": ""}
    compose = ["docker", "compose", "--project-name", projeto, "--file", str(RAIZ / "compose.yaml")]
    def docker(*argumentos, capturar=False):
        return subprocess.run(compose + list(argumentos), check=True, cwd=RAIZ, env=ambiente,
                              stdout=subprocess.PIPE if capturar else None, text=True)

    print(f"Iniciando o projeto temporário {projeto}...", flush=True)
    try:
        docker("up", "--build", "--detach", "--wait", "--wait-timeout", "240")
        endereco = docker("port", "api", "8080", capturar=True).stdout.strip()
        host, porta = endereco.rsplit(":", 1)
        if host != "127.0.0.1":
            raise RuntimeError(f"Endereço local inesperado: {endereco}")
        demonstrar(Api(int(porta)), email, senha)
    finally:
        docker("down", "--volumes", "--rmi", "local")
    print("Demonstração concluída e ambiente temporário removido.", flush=True)


if __name__ == "__main__":
    try:
        executar()
    except (OSError, HTTPException, URLError, ValueError, RuntimeError, subprocess.CalledProcessError) as exc:
        sys.exit(f"Falha na demonstração: {exc}")
