package com.estoque.excecao;

/**
 * Lançada quando uma regra de negócio é violada.
 * O código (ex.: "RN13") identifica a regra, o que facilita verificar nos testes
 * qual regra exatamente barrou a operação.
 */
public class RegraNegocioException extends RuntimeException {

    private final String codigoRegra;

    public RegraNegocioException(String codigoRegra, String mensagem) {
        super(mensagem);
        this.codigoRegra = codigoRegra;
    }

    public String getCodigoRegra() {
        return codigoRegra;
    }
}
