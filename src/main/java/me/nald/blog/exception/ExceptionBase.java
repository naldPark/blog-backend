package me.nald.blog.exception;

import me.nald.blog.response.ResponseCode;
import org.slf4j.Logger;

public abstract class ExceptionBase extends RuntimeException {
  public abstract int getStatusCode();
  protected ResponseCode errorCode;
  protected Logger logger;
  protected String additionalMessage = null;

  public int getErrorCode() {
    return errorCode.getCode();
  }

  public String getAdditionalMessage() {
    return additionalMessage;
  }
  public String getErrorMessage() {
    return errorCode.toString();
  }
  public String getI18nCode() {
    return errorCode.getI18nCode();
  }

  public String toString() {
    return String.format("[%d], ERR_CODE=%d(%s) : %s", getStatusCode(), errorCode.getCode(), errorCode.toString(), additionalMessage);
  }
}
