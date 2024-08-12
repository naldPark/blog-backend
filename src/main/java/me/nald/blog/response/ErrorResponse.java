package me.nald.blog.response;

import me.nald.blog.exception.ExceptionBase;
import org.springframework.http.HttpStatus;

import java.util.HashMap;


public class ErrorResponse extends HashMap<String, Object> {
    public ErrorResponse(ExceptionBase exception) {
        super();
        this.put("error", true);
        this.put("http_status_code", exception.getStatusCode());
        this.put("error_code", exception.getErrorCode());
        this.put("error_message", exception.getErrorMessage());
        this.put("code", exception.getI18nCode());
        String message = exception.getAdditionalMessage();
        if (message!=null && !message.isEmpty()) {
            this.put("message", message);
        }
    }

    public ErrorResponse() {
        super();
        this.put("error", true);
        this.put("http_status_code", HttpStatus.SERVICE_UNAVAILABLE.value());
        this.put("error_code", ResponseCode.UNKNOWN_ERROR.getCode());
        this.put("error_message", ResponseCode.UNKNOWN_ERROR.getMessage());
        this.put("code", ResponseCode.UNKNOWN_ERROR.getI18nCode());
    }
}