"""Consulta o OSV com coordenadas públicas da árvore Maven de runtime."""
import argparse
from datetime import date, datetime, timezone
import json
import re
import sys
from pathlib import Path
from urllib.request import Request, urlopen


EXCECOES = Path(__file__).with_name("excecoes-auditoria.json")


def carregar_excecoes(arquivo, hoje):
    permitidas = set()
    for entrada in json.loads(arquivo.read_text(encoding="utf-8")):
        for campo in ("id", "coordenada", "motivo", "responsavel", "expira_em"):
            if not isinstance(entrada.get(campo), str) or not entrada[campo].strip():
                raise ValueError(f"Campo obrigatório na exceção de auditoria: {campo}")
        chave = (entrada["coordenada"], entrada["id"])
        if hoje >= date.fromisoformat(entrada["expira_em"]):
            raise ValueError(f"Exceção vencida: {chave[1]} ({chave[0]}) em {entrada['expira_em']}")
        if chave in permitidas:
            raise ValueError(f"Exceção duplicada: {chave}")
        permitidas.add(chave)
    return permitidas


def main(arquivo, ci=False):
    excecoes = carregar_excecoes(EXCECOES, datetime.now(timezone.utc).date())
    conhecidos = excecoes if ci else set()
    coordenada = re.compile(r"([\w.-]+):([\w.-]+):jar:(?:[\w.-]+:)?([\w.-]+):(compile|runtime)$")
    consultas = []
    for linha in Path(arquivo).read_text(encoding="utf-8").splitlines():
        item = coordenada.search(linha.strip())
        if item:
            grupo, nome, versao, _ = item.groups()
            consultas.append({"package": {"ecosystem": "Maven", "name": f"{grupo}:{nome}"}, "version": versao})
    if not consultas:
        raise ValueError("Árvore de dependências vazia")
    requisicao = Request("https://api.osv.dev/v1/querybatch",
                         data=json.dumps({"queries": consultas}).encode(),
                         headers={"Content-Type": "application/json"})
    with urlopen(requisicao, timeout=30) as resposta:
        resultados = json.load(resposta)["results"]
    novos = 0
    encontrados = 0
    for consulta, resultado in zip(consultas, resultados, strict=True):
        avisos = resultado.get("vulns", [])
        if avisos:
            encontrados += len(avisos)
            novos += sum((f'{consulta["package"]["name"]}:{consulta["version"]}', a["id"]) not in conhecidos for a in avisos)
            print(consulta["package"]["name"], consulta["version"], ", ".join(a["id"] for a in avisos))
    print(f"{len(consultas)} coordenadas consultadas; {encontrados} ocorrências.")
    return int(novos > 0)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("arvore")
    parser.add_argument("--ci", action="store_true", help="Aplicar exceções vigentes à decisão de falha")
    args = parser.parse_args()
    try:
        sys.exit(main(args.arvore, ci=args.ci))
    except (OSError, ValueError, KeyError) as erro:
        sys.exit(f"Auditoria não concluída: {erro}")
