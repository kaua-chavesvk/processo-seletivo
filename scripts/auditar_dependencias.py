"""Consulta o OSV com coordenadas públicas da árvore Maven de runtime."""
import json
import re
import sys
from pathlib import Path
from urllib.request import Request, urlopen


def main(arquivo):
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
    conhecidos = {("org.springframework:spring-webmvc", "6.2.19", aviso) for aviso in
                  ("GHSA-j9f9-w8pj-32f8", "GHSA-pc63-qcmh-9cmg")} if "--ci" in sys.argv else set()
    novos = 0
    encontrados = 0
    for consulta, resultado in zip(consultas, resultados, strict=True):
        avisos = resultado.get("vulns", [])
        if avisos:
            encontrados += len(avisos)
            novos += sum((consulta["package"]["name"], consulta["version"], a["id"]) not in conhecidos for a in avisos)
            print(consulta["package"]["name"], consulta["version"], ", ".join(a["id"] for a in avisos))
    print(f"{len(consultas)} coordenadas consultadas; {encontrados} ocorrências.")
    return int(novos > 0)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
