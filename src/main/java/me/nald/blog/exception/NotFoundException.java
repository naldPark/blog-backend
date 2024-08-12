package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NotFoundException extends ExceptionBase {

  public NotFoundException(Logger l) {
    logger = l;
    errorCode = ResponseCode.NOT_FOUND;
  }

  public NotFoundException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public NotFoundException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.NOT_FOUND;
    this.additionalMessage = message;
  }

  public NotFoundException(Logger l, ResponseCode responseCode, String message) {
    logger = l;
    errorCode = responseCode;
    this.additionalMessage = message;
  }

  @Override
  public int getStatusCode() {
    return HttpStatus.NOT_FOUND.value();
  }
}
