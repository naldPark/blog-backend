package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.slf4j.Logger;
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends ExceptionBase {
  
  public InternalServerErrorException(Logger l) {
    logger = l;
    errorCode = ResponseCode.UNKNOWN_ERROR;
  }

  public InternalServerErrorException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public InternalServerErrorException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.UNKNOWN_ERROR;
    this.additionalMessage = message;
  }

  @Override
  public int getStatusCode() {
    return HttpStatus.INTERNAL_SERVER_ERROR.value();
  }
}
