"""Lê logs da API no stdin e emite alertas agregados em JSON no stdout."""
import json
import re
import sys
import time
from collections import deque

LIMITES = {"401": 10, "403": 10, "429": 5}
EVENTO = re.compile(r"\bcorrelationId=([0-9a-f-]{36}) status=(401|403|429)\b")


def acompanhar(linhas):
    eventos = {codigo: deque(maxlen=limite) for codigo, limite in LIMITES.items()}
    ultimo_alerta = {}
    for linha in linhas:
        evento = EVENTO.search(linha)
        if not evento:
            continue
        identificador, codigo = evento.groups()
        agora = time.monotonic()
        fila = eventos[codigo]
        fila.append(agora)
        if (len(fila) == LIMITES[codigo] and agora - fila[0] <= 60
                and agora - ultimo_alerta.get(codigo, float("-inf")) >= 60):
            ultimo_alerta[codigo] = agora
            print(json.dumps({"evento": "acessos_repetidos", "status": int(codigo),
                              "janelaSegundos": 60, "minimoOcorrencias": len(fila),
                              "correlationId": identificador}), flush=True)


if __name__ == "__main__":
    acompanhar(sys.stdin)
