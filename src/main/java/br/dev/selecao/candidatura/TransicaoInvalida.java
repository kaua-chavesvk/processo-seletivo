package br.dev.selecao.candidatura;

public class TransicaoInvalida extends RuntimeException {
    public TransicaoInvalida() { super("Transição de etapa não permitida"); }
}
