package me.nald.blog.response;

import me.nald.blog.exception.ExceptionBase;
import me.nald.blog.util.Constants;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

import static me.nald.blog.util.Constants.STATUS_CODE;


public class ErrorResponse extends HashMap<String, Object> {
    public ErrorResponse(ExceptionBase exception) {
        super();
        this.put(Constants.KEY_SUCCESS, false);
        this.put(STATUS_CODE, exception.getStatusCode());
        this.put("error_code", exception.getErrorCode());
        this.put("error_message", exception.getErrorMessage());
        this.put("error_i18n", exception.getI18nCode());
        String message = exception.getAdditionalMessage();
        if (message!=null && !message.isEmpty()) {
            this.put("message", message);
        }
    }

    public ErrorResponse() {
        super();
        this.put(Constants.KEY_SUCCESS, false);
        this.put(STATUS_CODE, HttpStatus.SERVICE_UNAVAILABLE.value());
        this.put("error_code", ResponseCode.UNKNOWN_ERROR.getCode());
        this.put("error_i18n", ResponseCode.UNKNOWN_ERROR.getMessage());
        this.put("code", ResponseCode.UNKNOWN_ERROR.getI18nCode());
    }
}