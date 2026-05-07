package de.mabis.hub.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validierungsfehler(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validierungsfehler");
        problem.setDetail(detail);
        problem.setType(URI.create("https://mabis-hub.de/errors/validation"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail ungueltigeAnfrage(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Ungültige Anfrage");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://mabis-hub.de/errors/bad-request"));
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail geschaeftsregelVerletzt(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Geschäftsregel verletzt");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://mabis-hub.de/errors/business-rule"));
        return problem;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail nichtGefunden(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().startsWith("Abrechnung nicht gefunden")) {
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            problem.setTitle("Nicht gefunden");
            problem.setDetail(ex.getMessage());
            return problem;
        }
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Interner Fehler");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
