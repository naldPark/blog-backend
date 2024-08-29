package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.slf4j.Logger;
@ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
public class MethodNotAllowedException extends ExceptionBase {

  public MethodNotAllowedException(Logger l) {
    logger = l;
    errorCode = ResponseCode.METHOD_NOT_ALLOWED;
  }

  public MethodNotAllowedException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public MethodNotAllowedException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.METHOD_NOT_ALLOWED;
    this.additionalMessage = message;
  }

  @Override
  public int getStatusCode() {
    return HttpStatus.METHOD_NOT_ALLOWED.value();
  }
}
