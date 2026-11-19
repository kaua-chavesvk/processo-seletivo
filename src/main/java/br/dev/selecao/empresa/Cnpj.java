package br.dev.selecao.empresa;

import java.util.Locale;
public final class Cnpj {
    private Cnpj() {}
    static String normalizar(String valor) { return valor.replaceAll("[./-]","").toUpperCase(Locale.ROOT); }
    public static boolean valido(String valor) {
        if (valor == null || !valor.matches("(?i)([a-z0-9]{12}[0-9]{2}|[a-z0-9]{2}\\.[a-z0-9]{3}\\.[a-z0-9]{3}/[a-z0-9]{4}-[0-9]{2})")) return false;
        String c=normalizar(valor);
        if (c.chars().distinct().count()==1) return false;
        for (int n=12;n<=13;n++) {
            int soma=0;
            for (int i=0;i<n;i++) soma+=(c.charAt(i)-'0')*(2+(n-1-i)%8);
            int resto=soma%11;
            if (c.charAt(n)-'0' != (resto<2 ? 0 : 11-resto)) return false;
        }
        return true;
    }
}
