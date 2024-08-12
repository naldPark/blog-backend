package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
public class NotAllowedMethodException extends ExceptionBase {

  public NotAllowedMethodException(Logger l) {
    logger = l;
    errorCode = ResponseCode.METHOD_NOT_ALLOWED;
  }

  public NotAllowedMethodException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public NotAllowedMethodException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.METHOD_NOT_ALLOWED;
    this.additionalMessage = message;
  }

  @Override
  public int getStatusCode() {
    return HttpStatus.METHOD_NOT_ALLOWED.value();
  }
}
