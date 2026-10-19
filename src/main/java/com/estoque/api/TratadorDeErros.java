package com.estoque.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.estoque.api.dto.Respostas.ErroResponse;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.excecao.RegraNegocioException;

/**
 * Converte exceções em respostas HTTP:
 * <ul>
 *   <li>422 Unprocessable Entity: regra de negócio violada (código = número da regra)</li>
 *   <li>404 Not Found: categoria ou produto inexistente</li>
 *   <li>400 Bad Request: JSON malformado ou parâmetro com tipo errado</li>
 *   <li>409 Conflict: violação de restrição do banco (ex.: dois cadastros simultâneos do mesmo EAN)</li>
 * </ul>
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse regraNegocio(RegraNegocioException e) {
        return new ErroResponse(e.getCodigoRegra(), e.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse naoEncontrado(RecursoNaoEncontradoException e) {
        return new ErroResponse("NAO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse requisicaoInvalida(Exception e) {
        return new ErroResponse("REQUISICAO_INVALIDA", "O corpo ou os parâmetros da requisição são inválidos");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflito(DataIntegrityViolationException e) {
        return new ErroResponse("CONFLITO", "A operação conflita com dados já existentes");
    }
}
