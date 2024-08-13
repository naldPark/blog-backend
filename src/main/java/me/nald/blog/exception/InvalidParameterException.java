package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidParameterException extends ExceptionBase {

  public InvalidParameterException(Logger l) {
    logger = l;
    errorCode = ResponseCode.INVALID_PARAMETER;
  }

  public InvalidParameterException(Logger l, String message) {
    logger = l;
    errorCode = ResponseCode.INVALID_PARAMETER;
    this.additionalMessage = message;
  }

  public InvalidParameterException(Logger l, ResponseCode responseCode) {
    logger = l;
    errorCode = responseCode;
  }

  public InvalidParameterException(Logger l, ResponseCode responseCode, String message) {
    logger = l;
    errorCode = responseCode;
    this.additionalMessage = message;
  }

  @Override
  public int getStatusCode() {
    return HttpStatus.BAD_REQUEST.value();
  }
}
