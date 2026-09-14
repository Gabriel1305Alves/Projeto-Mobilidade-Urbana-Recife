package br.com.embarcai.config;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}
