package br.dev.selecao.comum;

import br.dev.selecao.candidatura.TransicaoInvalida;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.*;
@RestControllerAdvice
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class Erros {
    private static final Logger LOG = LoggerFactory.getLogger(Erros.class);
    @ExceptionHandler(Exception.class)
    public ProblemDetail tratar(Exception ex, HttpServletRequest req) {
        int status = 500;
        String detalhe = "Erro interno";
        if (ex instanceof Falha f) { status = f.status.value(); detalhe = f.getMessage(); }
        else if (ex instanceof TransicaoInvalida) { status = 422; detalhe = ex.getMessage(); }
        else if (duplicado(ex)) { status = 409; detalhe = "Registro já existente"; }
        else if (ex instanceof MethodArgumentTypeMismatchException) {
            status = 400;
            detalhe = "Parâmetro inválido";
        }
        else if (ex instanceof ErrorResponse e) { status = e.getStatusCode().value(); detalhe = "Falha ao processar a requisição"; }
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detalhe);
        if (ex instanceof MethodArgumentTypeMismatchException parametro)
            p.setProperty("erros", List.of(Map.of("campo", parametro.getName(), "mensagem", "Formato ou valor inválido")));
        if (ex instanceof MethodArgumentNotValidException v)
            p.setProperty("erros", v.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("campo",f.getField(),"mensagem",Objects.toString(f.getDefaultMessage(),"inválido"))).toList());
        p.setProperty("correlationId", req.getAttribute("correlationId"));
        LOG.warn("correlationId={} status={} tipo={}", req.getAttribute("correlationId"), status, ex.getClass().getSimpleName());
        return p;
    }
    private boolean duplicado(Throwable t) {
        for (; t != null; t = t.getCause())
            if (t instanceof java.sql.SQLException e && "23505".equals(e.getSQLState())) return true;
        return false;
    }
}
