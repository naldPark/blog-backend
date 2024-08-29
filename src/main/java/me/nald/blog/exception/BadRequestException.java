package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends ExceptionBase {

  public BadRequestException(Logger l) {
    logger = l;
    errorCode = ResponseCode.INVALID_PARAMETER;
  }

  public BadRequestException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.INVALID_PARAMETER;
    this.additionalMessage = message;
  }

  public BadRequestException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public BadRequestException(Logger l, ResponseCode responseCode, String message) {
    logger = l;
    errorCode = responseCode;
    this.additionalMessage = message;
  }

  @Override
  public int getStatusCode() {
    return HttpStatus.BAD_REQUEST.value();
  }
}
