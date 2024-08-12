package me.nald.blog.exception;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import me.nald.blog.response.ErrorResponse;
import me.nald.blog.util.Constants;
import me.nald.blog.util.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionResolver {
  Logger logger = LogManager.getLogger(this.getClass());

  @ExceptionHandler({
          AuthException.class
  })
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ResponseBody
  public ErrorResponse authExceptionHandler(HttpServletRequest request, Exception exception) {
    ErrorResponse response = null;
    ExceptionBase e;
    if (exception instanceof ExceptionBase) {
      e = (ExceptionBase) exception;
    } else {
      e = new AuthException(logger);
      LogUtils.warnLog(logger, request, exception);
    }
    response = new ErrorResponse(e);
    LogUtils.errorLog(e.logger, request, e);
    request.setAttribute(Constants.EXCEPTION, e); // 발생한 Exception 은 로그 적재 시 필요함.
    return response;
  }

  @ExceptionHandler({
          InvalidParameterException.class,
          MissingServletRequestParameterException.class,
          MissingRequestHeaderException.class,
          HttpMessageNotReadableException.class,
          MethodArgumentNotValidException.class,
          MethodArgumentTypeMismatchException.class,
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public ErrorResponse invalidParameterExceptionHandler(HttpServletRequest request, Exception exception) {
    ErrorResponse response = null;
    ExceptionBase e;
    if (exception instanceof ExceptionBase) {
      e = (ExceptionBase) exception;
    } else if (exception instanceof MethodArgumentNotValidException) {
      MethodArgumentNotValidException ex = (MethodArgumentNotValidException) exception;
      String message = ex.getBindingResult().getFieldErrors().stream()
              .map(f -> String.format("%s %s", f.getDefaultMessage(), f.getField()))
              .collect(Collectors.joining(", "));
      e = new InvalidParameterException(logger, message);

    } else {
      e = new InvalidParameterException(logger, exception.getClass().getSimpleName());
    }
    response = new ErrorResponse(e);
    LogUtils.errorLog(e.logger, request, e);
    request.setAttribute(Constants.EXCEPTION, e); // 발생한 Exception 은 로그 적재 시 필요함. (Convert 된 ExceptionBase 로 전달)
    return response;
  }

  @ExceptionHandler({
          NotFoundException.class,
          NoHandlerFoundException.class,
          HttpMediaTypeNotAcceptableException.class
  })
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public ErrorResponse notFoundExceptionHandler(HttpServletRequest request, Exception exception) {
    ErrorResponse response = null;
    ExceptionBase e;
    if (exception instanceof ExceptionBase) {
      e = (ExceptionBase) exception;
    } else {
      e = new NotFoundException(logger, exception.getClass().getSimpleName());
    }
    response = new ErrorResponse(e);
    LogUtils.errorLog(e.logger, request, e);
    request.setAttribute(Constants.EXCEPTION, e); // 발생한 Exception 은 로그 적재 시 필요함. (Convert 된 ExceptionBase 로 전달)
    return response;
  }

  @ExceptionHandler({
          PermissionDeniedException.class
  })
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ResponseBody
  public ErrorResponse permissionDeniedException(HttpServletRequest request, Exception exception) {
    ErrorResponse response = null;
    ExceptionBase e;
    if (exception instanceof ExceptionBase) {
      e = (ExceptionBase) exception;
    } else {
      e = new PermissionDeniedException(logger, exception.getClass().getSimpleName());
    }
    response = new ErrorResponse(e);
    LogUtils.errorLog(e.logger, request, e);
    request.setAttribute(Constants.EXCEPTION, e); // 발생한 Exception 은 로그 적재 시 필요함. (Convert 된 ExceptionBase 로 전달)
    return response;
  }

  @ExceptionHandler({
          NotAllowedMethodException.class,
          HttpRequestMethodNotSupportedException.class
  })
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  @ResponseBody
  public ErrorResponse notAllowedMethodExceptionHandler(HttpServletRequest request, Exception exception) {
    ErrorResponse response = null;
    ExceptionBase e;
    if (exception instanceof ExceptionBase) {
      e = (ExceptionBase) exception;
    } else {
      e = new NotAllowedMethodException(logger, exception.getClass().getSimpleName());
    }
    response = new ErrorResponse(e);
    LogUtils.errorLog(e.logger, request, e);
    request.setAttribute(Constants.EXCEPTION, e); // 발생한 Exception 은 로그 적재 시 필요함. (Convert 된 ExceptionBase 로 전달)
    return response;
  }


  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public ErrorResponse unknownErrorHandler(HttpServletRequest request, Exception exception) {
    ErrorResponse response = null;
    ExceptionBase e;
    if (exception instanceof ExceptionBase) {
      e = (ExceptionBase) exception;
    } else {
      e = new InternalServerErrorException(logger, exception.getClass().getSimpleName());
    }
    logger.error(exception.getMessage(), exception);
    LogUtils.errorLog(logger, request, e);
    request.setAttribute(Constants.EXCEPTION, e); // 발생한 Exception 은 로그 적재 시 필요함.
    return response;
  }
}
