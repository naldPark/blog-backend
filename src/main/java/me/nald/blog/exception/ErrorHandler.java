package me.nald.blog.exception;


import io.sentry.Sentry;
import me.nald.blog.response.ErrorResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleAllExceptions(HttpServletRequest request,
                                             Exception exception) {
        Sentry.captureException(exception);
        return ErrorResponse.of(request, exception);
    }

    @ExceptionHandler(BindException.class)
    public String processValidationError(HttpServletRequest request, BindException exception) {
        Sentry.captureException(exception);
        BindingResult bindingResult = exception.getBindingResult();

        StringBuilder builder = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            builder.append("[");
            builder.append(fieldError.getField());
            builder.append("] is ");
            builder.append(fieldError.getDefaultMessage());
            builder.append(" requested value: [");
            builder.append(fieldError.getRejectedValue());
            builder.append("]");
        }

        return builder.toString();
    }
}
