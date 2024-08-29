package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ForbiddenException extends ExceptionBase {
  //403
  public ForbiddenException(Logger l) {
    logger = l;
    errorCode = ResponseCode.NOT_ALLOWED;
  }

  public ForbiddenException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public ForbiddenException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.NOT_ALLOWED;
    this.additionalMessage = message;
  }

  public ForbiddenException(Logger l, ResponseCode responseCode, String message) {
    logger = l;
    errorCode = responseCode;
    this.additionalMessage = message;
  }

  @Override
  public int getStatusCode() {
    return HttpStatus.FORBIDDEN.value();
  }
}
