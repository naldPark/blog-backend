package me.nald.blog.response;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.exception.Errors;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;

@Data
@Slf4j
public class ErrorResponse extends Response {

    private String error;
    private String message;
    private String stackTrace;

    private ErrorResponse() {
        this(HttpStatus.BAD_REQUEST);
    }

    private ErrorResponse(HttpStatus httpStatus) {
        super();
        this.setStatus(Status.FAIL);
        this.setStatusCode(httpStatus.value());
    }

    public ErrorResponse error(String error) {
        setError(error);
        return this;
    }

    public ErrorResponse message(String message) {
        setMessage(message);
        return this;
    }

    public ErrorResponse stackTrace(String stackTrace) {
        setStackTrace(stackTrace);
        return this;
    }

    public ErrorResponse statusCode(HttpStatus httpStatus) {
        return statusCode(httpStatus.value());
    }

    @Override
    public ErrorResponse statusCode(int statusCode) {
        setStatusCode(statusCode);
        return this;
    }

    public static ErrorResponse of (HttpServletRequest request,
                                    Exception e) {

        ErrorResponse errorResponse =  new ErrorResponse()
                .error(e.getClass().getSimpleName())
                .message(e.getMessage())
                .statusCode(isCommonException(e) ?  ((Errors.CommonException) e).getHttpStatus() : HttpStatus.BAD_REQUEST);

        return errorResponse;
    }

    private static boolean isCommonException(Exception e) {
        return e instanceof Errors.CommonException;
    }
}
