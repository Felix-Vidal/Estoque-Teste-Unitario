package com.estoque.dominio;

/**
 * Validação de códigos de barras EAN-13 (RN03).
 *
 * Um EAN-13 tem exatamente 13 dígitos. O último é o dígito verificador, calculado assim:
 * os 12 primeiros dígitos são multiplicados alternadamente por 1 e 3 (começando por 1),
 * os resultados são somados, e o verificador é o número que falta para a soma chegar
 * ao próximo múltiplo de 10.
 *
 * Exemplo: 789123456789? -> soma 135 -> verificador 5 -> 7891234567895
 */
public final class Ean13 {

    private Ean13() {
    }

    /** Retorna true se o código tiver 13 dígitos e o dígito verificador estiver correto. */
    public static boolean ehValido(String codigo) {
        if (codigo == null || !codigo.matches("\\d{13}")) {
            return false;
        }
        int verificador = codigo.charAt(12) - '0';
        return calcularDigitoVerificador(codigo.substring(0, 12)) == verificador;
    }

    /**
     * Calcula o dígito verificador a partir dos 12 primeiros dígitos.
     * Útil para gerar códigos válidos nos dados de teste.
     */
    public static int calcularDigitoVerificador(String dozeDigitos) {
        if (dozeDigitos == null || !dozeDigitos.matches("\\d{12}")) {
            throw new IllegalArgumentException("Informe exatamente 12 dígitos numéricos");
        }
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            int digito = dozeDigitos.charAt(i) - '0';
            soma += (i % 2 == 0) ? digito : digito * 3;
        }
        return (10 - soma % 10) % 10;
    }

    /** Gera um EAN-13 válido a partir dos 12 primeiros dígitos. */
    public static String gerar(String dozeDigitos) {
        return dozeDigitos + calcularDigitoVerificador(dozeDigitos);
    }
}
