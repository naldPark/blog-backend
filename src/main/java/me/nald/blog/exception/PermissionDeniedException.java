package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class PermissionDeniedException extends ExceptionBase {

  public PermissionDeniedException(Logger l) {
    logger = l;
    errorCode = ResponseCode.NOT_ALLOWED;
  }

  public PermissionDeniedException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public PermissionDeniedException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.NOT_ALLOWED;
    this.additionalMessage = message;
  }

  public PermissionDeniedException(Logger l, ResponseCode responseCode, String message) {
    logger = l;
    errorCode = responseCode;
    this.additionalMessage = message;
  }

  @Override
  public int getStatusCode() {
    return HttpStatus.FORBIDDEN.value();
  }
}
