package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends ExceptionBase {
  //401

  public UnauthorizedException(Logger l) {
    logger = l;
    errorCode = ResponseCode.UNKNOWN_AUTH_ERROR;
  }

  public UnauthorizedException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public UnauthorizedException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.UNKNOWN_AUTH_ERROR;
    this.additionalMessage = message;
  }

  public UnauthorizedException(Logger l, ResponseCode responseCode, String message) {
    logger = l;
    errorCode = responseCode;
    this.additionalMessage = message;
  }


  @Override
  public int getStatusCode() {
    return HttpStatus.UNAUTHORIZED.value();
  }
}
