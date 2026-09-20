package br.com.embarcai.config;

import br.com.embarcai.dto.ErroResposta;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResposta> tratar(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErroResposta(ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErroResposta("Preenche código, nome, origem e destino."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarDuplicata(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErroResposta("Já existe uma linha com esse código."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarGeral(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErroResposta("Erro interno do servidor."));
    }
}
